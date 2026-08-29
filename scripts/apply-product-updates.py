from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PAGE = ROOT / "app/src/main/java/me/rerere/rikkahub/ui/pages/extensions/workspace/WorkspaceDetailPage.kt"
PREFERENCES = ROOT / "app/src/main/java/me/rerere/rikkahub/data/datastore/PreferencesStore.kt"
CHAT_SERVICE = ROOT / "app/src/main/java/me/rerere/rikkahub/service/ChatService.kt"
LOCAL_TOOLS = ROOT / "app/src/main/java/me/rerere/rikkahub/data/ai/tools/LocalTools.kt"
APP_MODULE = ROOT / "app/src/main/java/me/rerere/rikkahub/di/AppModule.kt"


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

# ChatService historically gated all client tools on the manual ModelAbility.TOOL flag. Use the
# unified capability resolver so provider-advertised `tools`/`tool_choice` metadata and registry
# inference activate the same agent tool path without requiring manual per-model toggles.
chat = CHAT_SERVICE.read_text(encoding="utf-8")
chat = required_replace(
    chat,
    "import me.rerere.ai.provider.ModelAbility\n",
    "import me.rerere.ai.provider.supportsToolCalling\n",
    "capability-aware tool import",
)
chat = required_replace(
    chat,
    "if (!model.abilities.contains(ModelAbility.TOOL)) {",
    "if (!model.supportsToolCalling()) {",
    "capability-aware chat tool gate",
)
CHAT_SERVICE.write_text(chat, encoding="utf-8")

# Generate a durable audio attachment with the user's selected speech provider instead of merely
# firing a transient Speak event. The common chat model already renders UIMessagePart.Audio, and
# OpenAI-compatible tool-result encoding ignores audio parts when feeding results back to a model,
# so models without audio input remain compatible.
tools = LOCAL_TOOLS.read_text(encoding="utf-8")
tools = required_replace(
    tools,
    "import kotlinx.coroutines.withTimeoutOrNull\n",
    "import kotlinx.coroutines.withTimeoutOrNull\nimport kotlinx.coroutines.flow.first\n",
    "TTS flow import",
)
tools = required_replace(
    tools,
    "import me.rerere.rikkahub.utils.writeClipboardText\n",
    "import me.rerere.rikkahub.utils.writeClipboardText\nimport me.rerere.rikkahub.data.datastore.getSelectedTTSProvider\n",
    "selected TTS provider import",
)
tools = required_replace(
    tools,
    "    private val settingsStore: me.rerere.rikkahub.data.datastore.SettingsStore,\n",
    "    private val settingsStore: me.rerere.rikkahub.data.datastore.SettingsStore,\n    private val ttsManager: me.rerere.tts.provider.TTSManager,\n",
    "TTS manager dependency",
)
old_tts = '''    val ttsTool by lazy {
        Tool(
            name = "text_to_speech",
            description = """
                Speak text aloud to the user using the device's text-to-speech engine.
                Use this when the user asks you to read something aloud, or when audio output is appropriate.
                The tool returns immediately; audio plays in the background on the device.
                Provide natural, readable text without markdown formatting.
            """.trimIndent().replace("\\n", " "),
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("text", buildJsonObject {
                            put("type", "string")
                            put("description", "The text to speak aloud")
                        })
                    },
                    required = listOf("text")
                )
            },
            execute = {
                val text = it.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                    ?: error("text is required")
                eventBus.emit(AppEvent.Speak(text))
                val payload = buildJsonObject {
                    put("success", true)
                }
                listOf(UIMessagePart.Text(payload.toString()))
            }
        )
    }
'''
new_tts = '''    val ttsTool by lazy {
        Tool(
            name = "text_to_speech",
            description = """
                Generate spoken audio from text using the user's selected TTS provider and attach the audio to the chat.
                Use this when the user asks for speech, narration, a voice answer, or an audio version of content.
                The generated audio remains available in the conversation for replay instead of being only transient playback.
                Provide natural, readable text without markdown formatting.
            """.trimIndent().replace("\\n", " "),
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("text", buildJsonObject {
                            put("type", "string")
                            put("description", "The text to synthesize as speech")
                        })
                    },
                    required = listOf("text")
                )
            },
            execute = {
                val text = it.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                    ?.trim()
                    ?.takeIf { value -> value.isNotBlank() }
                    ?: error("text is required")
                val provider = settingsStore.settingsFlow.first().getSelectedTTSProvider()
                    ?: error("No TTS provider selected")
                val response = me.rerere.tts.controller.TtsSynthesizer(ttsManager).synthesize(
                    setting = provider,
                    chunk = me.rerere.tts.controller.TtsChunk(index = 0, text = text),
                )
                val extension = when (response.format) {
                    me.rerere.tts.model.AudioFormat.MP3 -> "mp3"
                    me.rerere.tts.model.AudioFormat.WAV -> "wav"
                    me.rerere.tts.model.AudioFormat.OGG -> "ogg"
                    me.rerere.tts.model.AudioFormat.AAC -> "aac"
                    me.rerere.tts.model.AudioFormat.OPUS -> "opus"
                    me.rerere.tts.model.AudioFormat.PCM -> "pcm"
                }
                val outputDir = java.io.File(context.filesDir, "generated_audio").apply { mkdirs() }
                val audioFile = java.io.File(
                    outputDir,
                    "speech-${java.util.UUID.randomUUID()}.$extension",
                ).apply { writeBytes(response.audioData) }
                val payload = buildJsonObject {
                    put("success", true)
                    put("format", response.format.name.lowercase())
                    put("audio_url", audioFile.toURI().toString())
                }
                listOf(
                    UIMessagePart.Audio(url = audioFile.toURI().toString()),
                    UIMessagePart.Text(payload.toString()),
                )
            }
        )
    }
'''
tools = required_replace(tools, old_tts, new_tts, "persistent generated TTS audio")
LOCAL_TOOLS.write_text(tools, encoding="utf-8")

module = APP_MODULE.read_text(encoding="utf-8")
module = required_replace(
    module,
    "            settingsStore = get(),\n            sshHostRepository = get(),\n",
    "            settingsStore = get(),\n            ttsManager = get(),\n            sshHostRepository = get(),\n",
    "LocalTools TTS manager injection",
)
APP_MODULE.write_text(module, encoding="utf-8")

print("Moataz Alaqami product updates applied")
