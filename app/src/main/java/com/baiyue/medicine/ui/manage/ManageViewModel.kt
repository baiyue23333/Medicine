package com.baiyue.medicine.ui.manage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baiyue.medicine.MedicineApp
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageViewModel(app: Application) : AndroidViewModel(app) {
    private val medicineApp = app as MedicineApp
    private val repo = medicineApp.repository

    val medications: StateFlow<List<Medication>> = repo.observeAllMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 新建药品时「提前预警天数」的默认值，跟随设置页的「默认预警天数」 */
    val defaultWarningDays: StateFlow<Int> = medicineApp.settingsRepository.settings
        .map { it.defaultWarningDays }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    /** 是否显示药品图片（全局，持久化） */
    val showImage: StateFlow<Boolean> = medicineApp.settingsRepository.settings
        .map { it.showImage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setShowImage(enabled: Boolean) {
        viewModelScope.launch { medicineApp.settingsRepository.setShowImage(enabled) }
    }

    fun upsert(med: Medication) {
        viewModelScope.launch {
            // 新建时记录添加时间，补签不追溯添加之前的日期
            val toSave = if (med.id == 0L) med.copy(createdAt = System.currentTimeMillis()) else med
            repo.upsertMedication(toSave)
            reschedule()
        }
    }

    fun delete(med: Medication) {
        viewModelScope.launch {
            repo.deleteMedication(med)
            reschedule()
        }
    }

    /** 增减库存数量（正负 delta） */
    fun adjustQuantity(med: Medication, delta: Double) {
        viewModelScope.launch { repo.adjustQuantity(med.id, delta) }
    }

    private suspend fun reschedule() {
        runCatching {
            val meds = repo.observeAllMedications().first()
            val s = medicineApp.settingsRepository.settings.first()
            ReminderScheduler.sync(
                getApplication(),
                meds,
                s.reminderAdvanceMinutes,
                s.reminderEnabled,
                s.missedReminderEnabled
            )
        }
    }
}