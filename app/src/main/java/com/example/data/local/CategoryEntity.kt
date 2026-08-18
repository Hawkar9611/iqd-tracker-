package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String, // e.g. "restaurant", "shopping_cart", "local_gas_station", etc.
    val colorHex: String,
    val isDefault: Boolean = false,
    val isExpenseType: Boolean = true
)
