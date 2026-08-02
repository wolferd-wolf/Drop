package com.wolferdwolf.drop.actions

import com.wolferdwolf.drop.extraction.ExtractionResult
import com.wolferdwolf.drop.extraction.ExtractionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedActionEngineTest {
    @Test
    fun jobPostSuggestsUsefulActions() {
        val text = "Apply before 12 August 2026 at 5:30 PM. Email jobs@example.com or visit https://example.com/jobs"
        val results = listOf(
            result(ExtractionType.DATE, "12 August 2026"),
            result(ExtractionType.TIME, "5:30 PM"),
            result(ExtractionType.EMAIL, "jobs@example.com"),
            result(ExtractionType.URL, "https://example.com/jobs")
        )

        val types = SuggestedActionEngine.suggest(text, results).map { it.type }

        assertTrue(SuggestedActionType.SAVE_REFERENCE in types)
        assertTrue(SuggestedActionType.REMINDER in types)
        assertTrue(SuggestedActionType.CALENDAR in types)
        assertTrue(SuggestedActionType.CONTACT in types)
        assertTrue(SuggestedActionType.OPEN_LINK in types)
        assertTrue(SuggestedActionType.EMAIL in types)
    }

    @Test
    fun missingDataSuppressesUnsafeActions() {
        val types = SuggestedActionEngine.suggest("Keep this note for later", emptyList()).map { it.type }

        assertTrue(SuggestedActionType.SAVE_REFERENCE in types)
        assertTrue(SuggestedActionType.REMINDER in types)
        assertFalse(SuggestedActionType.CALL in types)
        assertFalse(SuggestedActionType.EMAIL in types)
        assertFalse(SuggestedActionType.OPEN_LINK in types)
        assertFalse(SuggestedActionType.CONTACT in types)
    }

    @Test
    fun listLikeTextSuggestsChecklist() {
        val text = "- Aadhaar copy\n- Resume\n- Passport photo\n- Certificates"
        val types = SuggestedActionEngine.suggest(text, emptyList()).map { it.type }
        assertTrue(SuggestedActionType.CHECKLIST in types)
    }

    private fun result(type: ExtractionType, value: String) = ExtractionResult(
        type = type,
        value = value,
        sourceStart = 0,
        sourceEndExclusive = value.length,
        confidence = 0.95f
    )
}
