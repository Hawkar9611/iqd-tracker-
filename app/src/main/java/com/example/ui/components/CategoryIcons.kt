package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {
    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "restaurant", "food" -> Icons.Default.Restaurant
            "shopping_cart", "groceries", "market" -> Icons.Default.ShoppingCart
            "local_gas_station", "fuel", "transport" -> Icons.Default.LocalGasStation
            "home", "housing", "rent" -> Icons.Default.Home
            "bolt", "utilities", "electricity" -> Icons.Default.Bolt
            "checkroom", "clothing", "shopping" -> Icons.Default.Checkroom
            "medication", "health", "pharmacy" -> Icons.Default.Medication
            "movie", "entertainment" -> Icons.Default.Movie
            "school", "education", "books" -> Icons.Default.School
            "payments", "salary", "cash" -> Icons.Default.Payments
            "work", "business", "freelance" -> Icons.Default.Work
            "flight", "travel" -> Icons.Default.Flight
            "fitness_center", "gym" -> Icons.Default.FitnessCenter
            "pets" -> Icons.Default.Pets
            "build", "maintenance" -> Icons.Default.Build
            "receipt", "invoice" -> Icons.AutoMirrored.Filled.ReceiptLong
            else -> Icons.Default.Category
        }
    }

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF008967)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(0xFF000000 or colorLong)
            } else {
                Color(colorLong)
            }
        } catch (e: Exception) {
            defaultColor
        }
    }

    val AVAILABLE_ICONS = listOf(
        "restaurant", "shopping_cart", "local_gas_station", "home",
        "bolt", "checkroom", "medication", "movie", "school",
        "payments", "work", "flight", "fitness_center", "pets", "category"
    )

    val AVAILABLE_COLORS = listOf(
        "#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#FF9800",
        "#E91E63", "#00BCD4", "#673AB7", "#3F51B5", "#008967",
        "#00796B", "#607D8B", "#D32F2F", "#F57C00", "#7CB342"
    )
}
