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

Dedicated `ModelType.IMAGE` endpoints are routed from the normal conversation surface through the provider's `generateImage` API. If the latest user message also contains input images, the same route uses `editImage`. Partial provider images replace their preview slot in-place and the final image is stored as a first-class chat image. Multimodal `CHAT` models are deliberately kept on the streaming response path so they can return mixed text, reasoning, tools and image output in one turn instead of being incorrectly reduced to an image-only endpoint.

Image-generation preview slot selection must remain a non-null `Int` after routing. Provider-supplied nullable partial indexes are stabilized in a local value before slot selection so Kotlin compilation and multi-preview replacement stay deterministic across provider implementations.

OpenAI Responses-style server tools and image-generation streaming continue through the common stream decoder. Provider-specific features are only exposed when the selected provider/model advertises or implements them.

## Default AL Agent profile

New assistants start with a useful agent core:

- Files
- Browser
- JavaScript engine
- Termux programming/runtime bridge
- Ask User
- TTS
- Sub-agents
- Workflows
- Cost guards
- Time information

The Termux bridge provides command execution, captured stdout/stderr and interactive-session support when the user has installed/configured Termux. Existing tool approvals, runtime permissions, loop guards, time budgets and hardline command blocks remain in force; enabling the capability does not bypass those controls.

Workspace-attached conversations also receive their workspace tools, allowing the agent to inspect/read/write project files and use the embedded Linux development environment from the same conversation.

Existing assistants are upgraded only when their tool list is exactly the historical untouched `TimeInfo`-only default. Any customized tool list is preserved.

Sensitive phone/device capabilities such as SMS sending, telephony data, call logs, microphone recording, camera capture, screen automation, keyboard control and Shizuku are not silently enabled by the new default. Existing approval and permission boundaries continue to apply.

## Media status

The underlying message model supports `Image`, `Audio`, `Video`, and `Document` parts. Image-producing conversational models remain available inside normal chat instead of being forced into the standalone image picker, while dedicated image endpoints now generate/edit images directly from chat.

The `text_to_speech` agent tool uses the user's selected TTS provider through the shared multi-provider speech subsystem, synthesizes a durable audio file under app-private storage, and returns it as a first-class `UIMessagePart.Audio` attachment together with a machine-readable text result. This keeps generated speech replayable from the conversation instead of limiting TTS to transient background playback.

Provider-native media decoding remains capability-gated: AL Agent only requests or decodes audio, image, video, document, server-search, or code-execution features when the selected provider/model actually advertises or implements them. Generic TTS and document transformation remain fallbacks rather than pretending unsupported model-native features exist.

## Compatibility rule

Do not broaden persisted enums in a way that breaks existing serialized settings. New provider capabilities are stored in additive metadata and helper functions, while the older TEXT/IMAGE modality fields remain readable for existing installations and backups.
