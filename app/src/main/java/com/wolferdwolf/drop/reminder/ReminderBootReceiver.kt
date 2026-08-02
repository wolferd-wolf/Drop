package com.wolferdwolf.drop.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        try {
            val scheduler = ReminderScheduler(context.applicationContext)
            val records = ReminderRestorePlanner.futureRecords(
                records = ReminderHistoryStore(context.applicationContext).load(),
                nowMillis = System.currentTimeMillis()
            )
            records.forEach { scheduler.schedule(it) }
        } finally {
            pendingResult.finish()
        }
    }
}
