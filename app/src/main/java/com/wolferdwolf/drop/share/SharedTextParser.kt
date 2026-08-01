package com.wolferdwolf.drop.share

import android.content.Intent

object SharedTextParser {
    const val MAX_SHARED_TEXT_LENGTH = 100_000

    fun parse(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        val raw = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (raw.isBlank()) return null
        return raw.take(MAX_SHARED_TEXT_LENGTH)
    }
}
