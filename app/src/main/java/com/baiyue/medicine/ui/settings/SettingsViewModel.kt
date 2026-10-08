package com.baiyue.medicine.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baiyue.medicine.MedicineApp
import com.baiyue.medicine.data.AppSettings
import com.baiyue.medicine.reminder.KeepAliveService
import com.baiyue.medicine.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val medicineApp = app as MedicineApp
    private val repo = medicineApp.settingsRepository

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setWarningDays(days: Int) {
        viewModelScope.launch { repo.setDefaultWarningDays(days) }
    }

    fun setKeepAlive(enabled: Boolean) {
        viewModelScope.launch {
            repo.setKeepAlive(enabled)
            if (enabled) KeepAliveService.start(getApplication())
            else KeepAliveService.stop(getApplication())
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setReminderEnabled(enabled)
            reschedule()
        }
    }

    fun setReminderAdvanceMinutes(minutes: Int) {
        viewModelScope.launch {
            repo.setReminderAdvanceMinutes(minutes)
            reschedule()
        }
    }

    fun setReminderMessage(message: String) {
        viewModelScope.launch {
            repo.setReminderMessage(message)
        }
    }

    fun setMissedReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setMissedReminderEnabled(enabled)
            reschedule()
        }
    }

    /** 导出全部数据为 CSV 文本 */
    suspend fun exportCsv(): String = medicineApp.repository.exportCsv(getApplication())

    /** 从 CSV 备份导入，返回导入的记录条数 */
    suspend fun importCsv(csv: String): Int = medicineApp.repository.importCsv(csv)

    /** 按最新设置同步所有闹钟 */
    private suspend fun reschedule() {
        runCatching {
            val meds = medicineApp.repository.observeAllMedications().first()
            val s = repo.settings.first()
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