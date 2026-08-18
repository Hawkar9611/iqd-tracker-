package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, TagEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iqd_expenses.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            val tagDao = database.tagDao()
            val expenseDao = database.expenseDao()

            if (categoryDao.getCount() == 0) {
                val defaultCategories = listOf(
                    CategoryEntity(name = "Food & Dining", icon = "restaurant", colorHex = "#FF5722", isDefault = true, isExpenseType = true),
                    CategoryEntity(name = "Groceries & Market", icon = "shopping_cart", colorHex = "#4CAF50", isDefault = true, isExpenseType = true),
                    CategoryEntity(name = "Transportation & Fuel", icon = "local_gas_station", colorHex = "#2196F3", isDefault = true, isExpenseType = true),
                    CategoryEntity(name = "Housing & Rent", icon = "home", colorHex = "#9C27B0", isDefault = true, isExpenseType = true),
                    CategoryEntity(name = "Utilities & Internet", icon = "bolt", colorHex = "#FF9800", isDefault = true, isExpenseType = true),
                    CategoryEntity(name = "Shopping & Clothing", icon = "checkroom", colorHex = "#E91E63", isDefault = true, isExpenseType = true),
                    CategoryEntity(name = "Health & Pharmacy", icon = "medication", colorHex = "#00BCD4", isDefault = true, isExpenseType = true),
                    CategoryEntity(name = "Entertainment & Leisure", icon = "movie", colorHex = "#673AB7", isDefault = true, isExpenseType = true),
                    CategoryEntity(name = "Education & Books", icon = "school", colorHex = "#3F51B5", isDefault = true, isExpenseType = true),
                    CategoryEntity(name = "Salary & Income", icon = "payments", colorHex = "#008967", isDefault = true, isExpenseType = false),
                    CategoryEntity(name = "Business & Freelance", icon = "work", colorHex = "#00796B", isDefault = true, isExpenseType = false),
                    CategoryEntity(name = "Other Expenses", icon = "category", colorHex = "#607D8B", isDefault = true, isExpenseType = true)
                )
                categoryDao.insertCategories(defaultCategories)
            }

            if (tagDao.getCount() == 0) {
                val defaultTags = listOf(
                    TagEntity(name = "Personal", colorHex = "#008967"),
                    TagEntity(name = "Family", colorHex = "#1E88E5"),
                    TagEntity(name = "Work", colorHex = "#FB8C00"),
                    TagEntity(name = "Urgent", colorHex = "#E53935"),
                    TagEntity(name = "Monthly Bills", colorHex = "#8E24AA"),
                    TagEntity(name = "Dining Out", colorHex = "#D81B60"),
                    TagEntity(name = "Travel", colorHex = "#00ACC1"),
                    TagEntity(name = "Subscription", colorHex = "#43A047")
                )
                tagDao.insertTags(defaultTags)
            }

            // Do not seed dummy expenses so app starts clean with 0 data
            // Default categories and tags are kept for selection
        }

        suspend fun clearAllExpenses(database: AppDatabase) {
            database.expenseDao().deleteAllExpenses()
        }
    }
}
