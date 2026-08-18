package com.example.data.gemini

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScannedReceiptResult(
    val merchantName: String = "",
    val amount: Double = 0.0,
    val dateString: String = "",
    val suggestedCategory: String = "Other Expenses",
    val suggestedTags: List<String> = emptyList(),
    val notes: String = "",
    val items: List<String> = emptyList(),
    val confidence: String = "High",
    val rawText: String? = null
)

data class GeminiPart(
    val text: String? = null,
    val inline_data: GeminiInlineData? = null
)

data class GeminiInlineData(
    val mime_type: String,
    val data: String
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiRequest(
    val contents: List<GeminiContent>
)
