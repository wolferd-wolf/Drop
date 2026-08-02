package com.wolferdwolf.drop.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentProfileClassifierTest {
    @Test
    fun timetableBeatsGenericClassification() {
        val profile = DocumentProfileClassifier.classify(
            "School Day\n9:00 Prayer\n9:30 English\n10:30 Break"
        )

        assertEquals(DocumentProfile.Kind.TIMETABLE, profile.kind)
        assertTrue(profile.confidence >= 0.8f)
    }

    @Test
    fun recognisesReceiptSignals() {
        val profile = DocumentProfileClassifier.classify("Invoice\nSubtotal ₹500\nTotal ₹590")
        assertEquals(DocumentProfile.Kind.RECEIPT, profile.kind)
    }

    @Test
    fun recognisesJobPostSignals() {
        val profile = DocumentProfileClassifier.classify("Vacancy: Technician\nExperience 2 years\nApply today")
        assertEquals(DocumentProfile.Kind.JOB_POST, profile.kind)
    }
}
