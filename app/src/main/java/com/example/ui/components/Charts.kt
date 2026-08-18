package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.utils.CurrencyUtils
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class ChartSliceData(
    val label: String,
    val value: Double,
    val color: Color
)

data class BarChartItem(
    val label: String,
    val value: Double,
    val isHighlighted: Boolean = false,
    val secondaryValue: Double = 0.0
)

@Composable
fun DonutPieChart(
    slices: List<ChartSliceData>,
    totalAmount: Double,
    modifier: Modifier = Modifier,
    title: String = "Total Expenses"
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val total = if (totalAmount > 0) totalAmount else slices.sumOf { it.value }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.categoryBreakdown,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (selectedIndex != null) {
                TextButton(
                    onClick = { selectedIndex = null },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(strings.resetFilter, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (slices.isEmpty() || total <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noDataForPeriod,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("donut_chart_canvas")
                        .pointerInput(slices, total) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = tapOffset.x - center.x
                                val dy = tapOffset.y - center.y
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                val radius = size.width / 2f
                                val innerRadius = radius * 0.58f

                                if (dist in innerRadius..radius) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    // Start from top (-90 degrees)
                                    val normalizedAngle = (angle + 90f) % 360f

                                    var currentAngle = 0f
                                    var clickedIndex: Int? = null
                                    for (i in slices.indices) {
                                        val sweep = ((slices[i].value / total) * 360f).toFloat()
                                        if (normalizedAngle >= currentAngle && normalizedAngle < currentAngle + sweep) {
                                            clickedIndex = i
                                            break
                                        }
                                        currentAngle += sweep
                                    }
                                    selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex
                                } else {
                                    selectedIndex = null
                                }
                            }
                        }
                ) {
                    val strokeWidth = 36.dp.toPx()
                    val selectedStrokeWidth = 44.dp.toPx()
                    val arcSize = Size(size.width - selectedStrokeWidth, size.height - selectedStrokeWidth)
                    val arcTopLeft = Offset(selectedStrokeWidth / 2f, selectedStrokeWidth / 2f)

                    var startAngle = -90f
                    slices.forEachIndexed { index, slice ->
                        val isSelected = selectedIndex == index
                        val sweepAngle = ((slice.value / total) * 360f * animationProgress.value).toFloat()
                        val currentStroke = if (isSelected) selectedStrokeWidth else strokeWidth

                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = if (sweepAngle > 0.5f) sweepAngle - 2f else sweepAngle, // Small gap
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }

                // Center Text Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (selectedIndex != null && selectedIndex!! < slices.size) {
                        val selected = slices[selectedIndex!!]
                        val pct = (selected.value / total) * 100
                        Text(
                            text = strings.localizeCategory(selected.label),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = selected.color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CurrencyUtils.formatLocalizedAmount(selected.value, language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%% of total", pct),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.formatLocalizedAmount(total, language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${slices.size} categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.take(6).forEachIndexed { index, slice ->
                    val isSelected = selectedIndex == index
                    val pct = (slice.value / total) * 100
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) slice.color.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedIndex = if (selectedIndex == index) null else index }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.localizeCategory(slice.label),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 140.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = slice.color.copy(alpha = 0.18f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f%%", pct),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = slice.color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = CurrencyUtils.formatLocalizedAmount(slice.value, language),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    title: String = "Spending Trend",
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.secondary
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    val maxValue = items.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (selectedBarIndex != null && selectedBarIndex!! < items.size) {
                val item = items[selectedBarIndex!!]
                Text(
                    text = "${item.label}: ${CurrencyUtils.formatLocalizedAmount(item.value, language)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Max: ${CurrencyUtils.formatLocalizedAmount(maxValue, language)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty() || maxValue <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noTransactionsFound,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Bars container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(items, maxValue) {
                            detectTapGestures { tapOffset ->
                                val slotWidth = size.width / items.size
                                val index = (tapOffset.x / slotWidth).toInt().coerceIn(0, items.size - 1)
                                selectedBarIndex = if (selectedBarIndex == index) null else index
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 24.dp.toPx() // Reserve space for bottom labels
                    val slotWidth = canvasWidth / items.size
                    val barWidth = (slotWidth * 0.55f).coerceIn(8.dp.toPx(), 36.dp.toPx())

                    // Draw 3 subtle horizontal dashed guide lines
                    val guideColor = Color.LightGray.copy(alpha = 0.35f)
                    for (i in 1..3) {
                        val guideY = canvasHeight * (i / 4f)
                        drawLine(
                            color = guideColor,
                            start = Offset(0f, guideY),
                            end = Offset(canvasWidth, guideY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    items.forEachIndexed { index, item ->
                        val isSelected = selectedBarIndex == index
                        val x = (index * slotWidth) + (slotWidth - barWidth) / 2f
                        val barHeight = (item.value / maxValue * canvasHeight * animationProgress.value).toFloat()
                        val y = canvasHeight - barHeight

                        val color = when {
                            isSelected -> highlightColor
                            item.isHighlighted -> highlightColor
                            else -> barColor
                        }

                        // Draw Bar with rounded top
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight.coerceAtLeast(3.dp.toPx())),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            // Labels row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedBarIndex == index
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected || item.isHighlighted) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

