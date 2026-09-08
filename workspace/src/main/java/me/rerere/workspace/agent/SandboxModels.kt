package me.rerere.workspace.agent

import java.io.File

enum class SandboxCapability {
    READ_WORKSPACE, WRITE_WORKSPACE, EXECUTE_COMMAND,
    NETWORK_READ, NETWORK_WRITE, GIT_READ, GIT_WRITE,
    GITHUB_READ, GITHUB_WRITE, DEVICE_CONTROL, EXTERNAL_STORAGE,
}

enum class SandboxStatus { ACTIVE, SUSPENDED, DESTROYED, STALE }
enum class SandboxNetworkPolicy { DENY, READ_ONLY, ALLOW }

data class SandboxResourceLimits(
    val maxDurationMs: Long = 30 * 60_000L,
    val maxToolCalls: Int = 200,
    val maxShellCommands: Int = 80,
    val maxSubagents: Int = 4,
    val maxModifiedFiles: Int = 100,
    val maxBytesWritten: Long = 100L * 1024 * 1024,
    val maxFileBytes: Long = 20L * 1024 * 1024,
    val maxNetworkRequests: Int = 100,
    val maxGithubWrites: Int = 20,
    val maxRetries: Int = 3,
    val maxOutputBytes: Int = 512 * 1024,
) {
    init {
        require(maxDurationMs > 0 && maxToolCalls > 0 && maxShellCommands > 0)
        require(maxModifiedFiles > 0 && maxBytesWritten > 0 && maxFileBytes > 0)
        require(maxNetworkRequests >= 0 && maxGithubWrites >= 0 && maxRetries >= 0)
        require(maxOutputBytes > 0)
    }
}

data class SandboxConfig(
    val conversationId: String,
    val capabilities: Set<SandboxCapability> = emptySet(),
    val networkPolicy: SandboxNetworkPolicy = SandboxNetworkPolicy.DENY,
    val limits: SandboxResourceLimits = SandboxResourceLimits(),
    val expiresAfterMs: Long = 24 * 60 * 60_000L,
)

data class SandboxUsage(
    val toolCalls: Int = 0,
    val shellCommands: Int = 0,
    val subagents: Int = 0,
    val modifiedFiles: Int = 0,
    val bytesWritten: Long = 0,
    val networkRequests: Int = 0,
    val githubWrites: Int = 0,
    val retries: Int = 0,
)

data class SandboxSession(
    val sessionId: String,
    val conversationId: String,
    val workingDirectory: File,
    val homeDirectory: File,
    val tempDirectory: File,
    val stateDirectory: File,
    val environment: Map<String, String>,
    val terminalSessionId: String? = null,
    val processIds: Set<String> = emptySet(),
    val createdAt: Long,
    val lastActiveAt: Long,
    val status: SandboxStatus,
    val resourceLimits: SandboxResourceLimits,
    val filesystemPermissions: Set<SandboxCapability>,
    val networkPolicy: SandboxNetworkPolicy,
    val expiresAfterMs: Long,
    val usage: SandboxUsage = SandboxUsage(),
)

class SandboxPolicy(private val capabilities: Set<SandboxCapability>) {
    fun allows(capability: SandboxCapability): Boolean = capability in capabilities

    fun require(capability: SandboxCapability) {
        check(allows(capability)) { "Sandbox capability denied: $capability" }
    }

    fun requireNetwork(write: Boolean) {
        require(if (write) SandboxCapability.NETWORK_WRITE else SandboxCapability.NETWORK_READ)
    }
}

enum class SandboxOperation {
    TOOL_CALL, SHELL_COMMAND, SUBAGENT, MODIFY_FILE, WRITE_BYTES,
    NETWORK_REQUEST, GITHUB_WRITE, RETRY,
}

class SandboxLimitExceeded(message: String) : IllegalStateException(message)
