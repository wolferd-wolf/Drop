package com.wolferdwolf.drop.reminder

object ReminderRestorePlanner {
    fun futureRecords(
        records: List<ReminderRecord>,
        nowMillis: Long
    ): List<ReminderRecord> = records
        .asSequence()
        .filter { it.triggerAtMillis > nowMillis }
        .distinctBy { it.id }
        .sortedBy(ReminderRecord::triggerAtMillis)
        .toList()
}
