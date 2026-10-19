package com.baiyue.medicine.data

import android.content.Context
import androidx.room.withTransaction
import com.baiyue.medicine.R
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.model.DoseType
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag
import com.baiyue.medicine.model.UsageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MedicineRepository(private val db: MedicineDatabase) {

    fun observeAllMedications(): Flow<List<Medication>> = db.medicationDao().observeAll()

    fun observeRegular(): Flow<List<Medication>> = db.medicationDao().observeRegular()

    fun observeOccasional(): Flow<List<Medication>> = db.medicationDao().observeOccasional()

    fun observeAllRecords(): Flow<List<DoseRecord>> = db.doseRecordDao().observeAll()

    fun observeRecordsByDate(date: String): Flow<List<DoseRecord>> = db.doseRecordDao().observeByDate(date)

    fun observeRecordsByPlannedDate(plannedDate: String): Flow<List<DoseRecord>> =
        db.doseRecordDao().observeByPlannedDate(plannedDate)

    suspend fun getMedication(id: Long): Medication? = db.medicationDao().getById(id)

    suspend fun upsertMedication(medication: Medication): Long = db.medicationDao().upsert(medication)

    suspend fun deleteMedication(medication: Medication) = db.medicationDao().delete(medication)

    suspend fun decrementQuantity(id: Long, amount: Double = 1.0) =
        db.medicationDao().decrementQuantity(id, amount)

    suspend fun adjustQuantity(id: Long, delta: Double) =
        db.medicationDao().adjustQuantity(id, delta)

    suspend fun addRecord(
        medicationId: Long,
        date: String,
        time: String,
        category: RecordCategory,
        note: String = "",
        scheduledTime: String = "",
        flag: RecordFlag = RecordFlag.NORMAL,
        plannedDate: String = "",
        medicationName: String = ""
    ): Long = db.doseRecordDao().insert(
        DoseRecord(
            medicationId = medicationId,
            date = date,
            time = time,
            category = category,
            taken = true,
            note = note,
            scheduledTime = scheduledTime,
            flag = flag,
            plannedDate = plannedDate.ifEmpty { date },
            medicationName = medicationName.ifBlank {
                db.medicationDao().getById(medicationId)?.name.orEmpty()
            }
        )
    )

    /** 撤销一条打卡记录并恢复库存；SKIPPED（确认未服）本身未扣库存，无需恢复 */
    suspend fun deleteRecord(record: DoseRecord) {
        db.doseRecordDao().delete(record)
        if (record.category != RecordCategory.SKIPPED) {
            val med = db.medicationDao().getById(record.medicationId)
            if (med != null) {
                val amount = if (med.dosePerIntake > 0.0) med.dosePerIntake else 1.0
                db.medicationDao().restoreQuantity(record.medicationId, amount)
            }
        }
    }

    /** 按 id 撤销一条打卡记录（用于「撤销上一次打卡」） */
    suspend fun deleteRecordById(id: Long) {
        db.doseRecordDao().getById(id)?.let { deleteRecord(it) }
    }

    /** 导出全部药品与打卡记录为 CSV 文本（便于备份/换机迁移），表头跟随应用语言 */
    suspend fun exportCsv(context: Context): String = withContext(Dispatchers.IO) {
        val res = AppLocale.localized(context)
        val meds = db.medicationDao().observeAll().first()
        val records = db.doseRecordDao().observeAll().first()
        val sb = StringBuilder()

        sb.append(res.getString(R.string.csv_medications_header)).append('\n')
        meds.forEach { m ->
            sb.append(
                CsvCodec.encodeRow(
                    listOf(
                        m.id.toString(), m.name, m.usageType.name, m.customUsage,
                        m.doseType.name, m.doseTimes, m.intervalHours.toString(), m.intervalStartTime,
                        m.timesPerDay.toString(), numText(m.dosePerIntake), quantityText(m.quantity),
                        numText(m.minKeepQuantity), m.warningDays.toString(),
                        boolNum(m.isScheduled), boolNum(m.isOccasional), boolNum(m.enabled),
                        m.note, m.reminderText, m.createdAt.toString()
                    )
                )
            ).append('\n')
        }

        sb.append('\n')
        sb.append(res.getString(R.string.csv_records_header)).append('\n')
        val nameMap = meds.associate { it.id to it.name }
        records.forEach { r ->
            val name = r.medicationName.ifBlank { nameMap[r.medicationId].orEmpty() }
            sb.append(
                CsvCodec.encodeRow(
                    listOf(
                        r.id.toString(), r.medicationId.toString(), name,
                        r.date, r.time, r.scheduledTime, r.category.name, r.flag.name, r.plannedDate
                    )
                )
            ).append('\n')
        }
        sb.toString()
    }

    private fun numText(d: Double): String =
        if (!d.isNaN() && !d.isInfinite() && d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

    private fun quantityText(q: Double): String = if (q < 0.0) "" else numText(q)

    private fun boolNum(b: Boolean): String = if (b) "1" else "0"

    /**
     * 从 CSV 备份导入药品与打卡记录。
     *
     * - 支持带逗号、引号、换行和 CRLF 的字段
     * - 整个导入在一个 Room 事务内完成，失败不会留下半套数据
     * - 药名相同时：只有一个同名药品则合并；有多个同名药品时优先按 createdAt 匹配
     * - 记录写入药品名称快照，之后删除药品也不会丢失历史名称
     */
    suspend fun importCsv(csv: String): Int = withContext(Dispatchers.IO) {
        db.withTransaction {
            val rows = CsvCodec.parse(csv)
            if (rows.isEmpty()) return@withTransaction 0

            var i = 0
            while (i < rows.size && CsvCodec.isBlankRow(rows[i])) i++
            if (i >= rows.size) return@withTransaction 0
            i++ // 跳过药品表头

            val existingByName: MutableMap<String, MutableList<Medication>> =
                db.medicationDao().observeAll().first().groupBy { it.name.trim() }
                    .mapValues { it.value.toMutableList() }
                    .toMutableMap()
            val medByOldId = mutableMapOf<String, Medication>()

            while (i < rows.size && !CsvCodec.isBlankRow(rows[i])) {
                val cells = rows[i]
                val name = cells.getOrElse(1) { "" }.trim()
                if (cells.size >= 16 && name.isNotEmpty()) {
                    val parsed = Medication(
                        name = name,
                        usageType = runCatching { UsageType.valueOf(cells.getOrElse(2) { "" }) }
                            .getOrDefault(UsageType.AFTER_MEAL),
                        customUsage = cells.getOrElse(3) { "" },
                        doseType = runCatching { DoseType.valueOf(cells.getOrElse(4) { "" }) }
                            .getOrDefault(DoseType.MORNING_NOON_NIGHT),
                        doseTimes = cells.getOrElse(5) { "08:00,12:00,18:00" },
                        intervalHours = cells.getOrNull(6)?.toIntOrNull() ?: 0,
                        intervalStartTime = cells.getOrElse(7) { "08:00" },
                        timesPerDay = cells.getOrNull(8)?.toIntOrNull() ?: 1,
                        dosePerIntake = cells.getOrNull(9)?.toDoubleOrNull() ?: 1.0,
                        quantity = parseQuantity(cells.getOrNull(10)),
                        minKeepQuantity = cells.getOrNull(11)?.toDoubleOrNull() ?: 0.0,
                        warningDays = cells.getOrNull(12)?.toIntOrNull() ?: 3,
                        isScheduled = cells.getOrNull(13) != "0",
                        isOccasional = cells.getOrNull(14) == "1",
                        enabled = cells.getOrNull(15) != "0",
                        note = cells.getOrElse(16) { "" },
                        reminderText = cells.getOrElse(17) { "" },
                        createdAt = cells.getOrNull(18)?.toLongOrNull() ?: 0L
                    )

                    val sameName = existingByName[name].orEmpty()
                    val existing = when {
                        sameName.size == 1 -> sameName.single()
                        sameName.isEmpty() -> null
                        else -> sameName.firstOrNull { it.createdAt == parsed.createdAt }
                    }
                    val resolved = existing?.copy(
                        usageType = parsed.usageType,
                        customUsage = parsed.customUsage,
                        doseType = parsed.doseType,
                        doseTimes = parsed.doseTimes,
                        intervalHours = parsed.intervalHours,
                        intervalStartTime = parsed.intervalStartTime,
                        timesPerDay = parsed.timesPerDay,
                        dosePerIntake = parsed.dosePerIntake,
                        quantity = parsed.quantity,
                        minKeepQuantity = parsed.minKeepQuantity,
                        warningDays = parsed.warningDays,
                        isScheduled = parsed.isScheduled,
                        isOccasional = parsed.isOccasional,
                        enabled = parsed.enabled,
                        note = parsed.note,
                        reminderText = parsed.reminderText
                    ) ?: parsed

                    val newId = db.medicationDao().upsert(resolved)
                    val saved = resolved.copy(id = newId)
                    medByOldId[cells[0]] = saved
                    existingByName[name] = (existingByName[name].orEmpty().filter { it.id != newId } + saved)
                        .toMutableList()
                }
                i++
            }

            while (i < rows.size && CsvCodec.isBlankRow(rows[i])) i++
            if (i < rows.size) i++ // 跳过记录表头

            val existingKeys = db.doseRecordDao().getAll()
                .map { recordKey(it.medicationId, it.date, it.scheduledTime, it.time) }
                .toMutableSet()

            var imported = 0
            while (i < rows.size) {
                val cells = rows[i]
                if (!CsvCodec.isBlankRow(cells) && cells.size >= 6) {
                    val med = medByOldId[cells[1]]
                    val date = cells[3]
                    val actualTime = cells[4]
                    val scheduledTime = cells[5]
                    if (med != null && date.isNotBlank() && actualTime.isNotBlank()) {
                        val key = recordKey(med.id, date, scheduledTime, actualTime)
                        if (key !in existingKeys) {
                            val flag = if (cells.size > 7) {
                                runCatching { RecordFlag.valueOf(cells[7]) }.getOrDefault(RecordFlag.NORMAL)
                            } else RecordFlag.NORMAL
                            val plannedDate = cells.getOrNull(8)?.takeIf { it.isNotBlank() } ?: date
                            val snapshotName = cells.getOrElse(2) { "" }.ifBlank { med.name }
                            db.doseRecordDao().insert(
                                DoseRecord(
                                    medicationId = med.id,
                                    date = date,
                                    time = actualTime,
                                    category = runCatching { RecordCategory.valueOf(cells[6]) }
                                        .getOrDefault(RecordCategory.NORMAL),
                                    taken = true,
                                    scheduledTime = scheduledTime,
                                    flag = flag,
                                    plannedDate = plannedDate,
                                    medicationName = snapshotName
                                )
                            )
                            existingKeys.add(key)
                            imported++
                        }
                    }
                }
                i++
            }
            imported
        }
    }

    private fun recordKey(medicationId: Long, date: String, scheduledTime: String, time: String): String =
        if (scheduledTime.isNotEmpty()) "$medicationId|$date|$scheduledTime"
        else "$medicationId|$date|$time"

    private fun parseQuantity(cell: String?): Double =
        if (cell.isNullOrBlank()) Medication.UNKNOWN_QUANTITY
        else cell.toDoubleOrNull() ?: Medication.UNKNOWN_QUANTITY
}