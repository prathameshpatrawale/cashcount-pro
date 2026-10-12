package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.ui.components.ReceiptPreviewModal
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.NewCountScreen
import com.example.ui.screens.QuickCountScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CashCountViewModel
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    DASHBOARD("CashCount Pro"),
    NEW_COUNT("New Cash Count"),
    QUICK_COUNT("Quick Count Mode"),
    HISTORY("Cash History"),
    REPORTS("Reports & Analytics"),
    SETTINGS("Settings & Admin")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CashCountViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()

            val isDarkTheme = when (settings?.isDarkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                CashCountApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashCountApp(viewModel: CashCountViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    val currency by viewModel.currentCurrency.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Receipt Modal State
    var viewingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingItems by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }

    // Listen to UI Toast/Snackbar Events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Hardware / System Back Button Handler
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        currentScreen = AppScreen.DASHBOARD
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentScreen.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Quick Report Button
                    IconButton(
                        onClick = { currentScreen = AppScreen.REPORTS },
                        modifier = Modifier.testTag("topbar_btn_reports")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Reports",
                            tint = if (currentScreen == AppScreen.REPORTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { currentScreen = AppScreen.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_item_dashboard")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.NEW_COUNT,
                    onClick = { currentScreen = AppScreen.NEW_COUNT },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = "New Count") },
                    label = { Text("Count") },
                    modifier = Modifier.testTag("nav_item_new_count")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.QUICK_COUNT,
                    onClick = { currentScreen = AppScreen.QUICK_COUNT },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = "Quick Count") },
                    label = { Text("Quick") },
                    modifier = Modifier.testTag("nav_item_quick_count")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.HISTORY,
                    onClick = { currentScreen = AppScreen.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_item_history")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = { currentScreen = AppScreen.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    currency = currency,
                    transactions = allTransactions,
                    onNavigateNewCount = { currentScreen = AppScreen.NEW_COUNT },
                    onNavigateQuickCount = { currentScreen = AppScreen.QUICK_COUNT },
                    onNavigateHistory = { currentScreen = AppScreen.HISTORY },
                    onNavigateReports = { currentScreen = AppScreen.REPORTS },
                    onViewTransaction = { tx ->
                        scope.launch {
                            val items = viewModel.getTransactionItems(tx.id)
                            viewingTransaction = tx
                            viewingItems = items
                        }
                    },
                    onEditTransaction = { tx ->
                        viewModel.loadTransactionForEdit(tx.id) {
                            currentScreen = AppScreen.NEW_COUNT
                        }
                    },
                    onDuplicateTransaction = { tx ->
                        viewModel.duplicateTransaction(tx.id) {
                            currentScreen = AppScreen.NEW_COUNT
                        }
                    }
                )

                AppScreen.NEW_COUNT -> NewCountScreen(
                    viewModel = viewModel,
                    currency = currency,
                    onCountSaved = { newId ->
                        scope.launch {
                            val tx = allTransactions.find { it.id == newId }
                            if (tx != null) {
                                val items = viewModel.getTransactionItems(tx.id)
                                viewingTransaction = tx
                                viewingItems = items
                            } else {
                                currentScreen = AppScreen.HISTORY
                            }
                        }
                    }
                )

                AppScreen.QUICK_COUNT -> QuickCountScreen(
                    viewModel = viewModel,
                    currency = currency,
                    onCountSaved = { newId ->
                        scope.launch {
                            val tx = allTransactions.find { it.id == newId }
                            if (tx != null) {
                                val items = viewModel.getTransactionItems(tx.id)
                                viewingTransaction = tx
                                viewingItems = items
                            } else {
                                currentScreen = AppScreen.HISTORY
                            }
                        }
                    }
                )

                AppScreen.HISTORY -> HistoryScreen(
                    viewModel = viewModel,
                    currency = currency,
                    onViewTransaction = { tx ->
                        scope.launch {
                            val items = viewModel.getTransactionItems(tx.id)
                            viewingTransaction = tx
                            viewingItems = items
                        }
                    },
                    onEditTransaction = { tx ->
                        viewModel.loadTransactionForEdit(tx.id) {
                            currentScreen = AppScreen.NEW_COUNT
                        }
                    },
                    onDuplicateTransaction = { tx ->
                        viewModel.duplicateTransaction(tx.id) {
                            currentScreen = AppScreen.NEW_COUNT
                        }
                    }
                )

                AppScreen.REPORTS -> ReportsScreen(
                    viewModel = viewModel,
                    currency = currency
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    currency = currency
                )
            }
        }
    }

    // Modal: Receipt Statement View
    if (viewingTransaction != null) {
        ReceiptPreviewModal(
            transaction = viewingTransaction!!,
            items = viewingItems,
            currency = currency,
            settings = settings,
            onDismiss = { viewingTransaction = null }
        )
    }
}
