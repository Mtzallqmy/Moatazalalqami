package me.rerere.rikkahub.terminal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import me.rerere.rikkahub.reliability.SecretRedactor

class TerminalEventBusTest {
    @Test fun transcriptIsBounded() {
        val id = "test-terminal-${System.nanoTime()}"
        TerminalEventBus.publish(TerminalEvent.SessionStarted(id, "test", "/workspace"))
        TerminalEventBus.publish(TerminalEvent.Stdout(id, "x".repeat(140 * 1024)))
        val state = requireNotNull(TerminalEventBus.sessions.value[id])
        assertTrue(state.screen.length < 140 * 1024)
        assertTrue(state.screen.contains("truncated"))
        assertFalse(state.running.not())
    }

    @Test fun transcriptRedactsRegisteredSecrets() {
        val id = "test-terminal-secret-${System.nanoTime()}"
        val secret = "not-a-real-secret-12345".toCharArray()
        SecretRedactor.registerKnownSecret(secret)
        TerminalEventBus.publish(TerminalEvent.Stdout(id, "credential=${secret.concatToString()}"))
        val transcript = requireNotNull(TerminalEventBus.sessions.value[id]).screen
        assertFalse(transcript.contains(secret.concatToString()))
        assertTrue(transcript.contains("redacted"))
        secret.fill('\u0000')
    }
}
