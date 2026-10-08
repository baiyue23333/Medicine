package com.baiyue.medicine.prediction

import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.DoseType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class DoseSlot(val label: String, val hour: Int, val minute: Int) {
    fun timeString(): String = "%02d:%02d".format(hour, minute)
}

/**
 * 打卡窗口（分钟）：计划时段前后该范围内视为「当前待打卡」；
 * 早于窗口结束仍未打卡则视为「已错过」，可补签并触发错过提醒。
 */
const val DOSE_WINDOW_MINUTES = 60

/**
 * 该时段属于哪一天：错过提醒在时段结束之后才触发，跨零点时段会落到次日，
 * 因此当「当前时刻早于时段本身」时，说明该时段属于前一天。
 */
fun doseSlotDate(
    slotHour: Int,
    slotMinute: Int,
    now: LocalTime = LocalTime.now(),
    today: LocalDate = LocalDate.now()
): LocalDate {
    val nowMin = now.hour * 60 + now.minute
    return if (nowMin >= slotHour * 60 + slotMinute) today else today.minusDays(1)
}

/**
 * 药品在指定日期是否已存在：createdAt 为 0（升级前的旧数据）视为一直存在；
 * 补签不追溯药品添加之前的日期，避免新添加的药品凭空产生大量漏服项。
 */
fun medicationExistsOn(med: Medication, date: LocalDate): Boolean {
    if (med.createdAt <= 0L) return true
    val created = Instant.ofEpochMilli(med.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    return !created.isAfter(date)
}

private fun spreadSlots(n: Int): List<DoseSlot> {
    val count = n.coerceAtLeast(1)
    val start = 8 * 60
    val step = if (count > 1) (24 * 60) / count else 0
    return (0 until count).map { i ->
        val total = (start + i * step) % (24 * 60)
        DoseSlot("第${i + 1}次", total / 60, total % 60)
    }
}

fun parseTimes(csv: String): List<Pair<Int, Int>> =
    csv.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { t ->
            val parts = t.split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                if (h != null && m != null) h to m else null
            } else null
        }

/** 解析单个 "HH:mm" 时间，非法时回退 08:00 */
fun parseSingleTime(s: String): Pair<Int, Int> {
    val parts = s.split(":")
    val h = (parts.getOrNull(0)?.toIntOrNull() ?: 8).coerceIn(0, 23)
    val m = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, 59)
    return h to m
}

/** 该药品一天内的所有服用时间点 */
fun doseTimeSlots(med: Medication): List<DoseSlot> {
    if (!med.isScheduled || !med.enabled) return emptyList()
    return when (med.doseType) {
        DoseType.MORNING_NOON_NIGHT -> parseTimes(med.doseTimes).mapIndexed { i, (h, m) ->
            DoseSlot(
                when (i) {
                    0 -> "早"
                    1 -> "午"
                    2 -> "晚"
                    else -> "第${i + 1}次"
                },
                h,
                m
            )
        }
        DoseType.TIMES_PER_DAY -> {
            val times = parseTimes(med.doseTimes)
            if (times.isNotEmpty()) {
                times.mapIndexed { i, (h, m) -> DoseSlot("第${i + 1}次", h, m) }
            } else {
                spreadSlots(med.timesPerDay)
            }
        }
        DoseType.CUSTOM -> {
            val times = parseTimes(med.doseTimes)
            if (times.isNotEmpty()) {
                times.mapIndexed { i, (h, m) -> DoseSlot("第${i + 1}次", h, m) }
            } else {
                spreadSlots(med.timesPerDay)
            }
        }
        DoseType.EVERY_HOURS -> {
            val step = med.intervalHours.coerceAtLeast(1)
            val count = kotlin.math.ceil(24.0 / step).toInt()
            val (sh, sm) = parseSingleTime(med.intervalStartTime)
            val start = sh * 60 + sm
            (0 until count).map { i ->
                val total = (start + i * step * 60) % (24 * 60)
                DoseSlot(
                    "%02d:%02d".format(total / 60, total % 60),
                    total / 60,
                    total % 60
                )
            }
        }
    }
}