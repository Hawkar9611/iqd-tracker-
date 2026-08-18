package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.GeminiReceiptScanner
import com.example.data.gemini.ScannedReceiptResult
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.ExpenseRepository
import com.example.data.local.TagEntity
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.BarChartItem
import com.example.ui.components.CategoryIcons
import com.example.ui.components.ChartSliceData
import com.example.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    val categories: StateFlow<List<CategoryEntity>>
    val tags: StateFlow<List<TagEntity>>

    // Selected Month & Year for Analytics & Reports (0-indexed month)
    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "EXPENSE", "INCOME"
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedCategoryFilter: StateFlow<Long?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    // Monthly Expenses Flow based on selectedCalendar
    val monthlyExpenses: StateFlow<List<ExpenseEntity>>

    // Filtered Expenses List for the Expenses Screen
    val filteredExpenses: StateFlow<List<ExpenseEntity>>

    // Dashboard & Analytics Computed Stats
    val monthlyIncomeSum: StateFlow<Double>
    val monthlyExpenseSum: StateFlow<Double>
    val monthlyBalance: StateFlow<Double>
    val categoryChartSlices: StateFlow<List<ChartSliceData>>
    val dailySpendingBars: StateFlow<List<BarChartItem>>
    val tagBreakdownMap: StateFlow<Map<String, Double>>

    // Receipt Scanning State
    private val _isScanningReceipt = MutableStateFlow(false)
    val isScanningReceipt: StateFlow<Boolean> = _isScanningReceipt.asStateFlow()

    private val _scannedResult = MutableStateFlow<ScannedReceiptResult?>(null)
    val scannedResult: StateFlow<ScannedReceiptResult?> = _scannedResult.asStateFlow()

    private val _scanReceiptError = MutableStateFlow<String?>(null)
    val scanReceiptError: StateFlow<String?> = _scanReceiptError.asStateFlow()

    // PDF Export State
    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    private val _generatedPdfFile = MutableStateFlow<File?>(null)
    val generatedPdfFile: StateFlow<File?> = _generatedPdfFile.asStateFlow()

    private val _pdfErrorMessage = MutableStateFlow<String?>(null)
    val pdfErrorMessage: StateFlow<String?> = _pdfErrorMessage.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = ExpenseRepository(db.expenseDao(), db.categoryDao(), db.tagDao())

        // Clear all expenses so the app starts completely empty
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllExpenses()
        }

        categories = repository.allCategories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        tags = repository.allTags.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Monthly Expenses Flow
        monthlyExpenses = _selectedCalendar.flatMapLatest { cal ->
            val start = CurrencyUtils.getStartOfMonth(cal)
            val end = CurrencyUtils.getEndOfMonth(cal)
            repository.getExpensesByDateRange(start, end)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Filtered Expenses
        filteredExpenses = combine(
            repository.allExpenses,
            _searchQuery,
            _selectedTypeFilter,
            _selectedCategoryFilter,
            _selectedTagFilter
        ) { all, query, typeFilter, catFilter, tagFilter ->
            all.filter { item ->
                val matchesQuery = query.isBlank() ||
                        item.title.contains(query, ignoreCase = true) ||
                        (item.merchantName?.contains(query, ignoreCase = true) == true) ||
                        item.categoryName.contains(query, ignoreCase = true) ||
                        item.tags.contains(query, ignoreCase = true)

                val matchesType = when (typeFilter) {
                    "EXPENSE" -> item.type == "EXPENSE"
                    "INCOME" -> item.type == "INCOME"
                    else -> true
                }

                val matchesCategory = catFilter == null || item.categoryId == catFilter

                val matchesTag = tagFilter == null || item.tags.split(",")
                    .map { it.trim().lowercase() }
                    .contains(tagFilter.lowercase())

                matchesQuery && matchesType && matchesCategory && matchesTag
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Summary KPI stats for selected month
        monthlyIncomeSum = monthlyExpenses.map { list ->
            list.filter { it.type == "INCOME" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        monthlyExpenseSum = monthlyExpenses.map { list ->
            list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        monthlyBalance = combine(monthlyIncomeSum, monthlyExpenseSum) { income, expense ->
            income - expense
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        // Category Chart Slices
        categoryChartSlices = monthlyExpenses.map { list ->
            val expensesOnly = list.filter { it.type == "EXPENSE" }
            val group = expensesOnly.groupBy { it.categoryName }
            group.map { (catName, items) ->
                val sum = items.sumOf { it.amount }
                val colorHex = items.firstOrNull()?.categoryColorHex ?: "#008967"
                ChartSliceData(
                    label = catName,
                    value = sum,
                    color = CategoryIcons.parseColor(colorHex)
                )
            }.sortedByDescending { it.value }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Daily Spending Bar Chart for the current month
        dailySpendingBars = combine(monthlyExpenses, _selectedCalendar) { list, cal ->
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = if (cal.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) &&
                cal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
            ) {
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            } else -1

            val expensesOnly = list.filter { it.type == "EXPENSE" }
            val dayFormat = SimpleDateFormat("d", Locale.getDefault())

            val dailyTotals = mutableMapOf<Int, Double>()
            for (i in 1..maxDay) {
                dailyTotals[i] = 0.0
            }

            expensesOnly.forEach { item ->
                val itemCal = Calendar.getInstance().apply { timeInMillis = item.dateMillis }
                val day = itemCal.get(Calendar.DAY_OF_MONTH)
                dailyTotals[day] = (dailyTotals[day] ?: 0.0) + item.amount
            }

            // Group into week slots or 7 sample days for clean chart representation
            val result = mutableListOf<BarChartItem>()
            if (maxDay > 15) {
                // Aggregate every 4-5 days to fit cleanly on screen (e.g., 1-5, 6-10, 11-15, 16-20, 21-25, 26-End)
                val step = 5
                for (startDay in 1..maxDay step step) {
                    val endDay = (startDay + step - 1).coerceAtMost(maxDay)
                    var rangeSum = 0.0
                    for (d in startDay..endDay) {
                        rangeSum += dailyTotals[d] ?: 0.0
                    }
                    val isHigh = currentDay in startDay..endDay
                    result.add(
                        BarChartItem(
                            label = "$startDay-$endDay",
                            value = rangeSum,
                            isHighlighted = isHigh
                        )
                    )
                }
            } else {
                dailyTotals.forEach { (day, amount) ->
                    result.add(
                        BarChartItem(
                            label = day.toString(),
                            value = amount,
                            isHighlighted = (day == currentDay)
                        )
                    )
                }
            }
            result
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Tag breakdown map
        tagBreakdownMap = monthlyExpenses.map { list ->
            val tagMap = mutableMapOf<String, Double>()
            list.filter { it.type == "EXPENSE" }.forEach { item ->
                if (item.tags.isNotBlank()) {
                    val tagsArray = item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    tagsArray.forEach { tag ->
                        tagMap[tag] = (tagMap[tag] ?: 0.0) + item.amount
                    }
                }
            }
            tagMap.toList().sortedByDescending { it.second }.toMap()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(filter: String) {
        _selectedTypeFilter.value = filter
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun previousMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        _selectedCalendar.value = cal
    }

    fun nextMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        _selectedCalendar.value = cal
    }

    fun setMonth(year: Int, month: Int) {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        _selectedCalendar.value = cal
    }

    fun addExpense(
        title: String,
        amount: Double,
        type: String,
        categoryId: Long,
        categoryName: String,
        categoryIcon: String,
        categoryColorHex: String,
        tags: String,
        paymentMethod: String,
        dateMillis: Long,
        receiptImageUri: String? = null,
        merchantName: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = ExpenseEntity(
                title = title,
                amount = amount,
                type = type,
                categoryId = categoryId,
                categoryName = categoryName,
                categoryIcon = categoryIcon,
                categoryColorHex = categoryColorHex,
                tags = tags,
                paymentMethod = paymentMethod,
                dateMillis = dateMillis,
                receiptImageUri = receiptImageUri,
                merchantName = merchantName,
                notes = notes
            )
            repository.insertExpense(expense)

            // Also auto-insert any new tags into tags table
            if (tags.isNotBlank()) {
                val tagList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                tagList.forEach { tagName ->
                    repository.insertTag(TagEntity(name = tagName))
                }
            }
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }

    fun addCategory(name: String, icon: String, colorHex: String, isExpense: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(
                CategoryEntity(
                    name = name,
                    icon = icon,
                    colorHex = colorHex,
                    isExpenseType = isExpense
                )
            )
        }
    }

    fun addTag(name: String, colorHex: String = "#008967") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTag(TagEntity(name = name, colorHex = colorHex))
        }
    }

    fun scanReceipt(imageUri: Uri) {
        viewModelScope.launch {
            _isScanningReceipt.value = true
            _scanReceiptError.value = null
            _scannedResult.value = null

            val result = GeminiReceiptScanner.scanReceipt(getApplication(), imageUri)
            result.onSuccess { scanned ->
                _scannedResult.value = scanned
                _isScanningReceipt.value = false
            }.onFailure { err ->
                _scanReceiptError.value = err.message ?: "Failed to scan receipt"
                _isScanningReceipt.value = false
            }
        }
    }

    fun clearScannedResult() {
        _scannedResult.value = null
        _scanReceiptError.value = null
    }

    fun generateMonthlyPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingPdf.value = true
            _pdfErrorMessage.value = null

            val cal = _selectedCalendar.value
            val start = CurrencyUtils.getStartOfMonth(cal)
            val end = CurrencyUtils.getEndOfMonth(cal)
            val monthName = CurrencyUtils.formatMonthYear(cal.timeInMillis)

            val expensesList = repository.getExpensesListByDateRange(start, end)
            val incomeSum = expensesList.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expenseSum = expensesList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val net = incomeSum - expenseSum

            val catMap = expensesList.filter { it.type == "EXPENSE" }
                .groupBy { it.categoryName }
                .mapValues { it.value.sumOf { exp -> exp.amount } }

            val tagMap = mutableMapOf<String, Double>()
            expensesList.filter { it.type == "EXPENSE" }.forEach { item ->
                if (item.tags.isNotBlank()) {
                    item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { t ->
                        tagMap[t] = (tagMap[t] ?: 0.0) + item.amount
                    }
                }
            }

            val reportData = PdfReportGenerator.ReportData(
                monthYearName = monthName,
                totalIncome = incomeSum,
                totalExpense = expenseSum,
                netBalance = net,
                expenses = expensesList,
                categoryBreakdown = catMap,
                tagBreakdown = tagMap
            )

            val generatedFile = PdfReportGenerator.generateMonthlyReportPdf(getApplication(), reportData)
            withContext(Dispatchers.Main) {
                if (generatedFile != null && generatedFile.exists()) {
                    _generatedPdfFile.value = generatedFile
                } else {
                    _pdfErrorMessage.value = "Unable to create PDF report file."
                }
                _isGeneratingPdf.value = false
            }
        }
    }

    fun clearGeneratedPdf() {
        _generatedPdfFile.value = null
        _pdfErrorMessage.value = null
    }

    fun clearAllExpenses() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllExpenses()
        }
    }
}
