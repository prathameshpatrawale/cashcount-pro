package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.LocationEntity
import com.example.data.model.Currency
import com.example.ui.components.CashSummaryCard
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.DenominationRow
import com.example.ui.viewmodel.CashCountViewModel
import com.example.util.DateUtils

@Composable
fun NewCountScreen(
    viewModel: CashCountViewModel,
    currency: Currency,
    onCountSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDenominations by viewModel.activeDenominations.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val openingCash by viewModel.openingCashInput.collectAsState()
    val expectedCash by viewModel.expectedCashInput.collectAsState()
    val notes by viewModel.countNotes.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val activeLocations by viewModel.activeLocations.collectAsState()
    val editingId by viewModel.editingTransactionId.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var locationMenuExpanded by remember { mutableStateOf(false) }

    val isEditing = editingId != null

    Scaffold(
        bottomBar = {
            CashSummaryCard(
                summary = summary,
                currency = currency,
                onSave = {
                    viewModel.saveTransaction { newId ->
                        onCountSaved(newId)
                    }
                },
                onReset = { showResetDialog = true },
                onUndo = { viewModel.undo() },
                isEditing = isEditing,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Context / Metadata Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = DateUtils.formatDate(System.currentTimeMillis()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = DateUtils.formatTime(System.currentTimeMillis()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Location Selector
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { locationMenuExpanded = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Till: ${selectedLocation?.name ?: "Main Register"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Change location")
                            }

                            DropdownMenu(
                                expanded = locationMenuExpanded,
                                onDismissRequest = { locationMenuExpanded = false }
                            ) {
                                activeLocations.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc.name) },
                                        onClick = {
                                            viewModel.setLocation(loc)
                                            locationMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Opening Cash & Expected Cash Fields
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = openingCash,
                        onValueChange = { viewModel.onOpeningCashChanged(it) },
                        label = { Text("Opening Cash (${currency.symbol})") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_opening_cash"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = expectedCash,
                        onValueChange = { viewModel.onExpectedCashChanged(it) },
                        label = { Text("Expected Cash (${currency.symbol})") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_expected_cash"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            // Denomination Table Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DENOMINATIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "QUANTITY & AMOUNT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Active Denominations Rows
            items(activeDenominations.filter { it.active }) { entry ->
                DenominationRow(
                    entry = entry,
                    currency = currency,
                    onQuantityChanged = { viewModel.setQuantity(entry.value, it) },
                    onIncrement = { viewModel.incrementQuantity(entry.value, 1) },
                    onDecrement = { viewModel.decrementQuantity(entry.value, 1) }
                )
            }

            // Optional Remarks / Notes field
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("Remarks / Shift Notes (Optional)") },
                    placeholder = { Text("e.g. Closing register count, petty cash reconciliation") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_count_notes"),
                    minLines = 2,
                    maxLines = 3
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showResetDialog) {
        ConfirmationDialog(
            title = "Clear Current Count?",
            message = "This will reset all entered denomination quantities to 0. Are you sure?",
            confirmButtonText = "Clear",
            isDestructive = true,
            onConfirm = {
                viewModel.resetCount()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}
