package com.example.smartstudent

import com.example.smartstudent.domain.model.DefaultCategoryRules
import com.example.smartstudent.util.parseAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoreLogicTest {
    @Test fun parsesSouthAfricanAndUsFormats() {
        assertEquals(1500.5, parseAmount("1 500,50")!!, 0.001)
        assertEquals(1500.5, parseAmount("1,500.50")!!, 0.001)
        assertEquals(20.0, parseAmount("20")!!, 0.001)
    }

    @Test fun rejectsInvalidAmounts() {
        listOf("", "0", "-5", "NaN", "Infinity", "abc").forEach { assertNull(it, parseAmount(it)) }
    }

    @Test fun categoryRulesUseWordBoundaries() {
        assertEquals("Rent", DefaultCategoryRules.suggest("Rent June"))
        assertNull(DefaultCategoryRules.suggest("Current account fee"))
        assertNull(DefaultCategoryRules.suggest("Spare parts"))
        assertEquals("Dining", DefaultCategoryRules.suggest("Uber Eats order"))
        assertEquals("Transport", DefaultCategoryRules.suggest("Uber to campus"))
    }
}
