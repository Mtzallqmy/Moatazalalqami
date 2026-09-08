package me.rerere.workspace.agent

import me.rerere.workspace.WorkspaceManager
import java.io.File
import java.util.UUID
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/** Owns isolated per-run directories and the processes registered in WorkspaceManager. */
class SandboxManager(
    private val baseDirectory: File,
    private val workspaceManager: WorkspaceManager,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val sessions = ConcurrentHashMap<String, SandboxSession>()

    init {
        baseDirectory.mkdirs()
        baseDirectory.listFiles()?.filter(File::isDirectory)?.forEach { root ->
            load(root)?.let { sessions[it.sessionId] = it.copy(status = SandboxStatus.STALE, processIds = emptySet()) }
        }
    }

    @Synchronized
    fun createSandbox(config: SandboxConfig): SandboxSession {
        require(config.conversationId.isNotBlank())
        val id = "sandbox_${UUID.randomUUID().toString().take(12)}"
        val root = File(baseDirectory, id).canonicalFile
        check(root.path.startsWith(baseDirectory.canonicalPath + File.separator))
        val home = File(root, "home").apply { mkdirs() }
        val workspace = File(root, "workspace").apply { mkdirs() }
        val tmp = File(root, "tmp").apply { mkdirs() }
        val state = File(root, "state").apply { mkdirs() }
        val now = clock()
        return SandboxSession(
            sessionId = id,
            conversationId = config.conversationId,
            workingDirectory = workspace,
            homeDirectory = home,
            tempDirectory = tmp,
            stateDirectory = state,
            environment = mapOf("HOME" to home.path, "TMPDIR" to tmp.path, "PWD" to workspace.path),
            createdAt = now,
            lastActiveAt = now,
            status = SandboxStatus.ACTIVE,
            resourceLimits = config.limits,
            filesystemPermissions = config.capabilities,
            networkPolicy = config.networkPolicy,
            expiresAfterMs = config.expiresAfterMs,
        ).also { sessions[id] = it; persist(it) }
    }

    fun getSandbox(sessionId: String): SandboxSession? = sessions[sessionId]

    @Synchronized
    fun resumeSandbox(sessionId: String): SandboxSession = update(sessionId) {
        check(it.status == SandboxStatus.SUSPENDED || it.status == SandboxStatus.STALE) {
            "Sandbox is not resumable"
        }
        it.copy(status = SandboxStatus.ACTIVE, lastActiveAt = clock())
    }

    @Synchronized
    fun suspendSandbox(sessionId: String): SandboxSession = update(sessionId) {
        check(it.status == SandboxStatus.ACTIVE) { "Sandbox is not active" }
        workspaceManager.killAllBackground(it.sessionId)
        it.copy(status = SandboxStatus.SUSPENDED, processIds = emptySet(), lastActiveAt = clock())
    }

    /** Refuses to erase a Git worktree with uncommitted changes unless force is explicit. */
    @Synchronized
    fun destroySandbox(sessionId: String, force: Boolean = false): Boolean {
        val session = sessions[sessionId] ?: return false
        val git = File(session.workingDirectory, ".git")
        if (git.exists() && !force) {
            val status = runCatching {
                ProcessBuilder("git", "-C", session.workingDirectory.path, "status", "--porcelain")
                    .start().inputStream.bufferedReader().readText()
            }.getOrDefault("")
            check(status.isBlank()) { "Refusing to destroy sandbox with uncommitted work" }
        }
        workspaceManager.killAllBackground(session.sessionId)
        session.tempDirectory.deleteRecursively()
        session.homeDirectory.deleteRecursively()
        // A dirty repository was rejected above. A clean worktree is therefore safe to
        // remove, while force is the only path that can remove a dirty one.
        session.workingDirectory.deleteRecursively()
        session.stateDirectory.deleteRecursively()
        session.workingDirectory.parentFile?.delete()
        sessions[sessionId] = session.copy(status = SandboxStatus.DESTROYED, processIds = emptySet())
        return true
    }

    @Synchronized
    fun cleanupExpiredSandboxes(now: Long = clock()): List<String> = sessions.values
        .filter { it.status != SandboxStatus.DESTROYED && now - it.lastActiveAt > it.expiresAfterMs }
        .mapNotNull { session -> runCatching { destroySandbox(session.sessionId) }.getOrNull()?.let { session.sessionId } }

    @Synchronized
    fun consume(sessionId: String, operation: SandboxOperation, amount: Long = 1): SandboxSession = update(sessionId) { s ->
        check(s.status == SandboxStatus.ACTIVE) { "Sandbox is not active" }
        val u = s.usage
        val next = when (operation) {
            SandboxOperation.TOOL_CALL -> u.copy(toolCalls = u.toolCalls + amount.toInt())
            SandboxOperation.SHELL_COMMAND -> u.copy(shellCommands = u.shellCommands + amount.toInt())
            SandboxOperation.SUBAGENT -> u.copy(subagents = u.subagents + amount.toInt())
            SandboxOperation.MODIFY_FILE -> u.copy(modifiedFiles = u.modifiedFiles + amount.toInt())
            SandboxOperation.WRITE_BYTES -> u.copy(bytesWritten = u.bytesWritten + amount)
            SandboxOperation.NETWORK_REQUEST -> u.copy(networkRequests = u.networkRequests + amount.toInt())
            SandboxOperation.GITHUB_WRITE -> u.copy(githubWrites = u.githubWrites + amount.toInt())
            SandboxOperation.RETRY -> u.copy(retries = u.retries + amount.toInt())
        }
        val l = s.resourceLimits
        if (next.toolCalls > l.maxToolCalls || next.shellCommands > l.maxShellCommands ||
            next.subagents > l.maxSubagents || next.modifiedFiles > l.maxModifiedFiles ||
            next.bytesWritten > l.maxBytesWritten || next.networkRequests > l.maxNetworkRequests ||
            next.githubWrites > l.maxGithubWrites || next.retries > l.maxRetries ||
            clock() - s.createdAt > l.maxDurationMs
        ) throw SandboxLimitExceeded("Sandbox resource limit exceeded: $operation")
        s.copy(usage = next, lastActiveAt = clock())
    }

    private fun update(id: String, block: (SandboxSession) -> SandboxSession): SandboxSession {
        val current = sessions[id] ?: error("Sandbox not found: $id")
        return block(current).also { sessions[id] = it; persist(it) }
    }

    private fun persist(session: SandboxSession) {
        if (session.status == SandboxStatus.DESTROYED) return
        val props = Properties().apply {
            setProperty("sessionId", session.sessionId)
            setProperty("conversationId", session.conversationId)
            setProperty("createdAt", session.createdAt.toString())
            setProperty("lastActiveAt", session.lastActiveAt.toString())
            setProperty("expiresAfterMs", session.expiresAfterMs.toString())
            setProperty("capabilities", session.filesystemPermissions.joinToString(",", transform = SandboxCapability::name))
            setProperty("networkPolicy", session.networkPolicy.name)
        }
        session.stateDirectory.mkdirs()
        File(session.stateDirectory, "session.properties").outputStream().use { props.store(it, null) }
    }

    private fun load(root: File): SandboxSession? = runCatching {
        val state = File(root, "state")
        val props = Properties().apply {
            File(state, "session.properties").inputStream().use { input -> load(input) }
        }
        val caps = props.getProperty("capabilities").orEmpty().split(',')
            .filter(String::isNotBlank).mapTo(mutableSetOf()) { SandboxCapability.valueOf(it) }
        val home = File(root, "home")
        val workspace = File(root, "workspace")
        val tmp = File(root, "tmp")
        SandboxSession(
            sessionId = props.getProperty("sessionId"), conversationId = props.getProperty("conversationId"),
            workingDirectory = workspace, homeDirectory = home, tempDirectory = tmp, stateDirectory = state,
            environment = mapOf("HOME" to home.path, "TMPDIR" to tmp.path, "PWD" to workspace.path),
            createdAt = props.getProperty("createdAt").toLong(), lastActiveAt = props.getProperty("lastActiveAt").toLong(),
            status = SandboxStatus.STALE, resourceLimits = SandboxResourceLimits(), filesystemPermissions = caps,
            networkPolicy = SandboxNetworkPolicy.valueOf(props.getProperty("networkPolicy")),
            expiresAfterMs = props.getProperty("expiresAfterMs").toLong(),
        )
    }.getOrNull()
}
