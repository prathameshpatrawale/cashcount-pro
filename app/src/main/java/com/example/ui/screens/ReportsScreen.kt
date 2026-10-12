package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.model.Currency
import com.example.ui.components.StatCard
import com.example.ui.viewmodel.CashCountViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.ExportHelper
import com.example.util.ReceiptPrinter

enum class ReportPeriod {
    TODAY,
    LAST_7_DAYS,
    THIS_MONTH,
    ALL_TIME
}

@Composable
fun ReportsScreen(
    viewModel: CashCountViewModel,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }

    // Filter transactions by period
    val filteredList = remember(allTransactions, selectedPeriod) {
        when (selectedPeriod) {
            ReportPeriod.TODAY -> {
                val (s, e) = DateUtils.getTodayRange()
                allTransactions.filter { it.timestamp in s..e }
            }
            ReportPeriod.LAST_7_DAYS -> {
                val (s, e) = DateUtils.getLast7DaysRange()
                allTransactions.filter { it.timestamp in s..e }
            }
            ReportPeriod.THIS_MONTH -> {
                val (s, e) = DateUtils.getThisMonthRange()
                allTransactions.filter { it.timestamp in s..e }
            }
            ReportPeriod.ALL_TIME -> allTransactions
        }
    }

    // Calculations
    val totalCash = filteredList.sumOf { it.actualCash }
    val txCount = filteredList.size
    val avgCash = if (txCount > 0) totalCash / txCount else 0.0

    val excessList = filteredList.filter { it.difference > 0 }
    val totalExcess = excessList.sumOf { it.difference }

    val shortageList = filteredList.filter { it.difference < 0 }
    val totalShortage = shortageList.sumOf { kotlin.math.abs(it.difference) }

    // Location performance
    val locationStats = remember(filteredList) {
        filteredList.groupBy { it.locationName }.map { (loc, list) ->
            Triple(loc, list.size, list.sumOf { it.actualCash })
        }.sortedByDescending { it.third }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Period Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriod.entries.forEach { period ->
                    val label = when (period) {
                        ReportPeriod.TODAY -> "Daily Report"
                        ReportPeriod.LAST_7_DAYS -> "Weekly (7 Days)"
                        ReportPeriod.THIS_MONTH -> "Monthly Report"
                        ReportPeriod.ALL_TIME -> "All Time"
                    }
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(label) },
                        modifier = Modifier.testTag("report_chip_${period.name}")
                    )
                }
            }
        }

        // Summary Metric Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Cash Counted",
                        value = CurrencyFormatter.format(totalCash, currency),
                        subtitle = "$txCount transaction(s)",
                        icon = Icons.Default.Money,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Average Per Count",
                        value = CurrencyFormatter.format(avgCash, currency),
                        subtitle = "Per register tally",
                        icon = Icons.Default.Assessment,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Excess",
                        value = CurrencyFormatter.format(totalExcess, currency),
                        subtitle = "${excessList.size} count(s) with surplus",
                        icon = Icons.Default.TrendingUp,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Total Shortage",
                        value = CurrencyFormatter.format(totalShortage, currency),
                        subtitle = "${shortageList.size} count(s) with deficit",
                        icon = Icons.Default.TrendingDown,
                        iconTint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Export Actions (CSV and Print)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val file = ExportHelper.generateTransactionsCsv(
                            context = context,
                            transactions = filteredList,
                            currency = currency,
                            title = "CashCount_Report_${selectedPeriod.name}"
                        )
                        if (file != null) {
                            ExportHelper.shareFile(context, file, "text/csv", "Export Report CSV")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_export_report_csv")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV")
                }
            }
        }

        // Register / Till Breakdown
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Register / Till Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (locationStats.isEmpty()) {
                        Text(
                            text = "No registers active in this period",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        locationStats.forEach { (loc, count, cash) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(loc, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("$count cash counts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    CurrencyFormatter.format(cash, currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
