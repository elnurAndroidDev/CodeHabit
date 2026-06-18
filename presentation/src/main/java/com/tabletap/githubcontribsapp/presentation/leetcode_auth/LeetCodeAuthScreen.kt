package com.tabletap.githubcontribsapp.presentation.leetcode_auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tabletap.githubcontribsapp.presentation.ui.theme.GithubContribsAppTheme

@Composable
fun LeetCodeAuthScreen(
    viewModel: LeetCodeAuthViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                LeetCodeAuthEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    LeetCodeAuthContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun LeetCodeAuthContent(
    state: LeetCodeAuthState,
    onIntent: (LeetCodeAuthIntent) -> Unit
) {
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Connect LeetCode",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your public LeetCode username to see your submission heatmap alongside GitHub.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("LeetCode username") },
            placeholder = { Text("e.g. neetcode") },
            singleLine = true,
            isError = state.error != null,
            supportingText = if (state.error != null) {
                { Text(text = state.error, color = MaterialTheme.colorScheme.error) }
            } else null,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { onIntent(LeetCodeAuthIntent.Submit(username)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotBlank()
            ) {
                Text("Continue")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { onIntent(LeetCodeAuthIntent.Skip) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now")
            }
        }
    }
}

@Preview(showBackground = true, name = "Idle")
@Composable
private fun LeetCodeAuthIdlePreview() {
    GithubContribsAppTheme {
        Surface { LeetCodeAuthContent(state = LeetCodeAuthState(), onIntent = {}) }
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun LeetCodeAuthErrorPreview() {
    GithubContribsAppTheme {
        Surface {
            LeetCodeAuthContent(
                state = LeetCodeAuthState(error = "LeetCode user not found"),
                onIntent = {}
            )
        }
    }
}
