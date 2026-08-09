package com.wolferdwolf.drop.checklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistEditorTest {
    @Test
    fun sourceLinesBecomeCleanEditableItems() {
        val items = ChecklistEditor.fromSource("- Milk\n• Eggs\n3. Call supplier")
        assertEquals(listOf("Milk", "Eggs", "Call supplier"), items.map { it.text })
        assertTrue(items.none { it.checked })
    }

    @Test
    fun importedTaskMarkersPreserveCompletionWithoutPollutingItemText() {
        val items = ChecklistEditor.fromSource(
            "- [x] Paid electricity bill\n[ ] Buy milk\n☒ Packed charger\n☑ Sent invoice\n☐ Call supplier"
        )

        assertEquals(
            listOf("Paid electricity bill", "Buy milk", "Packed charger", "Sent invoice", "Call supplier"),
            items.map { it.text }
        )
        assertEquals(listOf(true, false, true, true, false), items.map { it.checked })
    }

    @Test
    fun itemOperationsPreserveOrderAndCompletion() {
        var items = ChecklistEditor.fromSource("Milk\nEggs\nBread")
        items = ChecklistEditor.edit(items, 0, "Oat milk")
        items = ChecklistEditor.toggle(items, 1)
        items = ChecklistEditor.move(items, 2, -1)
        items = ChecklistEditor.add(items, "Dish soap")
        items = ChecklistEditor.delete(items, 0)

        assertEquals(listOf("Bread", "Eggs", "Dish soap"), items.map { it.text })
        assertTrue(items[1].checked)
        assertEquals("☐ Bread\n☒ Eggs\n☐ Dish soap", ChecklistEditor.serializeForSave(items))
    }

    @Test
    fun blankItemsCannotCreateAnEmptyChecklist() {
        val items = ChecklistEditor.fromSource("   ")
        assertFalse(ChecklistEditor.hasSavableItems(items))
        assertEquals(items, ChecklistEditor.add(items, "   "))
        assertEquals("", ChecklistEditor.serializeForSave(items))
    }

    @Test
    fun encodedStateRoundTripsAcrossActivityRecreation() {
        val items = listOf(ChecklistItem("Milk", true), ChecklistItem("Call supplier"))
        assertEquals(items, ChecklistEditor.decode(ChecklistEditor.encode(items)))
    }
}
