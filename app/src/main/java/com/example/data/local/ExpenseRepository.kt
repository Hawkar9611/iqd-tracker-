package com.example.data.local

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao
) {
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getExpensesByDateRange(startDateMillis: Long, endDateMillis: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByDateRange(startDateMillis, endDateMillis)
    }

    suspend fun getExpensesListByDateRange(startDateMillis: Long, endDateMillis: Long): List<ExpenseEntity> {
        return expenseDao.getExpensesListByDateRange(startDateMillis, endDateMillis)
    }

    fun getTotalExpenseSum(startDateMillis: Long, endDateMillis: Long): Flow<Double?> {
        return expenseDao.getTotalExpenseSum(startDateMillis, endDateMillis)
    }

    fun getTotalIncomeSum(startDateMillis: Long, endDateMillis: Long): Flow<Double?> {
        return expenseDao.getTotalIncomeSum(startDateMillis, endDateMillis)
    }

    suspend fun getExpenseById(id: Long): ExpenseEntity? {
        return expenseDao.getExpenseById(id)
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    fun searchExpenses(query: String): Flow<List<ExpenseEntity>> {
        return expenseDao.searchExpenses(query)
    }

    suspend fun deleteAllExpenses() {
        expenseDao.deleteAllExpenses()
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun insertTag(tag: TagEntity): Long {
        return tagDao.insertTag(tag)
    }
}
