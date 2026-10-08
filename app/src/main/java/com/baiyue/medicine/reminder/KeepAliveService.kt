package com.baiyue.medicine.reminder

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.baiyue.medicine.MainActivity
import com.baiyue.medicine.R
import com.baiyue.medicine.i18n.AppLocale

/** 常驻前台服务，用于尽量保证提醒进程存活（强保活） */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val res = AppLocale.localized(this)
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_KEEP)
            .setSmallIcon(R.drawable.ic_stat_med)
            .setContentTitle(res.getString(R.string.notif_keep_title))
            .setContentText(res.getString(R.string.notif_keep_text))
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    companion object {
        const val NOTIF_ID = 1001

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, KeepAliveService::class.java)
                )
            } catch (_: Exception) {
                // 某些机型/后台限制下启动失败，忽略
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }
}