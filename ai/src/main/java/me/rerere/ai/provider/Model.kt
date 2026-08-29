package me.rerere.ai.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Model(
    val modelId: String = "",
    val displayName: String = "",
    val id: Uuid = Uuid.random(),
    val type: ModelType = ModelType.CHAT,
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),
    val inputModalities: List<Modality> = listOf(Modality.TEXT),
    val outputModalities: List<Modality> = listOf(Modality.TEXT),
    val abilities: List<ModelAbility> = emptyList(),
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
