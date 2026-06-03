package com.tabletap.githubcontribsapp.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tabletap.githubcontribsapp.domain.Contrib
import com.tabletap.githubcontribsapp.presentation.home.components.ContributionGraph
import com.tabletap.githubcontribsapp.presentation.ui.theme.GithubContribsAppTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToAuth: () -> Unit = {},
    onNavigateToLeetCodeAuth: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToAuth -> onNavigateToAuth()
                HomeEffect.NavigateToLeetCodeAuth -> onNavigateToLeetCodeAuth()
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
                title = { Text(text = topBarTitle(state)) },
                actions = { OverflowMenu(onIntent) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SourceSection(
                title = "GitHub" + state.githubUsername.takeIf { it.isNotEmpty() }
                    ?.let { " · @$it" }.orEmpty(),
                source = state.github,
                onRetry = { onIntent(HomeIntent.LoadData) },
                onConfigure = null
            )
            Spacer(modifier = Modifier.height(24.dp))
            SourceSection(
                title = "LeetCode" + state.leetcodeUsername?.let { " · @$it" }.orEmpty(),
                source = state.leetcode,
                onRetry = { onIntent(HomeIntent.LoadData) },
                onConfigure = { onIntent(HomeIntent.EditLeetCode) }
            )
            Spacer(modifier = Modifier.height(24.dp))
            SourceSection(
                title = "Combined",
                source = state.combined,
                onRetry = { onIntent(HomeIntent.LoadData) },
                onConfigure = null
            )
        }
    }
}

private fun topBarTitle(state: HomeState): String {
    val parts = buildList {
        if (state.githubUsername.isNotEmpty()) add("@${state.githubUsername}")
        state.leetcodeUsername?.let { add("lc/@$it") }
    }
    return parts.joinToString(" · ").ifEmpty { "Code Habit" }
}

@Composable
private fun OverflowMenu(onIntent: (HomeIntent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = "⋮", style = MaterialTheme.typography.titleLarge)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit LeetCode username") },
                onClick = {
                    expanded = false
                    onIntent(HomeIntent.EditLeetCode)
                }
            )
            DropdownMenuItem(
                text = { Text("Log out") },
                onClick = {
                    expanded = false
                    onIntent(HomeIntent.Logout)
                }
            )
        }
    }
}

@Composable
private fun SourceSection(
    title: String,
    source: SourceState,
    onRetry: () -> Unit,
    onConfigure: (() -> Unit)?
) {
    when (source) {
        SourceState.Loading -> LoadingCard(title)
        is SourceState.Success -> ContributionGraph(
            title = title,
            contributions = source.contribs
        )
        is SourceState.Error -> ErrorCard(title = title, message = source.message, onRetry = onRetry)
        SourceState.NotConfigured -> NotConfiguredCard(title = title, onConfigure = onConfigure)
    }
}

@Composable
private fun LoadingCard(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ErrorCard(title: String, message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun NotConfiguredCard(title: String, onConfigure: (() -> Unit)?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Add your LeetCode username to see your submission heatmap.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (onConfigure != null) {
                    TextButton(onClick = onConfigure) { Text("Connect LeetCode") }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Both loading")
@Composable
private fun HomeLoadingPreview() {
    GithubContribsAppTheme {
        HomeScreenContent(state = HomeState(), onIntent = {})
    }
}

@Preview(showBackground = true, name = "GitHub error, LeetCode not configured")
@Composable
private fun HomeErrorPreview() {
    GithubContribsAppTheme {
        HomeScreenContent(
            state = HomeState(
                github = SourceState.Error("Network error"),
                leetcode = SourceState.NotConfigured
            ),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "Both populated")
@Composable
private fun HomeContentPreview() {
    val sample = (0 until 371).map { Contrib(date = "d$it", count = (0..15).random()) }
    GithubContribsAppTheme {
        HomeScreenContent(
            state = HomeState(
                githubUsername = "octocat",
                leetcodeUsername = "neetcode",
                github = SourceState.Success(sample),
                leetcode = SourceState.Success(sample)
            ),
            onIntent = {}
        )
    }
}
