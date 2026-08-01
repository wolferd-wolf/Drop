package com.wolferdwolf.drop.share

import android.content.Intent

object SharedTextParser {
    const val MAX_SHARED_TEXT_LENGTH = 100_000

    fun parse(intent: Intent?): String? = parse(
        action = intent?.action,
        mimeType = intent?.type,
        text = intent?.getStringExtra(Intent.EXTRA_TEXT)
    )

    fun parse(action: String?, mimeType: String?, text: String?): String? {
        if (action != Intent.ACTION_SEND || mimeType != "text/plain") return null
        val cleaned = text?.trim().orEmpty()
        if (cleaned.isBlank()) return null
        return cleaned.take(MAX_SHARED_TEXT_LENGTH)
    }
}
