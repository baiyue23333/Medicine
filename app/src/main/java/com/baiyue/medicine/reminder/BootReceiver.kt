package com.baiyue.medicine.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.baiyue.medicine.MedicineApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 开机后按用户设置重建提醒与保活服务 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MedicineApp
                val settings = app.settingsRepository.settings.first()
                val meds = app.repository.observeAllMedications().first()
                ReminderScheduler.sync(
                    context,
                    meds,
                    settings.reminderAdvanceMinutes,
                    settings.reminderEnabled,
                    settings.missedReminderEnabled
                )
                if (settings.keepAlive) KeepAliveService.start(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}