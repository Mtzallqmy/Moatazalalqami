package me.rerere.rikkahub.terminal

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.rerere.rikkahub.reliability.SecretRedactor

sealed interface TerminalEvent {
    val sessionId: String
    val timestamp: Long
    data class SessionStarted(override val sessionId: String, val name: String, val workingDirectory: String, override val timestamp: Long = System.currentTimeMillis()) : TerminalEvent
    data class CommandStarted(override val sessionId: String, val command: String, override val timestamp: Long = System.currentTimeMillis()) : TerminalEvent
    data class Stdout(override val sessionId: String, val chunk: String, override val timestamp: Long = System.currentTimeMillis()) : TerminalEvent
    data class Stderr(override val sessionId: String, val chunk: String, override val timestamp: Long = System.currentTimeMillis()) : TerminalEvent
    data class ScreenUpdated(override val sessionId: String, val screen: String, override val timestamp: Long = System.currentTimeMillis()) : TerminalEvent
    data class WaitingForInput(override val sessionId: String, val prompt: String?, override val timestamp: Long = System.currentTimeMillis()) : TerminalEvent
    data class CommandFinished(override val sessionId: String, val exitCode: Int?, override val timestamp: Long = System.currentTimeMillis()) : TerminalEvent
    data class CommandFailed(override val sessionId: String, val error: String, override val timestamp: Long = System.currentTimeMillis()) : TerminalEvent
    data class SessionStopped(override val sessionId: String, override val timestamp: Long = System.currentTimeMillis()) : TerminalEvent
}

data class TerminalSessionState(
    val sessionId: String,
    val name: String = sessionId,
    val workingDirectory: String = "~",
    val currentCommand: String? = null,
    val screen: String = "",
    val stderr: String = "",
    val running: Boolean = false,
    val exitCode: Int? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = startedAt,
)

/** Process-independent stream consumed by Compose; producers publish at most every 100 ms. */
object TerminalEventBus {
    private const val MAX_TRANSCRIPT_CHARS = 128 * 1024
    private const val MIN_SCREEN_INTERVAL_MS = 100L
    private val mutableEvents = MutableSharedFlow<TerminalEvent>(extraBufferCapacity = 128)
    private val mutableSessions = MutableStateFlow<Map<String, TerminalSessionState>>(emptyMap())
    private val lastScreenEmission = mutableMapOf<String, Long>()
    private val stopHandlers = mutableMapOf<String, suspend () -> Unit>()

    val events: SharedFlow<TerminalEvent> = mutableEvents.asSharedFlow()
    val sessions: StateFlow<Map<String, TerminalSessionState>> = mutableSessions.asStateFlow()

    @Synchronized
    fun publish(event: TerminalEvent) {
        if (event is TerminalEvent.ScreenUpdated) {
            val last = lastScreenEmission[event.sessionId] ?: 0L
            if (event.timestamp - last < MIN_SCREEN_INTERVAL_MS) return
            lastScreenEmission[event.sessionId] = event.timestamp
        }
        mutableEvents.tryEmit(event)
        mutableSessions.update { old ->
            val previous = old[event.sessionId] ?: TerminalSessionState(event.sessionId)
            val next = when (event) {
                is TerminalEvent.SessionStarted -> previous.copy(name = event.name, workingDirectory = event.workingDirectory, running = true, startedAt = event.timestamp, updatedAt = event.timestamp)
                is TerminalEvent.CommandStarted -> previous.copy(currentCommand = event.command, running = true, exitCode = null, updatedAt = event.timestamp)
                is TerminalEvent.Stdout -> previous.copy(screen = bounded(previous.screen + event.chunk), updatedAt = event.timestamp)
                is TerminalEvent.Stderr -> previous.copy(stderr = bounded(previous.stderr + event.chunk), updatedAt = event.timestamp)
                is TerminalEvent.ScreenUpdated -> previous.copy(screen = bounded(event.screen), updatedAt = event.timestamp)
                is TerminalEvent.WaitingForInput -> previous.copy(running = true, updatedAt = event.timestamp)
                is TerminalEvent.CommandFinished -> previous.copy(running = false, exitCode = event.exitCode, updatedAt = event.timestamp)
                is TerminalEvent.CommandFailed -> previous.copy(running = false, stderr = bounded(previous.stderr + "\n" + event.error), updatedAt = event.timestamp)
                is TerminalEvent.SessionStopped -> previous.copy(running = false, updatedAt = event.timestamp)
            }
            old + (event.sessionId to next)
        }
    }

    @Synchronized fun registerStopHandler(sessionId: String, handler: suspend () -> Unit) { stopHandlers[sessionId] = handler }
    suspend fun stop(sessionId: String) { stopHandlers[sessionId]?.invoke() }
    @Synchronized fun forget(sessionId: String) { stopHandlers.remove(sessionId); lastScreenEmission.remove(sessionId) }

    private fun bounded(value: String): String {
        val redacted = SecretRedactor.redact(value)
        if (redacted.length <= MAX_TRANSCRIPT_CHARS) return redacted
        val marker = "…[older output truncated]\n"
        return marker + redacted.takeLast(MAX_TRANSCRIPT_CHARS - marker.length)
    }
}
