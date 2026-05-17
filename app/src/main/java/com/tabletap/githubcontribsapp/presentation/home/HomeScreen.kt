package com.tabletap.githubcontribsapp.presentation.home

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tabletap.githubcontribsapp.domain.Contrib
import com.tabletap.githubcontribsapp.presentation.ui.theme.GithubContribsAppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToAuth: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToAuth -> onNavigateToAuth()
            }
        }
    }

    HomeScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (state.username.isNotEmpty()) "@${state.username}" else "") },
                actions = {
                    TextButton(onClick = { onIntent(HomeIntent.Logout) }) {
                        Text("Log out")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> ErrorContent(
                    message = state.error,
                    onRetry = { onIntent(HomeIntent.LoadData) }
                )
                else -> ContributionsContent(contributions = state.contributions)
            }
        }
    }
}

@Composable
private fun ContributionsContent(contributions: List<Contrib>) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Contributions",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        ContributionGraph(contributions = contributions, scrollState = scrollState)
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalScrollBar(
            scrollState = scrollState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ContributionGraph(
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

@Preview(showBackground = true, name = "Loading")
@Composable
private fun HomeLoadingPreview() {
    GithubContribsAppTheme {
        HomeScreenContent(state = HomeState(isLoading = true), onIntent = {})
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun HomeErrorPreview() {
    GithubContribsAppTheme {
        HomeScreenContent(state = HomeState(error = "Network error"), onIntent = {})
    }
}

@Preview(showBackground = true, name = "Content")
@Composable
private fun HomeContentPreview() {
    val contributions = (1..364).map { Contrib(date = "2025-01-$it", count = (0..15).random()) }
    GithubContribsAppTheme {
        HomeScreenContent(
            state = HomeState(username = "octocat", contributions = contributions),
            onIntent = {}
        )
    }
}
