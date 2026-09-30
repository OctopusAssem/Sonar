package com.assem.sonar.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.assem.sonar.R
import com.assem.sonar.util.LocaleHelper

object Notifications {

    const val CHANNEL_ID = "updates"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ctx = LocaleHelper.wrap(context)
            val mgr = ctx.getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    ctx.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = ctx.getString(R.string.notif_channel_description) }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun showUpdates(context: Context, count: Int, sample: String) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val ctx = LocaleHelper.wrap(context)
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(ctx.getString(R.string.notif_title, count))
            .setContentText(sample)
            .setStyle(NotificationCompat.BigTextStyle().bigText(sample))
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(ctx).notify(1001, n)
        }
    }
}
