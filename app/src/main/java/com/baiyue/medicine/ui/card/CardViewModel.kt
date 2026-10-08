package com.baiyue.medicine.ui.card

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baiyue.medicine.MedicineApp
import com.baiyue.medicine.data.entity.DoseRecord
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.model.RecordCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** 某药品的服药小结（常规次数 + 偶尔次数） */
data class CardEntry(
    val medication: Medication,
    val normalCount: Int,
    val occasionalCount: Int
) {
    val totalCount: Int get() = normalCount + occasionalCount
}

/** 依从性统计：本周/本月已服次数（不含「确认未服」） */
data class PeriodCount(val week: Int, val month: Int)

class CardViewModel(app: Application) : AndroidViewModel(app) {
    private val medicineApp = app as MedicineApp
    private val repo = medicineApp.repository
    private val today = LocalDate.now().toString()

    /** 是否显示药品图片（全局，持久化） */
    val showImage: StateFlow<Boolean> = medicineApp.settingsRepository.settings
        .map { it.showImage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** 今日概况 */
    val todayEntries: StateFlow<List<CardEntry>> = buildEntries(repo.observeRecordsByDate(today))

    /** 累计概况（全部历史） */
    val totalEntries: StateFlow<List<CardEntry>> = buildEntries(repo.observeAllRecords())

    /** 药方：所有药品（种类/次数/方式） */
    val prescriptions: StateFlow<List<Medication>> = repo.observeAllMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 依从性统计：本周/本月已服次数（不含「确认未服」） */
    val periodCounts: StateFlow<PeriodCount> = repo.observeAllRecords()
        .map { records ->
            val today = LocalDate.now()
            val weekStart = today.minusDays(today.dayOfWeek.value - 1L)
            val monthStart = today.withDayOfMonth(1)
            val todayKey = today.toString()
            var week = 0
            var month = 0
            for (r in records) {
                if (r.category == RecordCategory.SKIPPED) continue
                val d = r.date
                if (d >= weekStart.toString() && d <= todayKey) week++
                if (d >= monthStart.toString() && d <= todayKey) month++
            }
            PeriodCount(week, month)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodCount(0, 0))

    private fun buildEntries(recordsFlow: Flow<List<DoseRecord>>): StateFlow<List<CardEntry>> =
        combine(repo.observeAllMedications(), recordsFlow) { meds, records ->
            meds.map { med ->
                CardEntry(
                    medication = med,
                    normalCount = records.count { it.medicationId == med.id && it.category == RecordCategory.NORMAL },
                    occasionalCount = records.count { it.medicationId == med.id && it.category == RecordCategory.OCCASIONAL }
                )
            }.filter { it.totalCount > 0 }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}