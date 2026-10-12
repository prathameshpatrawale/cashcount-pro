package com.example.util

import com.example.data.model.Currency
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(amount: Double, currency: Currency): String {
        return "${currency.symbol}${formatAmountOnly(amount, currency)}"
    }

    fun formatAmountOnly(amount: Double, currency: Currency): String {
        val isWhole = amount % 1.0 == 0.0
        val formatter = when (currency) {
            Currency.INR -> {
                // Indian numbering format (e.g. 1,00,000)
                val nf = NumberFormat.getNumberInstance(Locale("en", "IN"))
                nf.minimumFractionDigits = if (isWhole) 0 else 2
                nf.maximumFractionDigits = 2
                nf
            }
            Currency.USD -> {
                val nf = NumberFormat.getNumberInstance(Locale.US)
                nf.minimumFractionDigits = if (isWhole) 0 else 2
                nf.maximumFractionDigits = 2
                nf
            }
            Currency.EUR -> {
                val nf = NumberFormat.getNumberInstance(Locale.GERMANY)
                nf.minimumFractionDigits = if (isWhole) 0 else 2
                nf.maximumFractionDigits = 2
                nf
            }
            Currency.GBP -> {
                val nf = NumberFormat.getNumberInstance(Locale.UK)
                nf.minimumFractionDigits = if (isWhole) 0 else 2
                nf.maximumFractionDigits = 2
                nf
            }
            Currency.AED -> {
                val nf = NumberFormat.getNumberInstance(Locale("ar", "AE"))
                nf.minimumFractionDigits = if (isWhole) 0 else 2
                nf.maximumFractionDigits = 2
                nf
            }
        }
        return formatter.format(amount)
    }

    fun formatDenominationOnly(value: Double, currency: Currency): String {
        val isWhole = value % 1.0 == 0.0
        return if (isWhole) {
            "${currency.symbol}${value.toInt()}"
        } else {
            "${currency.symbol}${String.format(Locale.US, "%.2f", value)}"
        }
    }
}
