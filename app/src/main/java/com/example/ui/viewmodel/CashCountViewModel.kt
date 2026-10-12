package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.DenominationConfigEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.SettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.CashCountSummary
import com.example.data.model.CountStatus
import com.example.data.model.Currency
import com.example.data.model.DenominationEntry
import com.example.data.model.DenominationType
import com.example.data.model.UserRole
import com.example.data.repository.CashCountRepository
import com.example.engine.DenominationEngine
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Stack

enum class DateFilterType {
    ALL,
    TODAY,
    YESTERDAY,
    LAST_7_DAYS,
    THIS_MONTH
}

enum class StatusFilterType {
    ALL,
    BALANCED,
    SHORTAGE,
    EXCESS
}

enum class SortOrderType {
    NEWEST,
    OLDEST,
    HIGHEST_AMOUNT,
    LOWEST_AMOUNT,
    LARGEST_DIFF
}

data class TransactionFilterState(
    val query: String = "",
    val dateFilter: DateFilterType = DateFilterType.ALL,
    val statusFilter: StatusFilterType = StatusFilterType.ALL,
    val cashierFilter: Int? = null,
    val locationFilter: Int? = null,
    val sortOrder: SortOrderType = SortOrderType.NEWEST
)

class CashCountViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CashCountRepository(AppDatabase.getInstance(application))

    // UI Message Events
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    // Persistent State
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLocations: StateFlow<List<LocationEntity>> = repository.allLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLocations: StateFlow<List<LocationEntity>> = repository.activeLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsEntity?> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Session & Configuration
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentCurrency = MutableStateFlow(Currency.INR)
    val currentCurrency: StateFlow<Currency> = _currentCurrency.asStateFlow()

    // Active Denominations List for Counting
    private val _activeDenominations = MutableStateFlow<List<DenominationEntry>>(emptyList())
    val activeDenominations: StateFlow<List<DenominationEntry>> = _activeDenominations.asStateFlow()

    // Form inputs for counting
    val openingCashInput = MutableStateFlow("0")
    val expectedCashInput = MutableStateFlow("0")
    val countNotes = MutableStateFlow("")
    val selectedLocation = MutableStateFlow<LocationEntity?>(null)
    val editingTransactionId = MutableStateFlow<Long?>(null)

    // Undo History Stack for Quantities
    private val undoStack = Stack<Map<Double, Int>>()

    // Calculated Summary for current count
    private val _summary = MutableStateFlow(CashCountSummary())
    val summary: StateFlow<CashCountSummary> = _summary.asStateFlow()

    // Filtering & Searching for History
    val searchQuery = MutableStateFlow("")
    val dateFilter = MutableStateFlow(DateFilterType.ALL)
    val statusFilter = MutableStateFlow(StatusFilterType.ALL)
    val cashierFilter = MutableStateFlow<Int?>(null)
    val locationFilter = MutableStateFlow<Int?>(null)
    val sortOrder = MutableStateFlow(SortOrderType.NEWEST)

    // Filtered Transactions Flow
    val filterState: StateFlow<TransactionFilterState> = combine(
        combine(searchQuery, dateFilter, statusFilter) { q, df, sf -> Triple(q, df, sf) },
        combine(cashierFilter, locationFilter, sortOrder) { cf, lf, so -> Triple(cf, lf, so) }
    ) { (q, df, sf), (cf, lf, so) ->
        TransactionFilterState(q, df, sf, cf, lf, so)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionFilterState())

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        filterState
    ) { txs, filter ->
        var list = txs

        // Search Query
        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            list = list.filter {
                it.transactionNumber.lowercase().contains(q) ||
                        it.userName.lowercase().contains(q) ||
                        it.locationName.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q) ||
                        it.actualCash.toString().contains(q)
            }
        }

        // Date filter
        when (filter.dateFilter) {
            DateFilterType.ALL -> {}
            DateFilterType.TODAY -> {
                val (start, end) = DateUtils.getTodayRange()
                list = list.filter { it.timestamp in start..end }
            }
            DateFilterType.YESTERDAY -> {
                val (start, end) = DateUtils.getYesterdayRange()
                list = list.filter { it.timestamp in start..end }
            }
            DateFilterType.LAST_7_DAYS -> {
                val (start, end) = DateUtils.getLast7DaysRange()
                list = list.filter { it.timestamp in start..end }
            }
            DateFilterType.THIS_MONTH -> {
                val (start, end) = DateUtils.getThisMonthRange()
                list = list.filter { it.timestamp in start..end }
            }
        }

        // Status filter
        when (filter.statusFilter) {
            StatusFilterType.ALL -> {}
            StatusFilterType.BALANCED -> list = list.filter { it.status == "BALANCED" }
            StatusFilterType.SHORTAGE -> list = list.filter { it.status == "SHORTAGE" }
            StatusFilterType.EXCESS -> list = list.filter { it.status == "EXCESS" }
        }

        // Cashier filter
        if (filter.cashierFilter != null) {
            list = list.filter { it.userId == filter.cashierFilter }
        }

        // Location filter
        if (filter.locationFilter != null) {
            list = list.filter { it.locationId == filter.locationFilter }
        }

        // Sort Order
        when (filter.sortOrder) {
            SortOrderType.NEWEST -> list.sortedByDescending { it.timestamp }
            SortOrderType.OLDEST -> list.sortedBy { it.timestamp }
            SortOrderType.HIGHEST_AMOUNT -> list.sortedByDescending { it.actualCash }
            SortOrderType.LOWEST_AMOUNT -> list.sortedBy { it.actualCash }
            SortOrderType.LARGEST_DIFF -> list.sortedByDescending { kotlin.math.abs(it.difference) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfEmpty()

            // Observe settings to sync active currency
            settings.collect { set ->
                if (set != null) {
                    val curr = Currency.fromCode(set.currencyCode)
                    if (_currentCurrency.value != curr) {
                        _currentCurrency.value = curr
                    }
                    loadDenominationsForCurrency(curr.code)
                }
            }
        }

        viewModelScope.launch {
            allUsers.collect { users ->
                if (_currentUser.value == null && users.isNotEmpty()) {
                    _currentUser.value = users.first()
                }
            }
        }

        viewModelScope.launch {
            activeLocations.collect { locs ->
                if (selectedLocation.value == null && locs.isNotEmpty()) {
                    selectedLocation.value = locs.first()
                }
            }
        }
    }

    private fun loadDenominationsForCurrency(currencyCode: String) {
        viewModelScope.launch {
            repository.ensureCurrencyDenominationsSeeded(currencyCode)
            val list = repository.getDenominationsForCurrencySync(currencyCode)
            val entries = list.map { cfg ->
                DenominationEntry(
                    id = cfg.id,
                    value = cfg.value,
                    type = if (cfg.type == "COIN") DenominationType.COIN else DenominationType.NOTE,
                    active = cfg.active,
                    quantity = 0,
                    amount = 0.0
                )
            }
            _activeDenominations.value = entries
            recalculate()
        }
    }

    fun switchCurrency(currency: Currency) {
        viewModelScope.launch {
            _currentCurrency.value = currency
            val set = settings.value ?: SettingsEntity()
            repository.updateSettings(
                set.copy(currencyCode = currency.code),
                _currentUser.value?.id ?: 1,
                _currentUser.value?.name ?: "User"
            )
            loadDenominationsForCurrency(currency.code)
            _uiEvents.emit("Currency switched to ${currency.displayName}")
        }
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
        viewModelScope.launch {
            repository.logAction(user.id, user.name, "USER_SWITCHED", "Switched active session to ${user.name}")
            _uiEvents.emit("Switched user to ${user.name} (${user.role})")
        }
    }

    fun setLocation(location: LocationEntity) {
        selectedLocation.value = location
    }

    // Calculation & Quantity Operations
    fun setQuantity(denomValue: Double, quantityStr: String) {
        val qty = quantityStr.filter { it.isDigit() }.toIntOrNull() ?: 0
        saveStateToUndoStack()
        updateDenominationQuantity(denomValue, qty.coerceIn(0, 999999))
    }

    fun incrementQuantity(denomValue: Double, step: Int = 1) {
        saveStateToUndoStack()
        val current = _activeDenominations.value.find { it.value == denomValue }?.quantity ?: 0
        updateDenominationQuantity(denomValue, (current + step).coerceIn(0, 999999))
    }

    fun decrementQuantity(denomValue: Double, step: Int = 1) {
        saveStateToUndoStack()
        val current = _activeDenominations.value.find { it.value == denomValue }?.quantity ?: 0
        updateDenominationQuantity(denomValue, (current - step).coerceAtLeast(0))
    }

    private fun updateDenominationQuantity(denomValue: Double, newQty: Int) {
        _activeDenominations.value = _activeDenominations.value.map { entry ->
            if (entry.value == denomValue) {
                entry.copy(
                    quantity = newQty,
                    amount = DenominationEngine.calculateItemAmount(entry.value, newQty)
                )
            } else entry
        }
        recalculate()
    }

    private fun saveStateToUndoStack() {
        val currentMap = _activeDenominations.value.associate { it.value to it.quantity }
        if (undoStack.size > 30) undoStack.removeAt(0)
        undoStack.push(currentMap)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previousMap = undoStack.pop()
            _activeDenominations.value = _activeDenominations.value.map { entry ->
                val prevQty = previousMap[entry.value] ?: 0
                entry.copy(
                    quantity = prevQty,
                    amount = DenominationEngine.calculateItemAmount(entry.value, prevQty)
                )
            }
            recalculate()
            viewModelScope.launch {
                _uiEvents.emit("Undo performed")
            }
        }
    }

    fun resetCount() {
        saveStateToUndoStack()
        _activeDenominations.value = _activeDenominations.value.map { entry ->
            entry.copy(quantity = 0, amount = 0.0)
        }
        openingCashInput.value = "0"
        expectedCashInput.value = "0"
        countNotes.value = ""
        editingTransactionId.value = null
        recalculate()
        viewModelScope.launch {
            _uiEvents.emit("Counts cleared")
        }
    }

    fun onOpeningCashChanged(text: String) {
        openingCashInput.value = text.filter { it.isDigit() || it == '.' }
        recalculate()
    }

    fun onExpectedCashChanged(text: String) {
        expectedCashInput.value = text.filter { it.isDigit() || it == '.' }
        recalculate()
    }

    fun onNotesChanged(text: String) {
        countNotes.value = text
    }

    private fun recalculate() {
        val opening = openingCashInput.value.toDoubleOrNull() ?: 0.0
        val expected = expectedCashInput.value.toDoubleOrNull() ?: 0.0
        _summary.value = DenominationEngine.calculateSummary(
            entries = _activeDenominations.value,
            openingCash = opening,
            expectedCash = expected
        )
    }

    fun saveTransaction(onSuccess: (Long) -> Unit) {
        val user = _currentUser.value
        if (user == null) {
            viewModelScope.launch { _uiEvents.emit("Please select a user first") }
            return
        }

        val location = selectedLocation.value
        val locId = location?.id ?: 1
        val locName = location?.name ?: "Main Till"

        val currentSum = summary.value
        val isEditing = editingTransactionId.value != null

        val txNumber = if (isEditing) {
            allTransactions.value.find { it.id == editingTransactionId.value }?.transactionNumber
                ?: "TXN-${System.currentTimeMillis() % 100000}"
        } else {
            "TXN-${(System.currentTimeMillis() / 1000) % 1000000}-${(10..99).random()}"
        }

        val tx = TransactionEntity(
            id = editingTransactionId.value ?: 0,
            transactionNumber = txNumber,
            userId = user.id,
            userName = user.name,
            locationId = locId,
            locationName = locName,
            timestamp = System.currentTimeMillis(),
            openingCash = currentSum.openingCash,
            expectedCash = currentSum.expectedCash,
            actualCash = currentSum.totalCash,
            difference = currentSum.difference,
            notes = countNotes.value.trim(),
            status = currentSum.status.name
        )

        val items = _activeDenominations.value
            .filter { it.quantity > 0 }
            .map { entry ->
                TransactionItemEntity(
                    transactionId = tx.id,
                    denominationValue = entry.value,
                    denominationType = entry.type.name,
                    quantity = entry.quantity,
                    amount = entry.amount
                )
            }

        viewModelScope.launch {
            if (isEditing) {
                repository.updateTransaction(tx, items)
                _uiEvents.emit("Transaction $txNumber updated successfully")
                onSuccess(tx.id)
            } else {
                val newId = repository.saveTransaction(tx, items)
                _uiEvents.emit("Cash count saved successfully (#$txNumber)")
                onSuccess(newId)
            }
            resetCount()
        }
    }

    fun loadTransactionForEdit(txId: Long, onLoaded: () -> Unit) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(txId) ?: return@launch
            val items = repository.getItemsForTransaction(txId)

            editingTransactionId.value = tx.id
            openingCashInput.value = if (tx.openingCash % 1.0 == 0.0) tx.openingCash.toInt().toString() else tx.openingCash.toString()
            expectedCashInput.value = if (tx.expectedCash % 1.0 == 0.0) tx.expectedCash.toInt().toString() else tx.expectedCash.toString()
            countNotes.value = tx.notes

            val itemMap = items.associate { it.denominationValue to it.quantity }
            _activeDenominations.value = _activeDenominations.value.map { entry ->
                val qty = itemMap[entry.value] ?: 0
                entry.copy(
                    quantity = qty,
                    amount = DenominationEngine.calculateItemAmount(entry.value, qty)
                )
            }
            recalculate()
            onLoaded()
        }
    }

    fun duplicateTransaction(txId: Long, onReady: () -> Unit) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(txId) ?: return@launch
            val items = repository.getItemsForTransaction(txId)

            editingTransactionId.value = null // creates a new copy
            openingCashInput.value = tx.openingCash.toString()
            expectedCashInput.value = tx.expectedCash.toString()
            countNotes.value = "Copy of ${tx.transactionNumber}: ${tx.notes}"

            val itemMap = items.associate { it.denominationValue to it.quantity }
            _activeDenominations.value = _activeDenominations.value.map { entry ->
                val qty = itemMap[entry.value] ?: 0
                entry.copy(
                    quantity = qty,
                    amount = DenominationEngine.calculateItemAmount(entry.value, qty)
                )
            }
            recalculate()
            _uiEvents.emit("Duplicated transaction details into new count")
            onReady()
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        val user = _currentUser.value
        viewModelScope.launch {
            repository.deleteTransaction(tx.id, tx.transactionNumber, user?.id ?: 1, user?.name ?: "User")
            _uiEvents.emit("Transaction ${tx.transactionNumber} deleted")
        }
    }

    suspend fun getTransactionItems(txId: Long): List<TransactionItemEntity> {
        return repository.getItemsForTransaction(txId)
    }

    // Settings Management
    fun updateBusinessSettings(
        name: String,
        address: String,
        phone: String,
        email: String,
        gstin: String,
        receiptFooter: String,
        reportFooter: String
    ) {
        val current = settings.value ?: SettingsEntity()
        val user = _currentUser.value
        viewModelScope.launch {
            repository.updateSettings(
                current.copy(
                    businessName = name,
                    address = address,
                    phone = phone,
                    email = email,
                    gstin = gstin,
                    receiptFooter = receiptFooter,
                    reportFooter = reportFooter
                ),
                user?.id ?: 1,
                user?.name ?: "User"
            )
            _uiEvents.emit("Business profile settings updated")
        }
    }

    // Denominations Admin
    fun toggleDenominationActive(entry: DenominationEntry) {
        viewModelScope.launch {
            val entity = DenominationConfigEntity(
                id = entry.id,
                currencyCode = _currentCurrency.value.code,
                value = entry.value,
                type = entry.type.name,
                active = !entry.active,
                displayOrder = 0
            )
            repository.updateDenomination(entity)
            loadDenominationsForCurrency(_currentCurrency.value.code)
            _uiEvents.emit("Denomination ${entry.value} ${if (!entry.active) "enabled" else "disabled"}")
        }
    }

    fun addCustomDenomination(value: Double, type: DenominationType) {
        viewModelScope.launch {
            val count = repository.getDenominationsForCurrencySync(_currentCurrency.value.code).size
            val entity = DenominationConfigEntity(
                currencyCode = _currentCurrency.value.code,
                value = value,
                type = type.name,
                active = true,
                displayOrder = count + 1
            )
            repository.addDenomination(entity)
            loadDenominationsForCurrency(_currentCurrency.value.code)
            _uiEvents.emit("Added denomination ${CurrencyFormatter.formatDenominationOnly(value, _currentCurrency.value)}")
        }
    }

    fun deleteCustomDenomination(id: Int) {
        viewModelScope.launch {
            repository.deleteDenomination(id)
            loadDenominationsForCurrency(_currentCurrency.value.code)
            _uiEvents.emit("Denomination removed")
        }
    }

    // User Admin
    fun addNewUser(name: String, email: String, role: UserRole, pin: String) {
        viewModelScope.launch {
            val user = UserEntity(
                name = name,
                email = email,
                role = role.name,
                pin = pin.ifBlank { "0000" }
            )
            repository.addUser(user)
            _uiEvents.emit("User $name created successfully")
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user.id)
            _uiEvents.emit("User ${user.name} removed")
        }
    }

    // Locations Admin
    fun addNewLocation(name: String, address: String) {
        viewModelScope.launch {
            val loc = LocationEntity(name = name, address = address, active = true)
            repository.addLocation(loc)
            _uiEvents.emit("Location $name created")
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch {
            repository.deleteLocation(location.id)
            _uiEvents.emit("Location ${location.name} removed")
        }
    }

    // Demo Data
    fun loadDemoData() {
        val user = _currentUser.value
        viewModelScope.launch {
            repository.loadDemoData(user?.id ?: 1, user?.name ?: "User")
            _uiEvents.emit("Sample demo transactions loaded successfully")
        }
    }

    fun clearAllData() {
        val user = _currentUser.value
        viewModelScope.launch {
            repository.clearAllTransactions(user?.id ?: 1, user?.name ?: "User")
            _uiEvents.emit("All transaction records cleared")
        }
    }

    // Backup & Restore
    suspend fun getBackupJson(): String {
        return repository.exportBackupJson()
    }

    fun restoreBackup(jsonString: String) {
        val user = _currentUser.value
        viewModelScope.launch {
            val result = repository.restoreBackupJson(jsonString, user?.id ?: 1, user?.name ?: "User")
            if (result.isSuccess) {
                _uiEvents.emit("Successfully restored ${result.getOrNull()} transactions from backup!")
            } else {
                _uiEvents.emit("Failed to restore backup: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // Dark Mode Toggle
    fun toggleDarkMode(isDark: Boolean?) {
        val current = settings.value ?: SettingsEntity()
        val user = _currentUser.value
        viewModelScope.launch {
            repository.updateSettings(
                current.copy(isDarkMode = isDark),
                user?.id ?: 1,
                user?.name ?: "User"
            )
        }
    }
}
