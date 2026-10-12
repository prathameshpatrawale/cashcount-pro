package com.example

import com.example.data.model.CountStatus
import com.example.data.model.Currency
import com.example.data.model.DenominationEntry
import com.example.data.model.DenominationType
import com.example.engine.DenominationEngine
import com.example.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class DenominationEngineTest {

    @Test
    fun testIndividualDenominationCalculations() {
        // 500 * 10 = 5000
        assertEquals(5000.0, DenominationEngine.calculateItemAmount(500.0, 10), 0.001)

        // 200 * 5 = 1000
        assertEquals(1000.0, DenominationEngine.calculateItemAmount(200.0, 5), 0.001)

        // 100 * 10 = 1000
        assertEquals(1000.0, DenominationEngine.calculateItemAmount(100.0, 10), 0.001)

        // Zero quantity
        assertEquals(0.0, DenominationEngine.calculateItemAmount(500.0, 0), 0.001)

        // Negative quantity
        assertEquals(0.0, DenominationEngine.calculateItemAmount(500.0, -5), 0.001)

        // Decimal coin 0.50 * 20 = 10.00
        assertEquals(10.0, DenominationEngine.calculateItemAmount(0.50, 20), 0.001)
    }

    @Test
    fun testCashCountSummaryCriticalCalculation() {
        val entries = listOf(
            DenominationEntry(value = 500.0, type = DenominationType.NOTE, quantity = 10), // 5000
            DenominationEntry(value = 200.0, type = DenominationType.NOTE, quantity = 5),  // 1000
            DenominationEntry(value = 100.0, type = DenominationType.NOTE, quantity = 10), // 1000
            DenominationEntry(value = 10.0, type = DenominationType.COIN, quantity = 5)    // 50
        )

        // Total cash = 5000 + 1000 + 1000 + 50 = 7050
        // Expected cash = 7000
        val summary = DenominationEngine.calculateSummary(
            entries = entries,
            openingCash = 500.0,
            expectedCash = 7000.0
        )

        assertEquals(25, summary.totalNotes)
        assertEquals(5, summary.totalCoins)
        assertEquals(30, summary.totalQuantity)
        assertEquals(7050.0, summary.totalCash, 0.001)
        assertEquals(50.0, summary.difference, 0.001)
        assertEquals(CountStatus.EXCESS, summary.status)
    }

    @Test
    fun testBalancedAndShortageStatus() {
        val balancedEntries = listOf(
            DenominationEntry(value = 500.0, type = DenominationType.NOTE, quantity = 10)
        )
        val balancedSummary = DenominationEngine.calculateSummary(balancedEntries, expectedCash = 5000.0)
        assertEquals(0.0, balancedSummary.difference, 0.001)
        assertEquals(CountStatus.BALANCED, balancedSummary.status)

        val shortageEntries = listOf(
            DenominationEntry(value = 500.0, type = DenominationType.NOTE, quantity = 8) // 4000
        )
        val shortageSummary = DenominationEngine.calculateSummary(shortageEntries, expectedCash = 4200.0)
        assertEquals(-200.0, shortageSummary.difference, 0.001)
        assertEquals(CountStatus.SHORTAGE, shortageSummary.status)
    }

    @Test
    fun testInactiveDenominationExcluded() {
        val entries = listOf(
            DenominationEntry(value = 500.0, type = DenominationType.NOTE, active = true, quantity = 2), // 1000
            DenominationEntry(value = 2000.0, type = DenominationType.NOTE, active = false, quantity = 10) // disabled
        )
        val summary = DenominationEngine.calculateSummary(entries, expectedCash = 1000.0)
        assertEquals(1000.0, summary.totalCash, 0.001)
        assertEquals(2, summary.totalNotes)
    }

    @Test
    fun testCurrencyFormatting() {
        val inrFormatted = CurrencyFormatter.format(5000.0, Currency.INR)
        assert(inrFormatted.startsWith("₹"))

        val usdFormatted = CurrencyFormatter.format(1250.50, Currency.USD)
        assert(usdFormatted.startsWith("$"))

        val eurFormatted = CurrencyFormatter.format(300.0, Currency.EUR)
        assert(eurFormatted.startsWith("€"))
    }
}
