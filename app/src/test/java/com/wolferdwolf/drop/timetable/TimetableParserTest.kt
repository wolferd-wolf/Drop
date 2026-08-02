package com.wolferdwolf.drop.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableParserTest {
    @Test
    fun detectsHandwrittenStyleTimetableText() {
        val result = TimetableParser.parse(
            """
            Highschool Girls Plus
            9.00 Vadapada
            9:05 Prayer
            10-00 Class
            10.40 Break
            """.trimIndent()
        )

        requireNotNull(result)
        assertEquals("Highschool Girls Plus", result.title)
        assertEquals(listOf("09:00", "09:05", "10:00", "10:40"), result.entries.map { it.time })
        assertEquals("Vadapada", result.entries.first().label)
    }

    @Test
    fun rejectsGenericTextWithTooFewTimes() {
        assertNull(TimetableParser.parse("Meeting tomorrow at 10:30"))
    }

    @Test
    fun removesDuplicateRows() {
        val result = TimetableParser.parse("9:00 Start\n9.00 Start\n10:00 Work\n11:00 End")
        requireNotNull(result)
        assertEquals(3, result.entries.size)
        assertTrue(result.entries.any { it.time == "09:00" })
    }
}
