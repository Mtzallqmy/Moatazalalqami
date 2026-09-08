package me.rerere.rikkahub.ui.components.message

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.terminal.TerminalEventBus

@Composable
fun LiveTerminalCard(sessionId: String) {
    val all by TerminalEventBus.sessions.collectAsState()
    val state = all[sessionId] ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember(sessionId) { mutableStateOf(true) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF101418)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.live_terminal_title, state.name), color = Color(0xFFE6EDF3), style = MaterialTheme.typography.titleSmall)
                if (state.running) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            Text(state.workingDirectory, color = Color(0xFF8B949E), fontFamily = FontFamily.Monospace)
            state.currentCommand?.let { Text("$ $it", color = Color(0xFF7EE787), fontFamily = FontFamily.Monospace) }
            if (expanded) {
                Text(
                    text = state.screen.ifBlank { stringResource(R.string.live_terminal_waiting) },
                    color = Color(0xFFC9D1D9),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                )
                if (state.stderr.isNotBlank()) Text(state.stderr, color = Color(0xFFFF7B72), fontFamily = FontFamily.Monospace)
            }
            Text(
                stringResource(
                    R.string.live_terminal_status,
                    stringResource(if (state.running) R.string.live_terminal_running else R.string.live_terminal_stopped),
                    state.exitCode?.toString() ?: "—",
                    (state.updatedAt - state.startedAt).coerceAtLeast(0) / 1000,
                ),
                color = Color(0xFF8B949E),
            )
            Row {
                TextButton(onClick = { expanded = !expanded }) { Text(stringResource(if (expanded) R.string.live_terminal_collapse else R.string.live_terminal_expand)) }
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("terminal", state.screen))
                }) { Text(stringResource(R.string.copy)) }
                if (state.running) TextButton(onClick = { scope.launch { TerminalEventBus.stop(sessionId) } }) { Text(stringResource(R.string.stop)) }
                TextButton(onClick = {
                    context.packageManager.getLaunchIntentForPackage("com.termux")?.let {
                        context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }) { Text(stringResource(R.string.live_terminal_open)) }
            }
        }
    }
}
