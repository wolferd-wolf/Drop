package com.wolferdwolf.drop.actions

import com.wolferdwolf.drop.extraction.ExtractionResult
import com.wolferdwolf.drop.extraction.ExtractionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedActionEngineTest {
    @Test
    fun jobDeadlineSuppressesFalseCalendarAndKeepsApplicationLinkVisible() {
        val text = "Job vacancy. Apply before 12 August 2026 at 5:30 PM. Email jobs@example.com or visit https://example.com/jobs"
        val results = listOf(
            result(ExtractionType.DATE, "12 August 2026"),
            result(ExtractionType.TIME, "5:30 PM"),
            result(ExtractionType.EMAIL, "jobs@example.com"),
            result(ExtractionType.URL, "https://example.com/jobs")
        )

        val actions = SuggestedActionEngine.suggest(text, results)
        val types = actions.map { it.type }

        assertEquals(4, actions.size)
        assertEquals(SuggestedActionType.REMINDER, types[0])
        assertEquals(SuggestedActionType.OPEN_LINK, types[1])
        assertEquals(SuggestedActionType.CONTACT, types[2])
        assertEquals(SuggestedActionType.SAVE_REFERENCE, types[3])
        assertFalse(SuggestedActionType.CALENDAR in types)
        assertTrue(actions.first { it.type == SuggestedActionType.REMINDER }.reason.contains("deadline", true))
        assertTrue(actions.first { it.type == SuggestedActionType.SAVE_REFERENCE }.reason.contains("job post", true))
        assertTrue(actions.first { it.type == SuggestedActionType.OPEN_LINK }.reason.contains("application", true))
        assertEquals(actions.size, types.distinct().size)
    }

    @Test
    fun receiptUsesCategorySpecificReasonAndSuppressesChecklistNoise() {
        val text = "RECEIPT\n1. Rice ₹500\n2. Oil ₹250\n3. Soap ₹75\nSubtotal ₹825\nGST ₹41\nTotal ₹866"
        val results = listOf(
            result(ExtractionType.PRICE, "₹500"),
            result(ExtractionType.PRICE, "₹250"),
            result(ExtractionType.PRICE, "₹75"),
            result(ExtractionType.PRICE, "₹866")
        )

        val actions = SuggestedActionEngine.suggest(text, results)

        assertEquals(listOf(SuggestedActionType.SAVE_REFERENCE), actions.map { it.type })
        assertTrue(actions.single().reason.contains("receipt", true))
    }

    @Test
    fun realEventWithDateAndTimeStillOffersCalendar() {
        val text = "Product launch meeting on 12 August 2026 at 5:30 PM"
        val results = listOf(
            result(ExtractionType.DATE, "12 August 2026"),
            result(ExtractionType.TIME, "5:30 PM")
        )

        val types = SuggestedActionEngine.suggest(text, results).map { it.type }
        assertTrue(SuggestedActionType.REMINDER in types)
        assertTrue(SuggestedActionType.CALENDAR in types)
    }

    @Test
    fun eventCategoryWithoutDateDoesNotSuggestCalendar() {
        val text = "Team meeting in Wolf Hall. Agenda: launch review."
        val actions = SuggestedActionEngine.suggest(text, emptyList())
        val types = actions.map { it.type }

        assertTrue(SuggestedActionType.SAVE_REFERENCE in types)
        assertFalse(SuggestedActionType.CALENDAR in types)
        assertFalse(SuggestedActionType.REMINDER in types)
    }

    @Test
    fun timeOnlyEventDoesNotSuggestReminderOrCalendarWithoutDate() {
        val text = "Team meeting at 5:30 PM in Wolf Hall"
        val results = listOf(result(ExtractionType.TIME, "5:30 PM"))
        val types = SuggestedActionEngine.suggest(text, results).map { it.type }

        assertTrue(SuggestedActionType.SAVE_REFERENCE in types)
        assertFalse(SuggestedActionType.REMINDER in types)
        assertFalse(SuggestedActionType.CALENDAR in types)
    }

    @Test
    fun deadlineLanguageWithoutDateDoesNotCreateAnUnusableReminderSuggestion() {
        val text = "Application deadline soon. Submit before the office closes."
        val types = SuggestedActionEngine.suggest(text, emptyList()).map { it.type }

        assertTrue(SuggestedActionType.SAVE_REFERENCE in types)
        assertFalse(SuggestedActionType.REMINDER in types)
    }

    @Test
    fun missingDataSuppressesDependentSuggestedActions() {
        val actions = SuggestedActionEngine.suggest("Keep this note for later", emptyList())
        val types = actions.map { it.type }

        assertEquals(listOf(SuggestedActionType.SAVE_REFERENCE), types)
        assertFalse(SuggestedActionType.CALL in types)
        assertFalse(SuggestedActionType.EMAIL in types)
        assertFalse(SuggestedActionType.OPEN_LINK in types)
        assertFalse(SuggestedActionType.CONTACT in types)
    }

    @Test
    fun manualChooserAlwaysOffersActionsWhoseFormsCanCollectMissingData() {
        val actions = SuggestedActionEngine.manualActions(emptyList())
        val types = actions.map { it.type }

        assertTrue(SuggestedActionType.SAVE_REFERENCE in types)
        assertTrue(SuggestedActionType.REMINDER in types)
        assertTrue(SuggestedActionType.CALENDAR in types)
        assertTrue(SuggestedActionType.CHECKLIST in types)
        assertTrue(SuggestedActionType.CONTACT in types)
        assertTrue(SuggestedActionType.MAPS in types)
        assertTrue(SuggestedActionType.EMAIL in types)
        assertFalse(SuggestedActionType.OPEN_LINK in types)
        assertFalse(SuggestedActionType.CALL in types)
    }

    @Test
    fun manualChooserUnlocksDetectedLinkAndCallWithoutDuplicates() {
        val results = listOf(
            result(ExtractionType.PHONE, "+91 98765 43210"),
            result(ExtractionType.EMAIL, "team@example.com"),
            result(ExtractionType.URL, "https://example.com")
        )
        val types = SuggestedActionEngine.manualActions(results).map { it.type }

        assertTrue(SuggestedActionType.CONTACT in types)
        assertTrue(SuggestedActionType.OPEN_LINK in types)
        assertTrue(SuggestedActionType.EMAIL in types)
        assertTrue(SuggestedActionType.CALL in types)
        assertEquals(types.size, types.distinct().size)
    }

    @Test
    fun listLikeTextRanksChecklistWithoutAddingGenericManualCard() {
        val text = "- Aadhaar copy\n- Resume\n- Passport photo\n- Certificates"
        val actions = SuggestedActionEngine.suggest(text, emptyList())
        val checklist = actions.first { it.type == SuggestedActionType.CHECKLIST }

        assertTrue(checklist.priority > 0)
        assertTrue(checklist.reason.contains("list-like"))
        assertFalse(actions.any { it.reason.startsWith("Manual choice:") })
    }

    private fun result(type: ExtractionType, value: String) = ExtractionResult(
        type = type,
        value = value,
        sourceStart = 0,
        sourceEndExclusive = value.length,
        confidence = 0.95f
    )
}
