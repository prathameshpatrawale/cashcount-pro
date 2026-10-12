package com.example.data.model

enum class Currency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val defaultNotes: List<Double>,
    val defaultCoins: List<Double>
) {
    INR(
        code = "INR",
        symbol = "₹",
        displayName = "Indian Rupee (₹)",
        defaultNotes = listOf(500.0, 200.0, 100.0, 50.0, 20.0, 10.0),
        defaultCoins = listOf(20.0, 10.0, 5.0, 2.0, 1.0)
    ),
    USD(
        code = "USD",
        symbol = "$",
        displayName = "US Dollar ($)",
        defaultNotes = listOf(100.0, 50.0, 20.0, 10.0, 5.0, 2.0, 1.0),
        defaultCoins = listOf(1.0, 0.50, 0.25, 0.10, 0.05, 0.01)
    ),
    EUR(
        code = "EUR",
        symbol = "€",
        displayName = "Euro (€)",
        defaultNotes = listOf(500.0, 200.0, 100.0, 50.0, 20.0, 10.0, 5.0),
        defaultCoins = listOf(2.0, 1.0, 0.50, 0.20, 0.10, 0.05, 0.02, 0.01)
    ),
    GBP(
        code = "GBP",
        symbol = "£",
        displayName = "British Pound (£)",
        defaultNotes = listOf(50.0, 20.0, 10.0, 5.0),
        defaultCoins = listOf(2.0, 1.0, 0.50, 0.20, 0.10, 0.05, 0.02, 0.01)
    ),
    AED(
        code = "AED",
        symbol = "د.إ",
        displayName = "UAE Dirham (د.إ)",
        defaultNotes = listOf(1000.0, 500.0, 200.0, 100.0, 50.0, 20.0, 10.0, 5.0),
        defaultCoins = listOf(1.0, 0.50, 0.25)
    );

    companion object {
        fun fromCode(code: String): Currency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
        }
    }
}

enum class DenominationType {
    NOTE,
    COIN
}

enum class UserRole {
    ADMIN,
    MANAGER,
    CASHIER
}

enum class CountStatus {
    BALANCED,
    SHORTAGE,
    EXCESS
}

data class DenominationEntry(
    val id: Int = 0,
    val value: Double,
    val type: DenominationType,
    val active: Boolean = true,
    val quantity: Int = 0,
    val amount: Double = 0.0
)

data class CashCountSummary(
    val totalNotes: Int = 0,
    val totalCoins: Int = 0,
    val totalQuantity: Int = 0,
    val totalCash: Double = 0.0,
    val expectedCash: Double = 0.0,
    val openingCash: Double = 0.0,
    val difference: Double = 0.0,
    val status: CountStatus = CountStatus.BALANCED
)
