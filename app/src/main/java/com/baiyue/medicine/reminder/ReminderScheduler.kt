package com.baiyue.medicine.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.baiyue.medicine.data.entity.Medication
import com.baiyue.medicine.prediction.DOSE_WINDOW_MINUTES
import com.baiyue.medicine.prediction.doseTimeSlots
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderScheduler {

    const val ACTION_DOSE = "com.baiyue.medicine.action.DOSE"
    const val ACTION_VISIT = "com.baiyue.medicine.action.VISIT"
    const val ACTION_MISSED = "com.baiyue.medicine.action.MISSED"

    const val EXTRA_MED_ID = "med_id"
    const val EXTRA_MED_NAME = "med_name"
    const val EXTRA_SLOT_LABEL = "slot_label"
    const val EXTRA_SLOT_HOUR = "slot_hour"
    const val EXTRA_SLOT_MINUTE = "slot_minute"
    const val EXTRA_SLOT_INDEX = "slot_index"

    private const val REQUEST_VISIT = 991

    /** 错过提醒的 requestCode 偏移，避免与服药提醒（medId*100+idx）冲突 */
    private const val REQUEST_MISSED_BASE = 100_000

    /** 错过判定：时段结束后再多等 1 分钟，确保已跨过打卡窗口 */
    private const val MISSED_DELAY_MINUTES = DOSE_WINDOW_MINUTES + 1

    private val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /**
     * 按设置同步所有闹钟（统一入口，避免开关组合出现遗漏）：
     * - [doseEnabled] 按时吃药提醒
     * - [missedEnabled] 错过服药提醒
     */
    fun sync(
        context: Context,
        meds: List<Medication>,
        advanceMinutes: Int = 0,
        doseEnabled: Boolean = true,
        missedEnabled: Boolean = false
    ) {
        meds.filter { it.enabled && it.isScheduled }.forEach { med ->
            if (doseEnabled) scheduleDoseSlots(context, med, advanceMinutes) else cancelDoseSlots(context, med)
            if (missedEnabled) scheduleMissedSlots(context, med) else cancelMissedSlots(context, med)
        }
        if (doseEnabled) scheduleVisitCheck(context) else cancelVisitCheck(context)
    }

    fun scheduleDoseSlots(context: Context, med: Medication, advanceMinutes: Int = 0) {
        val am = context.getSystemService(AlarmManager::class.java)
        val now = LocalDateTime.now()
        doseTimeSlots(med).forEachIndexed { idx, slot ->
            val trigger = nextTriggerMillis(now, slot.hour, slot.minute) - advanceMinutes.coerceAtLeast(0) * 60_000L
            val pi = dosePendingIntent(context, med.id, med.name, slot.label, slot.hour, slot.minute, idx)
            scheduleAlarm(am, trigger, pi)
        }
    }

    private fun cancelDoseSlots(context: Context, med: Medication) {
        val am = context.getSystemService(AlarmManager::class.java)
        doseTimeSlots(med).forEachIndexed { idx, slot ->
            am.cancel(dosePendingIntent(context, med.id, med.name, slot.label, slot.hour, slot.minute, idx))
        }
    }

    /** 每个时段结束后 [MISSED_DELAY_MINUTES] 分钟触发一次「是否已打卡」检查 */
    fun scheduleMissedSlots(context: Context, med: Medication) {
        val am = context.getSystemService(AlarmManager::class.java)
        val now = LocalDateTime.now()
        doseTimeSlots(med).forEachIndexed { idx, slot ->
            val trigger = nextTriggerMillis(now, slot.hour, slot.minute) + MISSED_DELAY_MINUTES * 60_000L
            val pi = missedPendingIntent(context, med.id, med.name, slot.label, slot.hour, slot.minute, idx)
            scheduleAlarm(am, trigger, pi)
        }
    }

    private fun cancelMissedSlots(context: Context, med: Medication) {
        val am = context.getSystemService(AlarmManager::class.java)
        doseTimeSlots(med).forEachIndexed { idx, slot ->
            am.cancel(missedPendingIntent(context, med.id, med.name, slot.label, slot.hour, slot.minute, idx))
        }
    }

    fun rescheduleDose(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(EXTRA_MED_ID, -1L)
        val name = intent.getStringExtra(EXTRA_MED_NAME) ?: "药品"
        val label = intent.getStringExtra(EXTRA_SLOT_LABEL) ?: ""
        val hour = intent.getIntExtra(EXTRA_SLOT_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_SLOT_MINUTE, 0)
        val idx = intent.getIntExtra(EXTRA_SLOT_INDEX, 0)
        if (medId <= 0 || hour !in 0..23) return
        val am = context.getSystemService(AlarmManager::class.java)
        val trigger = nextTriggerMillis(LocalDateTime.now(), hour, minute)
        val pi = dosePendingIntent(context, medId, name, label, hour, minute, idx)
        scheduleAlarm(am, trigger, pi)
    }

    fun rescheduleMissed(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(EXTRA_MED_ID, -1L)
        val name = intent.getStringExtra(EXTRA_MED_NAME) ?: "药品"
        val label = intent.getStringExtra(EXTRA_SLOT_LABEL) ?: ""
        val hour = intent.getIntExtra(EXTRA_SLOT_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_SLOT_MINUTE, 0)
        val idx = intent.getIntExtra(EXTRA_SLOT_INDEX, 0)
        if (medId <= 0 || hour !in 0..23) return
        val am = context.getSystemService(AlarmManager::class.java)
        val trigger = nextTriggerMillis(LocalDateTime.now(), hour, minute) + MISSED_DELAY_MINUTES * 60_000L
        val pi = missedPendingIntent(context, medId, name, label, hour, minute, idx)
        scheduleAlarm(am, trigger, pi)
    }

    fun scheduleVisitCheck(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val trigger = nextTriggerMillis(LocalDateTime.now(), 9, 0)
        scheduleAlarm(am, trigger, visitPendingIntent(context))
    }

    private fun cancelVisitCheck(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(visitPendingIntent(context))
    }

    private fun visitPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_VISIT,
        Intent(context, ReminderReceiver::class.java).apply { action = ACTION_VISIT },
        FLAGS
    )

    private fun dosePendingIntent(
        context: Context,
        medId: Long,
        name: String,
        label: String,
        hour: Int,
        minute: Int,
        idx: Int
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        medId.toInt() * 100 + idx,
        slotIntent(context, ACTION_DOSE, medId, name, label, hour, minute, idx),
        FLAGS
    )

    private fun missedPendingIntent(
        context: Context,
        medId: Long,
        name: String,
        label: String,
        hour: Int,
        minute: Int,
        idx: Int
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_MISSED_BASE + medId.toInt() * 100 + idx,
        slotIntent(context, ACTION_MISSED, medId, name, label, hour, minute, idx),
        FLAGS
    )

    private fun slotIntent(
        context: Context,
        action: String,
        medId: Long,
        name: String,
        label: String,
        hour: Int,
        minute: Int,
        idx: Int
    ): Intent = Intent(context, ReminderReceiver::class.java).apply {
        this.action = action
        putExtra(EXTRA_MED_ID, medId)
        putExtra(EXTRA_MED_NAME, name)
        putExtra(EXTRA_SLOT_LABEL, label)
        putExtra(EXTRA_SLOT_HOUR, hour)
        putExtra(EXTRA_SLOT_MINUTE, minute)
        putExtra(EXTRA_SLOT_INDEX, idx)
    }

    private fun nextTriggerMillis(now: LocalDateTime, hour: Int, minute: Int): Long {
        var t = now.toLocalDate().atTime(hour, minute)
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun scheduleAlarm(am: AlarmManager, trigger: Long, pi: PendingIntent) {
        val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }
}