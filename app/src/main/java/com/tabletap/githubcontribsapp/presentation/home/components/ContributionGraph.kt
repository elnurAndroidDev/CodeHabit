package com.tabletap.githubcontribsapp.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabletap.githubcontribsapp.domain.Contrib
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt
import java.time.format.TextStyle as JavaTextStyle

enum class HeatmapStyle { GitHub, LeetCode }

@Composable
fun ContributionGraph(
    title: String,
    contributions: List<Contrib>,
    style: HeatmapStyle,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Heatmap(contributions = contributions, scrollState = scrollState, style = style)
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalScrollBar(
            scrollState = scrollState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Heatmap(
    contributions: List<Contrib>,
    scrollState: ScrollState,
    style: HeatmapStyle,
    modifier: Modifier = Modifier
) {
    if (contributions.isEmpty()) return
    when (style) {
        HeatmapStyle.GitHub -> GitHubHeatmap(contributions, scrollState, modifier)
        HeatmapStyle.LeetCode -> LeetCodeHeatmap(contributions, scrollState, modifier)
    }
}

/**
 * GitHub-style: continuous Sun→Sat weeks, uniform spacing, sticky day labels
 * (Mon/Wed/Fri) on the left, GitHub's classic green palette. Month labels above
 * the first week of each month.
 */
@Composable
private fun GitHubHeatmap(
    contributions: List<Contrib>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val weeks = remember(contributions) { buildWeeks(contributions) } ?: return

    val cellSize = 11.dp
    val cornerRadius = 2.dp
    val gap = 2.dp
    val monthLabelHeight = 14.dp
    val palette = githubPalette

    val gridHeight = cellSize * 7 + gap * 6
    val canvasHeight = monthLabelHeight + gridHeight
    val gridWidth = cellSize * weeks.size + gap * (weeks.size - 1)

    val labelColor = Color(0xFF6E7681)
    val textMeasurer = rememberTextMeasurer()
    val monthLabelTextStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    Row(modifier = modifier) {
        DayLabelColumn(
            cellSize = cellSize,
            gap = gap,
            topPadding = monthLabelHeight,
            labelColor = labelColor
        )

        Box(modifier = Modifier.horizontalScroll(scrollState)) {
            Canvas(
                modifier = Modifier
                    .width(gridWidth)
                    .height(canvasHeight)
            ) {
                val cellPx = cellSize.toPx()
                val gapPx = gap.toPx()
                val cornerPx = cornerRadius.toPx()
                val gridTopPx = monthLabelHeight.toPx()

                var lastLabeledMonth: Month? = null
                weeks.forEachIndexed { weekIndex, week ->
                    val x = weekIndex * (cellPx + gapPx)
                    week.days.forEachIndexed { dayIndex, count ->
                        if (count != null) {
                            drawRoundRect(
                                color = paletteColor(palette, count),
                                topLeft = Offset(x = x, y = gridTopPx + dayIndex * (cellPx + gapPx)),
                                size = Size(cellPx, cellPx),
                                cornerRadius = CornerRadius(cornerPx)
                            )
                        }
                    }
                    val month = week.weekFirstDay.month
                    if (month != lastLabeledMonth &&
                        (week.startsNewMonth || (weekIndex == 0 && lastLabeledMonth == null))
                    ) {
                        val measured = textMeasurer.measure(
                            AnnotatedString(monthAbbrev(month)),
                            style = monthLabelTextStyle
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(x = x, y = 0f)
                        )
                        lastLabeledMonth = month
                    }
                }
            }
        }
    }
}

/**
 * LeetCode-style: one block of week-columns per calendar month, with a visible
 * gap between months. Weeks that straddle two months appear in both blocks,
 * showing only the cells belonging to each month — matching leetcode.com's look.
 *
 * Layout algorithm (per month, mirroring the reference Python implementation):
 *   for each week-column in this month:
 *     for dayIndex 0..6 (Sun..Sat):
 *       if the date belongs to this month, draw a cell
 *     advance x by (cellSize + gap)
 *   advance x by monthGap before the next month
 */
@Composable
private fun LeetCodeHeatmap(
    contributions: List<Contrib>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val blocks = remember(contributions) { buildMonthBlocks(contributions) } ?: return
    val byDate = remember(contributions) {
        contributions.mapNotNull { c ->
            runCatching { LocalDate.parse(c.date) to c.count }.getOrNull()
        }.toMap()
    }

    val cellSize = 12.dp
    val cornerRadius = 3.dp
    val gap = 2.dp
    val monthGap = 8.dp
    val monthLabelHeight = 14.dp
    val palette = leetcodePalette

    val gridHeight = cellSize * 7 + gap * 6
    val canvasHeight = monthLabelHeight + gridHeight
    val gridWidth = run {
        var w = 0.dp
        blocks.forEachIndexed { i, block ->
            if (i > 0) w += monthGap
            val n = block.weekColumns.size
            w += cellSize * n + gap * (n - 1).coerceAtLeast(0)
        }
        w
    }

    val labelColor = Color(0xFF6E7681)
    val textMeasurer = rememberTextMeasurer()
    val monthLabelTextStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    Box(modifier = modifier.horizontalScroll(scrollState)) {
        Canvas(
            modifier = Modifier
                .width(gridWidth)
                .height(canvasHeight)
        ) {
            val cellPx = cellSize.toPx()
            val gapPx = gap.toPx()
            val monthGapPx = monthGap.toPx()
            val cornerPx = cornerRadius.toPx()
            val gridTopPx = monthLabelHeight.toPx()

            var x = 0f
            blocks.forEachIndexed { blockIndex, block ->
                if (blockIndex > 0) x += monthGapPx

                val n = block.weekColumns.size
                val monthWidthPx = cellPx * n + gapPx * (n - 1).coerceAtLeast(0)

                // Month label centered above this month's columns
                val measured = textMeasurer.measure(
                    AnnotatedString(monthAbbrev(block.month)),
                    style = monthLabelTextStyle
                )
                val labelX = (x + (monthWidthPx - measured.size.width) / 2f).coerceAtLeast(x)
                drawText(textLayoutResult = measured, topLeft = Offset(x = labelX, y = 0f))

                // Draw each week-column for this month
                block.weekColumns.forEachIndexed { weekIndex, week ->
                    val columnX = x + weekIndex * (cellPx + gapPx)
                    week.forEachIndexed { dayIndex, date ->
                        if (date != null) {
                            val count = byDate[date] ?: 0
                            drawRoundRect(
                                color = paletteColor(palette, count),
                                topLeft = Offset(
                                    x = columnX,
                                    y = gridTopPx + dayIndex * (cellPx + gapPx)
                                ),
                                size = Size(cellPx, cellPx),
                                cornerRadius = CornerRadius(cornerPx)
                            )
                        }
                    }
                }

                x += monthWidthPx
            }
        }
    }
}

@Composable
private fun DayLabelColumn(
    cellSize: Dp,
    gap: Dp,
    topPadding: Dp,
    labelColor: Color
) {
    Column(
        modifier = Modifier.padding(top = topPadding, end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Sun, Mon, Tue, Wed, Thu, Fri, Sat — only odd weekdays get labels.
        listOf("", "Mon", "", "Wed", "", "Fri", "").forEach { label ->
            Box(
                modifier = Modifier.height(cellSize),
                contentAlignment = Alignment.CenterStart
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = labelColor
                    )
                }
            }
        }
    }
}

/** One column in the heatmap: 7 cells from Sunday to Saturday. */
private data class Week(
    val days: List<Int?>,
    val startsNewMonth: Boolean,
    val weekFirstDay: LocalDate
)

private fun buildWeeks(contributions: List<Contrib>): List<Week>? {
    val parsed = contributions.mapNotNull { c ->
        runCatching { LocalDate.parse(c.date) to c.count }.getOrNull()
    }
    if (parsed.isEmpty()) return null

    val byDate = parsed.toMap()
    val sortedDates = parsed.map { it.first }.sorted()
    val first = sortedDates.first()
    val last = sortedDates.last()

    // DayOfWeek.value: MONDAY=1..SUNDAY=7. We want SUN=0, MON=1, ..., SAT=6.
    fun sundayOffset(date: LocalDate): Int = date.dayOfWeek.value % 7

    val gridStart = first.minusDays(sundayOffset(first).toLong())
    val gridEnd = last.plusDays((6 - sundayOffset(last)).toLong())
    val totalWeeks = (ChronoUnit.DAYS.between(gridStart, gridEnd).toInt() + 1) / 7

    var prevMonth = gridStart.month
    return (0 until totalWeeks).map { weekIndex ->
        val weekFirstDay = gridStart.plusDays((weekIndex * 7).toLong())
        val days = (0 until 7).map { dayIndex ->
            byDate[weekFirstDay.plusDays(dayIndex.toLong())]
        }
        val startsNewMonth = weekIndex > 0 && weekFirstDay.month != prevMonth
        prevMonth = weekFirstDay.month
        Week(days = days, startsNewMonth = startsNewMonth, weekFirstDay = weekFirstDay)
    }
}

/** One month's set of week-columns for the LeetCode-style layout. */
private data class MonthBlock(
    val month: Month,
    val weekColumns: List<List<LocalDate?>>
)

private fun buildMonthBlocks(contributions: List<Contrib>): List<MonthBlock>? {
    val parsed = contributions.mapNotNull { c ->
        runCatching { LocalDate.parse(c.date) to c.count }.getOrNull()
    }
    if (parsed.isEmpty()) return null

    val sortedDates = parsed.map { it.first }.sorted()
    val lastDate = sortedDates.last()
    val firstMonth = YearMonth.from(sortedDates.first())
    val lastMonth = YearMonth.from(lastDate)

    val months = mutableListOf<YearMonth>()
    var current = firstMonth
    while (!current.isAfter(lastMonth)) {
        months.add(current)
        current = current.plusMonths(1)
    }

    return months.map { ym ->
        val firstOfMonth = ym.atDay(1)
        val lastOfMonth = ym.atEndOfMonth()
        // Sunday on or before the first of the month.
        val sundayOffset = firstOfMonth.dayOfWeek.value % 7
        val gridStart = firstOfMonth.minusDays(sundayOffset.toLong())

        val weeks = mutableListOf<List<LocalDate?>>()
        var weekStart = gridStart
        while (!weekStart.isAfter(lastOfMonth)) {
            val week = (0 until 7).map { dayIndex ->
                val date = weekStart.plusDays(dayIndex.toLong())
                if (YearMonth.from(date) == ym && !date.isAfter(lastDate)) date else null
            }
            weeks.add(week)
            weekStart = weekStart.plusDays(7)
        }
        MonthBlock(month = ym.month, weekColumns = weeks)
    }
}

private fun monthAbbrev(month: Month): String =
    month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())

