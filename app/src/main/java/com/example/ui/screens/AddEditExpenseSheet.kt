package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.gemini.ScannedReceiptResult
import com.example.data.local.CategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.TagEntity
import com.example.ui.components.CategoryIcons
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.utils.CurrencyUtils
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseSheet(
    categories: List<CategoryEntity>,
    existingTags: List<TagEntity>,
    editingExpense: ExpenseEntity? = null,
    prefilledScannedData: ScannedReceiptResult? = null,
    prefilledImageUri: Uri? = null,
    onDismiss: () -> Unit,
    onSave: (
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
        receiptImageUri: String?,
        merchantName: String?,
        notes: String?
    ) -> Unit,
    onAddNewCategory: (name: String, icon: String, colorHex: String, isExpense: Boolean) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current

    var type by remember {
        mutableStateOf(editingExpense?.type ?: (if (prefilledScannedData != null) "EXPENSE" else "EXPENSE"))
    }
    var title by remember {
        mutableStateOf(editingExpense?.title ?: prefilledScannedData?.merchantName ?: "")
    }
    var amountText by remember {
        mutableStateOf(
            editingExpense?.let { if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString() }
                ?: prefilledScannedData?.let { if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString() }
                ?: ""
        )
    }
    var merchantName by remember {
        mutableStateOf(editingExpense?.merchantName ?: prefilledScannedData?.merchantName ?: "")
    }
    var notes by remember {
        mutableStateOf(editingExpense?.notes ?: prefilledScannedData?.notes ?: "")
    }
    var paymentMethod by remember {
        mutableStateOf(editingExpense?.paymentMethod ?: "Cash")
    }
    var dateMillis by remember {
        mutableStateOf(editingExpense?.dateMillis ?: System.currentTimeMillis())
    }
    var receiptImageUri by remember {
        mutableStateOf<String?>(editingExpense?.receiptImageUri ?: prefilledImageUri?.toString())
    }

    // Selected Category
    var selectedCategory by remember(categories, type) {
        val initial = if (editingExpense != null) {
            categories.find { it.id == editingExpense.categoryId }
        } else if (prefilledScannedData != null) {
            categories.find { it.name.contains(prefilledScannedData.suggestedCategory, ignoreCase = true) }
        } else null
        mutableStateOf(initial ?: categories.firstOrNull { if (type == "INCOME") !it.isExpenseType else it.isExpenseType } ?: categories.firstOrNull())
    }

    // Tags Management
    val selectedTags = remember {
        mutableStateListOf<String>().apply {
            if (editingExpense != null && editingExpense.tags.isNotBlank()) {
                addAll(editingExpense.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            } else if (prefilledScannedData != null) {
                addAll(prefilledScannedData.suggestedTags)
            }
        }
    }
    var newTagInput by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Camera Image Capture Launcher
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            receiptImageUri = tempPhotoUri.toString()
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            receiptImageUri = it.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_edit_expense_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingExpense != null) strings.editTransaction else strings.newTransaction,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction Type Selector (Expense vs Income)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { type = "EXPENSE" }
                        .testTag("type_expense_button"),
                    color = if (type == "EXPENSE") ExpenseRed else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = strings.expenses,
                            tint = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.expenses,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { type = "INCOME" }
                        .testTag("type_income_button"),
                    color = if (type == "INCOME") IncomeGreen else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = strings.income,
                            tint = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.income,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Field (IQD)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("${strings.amount} (${strings.currencyCode} - ${strings.currencySymbol})") },
                placeholder = { Text("e.g. 25000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Text(
                        text = strings.currencySymbol,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input")
            )

            // Quick Iraqi Dinar Increment Chips
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1000L, 5000L, 10000L, 25000L, 50000L, 100000L).forEach { quickVal ->
                    SuggestionChip(
                        onClick = {
                            val cur = amountText.toDoubleOrNull() ?: 0.0
                            val updated = cur + quickVal
                            amountText = if (updated % 1.0 == 0.0) updated.toLong().toString() else updated.toString()
                        },
                        label = {
                            Text(
                                text = "+${CurrencyUtils.formatLocalizedAmount(quickVal.toDouble(), language)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title / Description
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(strings.titleDescription) },
                placeholder = { Text("e.g. Carrefour Market") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Merchant / Store Name
            OutlinedTextField(
                value = merchantName,
                onValueChange = { merchantName = it },
                label = { Text("${strings.merchant} (${strings.notes.lowercase()})") },
                placeholder = { Text("e.g. City Center Mall") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merchant_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.category,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = strings.newCategory, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.newCategory, style = MaterialTheme.typography.labelMedium)
                }
            }

            val filteredCategories = categories.filter { if (type == "INCOME") !it.isExpenseType else it.isExpenseType }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(if (filteredCategories.isNotEmpty()) filteredCategories else categories) { category ->
                    val isSelected = selectedCategory?.id == category.id
                    val catColor = CategoryIcons.parseColor(category.colorHex)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) catColor else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedCategory = category }
                            .testTag("category_chip_${category.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = CategoryIcons.getIcon(category.icon),
                                contentDescription = category.name,
                                tint = if (isSelected) Color.White else catColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.localizeCategory(category.name),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tagging System
            Text(
                text = strings.categoryTags,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Add new tag row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text(strings.addTagPlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val cleaned = newTagInput.trim().removePrefix("#")
                        if (cleaned.isNotBlank() && !selectedTags.contains(cleaned)) {
                            selectedTags.add(cleaned)
                            newTagInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Tag",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Selected & Suggestion Tag Chips
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Currently Selected Tags
                selectedTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { selectedTags.remove(tag) },
                        label = { Text("#$tag") },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove tag", modifier = Modifier.size(14.dp))
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Unselected Existing Tags
                existingTags.forEach { tagEntity ->
                    if (!selectedTags.contains(tagEntity.name)) {
                        SuggestionChip(
                            onClick = { selectedTags.add(tagEntity.name) },
                            label = { Text("#${tagEntity.name}") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Selector
            Text(
                text = strings.paymentMethod,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurrencyUtils.PAYMENT_METHODS.forEach { method ->
                    val isSelected = paymentMethod == method
                    FilterChip(
                        selected = isSelected,
                        onClick = { paymentMethod = method },
                        label = { Text(strings.localizePaymentMethod(method)) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date & Time Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                dateMillis = selectedCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = strings.transactionDate, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = strings.transactionDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = CurrencyUtils.formatDate(dateMillis, "EEEE, dd MMMM yyyy"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                Icon(imageVector = Icons.Default.EditCalendar, contentDescription = "Change Date", tint = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Receipt Photo Attachment
            Text(
                text = strings.receiptAttachment,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (receiptImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    AsyncImage(
                        model = receiptImageUri,
                        contentDescription = "Receipt Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { receiptImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val photoFile = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                                tempPhotoUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = strings.takePhoto, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.takePhoto)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = strings.uploadImage, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.uploadImage)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(strings.notes) },
                placeholder = { Text("Add any extra details or item breakdown") },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                    val finalTitle = title.ifBlank { selectedCategory?.name ?: "Expense" }
                    val cat = selectedCategory ?: categories.firstOrNull() ?: CategoryEntity(name = "General", icon = "category", colorHex = "#008967")

                    onSave(
                        finalTitle,
                        parsedAmount,
                        type,
                        cat.id,
                        cat.name,
                        cat.icon,
                        cat.colorHex,
                        selectedTags.joinToString(","),
                        paymentMethod,
                        dateMillis,
                        receiptImageUri,
                        merchantName.ifBlank { null },
                        notes.ifBlank { null }
                    )
                    onDismiss()
                },
                enabled = amountText.toDoubleOrNull() != null && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_expense_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "INCOME") IncomeGreen else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingExpense != null) strings.updateTransaction else strings.saveTransaction,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Add Custom Category Dialog
    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var catIcon by remember { mutableStateOf("shopping_cart") }
        var catColor by remember { mutableStateOf("#FF5722") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(strings.createCustomCategory, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text(strings.categoryName) },
                        placeholder = { Text("e.g. Gym & Fitness") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(strings.selectIcon, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcons.AVAILABLE_ICONS.forEach { icName ->
                            val isSel = catIcon == icName
                            IconButton(
                                onClick = { catIcon = icName },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Icon(
                                    imageVector = CategoryIcons.getIcon(icName),
                                    contentDescription = icName,
                                    tint = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(strings.selectColor, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcons.AVAILABLE_COLORS.forEach { hex ->
                            val isSel = catColor == hex
                            val parsed = CategoryIcons.parseColor(hex)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .clickable { catColor = hex }
                                    .padding(if (isSel) 4.dp else 0.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            onAddNewCategory(catName.trim(), catIcon, catColor, type != "INCOME")
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = catName.isNotBlank()
                ) {
                    Text(strings.addCategory)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

