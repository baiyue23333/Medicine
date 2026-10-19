package com.baiyue.medicine.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baiyue.medicine.MedicineApp
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag
import com.baiyue.medicine.prediction.DOSE_WINDOW_MINUTES
import com.baiyue.medicine.prediction.doseTimeSlots
import com.baiyue.medicine.prediction.medicationExistsOn
import com.baiyue.medicine.prediction.predict
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

/** 待打卡 / 可补签的时段；[date] 为该时段所属日期（可能是昨天） */
data class DueItem(val medication: Medication, val time: String, val date: String)

/** 下一次（未来）用药：同一时间点的所有药品；[missed] 为 true 表示这是今天已错过、待补签的时段 */
data class NextDose(val time: String, val tomorrow: Boolean, val items: List<Medication>, val missed: Boolean = false)

/** 两次服用过近的提醒（打卡页小条提示） */
data class CloseDoseWarning(val medName: String, val minutes: Int)

private data class NextDoseCand(val med: Medication, val time: String, val minute: Int, val tomorrow: Boolean)

/** 最近两次服用间隔小于该分钟数时，判定为「两次服用过近」 */
private const val MIN_GAP_MINUTES = 60

/** 打卡窗口内的待服药时段 / 已错过可补签的时段 */
private data class DoseBuckets(val due: List<DueItem>, val missed: List<DueItem>) {
    companion object {
        val EMPTY = DoseBuckets(emptyList(), emptyList())
    }
}

private const val DAY_MINUTES = 24 * 60

class CheckInViewModel(app: Application) : AndroidViewModel(app) {
    private val medicineApp = app as MedicineApp
    private val repo = medicineApp.repository

