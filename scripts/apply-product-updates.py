from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PAGE = ROOT / "app/src/main/java/me/rerere/rikkahub/ui/pages/extensions/workspace/WorkspaceDetailPage.kt"
PREFERENCES = ROOT / "app/src/main/java/me/rerere/rikkahub/data/datastore/PreferencesStore.kt"


def required_replace(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        print(f"already applied: {label}")
        return text
    if old not in text:
        raise RuntimeError(f"Expected pattern not found for {label}")
    print(f"updated: {label}")
    return text.replace(old, new, 1)


text = PAGE.read_text(encoding="utf-8")

# The app packages Alpine Linux in linux-rootfs.tar.gz. Keep the fallback URL installer in the
# repository for internal recovery/migrations, but never expose a rootfs URL in the normal UI.
text = required_replace(
    text,
    """            InstallRootfsDialog(\n                workspace = workspace,\n                onDismiss = { showInstallDialog = false },\n                onConfirm = { url ->\n                    vm.installRootfs(url)\n                    showInstallDialog = false\n                },\n            )""",
    """            InstallRootfsDialog(\n                workspace = workspace,\n                onDismiss = { showInstallDialog = false },\n                onConfirm = {\n                    vm.installEmbeddedRootfs()\n                    showInstallDialog = false\n                },\n            )""",
    "embedded Linux repair action",
)

old_dialog = """@Composable\nprivate fun InstallRootfsDialog(\n    workspace: WorkspaceEntity,\n    onDismiss: () -> Unit,\n    onConfirm: (String) -> Unit,\n) {\n    var url by rememberSaveable(workspace.id) { mutableStateOf(DEFAULT_ROOTFS_URL) }\n\n    AlertDialog(\n        onDismissRequest = onDismiss,\n        title = { Text(stringResource(R.string.workspace_detail_install_rootfs)) },\n        text = {\n            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {\n                Text(\n                    text = stringResource(R.string.workspace_detail_install_rootfs_desc, workspace.name),\n                    style = MaterialTheme.typography.bodyMedium,\n                    color = MaterialTheme.colorScheme.onSurfaceVariant,\n                )\n                OutlinedTextField(\n                    value = url,\n                    onValueChange = { url = it },\n                    modifier = Modifier.fillMaxWidth(),\n                    label = { Text(stringResource(R.string.workspace_detail_download_url)) },\n                    maxLines = 5,\n                )\n            }\n        },\n        confirmButton = {\n            TextButton(\n                onClick = { onConfirm(url.trim()) },\n                enabled = url.isNotBlank(),\n            ) {\n                Text(stringResource(R.string.common_install))\n            }\n        },\n        dismissButton = {\n            TextButton(onClick = onDismiss) {\n                Text(stringResource(R.string.common_cancel))\n            }\n        },\n    )\n}\n"""

new_dialog = """@Composable\nprivate fun InstallRootfsDialog(\n    workspace: WorkspaceEntity,\n    onDismiss: () -> Unit,\n    onConfirm: () -> Unit,\n) {\n    AlertDialog(\n        onDismissRequest = onDismiss,\n        title = { Text(\"Repair embedded Linux\") },\n        text = {\n            Text(\n                text = \"${workspace.name} uses the Alpine Linux environment packaged inside this APK. Repair reinstalls it locally; no rootfs URL or separate download is required.\",\n                style = MaterialTheme.typography.bodyMedium,\n                color = MaterialTheme.colorScheme.onSurfaceVariant,\n            )\n        },\n        confirmButton = {\n            TextButton(onClick = onConfirm) {\n                Text(\"Repair\")\n            }\n        },\n        dismissButton = {\n            TextButton(onClick = onDismiss) {\n                Text(stringResource(R.string.common_cancel))\n            }\n        },\n    )\n}\n"""
text = required_replace(text, old_dialog, new_dialog, "embedded Linux repair dialog")

text = text.replace("import androidx.compose.material3.OutlinedTextField\n", "")
text = text.replace("import androidx.compose.runtime.saveable.rememberSaveable\n", "")
text = text.replace(
    "\nprivate const val DEFAULT_ROOTFS_URL =\n    \"https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-arm64.tar.gz\"",
    "",
)

PAGE.write_text(text, encoding="utf-8")

# Upgrade only assistants that still have the historical untouched one-tool default. This is
# deliberately conservative: any assistant whose tool list differs from [TimeInfo] is considered
# user-configured and is left exactly as-is.
prefs = PREFERENCES.read_text(encoding="utf-8")
prefs = required_replace(
    prefs,
    "import me.rerere.rikkahub.data.model.Assistant\n",
    "import me.rerere.rikkahub.data.model.Assistant\nimport me.rerere.rikkahub.data.model.DEFAULT_AGENT_LOCAL_TOOLS\nimport me.rerere.rikkahub.data.ai.tools.LocalToolOption\n",
    "agent default migration imports",
)

legacy_block = """            // One-shot upgrade for existing installs that pre-date the agent-core auto-load:\n            // if a default-IDed assistant has an empty enabledSkills, treat it as fresh and\n            // pin agent-core. Users who deliberately added other skills are untouched.\n            assistants = assistants.map { assistant ->\n                val isDefault = DEFAULT_ASSISTANTS.any { it.id == assistant.id }\n                if (isDefault && assistant.enabledSkills.isEmpty()) {\n                    assistant.copy(enabledSkills = setOf(\"agent-core\"))\n                } else assistant\n            }.toMutableList()\n"""

agent_block = legacy_block + """            // Upgrade the old untouched TimeInfo-only assistant default to the AL Agent tool\n            // profile. Any non-default tool selection is treated as an explicit user choice and\n            // preserved, so upgrades never re-enable tools a user intentionally disabled.\n            assistants = assistants.map { assistant ->\n                if (assistant.localTools == listOf(LocalToolOption.TimeInfo)) {\n                    assistant.copy(localTools = DEFAULT_AGENT_LOCAL_TOOLS)\n                } else assistant\n            }.toMutableList()\n"""

prefs = required_replace(
    prefs,
    legacy_block,
    agent_block,
    "legacy assistant agent-tool migration",
)

PREFERENCES.write_text(prefs, encoding="utf-8")
print("Moataz Alaqami product updates applied")
