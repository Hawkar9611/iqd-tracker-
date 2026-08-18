package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateMillis >= :startDateMillis AND dateMillis <= :endDateMillis ORDER BY dateMillis DESC")
    fun getExpensesByDateRange(startDateMillis: Long, endDateMillis: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateMillis >= :startDateMillis AND dateMillis <= :endDateMillis ORDER BY dateMillis DESC")
    suspend fun getExpensesListByDateRange(startDateMillis: Long, endDateMillis: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'EXPENSE' AND dateMillis >= :startDateMillis AND dateMillis <= :endDateMillis")
    fun getTotalExpenseSum(startDateMillis: Long, endDateMillis: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'INCOME' AND dateMillis >= :startDateMillis AND dateMillis <= :endDateMillis")
    fun getTotalIncomeSum(startDateMillis: Long, endDateMillis: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getCount(): Int

    @Query("SELECT * FROM expenses WHERE (title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR merchantName LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY dateMillis DESC")
    fun searchExpenses(query: String): Flow<List<ExpenseEntity>>

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}
