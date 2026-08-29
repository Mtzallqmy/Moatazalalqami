# AL Agent capability architecture

AL Agent treats a provider model as a set of capabilities rather than assuming that every model is text-only.

## Capability sources

Capability resolution is layered:

1. **Provider metadata** — authoritative when the provider publishes modalities, supported parameters, or server tools.
2. **Model registry fallback** — known Gemini, Claude, GPT, DeepSeek, Qwen, Grok, Kimi, GLM, MiniMax, MiMo and related ids inherit known vision/tool/reasoning behavior when a provider endpoint returns only id/name.
3. **User model configuration** — explicit saved settings remain supported for custom or private endpoints.

Explicit provider/model metadata takes precedence over registry defaults.

## Chat and agent routing

The common model layer can currently represent:

- text and image input/output through the legacy modality contract;
- audio input/output;
- video input/output;
- document input/output;
- image generation;
- function/tool calling;
- reasoning;
- provider-side web search and URL context;
- provider-side file search;
- provider-side code execution.

`ChatService` uses the unified `supportsToolCalling()` resolver rather than requiring the legacy `ModelAbility.TOOL` flag. Models discovered from provider metadata or the registry can therefore use the same client tools, MCP tools, skills and workspace tools when appropriate.

## Default AL Agent profile

New assistants start with a useful non-device-control core:

- Files
- Browser
- JavaScript engine
- Ask User
- TTS
- Sub-agents
- Workflows
- Cost guards
- Time information

Existing assistants are upgraded only when their tool list is exactly the historical untouched `TimeInfo`-only default. Any customized tool list is preserved.

Sensitive phone/device capabilities such as SMS sending, telephony data, call logs, microphone recording, camera capture, screen automation, keyboard control and Shizuku are not silently enabled by the new default. Existing approval and permission boundaries continue to apply.

## Media status

The underlying message model already supports `Image`, `Audio`, `Video`, and `Document` parts. Image-producing conversational models remain available inside normal chat instead of being forced into the standalone image picker. The speech subsystem already supports multiple TTS backends; the next integration stage persists generated speech as first-class `Audio` message attachments in addition to playback.

## Compatibility rule

Do not broaden persisted enums in a way that breaks existing serialized settings. New provider capabilities are stored in additive metadata and helper functions, while the older TEXT/IMAGE modality fields remain readable for existing installations and backups.
