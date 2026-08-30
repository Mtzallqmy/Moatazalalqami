from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GENERATION_HANDLER = ROOT / "app/src/main/java/me/rerere/rikkahub/data/ai/GenerationHandler.kt"
ASSISTANT = ROOT / "app/src/main/java/me/rerere/rikkahub/data/model/Assistant.kt"


def required_replace(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        print(f"already applied: {label}")
        return text
    if old not in text:
        raise RuntimeError(f"Expected pattern not found for {label}")
    print(f"updated: {label}")
    return text.replace(old, new, 1)


text = GENERATION_HANDLER.read_text(encoding="utf-8")
text = required_replace(
    text,
    "import me.rerere.ai.provider.CustomBody\nimport me.rerere.ai.provider.Model\n",
    "import me.rerere.ai.provider.CustomBody\nimport me.rerere.ai.provider.ImageEditParams\nimport me.rerere.ai.provider.ImageGenerationParams\nimport me.rerere.ai.provider.Model\nimport me.rerere.ai.provider.ModelType\n",
    "native image generation imports",
)

# Kotlin cannot reliably smart-cast a nullable property when it is read more than once
# (for example when the property comes from an interface/custom getter). Stabilize the
# provider-supplied partial index in a local val so the selected slot is always Int.
old_slot_routing = """            imageFlow.collect { item ->
                val slot = when {
                    item.partialImageIndex != null -> item.partialImageIndex
                    !item.partial && partialSlots.isNotEmpty() -> partialSlots.first()
                    else -> nextSlot++
                }
                nextSlot = maxOf(nextSlot, slot + 1)
                if (item.partial) partialSlots += slot else partialSlots -= slot
"""
fixed_slot_routing = """            imageFlow.collect { item ->
                val partialImageIndex = item.partialImageIndex
                val slot: Int = when {
                    partialImageIndex != null -> partialImageIndex
                    !item.partial && partialSlots.isNotEmpty() -> partialSlots.first()
                    else -> nextSlot++
                }
                nextSlot = maxOf(nextSlot, slot + 1)
                if (item.partial) partialSlots += slot else partialSlots -= slot
"""
if old_slot_routing in text:
    print("updated: stable image generation slot index")
    text = text.replace(old_slot_routing, fixed_slot_routing, 1)

anchor = """        val provider = model.findProvider(settings.providers) ?: error(\"Provider not found\")
        val providerImpl = providerManager.getProviderByType(provider)

        // Replay safety: scan the input messages for tools that were Approved + began
"""
replacement = """        val provider = model.findProvider(settings.providers) ?: error(\"Provider not found\")
        val providerImpl = providerManager.getProviderByType(provider)

        // Dedicated image endpoints are real generation models, not text-chat endpoints. Route
        // them through the provider's image API while keeping multimodal CHAT models on the
        // normal streaming path (those models can still return text + image/tool parts together).
        if (model.type == ModelType.IMAGE) {
            val latestUserMessage = messages.lastOrNull { it.role == MessageRole.USER }
                ?: error(\"Image generation requires a user message\")
            val prompt = latestUserMessage.toText().trim()
            require(prompt.isNotBlank()) { \"Image generation requires a text prompt\" }

            onBeforeModelRequest()
            processingStatus.value = \"Generating image…\"

            val imageMessageId = Uuid.random()
            val imageParts = linkedMapOf<Int, UIMessagePart.Image>()
            val partialSlots = sortedSetOf<Int>()
            var nextSlot = 0

            val inputImages = latestUserMessage.parts
                .filterIsInstance<UIMessagePart.Image>()
                .map(UIMessagePart.Image::url)
                .filter(String::isNotBlank)

            val imageFlow = if (inputImages.isEmpty()) {
                providerImpl.generateImage(
                    providerSetting = provider,
                    params = ImageGenerationParams(
                        model = model,
                        prompt = prompt,
                        customHeaders = model.customHeaders,
                        customBody = model.customBodies,
                    ),
                )
            } else {
                providerImpl.editImage(
                    providerSetting = provider,
                    params = ImageEditParams(
                        model = model,
                        prompt = prompt,
                        images = inputImages,
                        customHeaders = model.customHeaders,
                        customBody = model.customBodies,
                    ),
                )
            }

            imageFlow.collect { item ->
                val partialImageIndex = item.partialImageIndex
                val slot: Int = when {
                    partialImageIndex != null -> partialImageIndex
                    !item.partial && partialSlots.isNotEmpty() -> partialSlots.first()
                    else -> nextSlot++
                }
                nextSlot = maxOf(nextSlot, slot + 1)
                if (item.partial) partialSlots += slot else partialSlots -= slot

                val raw = item.data.trim()
                val imageUrl = when {
                    raw.startsWith(\"data:\") ||
                        raw.startsWith(\"http://\") ||
                        raw.startsWith(\"https://\") ||
                        raw.startsWith(\"file://\") ||
                        raw.startsWith(\"content://\") -> raw
                    else -> \"data:${item.mimeType};base64,$raw\"
                }
                imageParts[slot] = UIMessagePart.Image(url = imageUrl)

                emit(
                    GenerationChunk.Messages(
                        messages + UIMessage(
                            id = imageMessageId,
                            role = MessageRole.ASSISTANT,
                            parts = imageParts.toSortedMap().values.toList(),
                            modelId = model.id,
                        )
                    )
                )
            }

            if (imageParts.isEmpty()) {
                error(\"Provider returned no image data\")
            }

            processingStatus.value = null
            emit(
                GenerationChunk.Messages(
                    messages + UIMessage(
                        id = imageMessageId,
                        role = MessageRole.ASSISTANT,
                        parts = imageParts.toSortedMap().values.toList(),
                        finishedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                        modelId = model.id,
                    )
                )
            )
            return@flow
        }

        // Replay safety: scan the input messages for tools that were Approved + began
"""
text = required_replace(text, anchor, replacement, "image-model chat routing")
GENERATION_HANDLER.write_text(text, encoding="utf-8")

assistant = ASSISTANT.read_text(encoding="utf-8")
assistant = required_replace(
    assistant,
    """    LocalToolOption.JavascriptEngine,
    LocalToolOption.Files,
    LocalToolOption.Browser,
""",
    """    LocalToolOption.JavascriptEngine,
    LocalToolOption.Termux,
    LocalToolOption.Files,
    LocalToolOption.Browser,
""",
    "AL Agent Termux programming environment",
)
ASSISTANT.write_text(assistant, encoding="utf-8")

print("Capability-aware provider routing and AL Agent defaults applied")
