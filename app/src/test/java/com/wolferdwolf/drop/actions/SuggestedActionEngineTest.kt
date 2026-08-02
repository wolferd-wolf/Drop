package com.wolferdwolf.drop.actions

import com.wolferdwolf.drop.extraction.ExtractionResult
import com.wolferdwolf.drop.extraction.ExtractionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedActionEngineTest {
    @Test
    fun richContentReturnsOnlyTheHighestRankedActionsPlusOneManualChoice() {
        val text = "Apply before 12 August 2026 at 5:30 PM. Email jobs@example.com or visit https://example.com/jobs"
        val results = listOf(
            result(ExtractionType.DATE, "12 August 2026"),
            result(ExtractionType.TIME, "5:30 PM"),
            result(ExtractionType.EMAIL, "jobs@example.com"),
            result(ExtractionType.URL, "https://example.com/jobs")
        )

        val actions = SuggestedActionEngine.suggest(text, results)
        val types = actions.map { it.type }

        assertEquals(5, actions.size)
        assertEquals(SuggestedActionType.SAVE_REFERENCE, types[0])
        assertEquals(SuggestedActionType.REMINDER, types[1])
        assertEquals(SuggestedActionType.CALENDAR, types[2])
        assertEquals(SuggestedActionType.CONTACT, types[3])
        assertEquals(1, actions.count { it.reason.startsWith("Manual choice:") })
        assertEquals(actions.size, types.distinct().size)
    }

    @Test
    fun missingDataSuppressesActionsThatRequireDetectedValues() {
        val actions = SuggestedActionEngine.suggest("Keep this note for later", emptyList())
        val types = actions.map { it.type }

        assertEquals(2, actions.size)
        assertTrue(SuggestedActionType.SAVE_REFERENCE in types)
        assertTrue(SuggestedActionType.CHECKLIST in types)
        assertFalse(SuggestedActionType.CALL in types)
        assertFalse(SuggestedActionType.EMAIL in types)
        assertFalse(SuggestedActionType.OPEN_LINK in types)
        assertFalse(SuggestedActionType.CONTACT in types)
        assertTrue(actions.last().reason.startsWith("Manual choice:"))
    }

    @Test
    fun detectedPhoneUnlocksContactAndCallButRankingRemainsFocused() {
        val actions = SuggestedActionEngine.suggest(
            "Call support at +91 98765 43210",
            listOf(result(ExtractionType.PHONE, "+91 98765 43210"))
        )

        assertTrue(actions.any { it.type == SuggestedActionType.CONTACT })
        assertTrue(actions.any { it.type == SuggestedActionType.CALL })
        assertTrue(actions.size <= 5)
        assertEquals(actions.size, actions.map { it.type }.distinct().size)
    }

    @Test
    fun listLikeTextRanksChecklistAndStillOffersOneDifferentManualChoice() {
        val text = "- Aadhaar copy\n- Resume\n- Passport photo\n- Certificates"
        val actions = SuggestedActionEngine.suggest(text, emptyList())
        val checklist = actions.first { it.type == SuggestedActionType.CHECKLIST }

        assertTrue(checklist.priority > 40)
        assertTrue(checklist.reason.contains("list-like"))
        assertEquals(1, actions.count { it.reason.startsWith("Manual choice:") })
        assertFalse(actions.last().type == SuggestedActionType.CHECKLIST)
    }

    private fun result(type: ExtractionType, value: String) = ExtractionResult(
        type = type,
        value = value,
        sourceStart = 0,
        sourceEndExclusive = value.length,
        confidence = 0.95f
    )
}
