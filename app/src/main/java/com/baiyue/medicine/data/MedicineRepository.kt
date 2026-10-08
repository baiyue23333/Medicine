package com.baiyue.medicine.data

import android.content.Context
import com.baiyue.medicine.R
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.model.DoseType
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag
import com.baiyue.medicine.model.UsageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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
        plannedDate: String = ""
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
            plannedDate = plannedDate.ifEmpty { date }
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
    suspend fun exportCsv(context: Context): String {
        val res = AppLocale.localized(context)
        val meds = db.medicationDao().observeAll().first()
        val records = db.doseRecordDao().observeAll().first()
        val sb = StringBuilder()

        sb.append(res.getString(R.string.csv_medications_header)).append('\n')
        meds.forEach { m ->
            sb.append(
                listOf(
                    m.id.toString(), m.name, m.usageType.name, m.customUsage,
                    m.doseType.name, m.doseTimes, m.intervalHours.toString(), m.intervalStartTime,
                    m.timesPerDay.toString(), numText(m.dosePerIntake), quantityText(m.quantity),
                    numText(m.minKeepQuantity), m.warningDays.toString(),
                    boolNum(m.isScheduled), boolNum(m.isOccasional), boolNum(m.enabled),
                    m.note, m.reminderText, m.createdAt.toString()
                ).joinToString(",") { csvCell(it) }
            ).append('\n')
        }

        sb.append('\n')
        sb.append(res.getString(R.string.csv_records_header)).append('\n')
        val nameMap = meds.associate { it.id to it.name }
        records.forEach { r ->
            sb.append(
                listOf(
                    r.id.toString(), r.medicationId.toString(), nameMap[r.medicationId] ?: "",
                    r.date, r.time, r.scheduledTime, r.category.name, r.flag.name, r.plannedDate
                ).joinToString(",") { csvCell(it) }
            ).append('\n')
        }
        return sb.toString()
    }

    private fun numText(d: Double): String =
        if (!d.isNaN() && !d.isInfinite() && d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

    private fun quantityText(q: Double): String = if (q < 0.0) "" else numText(q)

    private fun boolNum(b: Boolean): String = if (b) "1" else "0"

    private fun csvCell(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    /** 从 CSV 备份导入药品与打卡记录；按药名合并（保留现有 id），避免 id 冲突与重复记录 */
    suspend fun importCsv(csv: String): Int {
        val lines = csv.lines()
        if (lines.isEmpty()) return 0

        var i = 0
        while (i < lines.size && lines[i].isBlank()) i++
        if (i >= lines.size) return 0
        i++ // 跳过药品表头

        val existingByName = db.medicationDao().observeAll().first().associateBy { it.name.trim() }
        val medByOldId = mutableMapOf<String, Medication>()

        while (i < lines.size && lines[i].isNotBlank()) {
            val cells = splitCsvLine(lines[i])
            if (cells.size >= 16) {
                val name = cells[1]
                val parsed = Medication(
                    name = name,
                    usageType = runCatching { UsageType.valueOf(cells.getOrElse(2) { "" }) }.getOrDefault(UsageType.AFTER_MEAL),
                    customUsage = cells.getOrElse(3) { "" },
                    doseType = runCatching { DoseType.valueOf(cells.getOrElse(4) { "" }) }.getOrDefault(DoseType.MORNING_NOON_NIGHT),
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
                val resolved = existingByName[name.trim()]?.let { old ->
                    old.copy(
                        usageType = parsed.usageType, customUsage = parsed.customUsage,
                        doseType = parsed.doseType, doseTimes = parsed.doseTimes, intervalHours = parsed.intervalHours,
                        intervalStartTime = parsed.intervalStartTime, timesPerDay = parsed.timesPerDay,
                        dosePerIntake = parsed.dosePerIntake, quantity = parsed.quantity, minKeepQuantity = parsed.minKeepQuantity,
                        warningDays = parsed.warningDays, isScheduled = parsed.isScheduled, isOccasional = parsed.isOccasional,
                        enabled = parsed.enabled, note = parsed.note, reminderText = parsed.reminderText
                    )
                } ?: parsed
                val newId = db.medicationDao().upsert(resolved)
                medByOldId[cells[0]] = resolved.copy(id = newId)
            }
            i++
        }

        while (i < lines.size && lines[i].isBlank()) i++
        if (i < lines.size) i++ // 跳过记录表头

        var imported = 0
        while (i < lines.size) {
            val cells = splitCsvLine(lines[i])
            if (cells.size >= 6) {
                val med = medByOldId[cells[1]]
                if (med != null && med.id != 0L) {
                    val date = cells[3]
                    val actualTime = cells[4]
                    val scheduledTime = cells[5]
                    val flag = if (cells.size > 7) runCatching { RecordFlag.valueOf(cells[7]) }.getOrDefault(RecordFlag.NORMAL) else RecordFlag.NORMAL
                    val plannedDate = cells.getOrNull(8)?.takeIf { it.isNotBlank() } ?: date
                    // 常规记录按计划时段去重；偶尔服药无计划时段，改按实际时间+日期去重，避免空时段导致误判重复
                    val exists = db.doseRecordDao().observeByDate(date).first().any {
                        it.medicationId == med.id &&
                            (if (scheduledTime.isNotEmpty()) it.scheduledTime == scheduledTime else it.time == actualTime)
                    }
                    if (!exists) {
                        db.doseRecordDao().insert(
                            DoseRecord(
                                medicationId = med.id,
                                date = date,
                                time = actualTime,
                                category = runCatching { RecordCategory.valueOf(cells[6]) }.getOrDefault(RecordCategory.NORMAL),
                                taken = true,
                                scheduledTime = scheduledTime,
                                flag = flag,
                                plannedDate = plannedDate
                            )
                        )
                        imported++
                    }
                }
            }
            i++
        }
        return imported
    }

    private fun parseQuantity(cell: String?): Double =
        if (cell.isNullOrBlank()) Medication.UNKNOWN_QUANTITY
        else cell.toDoubleOrNull() ?: Medication.UNKNOWN_QUANTITY

    private fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var j = 0
        while (j < line.length) {
            val c = line[j]
            if (inQuotes) {
                if (c == '"') {
                    if (j + 1 < line.length && line[j + 1] == '"') { cur.append('"'); j++ }
                    else inQuotes = false
                } else cur.append(c)
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> { out.add(cur.toString()); cur.setLength(0) }
                    else -> cur.append(c)
                }
            }
            j++
        }
        out.add(cur.toString())
        return out
    }
}