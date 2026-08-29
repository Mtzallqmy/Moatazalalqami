package me.rerere.ai.provider

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCapabilityTest {
    @Test
    fun `legacy and discovered tool capability are both accepted`() {
        assertTrue(Model(abilities = listOf(ModelAbility.TOOL)).supportsToolCalling())
        assertTrue(Model(capabilities = setOf(ModelCapability.TOOL_CALLING)).supportsToolCalling())
        assertTrue(Model(supportedParameters = listOf("tools")).supportsToolCalling())
        assertFalse(Model().supportsToolCalling())
    }

    @Test
    fun `known provider model ids inherit registry abilities when endpoint omits metadata`() {
        val gemini = Model(modelId = "gemini-2.5-pro")
        val claude = Model(modelId = "claude-sonnet-4-6")
        val gpt = Model(modelId = "gpt-5.6-sol")

        assertTrue(Modality.IMAGE in gemini.inputModalities)
        assertTrue(gemini.supportsToolCalling())
        assertTrue(gemini.supportsReasoning())
        assertTrue(Modality.IMAGE in claude.inputModalities)
        assertTrue(claude.supportsToolCalling())
        assertTrue(Modality.IMAGE in gpt.inputModalities)
        assertTrue(gpt.supportsToolCalling())
    }

    @Test
    fun `explicit provider metadata overrides registry fallback`() {
        val explicit = Model(
            modelId = "gemini-2.5-pro",
            inputModalities = listOf(Modality.TEXT),
            abilities = emptyList(),
            capabilities = setOf(ModelCapability.AUDIO_INPUT),
        )

        assertFalse(Modality.IMAGE in explicit.inputModalities)
        assertFalse(explicit.supportsToolCalling())
        assertTrue(explicit.supportsAudioInput())
    }

    @Test
    fun `multimedia capability helpers reflect provider metadata`() {
        val model = Model(
            capabilities = setOf(
                ModelCapability.AUDIO_INPUT,
                ModelCapability.AUDIO_OUTPUT,
                ModelCapability.VIDEO_INPUT,
                ModelCapability.DOCUMENT_INPUT,
            ),
        )

        assertTrue(model.supportsAudioInput())
        assertTrue(model.supportsAudioOutput())
        assertTrue(model.supportsVideoInput())
        assertTrue(model.supportsDocumentInput())
        assertFalse(model.supportsVideoOutput())
        assertFalse(model.supportsDocumentOutput())
    }

    @Test
    fun `image generation works for image models and multimodal chat models`() {
        assertTrue(Model(type = ModelType.IMAGE).supportsImageGeneration())
        assertTrue(Model(outputModalities = listOf(Modality.TEXT, Modality.IMAGE)).supportsImageGeneration())
        assertTrue(Model(capabilities = setOf(ModelCapability.IMAGE_GENERATION)).supportsImageGeneration())
        assertFalse(Model().supportsImageGeneration())
    }
}