private fun paletteColor(palette: List<Color>, count: Int): Color = when {
    count == 0 -> palette[0]
    count <= 3 -> palette[1]
    count <= 6 -> palette[2]
    count <= 9 -> palette[3]
    else       -> palette[4]
}

private val githubPalette = listOf(
    Color(0xFFEBEDF0),
    Color(0xFF9BE9A8),
    Color(0xFF40C463),
    Color(0xFF30A14E),
    Color(0xFF216E39),
)

private val leetcodePalette = listOf(
    Color(0xFFEFEFEF),
    Color(0xFFC6E48B),
    Color(0xFF7BC96F),
    Color(0xFF239A3B),
    Color(0xFF196127),
)

@Composable
private fun HorizontalScrollBar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 6.dp,
    minThumbWidth: Dp = 24.dp,
    trackColor: Color = Color(0xFFEBEDF0),
    thumbColor: Color = Color(0xFF8A8A8A),
) {
    val maxValue = scrollState.maxValue
    val viewportPx = scrollState.viewportSize
    if (maxValue == 0 || viewportPx == 0) return

    val density = LocalDensity.current
    val minThumbPx = with(density) { minThumbWidth.toPx() }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .height(trackHeight)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .pointerInput(maxValue, viewportPx) {
                detectTapGestures { offset ->
                    val trackPx = size.width.toFloat()
                    val contentPx = viewportPx + maxValue.toFloat()
                    val thumbPx = (trackPx * (viewportPx / contentPx))
                        .coerceAtLeast(minThumbPx)
                        .coerceAtMost(trackPx)
                    val travel = (trackPx - thumbPx).coerceAtLeast(0f)
                    if (travel == 0f) return@detectTapGestures
                    val newThumbLeft = (offset.x - thumbPx / 2f).coerceIn(0f, travel)
                    val fraction = newThumbLeft / travel
                    scope.launch {
                        scrollState.animateScrollTo((fraction * maxValue).toInt())
                    }
                }
            }
    ) {
        val trackPx = constraints.maxWidth.toFloat()
        val contentPx = viewportPx + maxValue.toFloat()
        val thumbPx = (trackPx * (viewportPx / contentPx))
            .coerceAtLeast(minThumbPx)
            .coerceAtMost(trackPx)
        val travel = (trackPx - thumbPx).coerceAtLeast(0f)

        val dragState = rememberDraggableState { delta ->
            if (travel > 0f) {
                val scale = maxValue / travel
                scrollState.dispatchRawDelta(delta * scale)
            }
        }

        Box(
            modifier = Modifier
                .offset {
                    val progress = if (maxValue > 0) {
                        scrollState.value.toFloat() / maxValue
                    } else 0f
                    IntOffset((travel * progress).roundToInt(), 0)
                }
                .size(
                    width = with(density) { thumbPx.toDp() },
                    height = trackHeight
                )
                .clip(RoundedCornerShape(50))
                .background(thumbColor)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal
                )
        )
    }
}