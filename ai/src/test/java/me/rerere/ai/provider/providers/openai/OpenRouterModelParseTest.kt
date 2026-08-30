package me.rerere.ai.provider.providers.openai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import me.rerere.ai.provider.Modality
import me.rerere.ai.provider.ModelAbility
import me.rerere.ai.provider.ModelCapability
import me.rerere.ai.provider.ModelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterModelParseTest {
    private val imageToolModel = """
        {
          "id": "google/gemini-2.5-flash-image",
          "name": "Gemini 2.5 Flash Image",
          "context_length": 1048576,
          "architecture": {
            "input_modalities": ["image", "text"],
            "output_modalities": ["image", "text"]
          },
          "supported_parameters": ["tools", "tool_choice", "reasoning"],
          "pricing": { "prompt": "0.0000003", "completion": "0.0000025" }
        }
    """.trimIndent()

    @Test
    fun parses_image_tool_reasoning_and_pricing() {
        val obj = Json.parseToJsonElement(imageToolModel).jsonObject
        val m = openRouterModelFromJson(obj)!!
        assertEquals("google/gemini-2.5-flash-image", m.modelId)
        assertEquals("Gemini 2.5 Flash Image", m.displayName)
        assertEquals(ModelType.CHAT, m.type)
        assertTrue(Modality.IMAGE in m.outputModalities)
        assertTrue(Modality.IMAGE in m.inputModalities)
        assertTrue(ModelAbility.TOOL in m.abilities)
        assertTrue(ModelAbility.REASONING in m.abilities)
        assertTrue(ModelCapability.IMAGE_GENERATION in m.capabilities)
        assertTrue(ModelCapability.TOOL_CALLING in m.capabilities)
        assertTrue(ModelCapability.REASONING in m.capabilities)
        assertEquals(1048576, m.contextLength)
        assertEquals(0.0000003, m.pricePromptPerToken!!, 1e-12)
    }

    @Test
    fun parses_extended_audio_video_document_and_server_tool_capabilities() {
        val json = """
            {"id":"x/omni-agent","name":"Omni Agent",
             "architecture":{
               "input_modalities":["text","image","audio","video","document"],
               "output_modalities":["text","audio"]},
             "supported_parameters":["tools","reasoning","web_search","url_context","file_search","code_interpreter"]}
        """.trimIndent()
        val m = openRouterModelFromJson(Json.parseToJsonElement(json).jsonObject)!!
        assertEquals(ModelType.CHAT, m.type)
        assertTrue(ModelCapability.AUDIO_INPUT in m.capabilities)
        assertTrue(ModelCapability.AUDIO_OUTPUT in m.capabilities)
        assertTrue(ModelCapability.VIDEO_INPUT in m.capabilities)
        assertTrue(ModelCapability.DOCUMENT_INPUT in m.capabilities)
        assertTrue(ModelCapability.WEB_SEARCH in m.capabilities)
        assertTrue(ModelCapability.URL_CONTEXT in m.capabilities)
        assertTrue(ModelCapability.FILE_SEARCH in m.capabilities)
        assertTrue(ModelCapability.CODE_EXECUTION in m.capabilities)
    }

    @Test
    fun image_only_model_stays_in_image_picker() {
        val json = """
            {"id":"x/image-only","name":"Image Only",
             "architecture":{"input_modalities":["text"],"output_modalities":["image"]},
             "supported_parameters":[]}
        """.trimIndent()
        val m = openRouterModelFromJson(Json.parseToJsonElement(json).jsonObject)!!
        assertEquals(ModelType.IMAGE, m.type)
        assertTrue(Modality.IMAGE in m.outputModalities)
        assertTrue(ModelCapability.IMAGE_GENERATION in m.capabilities)
    }

    @Test
    fun text_only_model_has_no_image_or_tool() {
        val json = """
            {"id":"x/text-only","name":"Text Only",
             "architecture":{"input_modalities":["text"],"output_modalities":["text"]},
             "supported_parameters":["max_tokens","temperature"]}
        """.trimIndent()
        val m = openRouterModelFromJson(Json.parseToJsonElement(json).jsonObject)!!
        assertEquals(ModelType.CHAT, m.type)
        assertTrue(Modality.IMAGE !in m.outputModalities)
        assertTrue(ModelAbility.TOOL !in m.abilities)
        assertTrue(m.capabilities.isEmpty())
    }
}
