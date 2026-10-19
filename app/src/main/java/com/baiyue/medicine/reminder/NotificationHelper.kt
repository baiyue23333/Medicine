package com.baiyue.medicine.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.baiyue.medicine.MainActivity
import com.baiyue.medicine.R
import com.baiyue.medicine.i18n.AppLocale

object NotificationHelper {

    const val CHANNEL_MED = "med_reminder"
    const val CHANNEL_VISIT = "visit_reminder"
    const val CHANNEL_MISSED = "missed_reminder"
    const val CHANNEL_KEEP = "keep_alive"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        // 渠道名会随应用语言变化，每次启动重建即可刷新
        val res = AppLocale.localized(context)
        val med = NotificationChannel(
            CHANNEL_MED,
            res.getString(R.string.notif_channel_med),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = res.getString(R.string.notif_channel_med_desc)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val missed = NotificationChannel(
            CHANNEL_MISSED,
            res.getString(R.string.notif_channel_missed),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = res.getString(R.string.notif_channel_missed_desc)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val visit = NotificationChannel(
            CHANNEL_VISIT,
            res.getString(R.string.notif_channel_visit),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = res.getString(R.string.notif_channel_visit_desc)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val keep = NotificationChannel(
            CHANNEL_KEEP,
            res.getString(R.string.notif_channel_keep),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = res.getString(R.string.notif_channel_keep_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(med)
        nm.createNotificationChannel(missed)
        nm.createNotificationChannel(visit)
        nm.createNotificationChannel(keep)
    }

    private fun contentIntent(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun show(
        context: Context,
        channelId: String,
        id: Int,
        title: String,
        text: String,
        actions: List<NotificationCompat.Action> = emptyList()
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_med)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
        actions.forEach(builder::addAction)
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // 无通知权限时静默忽略
        }
    }

    fun cancel(context: Context, id: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(id) }
    }
}