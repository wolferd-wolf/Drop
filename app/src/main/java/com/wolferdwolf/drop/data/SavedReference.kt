package com.wolferdwolf.drop.data

data class SavedReference(
    val id: Long,
    val title: String,
    val originalText: String,
    val createdAtEpochMillis: Long
)
