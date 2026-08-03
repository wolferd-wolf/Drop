package com.wolferdwolf.drop.pdf

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfPageOcrProcessorTest {
    @Test
    fun neverAttemptsMoreThanThreePages() {
        assertEquals(3, PdfPageOcrProcessor.boundedPageCount(25))
    }

    @Test
    fun usesAllPagesWhenDocumentIsWithinLimit() {
        assertEquals(2, PdfPageOcrProcessor.boundedPageCount(2))
    }

    @Test
    fun handlesEmptyOrInvalidPageCountsSafely() {
        assertEquals(0, PdfPageOcrProcessor.boundedPageCount(0))
        assertEquals(0, PdfPageOcrProcessor.boundedPageCount(-4))
    }
}
