package com.tabletap.githubcontribsapp.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tabletap.githubcontribsapp.presentation.ui.theme.GithubContribsAppTheme

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLeetCodeAuth: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthEffect.NavigateToLeetCodeAuth -> onNavigateToLeetCodeAuth()
            }
        }
    }

    AuthScreenContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun AuthScreenContent(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit
) {
    var token by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GitHub Contributions",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your personal access token to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("GitHub Token") },
            placeholder = { Text("ghp_xxxxxxxxxxxx") },
            singleLine = true,
            isError = state.error != null,
            supportingText = if (state.error != null) {
                { Text(text = state.error, color = MaterialTheme.colorScheme.error) }
            } else null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { onIntent(AuthIntent.Login(token)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = token.isNotBlank()
            ) {
                Text("Log In")
            }
        }
    }
}

@Preview(showBackground = true, name = "Idle")
@Composable
private fun AuthScreenIdlePreview() {
    GithubContribsAppTheme {
        Surface { AuthScreenContent(state = AuthState(), onIntent = {}) }
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun AuthScreenLoadingPreview() {
    GithubContribsAppTheme {
        Surface { AuthScreenContent(state = AuthState(isLoading = true), onIntent = {}) }
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun AuthScreenErrorPreview() {
    GithubContribsAppTheme {
        Surface {
            AuthScreenContent(
                state = AuthState(error = "Invalid token (HTTP 401)"),
                onIntent = {}
            )
        }
    }
}
