package com.baiyue.medicine.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baiyue.medicine.MedicineApp
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.prediction.predict
import com.baiyue.medicine.prediction.visitWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DayFlags(
    val medication: Boolean = false,
    val visit: Boolean = false,
    val occasional: Boolean = false
)

/** 药品展示信息：full 全称，short 缩写（前 4 字 + …），dose 一次剂量 */
data class MedNames(val full: String, val short: String, val dose: Double = 1.0)

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as MedicineApp).repository

    val flags: StateFlow<Map<String, DayFlags>> = combine(
        repo.observeAllMedications(),
        repo.observeAllRecords()
    ) { meds, records -> computeFlags(meds, records) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val medNameMap: StateFlow<Map<Long, MedNames>> = repo.observeAllMedications()
        .map { list ->
            list.associate {
                it.id to MedNames(
                    full = it.name,
                    short = it.name.take(4) + "…",
                    dose = it.dosePerIntake
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun recordsFor(date: String): Flow<List<DoseRecord>> = repo.observeRecordsByDate(date)

    /** 撤销一条打卡记录（恢复库存） */
    fun deleteRecord(record: DoseRecord) {
        viewModelScope.launch { repo.deleteRecord(record) }
    }

    companion object {
        fun computeFlags(meds: List<Medication>, records: List<DoseRecord>): Map<String, DayFlags> {
            val today = LocalDate.now()
            val map = mutableMapOf<String, DayFlags>()
            fun flag(key: String, transform: (DayFlags) -> DayFlags) {
                map[key] = transform(map[key] ?: DayFlags())
            }
            meds.filter { it.isScheduled && it.enabled }.forEach { med ->
                predict(med, today)?.let { p ->
                    if (p.needVisit) {
                        visitWindow(p).forEach { d -> flag(d.toString()) { f -> f.copy(visit = true) } }
                    }
                }
            }
            records.forEach { r ->
                // SKIPPED 是「确认未服」的占位记录，不计入日历标记
                when (r.category) {
                    RecordCategory.OCCASIONAL -> flag(r.date) { f -> f.copy(occasional = true) }
                    RecordCategory.NORMAL -> flag(r.date) { f -> f.copy(medication = true) }
                    RecordCategory.SKIPPED -> Unit
                }
            }
            return map
        }
    }
}