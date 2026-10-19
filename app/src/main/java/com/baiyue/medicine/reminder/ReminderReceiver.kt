package com.baiyue.medicine.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.baiyue.medicine.MedicineApp
import com.baiyue.medicine.R
import com.baiyue.medicine.i18n.AppLocale
import com.baiyue.medicine.model.RecordCategory
import com.baiyue.medicine.model.RecordFlag
import com.baiyue.medicine.prediction.doseSlotDate
import com.baiyue.medicine.prediction.doseTimeSlots
import com.baiyue.medicine.prediction.predict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return
        when (intent.action) {
            ReminderScheduler.ACTION_DOSE -> handleDose(context, intent)
            ReminderScheduler.ACTION_MISSED -> handleMissed(context, intent)
            ReminderScheduler.ACTION_VISIT -> handleVisit(context)
            ReminderScheduler.ACTION_TAKEN -> handleTaken(context, intent)
            ReminderScheduler.ACTION_SNOOZE -> handleSnooze(context, intent)
        }
    }

    private fun handleDose(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MedicineApp
                val settings = app.settingsRepository.settings.first()
                if (!settings.reminderEnabled) return@launch

                val medId = intent.getLongExtra(ReminderScheduler.EXTRA_MED_ID, -1L)
                val hour = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_HOUR, -1)
                val minute = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_MINUTE, 0)
                val slotIndex = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_INDEX, 0)
                if (medId <= 0 || hour !in 0..23) return@launch

                val med = app.repository.getMedication(medId) ?: return@launch
                if (!med.enabled || !med.isScheduled) return@launch
                val slot = "%02d:%02d".format(hour, minute)
                if (doseTimeSlots(med).none { it.timeString() == slot }) return@launch

                val date = doseSlotDate(hour, minute).toString()
                val alreadyTaken = app.repository.observeRecordsByPlannedDate(date).first()
                    .any { it.medicationId == medId && it.scheduledTime == slot }

                val res = AppLocale.localized(context)
                val notificationId = ReminderScheduler.doseNotificationId(medId, slotIndex)
                if (!alreadyTaken) {
                    val title = res.getString(R.string.notif_dose_title)
                    val medText = med.reminderText.trim()
                    val text = when {
                        medText.isNotEmpty() -> medText
                        settings.reminderMessage.isNotBlank() -> settings.reminderMessage
                        else -> res.getString(R.string.notif_dose_text, med.name, slot)
                    }
                    NotificationHelper.show(
                        context,
                        NotificationHelper.CHANNEL_MED,
                        notificationId,
                        title,
                        text,
                        actions = doseActions(context, intent, med.name, slot)
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

                val med = app.repository.getMedication(medId) ?: return@launch
                if (!med.enabled || !med.isScheduled) return@launch
                if (doseTimeSlots(med).none { it.timeString() == slot }) return@launch

                // 闹钟在时段结束之后触发；跨零点时段要归到前一天
                val date = doseSlotDate(hour, minute).toString()
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

    /** Notification action: mark the dose as taken without opening the app. */
    private fun handleTaken(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as MedicineApp
                val medId = intent.getLongExtra(ReminderScheduler.EXTRA_MED_ID, -1L)
                val hour = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_HOUR, -1)
                val minute = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_MINUTE, 0)
                val slotIndex = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_INDEX, 0)
                if (medId <= 0 || hour !in 0..23) return@launch

                val med = app.repository.getMedication(medId) ?: return@launch
                if (!med.enabled || !med.isScheduled) return@launch
                val slot = "%02d:%02d".format(hour, minute)
                if (doseTimeSlots(med).none { it.timeString() == slot }) return@launch

                val date = doseSlotDate(hour, minute).toString()
                val exists = app.repository.observeRecordsByPlannedDate(date).first()
                    .any { it.medicationId == medId && it.scheduledTime == slot }
                if (!exists) {
                    val now = LocalDateTime.now()
                    val actual = "%02d:%02d".format(now.hour, now.minute)
                    val flag = if (now.toLocalTime().isBefore(LocalTime.of(hour, minute))) {
                        RecordFlag.EARLY
                    } else {
                        RecordFlag.NORMAL
                    }
                    app.repository.addRecord(
                        medId,
                        date,
                        actual,
                        RecordCategory.NORMAL,
                        scheduledTime = slot,
                        flag = flag,
                        plannedDate = date,
                        medicationName = med.name
                    )
                    val amount = if (med.dosePerIntake > 0.0) med.dosePerIntake else 1.0
                    app.repository.decrementQuantity(medId, amount)
                }
                NotificationHelper.cancel(context, ReminderScheduler.doseNotificationId(medId, slotIndex))
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(ReminderScheduler.EXTRA_MED_ID, -1L)
        val slotIndex = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_INDEX, 0)
        if (medId <= 0) return
        NotificationHelper.cancel(context, ReminderScheduler.doseNotificationId(medId, slotIndex))
        ReminderScheduler.scheduleSnooze(context, intent)
        val res = AppLocale.localized(context)
        Toast.makeText(
            context,
            res.getString(R.string.notif_snoozed, ReminderScheduler.SNOOZE_MINUTES),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun doseActions(
        context: Context,
        intent: Intent,
        medName: String,
        slot: String
    ): List<NotificationCompat.Action> {
        val medId = intent.getLongExtra(ReminderScheduler.EXTRA_MED_ID, -1L)
        val hour = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_HOUR, -1)
        val minute = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_MINUTE, 0)
        val slotIndex = intent.getIntExtra(ReminderScheduler.EXTRA_SLOT_INDEX, 0)
        if (medId <= 0) return emptyList()
        val res = AppLocale.localized(context)

        fun actionIntent(action: String): Intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderScheduler.EXTRA_MED_ID, medId)
            putExtra(ReminderScheduler.EXTRA_MED_NAME, medName)
            putExtra(ReminderScheduler.EXTRA_SLOT_LABEL, slot)
            putExtra(ReminderScheduler.EXTRA_SLOT_HOUR, hour)
            putExtra(ReminderScheduler.EXTRA_SLOT_MINUTE, minute)
            putExtra(ReminderScheduler.EXTRA_SLOT_INDEX, slotIndex)
        }

        val taken = PendingIntent.getBroadcast(
            context,
            300_000 + medId.toInt() * 100 + slotIndex,
            actionIntent(ReminderScheduler.ACTION_TAKEN),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snooze = PendingIntent.getBroadcast(
            context,
            400_000 + medId.toInt() * 100 + slotIndex,
            actionIntent(ReminderScheduler.ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return listOf(
            NotificationCompat.Action.Builder(0, res.getString(R.string.notif_action_taken), taken).build(),
            NotificationCompat.Action.Builder(0, res.getString(R.string.notif_action_snooze), snooze).build()
        )
    }
}
