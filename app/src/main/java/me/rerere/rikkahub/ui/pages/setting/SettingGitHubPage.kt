package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.github.GitHubApiClient
import me.rerere.rikkahub.github.GitHubPreferences
import me.rerere.rikkahub.security.AgentKillSwitch
import me.rerere.rikkahub.security.AndroidCredentialVault
import me.rerere.rikkahub.security.CredentialVault
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.theme.CustomColors
import org.koin.compose.koinInject

@Composable
fun SettingGitHubPage() {
    val vault: CredentialVault = koinInject()
    val client: GitHubApiClient = koinInject()
    val preferences: GitHubPreferences = koinInject()
    val killSwitch: AgentKillSwitch = koinInject()
    val allowed by preferences.allowedRepositories.collectAsState()
    val killed by killSwitch.enabled.collectAsState()
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf("") }
    var status by remember {
        mutableStateOf(
            if (vault.contains(AndroidCredentialVault.GITHUB_PAT)) R.string.github_status_encrypted
            else R.string.github_status_not_connected,
        )
    }
    var username by remember { mutableStateOf<String?>(null) }
    var repositories by remember { mutableStateOf<List<String>>(emptyList()) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun refresh() = scope.launch {
        status = R.string.github_status_testing
        runCatching { client.request("GET", "/user").body }.fold(
            onSuccess = { userBody ->
                val login = Regex("\"login\"\\s*:\\s*\"([^\"]+)\"").find(userBody)?.groupValues?.get(1)
                username = login
                status = R.string.github_status_connected
                val body = client.request("GET", "/user/repos?per_page=100&sort=updated").body
                repositories = Regex("\"full_name\"\\s*:\\s*\"([^\"]+)\"").findAll(body).map { it.groupValues[1] }.distinct().toList()
            },
            onFailure = { status = R.string.github_status_failed },
        )
    }

    Scaffold(
        topBar = { LargeFlexibleTopAppBar(title = { Text(stringResource(R.string.setting_page_github)) }, navigationIcon = { BackButton() }, scrollBehavior = scroll, colors = CustomColors.topBarColors) },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(
                    if (status == R.string.github_status_connected && username != null) {
                        stringResource(status, username!!)
                    } else stringResource(status),
                )
            }
            item {
                OutlinedTextField(value = token, onValueChange = { token = it.trim() }, label = { Text(stringResource(R.string.github_pat_label)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (token.isNotBlank()) {
                            vault.encrypt(AndroidCredentialVault.GITHUB_PAT, token.toCharArray())
                            token = ""
                            refresh()
                        }
                    }) { Text(stringResource(R.string.github_save_test)) }
                    TextButton(onClick = { refresh() }, enabled = vault.contains(AndroidCredentialVault.GITHUB_PAT)) { Text(stringResource(R.string.github_test)) }
                    TextButton(onClick = { vault.delete(AndroidCredentialVault.GITHUB_PAT); preferences.clear(); repositories = emptyList(); username = null; status = R.string.github_status_disconnected }) { Text(stringResource(R.string.github_disconnect)) }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) { Text(stringResource(R.string.github_kill_switch)); Text(stringResource(R.string.github_kill_switch_desc)) }
                    Switch(checked = killed, onCheckedChange = killSwitch::setEnabled)
                }
            }
            item { Text(stringResource(R.string.github_repository_allowlist)) }
            items(repositories, key = { it }) { repo ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repo.lowercase() in allowed, onCheckedChange = { checked -> preferences.setAllowed(if (checked) allowed + repo else allowed - repo.lowercase()) })
                    Text(repo)
                }
            }
        }
    }
}
