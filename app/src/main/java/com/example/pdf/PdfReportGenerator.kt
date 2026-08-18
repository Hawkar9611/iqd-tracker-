package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.ExpenseEntity
import com.example.utils.CurrencyUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    data class ReportData(
        val monthYearName: String,
        val totalIncome: Double,
        val totalExpense: Double,
        val netBalance: Double,
        val expenses: List<ExpenseEntity>,
        val categoryBreakdown: Map<String, Double>,
        val tagBreakdown: Map<String, Double>
    )

    fun generateMonthlyReportPdf(
        context: Context,
        reportData: ReportData
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width in pt
            val pageHeight = 842 // Standard A4 height in pt

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Header Background Banner
            paint.color = Color.parseColor("#004D40")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

            // Gold accent bar
            paint.color = Color.parseColor("#FFD700")
            canvas.drawRect(0f, 95f, pageWidth.toFloat(), 100f, paint)

            // Header Title
            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 20f
            canvas.drawText("MONTHLY FINANCIAL REPORT", 30f, 45f, paint)

            // Subtitle
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Currency: Iraqi Dinar (IQD) • Period: ${reportData.monthYearName}", 30f, 68f, paint)

            // Generated date top right
            paint.textSize = 9f
            paint.color = Color.parseColor("#B2DFDB")
            val generatedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val dateText = "Generated: $generatedDate"
            val textWidth = paint.measureText(dateText)
            canvas.drawText(dateText, pageWidth - 30f - textWidth, 45f, paint)

            var currentY = 125f

            // --- KPI Summary Cards ---
            val cardWidth = (pageWidth - 60f - 20f) / 3f
            val cardHeight = 55f

            // Card 1: Income
            drawKpiCard(canvas, paint, 30f, currentY, cardWidth, cardHeight, "TOTAL INCOME", CurrencyUtils.formatIQD(reportData.totalIncome), "#E8F5E9", "#2E7D32")

            // Card 2: Expense
            drawKpiCard(canvas, paint, 30f + cardWidth + 10f, currentY, cardWidth, cardHeight, "TOTAL EXPENSES", CurrencyUtils.formatIQD(reportData.totalExpense), "#FFEBEE", "#C62828")

            // Card 3: Net Balance
            val balanceColor = if (reportData.netBalance >= 0) "#00695C" else "#C62828"
            val balanceBg = if (reportData.netBalance >= 0) "#E0F2F1" else "#FFEBEE"
            drawKpiCard(canvas, paint, 30f + (cardWidth + 10f) * 2, currentY, cardWidth, cardHeight, "NET SAVINGS", CurrencyUtils.formatIQD(reportData.netBalance), balanceBg, balanceColor)

            currentY += cardHeight + 25f

            // --- Section: Category Breakdown ---
            paint.color = Color.parseColor("#004D40")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText("Category Spending Summary", 30f, currentY, paint)
            currentY += 12f

            // Category Table Header
            paint.color = Color.parseColor("#ECEFF1")
            canvas.drawRoundRect(RectF(30f, currentY, pageWidth - 30f, currentY + 22f), 4f, 4f, paint)

            paint.color = Color.parseColor("#37474F")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Category", 40f, currentY + 15f, paint)
            canvas.drawText("Amount (IQD)", 340f, currentY + 15f, paint)
            canvas.drawText("% of Total", 460f, currentY + 15f, paint)
            currentY += 24f

            val totalExpense = if (reportData.totalExpense > 0) reportData.totalExpense else 1.0
            var rowIndex = 0
            for ((category, amount) in reportData.categoryBreakdown.entries.sortedByDescending { it.value }) {
                if (rowIndex % 2 == 1) {
                    paint.color = Color.parseColor("#F9FBE7")
                    canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 18f, paint)
                }

                val percentage = (amount / totalExpense) * 100

                paint.color = Color.parseColor("#263238")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 9.5f
                canvas.drawText(category, 40f, currentY + 13f, paint)
                canvas.drawText(CurrencyUtils.formatIQD(amount), 340f, currentY + 13f, paint)
                canvas.drawText(String.format(Locale.US, "%.1f%%", percentage), 460f, currentY + 13f, paint)

                currentY += 18f
                rowIndex++
            }

            currentY += 20f

            // --- Section: Itemized Transactions ---
            paint.color = Color.parseColor("#004D40")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText("Itemized Transactions (${reportData.expenses.size} records)", 30f, currentY, paint)
            currentY += 12f

            // Transactions Table Header
            paint.color = Color.parseColor("#ECEFF1")
            canvas.drawRoundRect(RectF(30f, currentY, pageWidth - 30f, currentY + 22f), 4f, 4f, paint)

            paint.color = Color.parseColor("#37474F")
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Date", 38f, currentY + 15f, paint)
            canvas.drawText("Description / Merchant", 105f, currentY + 15f, paint)
            canvas.drawText("Category", 255f, currentY + 15f, paint)
            canvas.drawText("Method & Tags", 365f, currentY + 15f, paint)
            canvas.drawText("Amount (IQD)", 465f, currentY + 15f, paint)
            currentY += 24f

            // Draw transaction rows with pagination support
            for ((index, item) in reportData.expenses.withIndex()) {
                if (currentY > pageHeight - 60f) {
                    // Draw Footer on previous page
                    drawFooter(canvas, paint, pageWidth, pageHeight, pageNumber)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    currentY = 40f
                    // Repeat Mini Header
                    paint.color = Color.parseColor("#004D40")
                    paint.textSize = 11f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("Monthly Financial Report - ${reportData.monthYearName} (Cont.)", 30f, currentY, paint)
                    currentY += 15f

                    // Repeat Table Header
                    paint.color = Color.parseColor("#ECEFF1")
                    canvas.drawRoundRect(RectF(30f, currentY, pageWidth - 30f, currentY + 20f), 4f, 4f, paint)
                    paint.color = Color.parseColor("#37474F")
                    paint.textSize = 9f
                    canvas.drawText("Date", 38f, currentY + 14f, paint)
                    canvas.drawText("Description / Merchant", 105f, currentY + 14f, paint)
                    canvas.drawText("Category", 255f, currentY + 14f, paint)
                    canvas.drawText("Method & Tags", 365f, currentY + 14f, paint)
                    canvas.drawText("Amount (IQD)", 465f, currentY + 14f, paint)
                    currentY += 22f
                }

                if (index % 2 == 1) {
                    paint.color = Color.parseColor("#F5F5F5")
                    canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 18f, paint)
                }

                val dateStr = CurrencyUtils.formatDate(item.dateMillis, "dd/MM")
                val isIncome = item.type == "INCOME"

                // Date
                paint.color = Color.parseColor("#546E7A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 8.5f
                canvas.drawText(dateStr, 38f, currentY + 13f, paint)

                // Description (truncated if too long)
                paint.color = Color.parseColor("#212121")
                val desc = if (item.title.length > 24) item.title.take(22) + "..." else item.title
                canvas.drawText(desc, 105f, currentY + 13f, paint)

                // Category
                paint.color = Color.parseColor("#455A64")
                val cat = if (item.categoryName.length > 18) item.categoryName.take(16) + "..." else item.categoryName
                canvas.drawText(cat, 255f, currentY + 13f, paint)

                // Method + Tags
                paint.color = Color.parseColor("#78909C")
                val tagsText = if (item.tags.isNotBlank()) " [${item.tags}]" else ""
                val methodTag = "${item.paymentMethod}$tagsText"
                val truncatedMethodTag = if (methodTag.length > 18) methodTag.take(16) + "..." else methodTag
                canvas.drawText(truncatedMethodTag, 365f, currentY + 13f, paint)

                // Amount
                paint.color = if (isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val sign = if (isIncome) "+" else "-"
                canvas.drawText("$sign${CurrencyUtils.formatIQD(item.amount)}", 465f, currentY + 13f, paint)

                currentY += 18f
            }

            // Draw Footer on last page
            drawFooter(canvas, paint, pageWidth, pageHeight, pageNumber)
            pdfDocument.finishPage(page)

            // Save PDF to cache dir
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val sanitizedMonth = reportData.monthYearName.replace(" ", "_")
            val pdfFile = File(reportsDir, "IQD_Report_${sanitizedMonth}_${System.currentTimeMillis()}.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun drawKpiCard(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        amount: String,
        bgColorHex: String,
        textColorHex: String
    ) {
        paint.color = Color.parseColor(bgColorHex)
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // Card Title
        paint.color = Color.parseColor("#546E7A")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 10f, y + 18f, paint)

        // Card Amount
        paint.color = Color.parseColor(textColorHex)
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(amount, x + 10f, y + 38f, paint)
    }

    private fun drawFooter(
        canvas: Canvas,
        paint: Paint,
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int
    ) {
        val y = pageHeight - 25f
        paint.color = Color.parseColor("#CFD8DC")
        canvas.drawLine(30f, y - 10f, pageWidth - 30f, y - 10f, paint)

        paint.color = Color.parseColor("#90A4AE")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated with IQD Expense Tracker • Official Statement", 30f, y + 5f, paint)

        val pageStr = "Page $pageNumber"
        val pw = paint.measureText(pageStr)
        canvas.drawText(pageStr, pageWidth - 30f - pw, y + 5f, paint)
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "IQD Monthly Expense Report")
            putExtra(Intent.EXTRA_TEXT, "Here is the monthly financial expense report in Iraqi Dinar (IQD).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Monthly Report PDF"))
    }

    fun viewPdf(context: Context, pdfFile: File) {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            sharePdf(context, pdfFile)
        }
    }
}
