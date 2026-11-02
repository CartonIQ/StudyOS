package com.cartoniq.studyos

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object Reminders {
    private const val CH = "study_reminders"

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 1, Intent(ctx, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Inexact daily alarm: no exact-alarm permission needed, may arrive a few minutes late. */
    fun schedule(ctx: Context, hour: Int, minute: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(ctx))
    }

    fun cancel(ctx: Context) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(ctx))
    }

    suspend fun notify(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val dao = Db.get(ctx).dao()
        val endOfToday = dayStart() + 86_400_000L
        val tasks = dao.allTasks().count { !it.done && it.dueAt != null && it.dueAt < endOfToday }
        val cards = dao.allCards().count { it.dueAt <= System.currentTimeMillis() }
        val text = if (tasks + cards == 0) "Ready for a focus session?"
        else "$tasks task${if (tasks == 1) "" else "s"} due and $cards card${if (cards == 1) "" else "s"} to review."
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CH, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(1, NotificationCompat.Builder(ctx, CH).setSmallIcon(R.drawable.ic_notif).setContentTitle("Time to study")
            .setContentText(text).setContentIntent(open).setAutoCancel(true).build())
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val p = ctx.store.data.first()
                if (p[K_REM_ON] == true) {
                    Reminders.schedule(ctx, p[K_REM_H] ?: 18, p[K_REM_M] ?: 0)
                    if (intent.action != Intent.ACTION_BOOT_COMPLETED) Reminders.notify(ctx)
                }
            } finally { pending.finish() }
        }
    }
}
