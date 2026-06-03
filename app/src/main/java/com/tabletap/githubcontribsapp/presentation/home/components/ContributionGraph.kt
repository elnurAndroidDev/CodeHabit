package com.tabletap.githubcontribsapp.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlin.math.roundToInt

@Composable
fun ContributionGraph(
    title: String,
    contributions: List<Contrib>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (contributions.isNotEmpty()) {
            LeetCodeHeatmap(contributions = contributions, scrollState = scrollState)
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalScrollBar(
            scrollState = scrollState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

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
    val palette = leetcodePaletteInts

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

                val measured = textMeasurer.measure(
                    AnnotatedString(monthAbbrev(block.month)),
                    style = monthLabelTextStyle
                )
                val labelX = (x + (monthWidthPx - measured.size.width) / 2f).coerceAtLeast(x)
                drawText(textLayoutResult = measured, topLeft = Offset(x = labelX, y = 0f))

                block.weekColumns.forEachIndexed { weekIndex, week ->
                    val columnX = x + weekIndex * (cellPx + gapPx)
                    week.forEachIndexed { dayIndex, date ->
                        if (date != null) {
                            val count = byDate[date] ?: 0
                            drawRoundRect(
                                color = Color(palette[paletteIndex(count)]),
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
