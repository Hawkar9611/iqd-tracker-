package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["dateMillis"]),
        Index(value = ["type"]),
        Index(value = ["categoryId"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double, // in IQD
    val type: String, // "EXPENSE" or "INCOME"
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val tags: String = "", // Comma-separated tags, e.g. "dinner,friends,erbil"
    val paymentMethod: String = "Cash", // "Cash", "ZainCash", "QI Card", "FastPay", "FIB", "Credit Card", "Bank Transfer"
    val dateMillis: Long = System.currentTimeMillis(),
    val receiptImageUri: String? = null,
    val notes: String? = null,
    val merchantName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

