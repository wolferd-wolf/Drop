package com.wolferdwolf.drop.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class ReminderScheduler(private val context: Context) {
    fun schedule(reminder: ReminderValidator.ValidReminder): Result<Unit> = schedule(
        title = reminder.title,
        notes = reminder.notes,
        triggerAtMillis = reminder.triggerAtMillis
    )

    fun schedule(record: ReminderRecord): Result<Unit> = schedule(
        title = record.title,
        notes = record.notes,
        triggerAtMillis = record.triggerAtMillis
    )

    fun cancel(record: ReminderRecord): Result<Unit> = runCatching {
        val requestCode = requestCode(record.title, record.triggerAtMillis)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            receiverIntent(record.title, record.notes, requestCode),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager().cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun schedule(
        title: String,
        notes: String,
        triggerAtMillis: Long
    ): Result<Unit> = runCatching {
        val requestCode = requestCode(title, triggerAtMillis)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            receiverIntent(title, notes, requestCode),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager().setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private fun receiverIntent(title: String, notes: String, requestCode: Int) =
        Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_NOTES, notes)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
        }

    private fun alarmManager(): AlarmManager =
        context.getSystemService(AlarmManager::class.java)
            ?: error("Alarm service is unavailable")

    internal companion object {
        fun requestCode(title: String, triggerAtMillis: Long): Int =
            (triggerAtMillis xor title.hashCode().toLong()).toInt()
    }
}