    /** 是否显示药品图片（全局，持久化） */
    val showImage: StateFlow<Boolean> = medicineApp.settingsRepository.settings
        .map { it.showImage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** 已提交/处理中的打卡键，防止双击或并发重复写入、重复扣库存 */
    private val processedKeys = ConcurrentHashMap.newKeySet<String>()

    /** 当前日期；跨过零点会自动重新发射，避免长期驻留后仍按昨天计算 */
    private val dateFlow: Flow<String> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now.toLocalDate().toString())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(now, nextMidnight).toMillis() + 1_000L)
        }
    }.distinctUntilChanged()

    /**
     * 把「今天与昨天」尚未打卡的时段分为「打卡窗口内」与「已错过（可补签）」。
     * 覆盖昨天是为了跨零点后仍能补签昨晚漏掉的药；更早的日期不再提示。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val doseBuckets: StateFlow<DoseBuckets> = dateFlow.flatMapLatest { today ->
        val yesterday = LocalDate.parse(today).minusDays(1).toString()
        combine(
            repo.observeRegular(),
            repo.observeRecordsByPlannedDate(today),
            repo.observeRecordsByPlannedDate(yesterday)
        ) { meds, todayRecords, yesterdayRecords ->
            val now = LocalTime.now()
            val nowMin = now.hour * 60 + now.minute
            val due = mutableListOf<DueItem>()
            val missed = mutableListOf<DueItem>()

            // dayOffset 以「今天零点」为原点，昨天记为负偏移，跨零点时段也能正确归类
            fun collect(date: String, records: List<DoseRecord>, dayOffset: Int) {
                val slotDate = LocalDate.parse(date)
                // 药品在当天还不存在时不追溯，避免新添加的药品把昨天的时段算成漏服
                meds.filter { medicationExistsOn(it, slotDate) }.forEach { med ->
                    doseTimeSlots(med).forEach { slot ->
                        val taken = records.any {
                            // 该时段已有记录（常规打卡或已跳过）即视为已处理
                            it.medicationId == med.id &&
                                it.scheduledTime == slot.timeString()
                        }
                        if (taken) return@forEach
                        val diff = nowMin - (slot.hour * 60 + slot.minute + dayOffset)
                        when {
                            // 还没进入打卡窗口
                            diff < -DOSE_WINDOW_MINUTES -> Unit
                            // 窗口内（含即将到来的一小时内）
                            diff <= DOSE_WINDOW_MINUTES -> due += DueItem(med, slot.timeString(), date)
                            // 已超出打卡窗口：需要补签
                            else -> missed += DueItem(med, slot.timeString(), date)
                        }
                    }
                }
            }
            collect(today, todayRecords, 0)
            collect(yesterday, yesterdayRecords, -DAY_MINUTES)

            val byChronology = compareBy<DueItem>({ it.date }, { it.time })
            DoseBuckets(due.sortedWith(byChronology), missed.sortedWith(byChronology))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DoseBuckets.EMPTY)

    val dueItems: StateFlow<List<DueItem>> = doseBuckets
        .map { it.due }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 已错过服药时段、可补签的记录 */
    val missedItems: StateFlow<List<DueItem>> = doseBuckets
        .map { it.missed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 下一次用药（当前无该吃的药时用于提示）：同一时间点的所有药品 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val nextDose: StateFlow<NextDose?> = dateFlow.flatMapLatest { date ->
        combine(
            repo.observeRegular(),
            repo.observeRecordsByPlannedDate(date)
        ) { meds, records ->
            val now = LocalTime.now()
            val nowMin = now.hour * 60 + now.minute

            fun isTaken(med: Medication, time: String): Boolean =
                records.any {
                    it.medicationId == med.id &&
                        it.scheduledTime == time
                }

            val todayCands = meds.flatMap { med ->
                doseTimeSlots(med)
                    .filter { slot ->
                        val t = slot.hour * 60 + slot.minute
                        !isTaken(med, slot.timeString()) && t > nowMin
                    }
                    .map { slot -> NextDoseCand(med, slot.timeString(), slot.hour * 60 + slot.minute, false) }
            }

            val todayTarget = todayCands.minByOrNull { it.minute }
            if (todayTarget != null) {
                val tm = todayTarget.time
                NextDose(tm, false, todayCands.filter { it.time == tm }.map { it.med })
            } else {
                // 今天没有未来待服时段；若今天还有已错过、未服的时段，先提示补签而不是直接跳到明天
                val missedToday = meds.flatMap { med ->
                    doseTimeSlots(med)
                        .filter { slot ->
                            val t = slot.hour * 60 + slot.minute
                            !isTaken(med, slot.timeString()) && nowMin - t >= DOSE_WINDOW_MINUTES
                        }
                        .map { slot -> NextDoseCand(med, slot.timeString(), slot.hour * 60 + slot.minute, false) }
                }
                val missedTarget = missedToday.minByOrNull { it.minute }
                if (missedTarget != null) {
                    val tm = missedTarget.time
                    NextDose(tm, false, missedToday.filter { it.time == tm }.map { it.med }, missed = true)
                } else {
                    val tomorrowCands = meds.mapNotNull { med ->
                        doseTimeSlots(med)
                            .minByOrNull { it.hour * 60 + it.minute }
                            ?.let { slot -> NextDoseCand(med, slot.timeString(), slot.hour * 60 + slot.minute, true) }
                    }
                    val tomorrowTarget = tomorrowCands.minByOrNull { it.minute }
                    if (tomorrowTarget != null) {
                        val tm = tomorrowTarget.time
                        NextDose(tm, true, tomorrowCands.filter { it.time == tm }.map { it.med })
                    } else null
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 已触发就医预警的药品 */
    val warningMeds: StateFlow<List<Medication>> = repo.observeAllMedications()
        .map { meds ->
            meds.mapNotNull { predict(it) }
                .filter { it.needVisit }
                .map { it.medication }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val occasionalMeds: StateFlow<List<Medication>> = repo.observeOccasional()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 是否已经添加过任何药品（用于空状态引导） */
    val hasMedications: StateFlow<Boolean> = repo.observeAllMedications()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 两次服用过近：同一药品最近两次（24h 内）间隔小于 [MIN_GAP_MINUTES] 分钟时提醒 */
    val closeDoseWarnings: StateFlow<List<CloseDoseWarning>> = combine(
        repo.observeAllMedications(),
        repo.observeAllRecords()
    ) { meds, records ->
        val nameById = meds.associate { it.id to it.name }
        val cutoff = LocalDateTime.now().minusHours(24)
        records.asSequence()
            .filter { it.category != RecordCategory.SKIPPED }
            .groupBy { it.medicationId }
            .mapNotNull { (medId, recs) ->
                val times = recs.mapNotNull { dateTimeOf(it.date, it.time) }
                    .filter { it.isAfter(cutoff) }
                    .sorted()
                if (times.size < 2) return@mapNotNull null
                val gap = Duration.between(times[times.size - 2], times.last()).toMinutes()
                val name = nameById[medId] ?: ""
                if (gap < MIN_GAP_MINUTES && name.isNotEmpty()) CloseDoseWarning(name, gap.toInt()) else null
            }
            .sortedBy { it.minutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 可撤销的上一次打卡（记录 id + 去重键），用于「撤销上一次打卡」 */
    private data class UndoEntry(val recordId: Long, val key: String)

    private val lastCheckIn = MutableStateFlow<List<UndoEntry>>(emptyList())

    /** 是否有可撤销的上一次打卡 */
    val canUndo: StateFlow<Boolean> = lastCheckIn
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 打卡：按各时段所属日期写入记录（可能包含昨天补签的项） */
    fun checkInNormal(items: List<DueItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val actual = "%02d:%02d".format(now.hour, now.minute)
            val entries = mutableListOf<UndoEntry>()
            items.forEach { item ->
                val key = "${item.medication.id}|${item.date}|${item.time}"
                if (!processedKeys.add(key)) return@forEach
                val id = repo.addRecord(
                    item.medication.id,
                    item.date,
                    actual,
                    RecordCategory.NORMAL,
                    scheduledTime = item.time,
                    plannedDate = item.date
                )
                repo.decrementQuantity(item.medication.id, intakeAmount(item.medication))
                entries += UndoEntry(id, key)
            }
            lastCheckIn.value = entries
        }
    }

    /** 补签：已错过时段，date 记为「实际当天」（跨零点补昨晚漏服时归到第二天），plannedDate 保留计划日，标注 BACKFILL */
    fun checkInBackfill(items: List<DueItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val today = now.toLocalDate().toString()
            val actual = "%02d:%02d".format(now.hour, now.minute)
            val entries = mutableListOf<UndoEntry>()
            items.forEach { item ->
                val key = "backfill|${item.medication.id}|${item.date}|${item.time}"
                if (!processedKeys.add(key)) return@forEach
                // ISO 日期字符串可字典序比较：计划日早于今天时记到实际当天
                val date = if (item.date < today) today else item.date
                val id = repo.addRecord(
                    item.medication.id,
                    date,
                    actual,
                    RecordCategory.NORMAL,
                    scheduledTime = item.time,
                    flag = RecordFlag.BACKFILL,
                    plannedDate = item.date
                )
                repo.decrementQuantity(item.medication.id, intakeAmount(item.medication))
                entries += UndoEntry(id, key)
            }
            lastCheckIn.value = entries
        }
    }

    /** 提前服用：现在提前吃「下一次未来时段」的药，记实际当天、标注 EARLY */
    fun checkInEarly(time: String, items: List<Medication>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val date = now.toLocalDate().toString()
            val actual = "%02d:%02d".format(now.hour, now.minute)
            val entries = mutableListOf<UndoEntry>()
            items.forEach { med ->
                val key = "early|${med.id}|$date|$time"
                if (!processedKeys.add(key)) return@forEach
                val id = repo.addRecord(
                    med.id,
                    date,
                    actual,
                    RecordCategory.NORMAL,
                    scheduledTime = time,
                    flag = RecordFlag.EARLY,
                    plannedDate = date
                )
                repo.decrementQuantity(med.id, intakeAmount(med))
                entries += UndoEntry(id, key)
            }
            lastCheckIn.value = entries
        }
    }

    /** 删除已错过的时段：记为「跳过」，不再提示补签，也不扣减库存 */
    fun skipMissed(items: List<DueItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            items.forEach { item ->
                val key = "${item.medication.id}|${item.date}|${item.time}"
                if (!processedKeys.add(key)) return@forEach
                repo.addRecord(
                    item.medication.id,
                    item.date,
                    item.time,
                    RecordCategory.SKIPPED,
                    scheduledTime = item.time,
                    plannedDate = item.date
                )
            }
        }
    }

    fun checkInOccasional(meds: List<Medication>) {
        if (meds.isEmpty()) return
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val date = now.toLocalDate().toString()
            val time = "%02d:%02d".format(now.hour, now.minute)
            val entries = mutableListOf<UndoEntry>()
            meds.forEach { med ->
                val key = "occ|${med.id}|$date|$time"
                if (!processedKeys.add(key)) return@forEach
                val id = repo.addRecord(med.id, date, time, RecordCategory.OCCASIONAL)
                repo.decrementQuantity(med.id, intakeAmount(med))
                entries += UndoEntry(id, key)
            }
            lastCheckIn.value = entries
        }
    }

    /** 撤销上一次打卡：删除对应记录并恢复库存，同时释放去重键以便重新打卡 */
    fun undoLastCheckIn() {
        val entries = lastCheckIn.value
        if (entries.isEmpty()) return
        lastCheckIn.value = emptyList()
        viewModelScope.launch {
            entries.forEach { e ->
                repo.deleteRecordById(e.recordId)
                processedKeys.remove(e.key)
            }
        }
    }

    private fun intakeAmount(med: Medication): Double =
        if (med.dosePerIntake > 0.0) med.dosePerIntake else 1.0

    private fun dateTimeOf(date: String, time: String): LocalDateTime? {
        val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return d.atTime(h, m)
    }
}