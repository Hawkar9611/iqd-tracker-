package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.TagEntity
import com.example.ui.components.CategoryIcons
import com.example.ui.components.ExpenseItemCard
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.utils.CurrencyUtils

@Composable
fun ExpensesListScreen(
    expenses: List<ExpenseEntity>,
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    searchQuery: String,
    selectedTypeFilter: String,
    selectedCategoryFilter: Long?,
    selectedTagFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onExpenseClick: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current

    val totalExpenseAmount = expenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalIncomeAmount = expenses.filter { it.type == "INCOME" }.sumOf { it.amount }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(bottom = 64.dp)
                    .testTag("fab_add_expense")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = strings.addExpense)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("expenses_list_screen"),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.outline)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expenses_search_bar")
                    )
                }
            }

            // Type Filter Tabs (All, Expense, Income)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to strings.allTypes, "EXPENSE" to strings.expensesOnly, "INCOME" to strings.incomeOnly).forEach { (typeKey, label) ->
                        val isSelected = selectedTypeFilter == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTypeFilterChange(typeKey) },
                            label = { Text(label) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("filter_type_$typeKey")
                        )
                    }
                }
            }

            // Category & Tag Filter Row
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = strings.filterByTagCategory,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tag filter chips
                        tags.forEach { tagEntity ->
                            val isTagSelected = selectedTagFilter.equals(tagEntity.name, ignoreCase = true)
                            FilterChip(
                                selected = isTagSelected,
                                onClick = {
                                    onTagFilterChange(if (isTagSelected) null else tagEntity.name)
                                },
                                label = { Text("#${tagEntity.name}") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Category filter chips
                        categories.forEach { cat ->
                            val isCatSelected = selectedCategoryFilter == cat.id
                            val catColor = CategoryIcons.parseColor(cat.colorHex)
                            FilterChip(
                                selected = isCatSelected,
                                onClick = {
                                    onCategoryFilterChange(if (isCatSelected) null else cat.id)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIcons.getIcon(cat.icon),
                                        contentDescription = cat.name,
                                        tint = if (isCatSelected) MaterialTheme.colorScheme.primary else catColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(strings.localizeCategory(cat.name)) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Matching Results Summary Pill
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${expenses.size} ${strings.transactionsFound}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (totalExpenseAmount > 0) {
                                Text(
                                    text = "-${CurrencyUtils.formatLocalizedAmount(totalExpenseAmount, language)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (totalIncomeAmount > 0) {
                                Text(
                                    text = "+${CurrencyUtils.formatLocalizedAmount(totalIncomeAmount, language)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            // Expenses List Items
            if (expenses.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No results",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.noTransactionsFound,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.tryClearingFilters,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onSearchQueryChange("")
                                onTypeFilterChange("ALL")
                                onCategoryFilterChange(null)
                                onTagFilterChange(null)
                            }
                        ) {
                            Text(strings.resetAllFilters)
                        }
                    }
                }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        ExpenseItemCard(
                            expense = expense,
                            onClick = { onExpenseClick(expense) },
                            onDelete = { onDeleteExpense(expense) }
                        )
                    }
                }
            }
        }
    }
}

