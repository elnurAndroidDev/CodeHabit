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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tabletap.githubcontribsapp.domain.Contrib
import kotlinx.coroutines.launch
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
        Heatmap(contributions = contributions, scrollState = scrollState)
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
    modifier: Modifier = Modifier
) {
    if (contributions.isEmpty()) return

    val cellSize = 12.dp
    val gap = 2.dp
    val weeks = contributions.chunked(7)
    val totalHeight = (cellSize + gap) * 7 - gap
    val totalWidth = (cellSize + gap) * weeks.size - gap

    Box(modifier = modifier.horizontalScroll(scrollState)) {
        Canvas(
            modifier = Modifier
                .width(totalWidth)
                .height(totalHeight)
        ) {
            val cellPx = cellSize.toPx()
            val gapPx = gap.toPx()

            weeks.forEachIndexed { weekIndex, week ->
                week.forEachIndexed { dayIndex, contrib ->
                    drawRoundRect(
                        color = contributionColor(contrib.count),
                        topLeft = Offset(
                            x = weekIndex * (cellPx + gapPx),
                            y = dayIndex * (cellPx + gapPx)
                        ),
                        size = Size(cellPx, cellPx),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
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

private fun contributionColor(count: Int): Color = when {
    count == 0 -> Color(0xFFEBEDF0)
    count <= 3 -> Color(0xFF9BE9A8)
    count <= 6 -> Color(0xFF40C463)
    count <= 9 -> Color(0xFF30A14E)
    else       -> Color(0xFF216E39)
}
