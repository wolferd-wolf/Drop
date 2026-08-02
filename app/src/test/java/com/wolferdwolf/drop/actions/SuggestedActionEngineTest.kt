package com.wolferdwolf.drop.actions

import com.wolferdwolf.drop.extraction.ExtractionResult
import com.wolferdwolf.drop.extraction.ExtractionType
import org.junit.Assert.assertEquals
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
    fun missingDataSuppressesUnsafeActionsButKeepsSafeManualChoices() {
        val actions = SuggestedActionEngine.suggest("Keep this note for later", emptyList())
        val types = actions.map { it.type }

        assertTrue(SuggestedActionType.SAVE_REFERENCE in types)
        assertTrue(SuggestedActionType.REMINDER in types)
        assertTrue(SuggestedActionType.CALENDAR in types)
        assertTrue(SuggestedActionType.CHECKLIST in types)
        assertTrue(SuggestedActionType.MAPS in types)
        assertFalse(SuggestedActionType.CALL in types)
        assertFalse(SuggestedActionType.EMAIL in types)
        assertFalse(SuggestedActionType.OPEN_LINK in types)
        assertFalse(SuggestedActionType.CONTACT in types)
        assertTrue(actions.filter { it.priority == 40 }.all { it.reason.startsWith("Manual choice:") })
    }

    @Test
    fun detectedPhoneUnlocksContactAndCallWithoutDuplicates() {
        val actions = SuggestedActionEngine.suggest(
            "Call support at +91 98765 43210",
            listOf(result(ExtractionType.PHONE, "+91 98765 43210"))
        )

        assertTrue(actions.any { it.type == SuggestedActionType.CONTACT })
        assertTrue(actions.any { it.type == SuggestedActionType.CALL })
        assertEquals(actions.size, actions.map { it.type }.distinct().size)
    }

    @Test
    fun listLikeTextRanksChecklistAboveManualChoices() {
        val text = "- Aadhaar copy\n- Resume\n- Passport photo\n- Certificates"
        val actions = SuggestedActionEngine.suggest(text, emptyList())
        val checklist = actions.first { it.type == SuggestedActionType.CHECKLIST }

        assertTrue(checklist.priority > 40)
        assertTrue(checklist.reason.contains("list-like"))
    }

    private fun result(type: ExtractionType, value: String) = ExtractionResult(
        type = type,
        value = value,
        sourceStart = 0,
        sourceEndExclusive = value.length,
        confidence = 0.95f
    )
}
