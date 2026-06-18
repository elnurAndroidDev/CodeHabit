package com.tabletap.githubcontribsapp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tabletap.githubcontribsapp.presentation.MainActivity
import com.tabletap.githubcontribsapp.presentation.ui.theme.GithubContribsAppTheme
import dagger.hilt.android.EntryPointAccessors

class ContribsWidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val ep = EntryPointAccessors.fromApplication<WidgetEntryPoint>(applicationContext)
        val hasGitHub = ep.tokenRepository().getToken() != null
        val hasLeetCode = ep.leetCodeProfileRepository().getUsername() != null

        setContent {
            GithubContribsAppTheme {
                ConfigScreen(
                    hasGitHub = hasGitHub,
                    hasLeetCode = hasLeetCode,
                    onConfirm = { source ->
                        ep.widgetSourcePrefs().set(widgetId, source)
                        val mgr = AppWidgetManager.getInstance(this@ContribsWidgetConfigActivity)
                        ContribsAppWidgetProvider.scheduleUpdate(
                            this@ContribsWidgetConfigActivity, mgr, widgetId
                        )
                        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onOpenApp = {
                        startActivity(Intent(this@ContribsWidgetConfigActivity, MainActivity::class.java))
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
private fun ConfigScreen(
    hasGitHub: Boolean,
    hasLeetCode: Boolean,
    onConfirm: (WidgetSource) -> Unit,
    onOpenApp: () -> Unit,
    onCancel: () -> Unit
) {
    val sources = buildList {
        if (hasGitHub) add(WidgetSource.GitHub)
        if (hasLeetCode) add(WidgetSource.LeetCode)
        if (hasGitHub && hasLeetCode) add(WidgetSource.Combined)
    }

    Scaffold { padding ->
        if (sources.isEmpty()) {
            NoSourcesContent(
                modifier = Modifier.padding(padding),
                onOpenApp = onOpenApp,
                onCancel = onCancel
            )
        } else {
            SourcePickerContent(
                modifier = Modifier.padding(padding),
                sources = sources,
                onConfirm = onConfirm,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun SourcePickerContent(
    sources: List<WidgetSource>,
    onConfirm: (WidgetSource) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(sources.first()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(text = "Choose data source", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.selectableGroup()) {
            sources.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = source == selected,
                            onClick = { selected = source },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = source == selected, onClick = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = source.displayName, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onConfirm(selected) }) { Text("Add Widget") }
        }
    }
}

@Composable
private fun NoSourcesContent(
    onOpenApp: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign in to the app first to add this widget.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onOpenApp) { Text("Open App") }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

private val WidgetSource.displayName: String
    get() = when (this) {
        WidgetSource.GitHub -> "GitHub"
        WidgetSource.LeetCode -> "LeetCode"
        WidgetSource.Combined -> "Combined"
    }
