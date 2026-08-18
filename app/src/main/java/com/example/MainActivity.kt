package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.ScannedReceiptResult
import com.example.data.local.ExpenseEntity
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.*
import com.example.ui.theme.DinarGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

enum class AppScreen(
    val titleKey: (com.example.ui.i18n.Strings) -> String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD({ it.navDashboard }, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    EXPENSES({ it.navExpenses }, Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    ANALYTICS({ it.navAnalytics }, Icons.Filled.PieChart, Icons.Outlined.PieChart),
    SCANNER({ it.navScanner }, Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner),
    REPORTS({ it.navReports }, Icons.Filled.PictureAsPdf, Icons.Outlined.PictureAsPdf)
}

class MainActivity : ComponentActivity() {

    private val expenseViewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }
            val strings = when (currentLanguage) {
                AppLanguage.ENGLISH -> AppStrings.en
                AppLanguage.ARABIC -> AppStrings.ar
                AppLanguage.KURDISH -> AppStrings.ku
            }

            CompositionLocalProvider(
                LocalAppLanguage provides currentLanguage,
                LocalAppStrings provides strings
            ) {
                MyApplicationTheme {
                    MainExpenseApp(
                        viewModel = expenseViewModel,
                        currentLanguage = currentLanguage,
                        onLanguageChange = { currentLanguage = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainExpenseApp(
    viewModel: ExpenseViewModel,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    // Bottom sheet state for Add/Edit
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var prefilledScannedData by remember { mutableStateOf<ScannedReceiptResult?>(null) }
    var prefilledImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Clear data confirmation dialog state
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // State Collection
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedCalendar by viewModel.selectedCalendar.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()

    val monthlyIncome by viewModel.monthlyIncomeSum.collectAsState()
    val monthlyExpense by viewModel.monthlyExpenseSum.collectAsState()
    val monthlyBalance by viewModel.monthlyBalance.collectAsState()
    val categoryChartSlices by viewModel.categoryChartSlices.collectAsState()
    val dailySpendingBars by viewModel.dailySpendingBars.collectAsState()
    val tagBreakdown by viewModel.tagBreakdownMap.collectAsState()

    val isScanningReceipt by viewModel.isScanningReceipt.collectAsState()
    val scannedResult by viewModel.scannedResult.collectAsState()
    val scanReceiptError by viewModel.scanReceiptError.collectAsState()

    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsState()
    val generatedPdfFile by viewModel.generatedPdfFile.collectAsState()
    val pdfErrorMessage by viewModel.pdfErrorMessage.collectAsState()

    // Listen to scan errors
    LaunchedEffect(scanReceiptError) {
        scanReceiptError?.let { err ->
            snackbarHostState.showSnackbar(
                message = "Scan note: $err",
                duration = SnackbarDuration.Short
            )
        }
    }

    // Listen to PDF errors
    LaunchedEffect(pdfErrorMessage) {
        pdfErrorMessage?.let { err ->
            snackbarHostState.showSnackbar(
                message = err,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "IQD Expense Tracker",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Iraqi Dinar (د.ع) Edition",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Menu
                    Box {
                        IconButton(
                            onClick = { showLanguageMenu = true },
                            modifier = Modifier.testTag("action_change_language")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = strings.changeLanguage,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            AppLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = lang.displayName,
                                            fontWeight = if (currentLanguage == lang) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentLanguage == lang) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onLanguageChange(lang)
                                        showLanguageMenu = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(strings.languageChangedNotice)
                                        }
                                    },
                                    leadingIcon = {
                                        if (currentLanguage == lang) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Clear All Data Action Button
                    IconButton(
                        onClick = { showClearDataDialog = true },
                        modifier = Modifier.testTag("action_clear_all_data")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = strings.clearAllData,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DinarGold)
                            )
                            Text(
                                text = "IQD",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .shadow(8.dp)
                    .testTag("main_bottom_nav")
            ) {
                AppScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    val labelText = screen.titleKey(strings)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = labelText
                            )
                        },
                        label = {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentScreen == AppScreen.DASHBOARD) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingExpense = null
                        prefilledScannedData = null
                        prefilledImageUri = null
                        showAddEditSheet = true
                    },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("main_fab_add_expense")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.DASHBOARD -> {
                        DashboardScreen(
                            monthlyIncome = monthlyIncome,
                            monthlyExpense = monthlyExpense,
                            monthlyBalance = monthlyBalance,
                            recentExpenses = monthlyExpenses,
                            categorySlices = categoryChartSlices,
                            selectedCalendar = selectedCalendar,
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            onAddExpenseClick = {
                                editingExpense = null
                                prefilledScannedData = null
                                prefilledImageUri = null
                                showAddEditSheet = true
                            },
                            onScanReceiptClick = { currentScreen = AppScreen.SCANNER },
                            onViewAllExpensesClick = { currentScreen = AppScreen.EXPENSES },
                            onExpenseClick = { expense ->
                                editingExpense = expense
                                prefilledScannedData = null
                                prefilledImageUri = null
                                showAddEditSheet = true
                            },
                            onDeleteExpense = { expense ->
                                viewModel.deleteExpense(expense)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Transaction deleted")
                                }
                            }
                        )
                    }

                    AppScreen.EXPENSES -> {
                        ExpensesListScreen(
                            expenses = filteredExpenses,
                            categories = categories,
                            tags = tags,
                            searchQuery = searchQuery,
                            selectedTypeFilter = selectedTypeFilter,
                            selectedCategoryFilter = selectedCategoryFilter,
                            selectedTagFilter = selectedTagFilter,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onTypeFilterChange = { viewModel.setTypeFilter(it) },
                            onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                            onTagFilterChange = { viewModel.setTagFilter(it) },
                            onExpenseClick = { expense ->
                                editingExpense = expense
                                prefilledScannedData = null
                                prefilledImageUri = null
                                showAddEditSheet = true
                            },
                            onDeleteExpense = { expense ->
                                viewModel.deleteExpense(expense)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Transaction deleted")
                                }
                            },
                            onAddExpenseClick = {
                                editingExpense = null
                                prefilledScannedData = null
                                prefilledImageUri = null
                                showAddEditSheet = true
                            }
                        )
                    }

                    AppScreen.ANALYTICS -> {
                        AnalyticsScreen(
                            selectedCalendar = selectedCalendar,
                            monthlyExpenseSum = monthlyExpense,
                            monthlyIncomeSum = monthlyIncome,
                            categorySlices = categoryChartSlices,
                            dailySpendingBars = dailySpendingBars,
                            tagBreakdown = tagBreakdown,
                            topExpenses = monthlyExpenses,
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() }
                        )
                    }

                    AppScreen.SCANNER -> {
                        ReceiptScannerScreen(
                            isScanning = isScanningReceipt,
                            scannedResult = scannedResult,
                            scanError = scanReceiptError,
                            onScanImage = { uri -> viewModel.scanReceipt(uri) },
                            onSaveScannedExpense = { scanned, uri ->
                                prefilledScannedData = scanned
                                prefilledImageUri = uri
                                editingExpense = null
                                showAddEditSheet = true
                            },
                            onClearScan = { viewModel.clearScannedResult() }
                        )
                    }

                    AppScreen.REPORTS -> {
                        PdfReportsScreen(
                            selectedCalendar = selectedCalendar,
                            monthlyIncome = monthlyIncome,
                            monthlyExpense = monthlyExpense,
                            monthlyBalance = monthlyBalance,
                            monthlyExpensesList = monthlyExpenses,
                            isGeneratingPdf = isGeneratingPdf,
                            generatedPdfFile = generatedPdfFile,
                            pdfErrorMessage = pdfErrorMessage,
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() },
                            onGeneratePdf = { viewModel.generateMonthlyPdf() },
                            onClearPdf = { viewModel.clearGeneratedPdf() }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Add/Edit
    if (showAddEditSheet) {
        AddEditExpenseSheet(
            categories = categories,
            existingTags = tags,
            editingExpense = editingExpense,
            prefilledScannedData = prefilledScannedData,
            prefilledImageUri = prefilledImageUri,
            onDismiss = {
                showAddEditSheet = false
                editingExpense = null
                prefilledScannedData = null
                prefilledImageUri = null
            },
            onSave = { title, amount, type, catId, catName, catIcon, catColorHex, tagsStr, payMethod, dateMs, imgUri, merchant, notes ->
                if (editingExpense != null) {
                    viewModel.updateExpense(
                        editingExpense!!.copy(
                            title = title,
                            amount = amount,
                            type = type,
                            categoryId = catId,
                            categoryName = catName,
                            categoryIcon = catIcon,
                            categoryColorHex = catColorHex,
                            tags = tagsStr,
                            paymentMethod = payMethod,
                            dateMillis = dateMs,
                            receiptImageUri = imgUri,
                            merchantName = merchant,
                            notes = notes
                        )
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Transaction updated")
                    }
                } else {
                    viewModel.addExpense(
                        title = title,
                        amount = amount,
                        type = type,
                        categoryId = catId,
                        categoryName = catName,
                        categoryIcon = catIcon,
                        categoryColorHex = catColorHex,
                        tags = tagsStr,
                        paymentMethod = payMethod,
                        dateMillis = dateMs,
                        receiptImageUri = imgUri,
                        merchantName = merchant,
                        notes = notes
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Transaction saved")
                    }
                }
                showAddEditSheet = false
                editingExpense = null
                prefilledScannedData = null
                prefilledImageUri = null
            },
            onAddNewCategory = { name, icon, colorHex, isExpense ->
                viewModel.addCategory(name, icon, colorHex, isExpense)
            }
        )
    }

    // Confirmation Dialog for Clearing All Data
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = strings.clearAllDataConfirmTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = strings.clearAllDataConfirmDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllExpenses()
                        showClearDataDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(strings.allDataCleared)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_all_data_button")
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDataDialog = false },
                    modifier = Modifier.testTag("cancel_clear_all_data_button")
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

