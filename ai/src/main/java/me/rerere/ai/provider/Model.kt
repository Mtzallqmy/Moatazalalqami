package me.rerere.ai.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.rerere.ai.registry.ModelRegistry
import kotlin.uuid.Uuid

@Serializable
data class Model(
    val modelId: String = "",
    val displayName: String = "",
    val id: Uuid = Uuid.random(),
    val type: ModelType = ModelType.CHAT,
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),
    /**
     * Provider metadata wins when supplied explicitly. Providers that return only an id/name
     * automatically inherit the project's known model registry so Gemini/Claude/GPT/etc. do not
     * silently degrade to text-only when their endpoint omits capability metadata.
     */
    val inputModalities: List<Modality> = ModelRegistry.MODEL_INPUT_MODALITIES.getData(modelId),
    val outputModalities: List<Modality> = ModelRegistry.MODEL_OUTPUT_MODALITIES.getData(modelId),
    val abilities: List<ModelAbility> = ModelRegistry.MODEL_ABILITIES.getData(modelId),
    val tools: Set<BuiltInTools> = emptySet(),
    val providerOverwrite: ProviderSetting? = null,
    // Optional capability/pricing metadata, populated from provider model metadata.
    val contextLength: Int? = null,
    val supportedParameters: List<String> = emptyList(),
    /** Provider-advertised capabilities that do not fit the legacy TEXT/IMAGE modality enum. */
    val capabilities: Set<ModelCapability> = emptySet(),
    val pricePromptPerToken: Double? = null,
    val priceCompletionPerToken: Double? = null,
)

/**
 * Capability checks intentionally accept both the legacy/manual registry fields and richer
 * provider metadata. This lets old saved models keep working while imported models can advertise
 * their actual features without requiring the user to toggle every ability by hand.
 */
fun Model.supportsToolCalling(): Boolean =
    ModelAbility.TOOL in abilities ||
        ModelCapability.TOOL_CALLING in capabilities ||
        supportedParameters.any { it == "tools" || it == "tool_choice" }

fun Model.supportsReasoning(): Boolean =
    ModelAbility.REASONING in abilities ||
        ModelCapability.REASONING in capabilities ||
        supportedParameters.any { it == "reasoning" || it == "include_reasoning" }

fun Model.supportsImageGeneration(): Boolean =
    type == ModelType.IMAGE ||
        Modality.IMAGE in outputModalities ||
        ModelCapability.IMAGE_GENERATION in capabilities ||
        BuiltInTools.ImageGeneration in tools

fun Model.supportsAudioInput(): Boolean = ModelCapability.AUDIO_INPUT in capabilities
fun Model.supportsAudioOutput(): Boolean = ModelCapability.AUDIO_OUTPUT in capabilities
fun Model.supportsVideoInput(): Boolean = ModelCapability.VIDEO_INPUT in capabilities
fun Model.supportsVideoOutput(): Boolean = ModelCapability.VIDEO_OUTPUT in capabilities
fun Model.supportsDocumentInput(): Boolean = ModelCapability.DOCUMENT_INPUT in capabilities
fun Model.supportsDocumentOutput(): Boolean = ModelCapability.DOCUMENT_OUTPUT in capabilities

@Serializable
enum class ModelType {
    CHAT,
    IMAGE,
    EMBEDDING,
}

@Serializable
enum class Modality {
    TEXT,
    IMAGE,
}

@Serializable
enum class ModelAbility {
    TOOL,
    REASONING,
}

/**
 * Extended provider capabilities used by chat/agent routing without expanding the legacy
 * [Modality] enum and breaking persisted settings or exhaustive UI `when` expressions.
 */
@Serializable
enum class ModelCapability {
    AUDIO_INPUT,
    AUDIO_OUTPUT,
    VIDEO_INPUT,
    VIDEO_OUTPUT,
    DOCUMENT_INPUT,
    DOCUMENT_OUTPUT,
    IMAGE_GENERATION,
    TOOL_CALLING,
    REASONING,
    WEB_SEARCH,
    URL_CONTEXT,
    FILE_SEARCH,
    CODE_EXECUTION,
}

// 模型(提供商)提供的内置工具选项
@Serializable
sealed class BuiltInTools {
    // https://ai.google.dev/gemini-api/docs/google-search?hl=zh-cn
    @Serializable
    @SerialName("search")
    data object Search : BuiltInTools()

    // https://ai.google.dev/gemini-api/docs/url-context?hl=zh-cn
    @Serializable
    @SerialName("url_context")
    data object UrlContext : BuiltInTools()

    @Serializable
    @SerialName("image_generation")
    data object ImageGeneration : BuiltInTools()
}
