package com.baiyue.medicine.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.baiyue.medicine.MedicineApp
import com.baiyue.medicine.R
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.prediction.doseSlotDate
import com.baiyue.medicine.prediction.doseTimeSlots
import com.baiyue.medicine.prediction.predict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return
        when (intent.action) {
            ReminderScheduler.ACTION_DOSE -> handleDose(context, intent)
            ReminderScheduler.ACTION_MISSED -> handleMissed(context, intent)
            ReminderScheduler.ACTION_VISIT -> handleVisit(context)
        }
    }

    private fun handleDose(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MedicineApp
                val settings = app.settingsRepository.settings.first()
                // 关闭提醒后不再弹通知，也不再续排
                if (!settings.reminderEnabled) return@launch
                val res = AppLocale.localized(context)
                val title = res.getString(R.string.notif_dose_title)
                // 优先用药品专属提醒文案，否则用全局文案，最终回落到标题
                val medId = intent.getLongExtra(ReminderScheduler.EXTRA_MED_ID, -1L)

                // 提前服用后该时段已打卡，不再弹出这一轮的提醒
                val hour = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_HOUR, -1)
                val minute = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_MINUTE, 0)
                val alreadyTaken = if (medId > 0 && hour in 0..23) {
                    val slot = "%02d:%02d".format(hour, minute)
                    val date = doseSlotDate(hour, minute).toString()
                    runCatching {
                        app.repository.observeRecordsByPlannedDate(date).first()
                            .any { it.medicationId == medId && it.scheduledTime == slot }
                    }.getOrDefault(false)
                } else false

                if (!alreadyTaken) {
                    val medText = if (medId > 0) app.repository.getMedication(medId)?.reminderText?.trim().orEmpty() else ""
                    val text = when {
                        medText.isNotEmpty() -> medText
                        settings.reminderMessage.isBlank() -> title
                        else -> settings.reminderMessage
                    }
                    NotificationHelper.show(
                        context,
                        NotificationHelper.CHANNEL_MED,
                        (System.nanoTime() % 100000).toInt(),
                        title,
                        text
                    )
                }
                ReminderScheduler.rescheduleDose(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** 错过服药检查：闹钟在时段结束（跨过打卡窗口）后触发，未打卡才提醒 */
    private fun handleMissed(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MedicineApp
                val settings = app.settingsRepository.settings.first()
                if (!settings.missedReminderEnabled) return@launch

                val medId = intent.getLongExtra(ReminderScheduler.EXTRA_MED_ID, -1L)
                val hour = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_HOUR, -1)
                val minute = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_MINUTE, 0)
                if (medId <= 0 || hour !in 0..23) return@launch
                val slot = "%02d:%02d".format(hour, minute)

                // 闹钟在时段结束之后触发；跨零点时段要归到前一天
                val date = doseSlotDate(hour, minute).toString()

                val med = app.repository.getMedication(medId)
                if (med != null && doseTimeSlots(med).any { it.timeString() == slot }) {
                    val taken = app.repository.observeRecordsByPlannedDate(date).first()
                        .any { it.medicationId == medId && it.scheduledTime == slot }
                    if (!taken) {
                        val res = AppLocale.localized(context)
                        NotificationHelper.show(
                            context,
                            NotificationHelper.CHANNEL_MISSED,
                            (5000 + medId).toInt(),
                            res.getString(R.string.notif_missed_title),
                            res.getString(R.string.notif_missed_text, slot)
                        )
                    }
                }
                // 排程可能已过期（时段已不在当前方案里），按当前方案续排
                ReminderScheduler.rescheduleMissed(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleVisit(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MedicineApp
                val settings = app.settingsRepository.settings.first()
                if (!settings.reminderEnabled) return@launch
                val meds = app.repository.observeAllMedications().first()
                val needVisit = meds.mapNotNull { predict(it) }.filter { it.needVisit }
                if (needVisit.isNotEmpty()) {
                    val res = AppLocale.localized(context)
                    val names = needVisit.joinToString(res.getString(R.string.notif_name_separator)) {
                        it.medication.name
                    }
                    NotificationHelper.show(
                        context,
                        NotificationHelper.CHANNEL_VISIT,
                        2000,
                        res.getString(R.string.notif_visit_title),
                        res.getString(R.string.notif_visit_text, names)
                    )
                }
                ReminderScheduler.scheduleVisitCheck(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}