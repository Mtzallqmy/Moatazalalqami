package me.rerere.rikkahub.data.model

import me.rerere.rikkahub.data.ai.tools.LocalToolOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantDefaultsTest {
    @Test
    fun `new assistants start with core agent capabilities`() {
        val tools = Assistant().localTools.toSet()

        val required = setOf(
            LocalToolOption.TimeInfo,
            LocalToolOption.AskUser,
            LocalToolOption.Tts,
            LocalToolOption.JavascriptEngine,
            LocalToolOption.Files,
            LocalToolOption.Browser,
            LocalToolOption.SubAgents,
            LocalToolOption.Workflows,
            LocalToolOption.CostGuards,
        )

        assertEquals(required, DEFAULT_AGENT_LOCAL_TOOLS.toSet())
        assertTrue(tools.containsAll(required))
    }

    @Test
    fun `new assistants do not silently enable sensitive device control`() {
        val tools = Assistant().localTools.toSet()

        val sensitive = setOf(
            LocalToolOption.SmsSend,
            LocalToolOption.TelephonyInfo,
            LocalToolOption.CallLog,
            LocalToolOption.SmsInbox,
            LocalToolOption.CameraPhoto,
            LocalToolOption.MicRecorder,
            LocalToolOption.ScreenAutomation,
            LocalToolOption.KeyboardControl,
            LocalToolOption.Shizuku,
        )

        assertFalse(tools.any { it in sensitive })
    }
}
