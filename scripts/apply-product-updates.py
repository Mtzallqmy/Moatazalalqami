from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PAGE = ROOT / "app/src/main/java/me/rerere/rikkahub/ui/pages/extensions/workspace/WorkspaceDetailPage.kt"


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
print("Moataz Alaqami product updates applied")
