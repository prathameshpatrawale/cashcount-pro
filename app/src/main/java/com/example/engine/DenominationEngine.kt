package com.example.engine

import com.example.data.model.CashCountSummary
import com.example.data.model.CountStatus
import com.example.data.model.DenominationEntry
import com.example.data.model.DenominationType
import kotlin.math.abs
import kotlin.math.roundToLong

object DenominationEngine {

    /**
     * Calculates the monetary amount for a single denomination entry:
     * amount = value * quantity
     */
    fun calculateItemAmount(value: Double, quantity: Int): Double {
        if (quantity <= 0 || value <= 0.0) return 0.0
        val unrounded = value * quantity
        // Round to 2 decimal places to avoid floating point inaccuracies
        return (unrounded * 100.0).roundToLong() / 100.0
    }

    /**
     * Aggregates a list of denomination entries into a full CashCountSummary.
     */
    fun calculateSummary(
        entries: List<DenominationEntry>,
        openingCash: Double = 0.0,
        expectedCash: Double = 0.0
    ): CashCountSummary {
        var totalNotes = 0
        var totalCoins = 0
        var totalCash = 0.0

        for (item in entries) {
            if (!item.active) continue
            val qty = item.quantity.coerceAtLeast(0)
            val amt = calculateItemAmount(item.value, qty)
            totalCash += amt

            when (item.type) {
                DenominationType.NOTE -> totalNotes += qty
                DenominationType.COIN -> totalCoins += qty
            }
        }

        // Round totalCash to 2 decimal places
        totalCash = (totalCash * 100.0).roundToLong() / 100.0

        // Difference = Actual Cash - Expected Cash
        val diff = ((totalCash - expectedCash) * 100.0).roundToLong() / 100.0

        val status = when {
            abs(diff) < 0.001 -> CountStatus.BALANCED
            diff > 0 -> CountStatus.EXCESS
            else -> CountStatus.SHORTAGE
        }

        return CashCountSummary(
            totalNotes = totalNotes,
            totalCoins = totalCoins,
            totalQuantity = totalNotes + totalCoins,
            totalCash = totalCash,
            expectedCash = expectedCash,
            openingCash = openingCash,
            difference = diff,
            status = status
        )
    }

    /**
     * Format a numerical denomination value to clean display string (e.g., 500 or 0.50).
     */
    fun formatDenominationValue(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", value)
        }
    }
}
