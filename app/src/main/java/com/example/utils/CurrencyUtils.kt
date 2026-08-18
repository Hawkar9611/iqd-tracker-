package com.example.utils

import com.example.ui.i18n.AppLanguage
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CurrencyUtils {
    private val decimalFormat = DecimalFormat("#,##0")
    private val decimalFormatWithDecimals = DecimalFormat("#,##0.##")

    const val CURRENCY_CODE = "IQD"
    const val CURRENCY_SYMBOL_AR = "د.ع"

    fun formatIQD(amount: Double, includeSymbol: Boolean = true): String {
        val formatted = if (amount % 1.0 == 0.0) {
            decimalFormat.format(amount)
        } else {
            decimalFormatWithDecimals.format(amount)
        }
        return if (includeSymbol) "$formatted IQD" else formatted
    }

    fun formatIQDWithArabic(amount: Double): String {
        val formatted = if (amount % 1.0 == 0.0) {
            decimalFormat.format(amount)
        } else {
            decimalFormatWithDecimals.format(amount)
        }
        return "$formatted $CURRENCY_SYMBOL_AR"
    }

    fun formatLocalizedAmount(amount: Double, language: AppLanguage): String {
        return when (language) {
            AppLanguage.ARABIC, AppLanguage.KURDISH -> formatIQDWithArabic(amount)
            AppLanguage.ENGLISH -> formatIQD(amount)
        }
    }

    fun formatDate(millis: Long, pattern: String = "dd MMM yyyy"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun getStartOfMonth(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfMonth(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    val PAYMENT_METHODS = listOf(
        "Cash",
        "ZainCash",
        "QI Card",
        "FIB",
        "FastPay",
        "Credit Card",
        "Bank Transfer"
    )
}
