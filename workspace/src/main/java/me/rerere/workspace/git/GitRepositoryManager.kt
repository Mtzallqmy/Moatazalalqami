package me.rerere.workspace.git

import me.rerere.workspace.WorkspaceCommandResult
import me.rerere.workspace.WorkspaceManager

enum class GitRiskLevel { READ_ONLY, LOW, MEDIUM, HIGH, CRITICAL }

enum class GitOperation(val risk: GitRiskLevel) {
    INIT(GitRiskLevel.LOW), CLONE(GitRiskLevel.MEDIUM), STATUS(GitRiskLevel.READ_ONLY),
    DIFF(GitRiskLevel.READ_ONLY), LOG(GitRiskLevel.READ_ONLY), FETCH(GitRiskLevel.READ_ONLY),
    PULL(GitRiskLevel.MEDIUM), ADD(GitRiskLevel.LOW), COMMIT(GitRiskLevel.MEDIUM),
    BRANCH_LIST(GitRiskLevel.READ_ONLY), BRANCH_CREATE(GitRiskLevel.LOW),
    CHECKOUT(GitRiskLevel.MEDIUM), SWITCH(GitRiskLevel.MEDIUM), MERGE(GitRiskLevel.HIGH),
    REBASE(GitRiskLevel.HIGH), PUSH(GitRiskLevel.HIGH), FORCE_PUSH(GitRiskLevel.CRITICAL),
    TAG_CREATE(GitRiskLevel.MEDIUM), TAG_DELETE(GitRiskLevel.HIGH),
    RESET_HARD(GitRiskLevel.CRITICAL), CLEAN(GitRiskLevel.CRITICAL), BRANCH_DELETE(GitRiskLevel.HIGH),
}

data class GitCommandResult(val operation: GitOperation, val command: List<String>, val result: WorkspaceCommandResult)

class GitRepositoryManager(private val workspaceManager: WorkspaceManager) {
    fun execute(root: String, operation: GitOperation, args: List<String> = emptyList(), approved: Boolean = false): GitCommandResult {
        if (operation.risk >= GitRiskLevel.HIGH) check(approved) { "$operation requires explicit approval" }
        val command = commandFor(operation, args)
        val shell = command.joinToString(" ") { shellQuote(it) }
        return GitCommandResult(operation, command, workspaceManager.executeCommand(root, shell))
    }

    fun commandFor(operation: GitOperation, args: List<String>): List<String> {
        require(args.none { it.contains('\u0000') || it.contains('\n') || it.contains('\r') })
        require(args.none { it.matches(Regex("https?://[^/\\s]+@.*", RegexOption.IGNORE_CASE)) }) {
            "Credentials must not be embedded in Git URLs"
        }
        return when (operation) {
            GitOperation.INIT -> listOf("git", "init") + args
            GitOperation.CLONE -> listOf("git", "clone", "--") + args
            GitOperation.STATUS -> listOf("git", "status", "--short", "--branch")
            GitOperation.DIFF -> listOf("git", "diff", "--no-ext-diff", "--") + args
            GitOperation.LOG -> listOf("git", "log", "--oneline", "--decorate") + args
            GitOperation.FETCH -> listOf("git", "fetch", "--prune") + args
            GitOperation.PULL -> listOf("git", "pull", "--ff-only") + args
            GitOperation.ADD -> listOf("git", "add", "--") + args
            GitOperation.COMMIT -> listOf("git", "commit", "-m") + args
            GitOperation.BRANCH_LIST -> listOf("git", "branch", "--list")
            GitOperation.BRANCH_CREATE -> listOf("git", "branch", "--") + args
            GitOperation.CHECKOUT -> {
                require(args.none { it.startsWith("-") }) { "checkout options are not allowed" }
                listOf("git", "checkout") + args
            }
            GitOperation.SWITCH -> listOf("git", "switch", "--") + args
            GitOperation.MERGE -> listOf("git", "merge", "--no-edit", "--") + args
            GitOperation.REBASE -> listOf("git", "rebase", "--") + args
            GitOperation.PUSH -> listOf("git", "push", "--") + args
            GitOperation.FORCE_PUSH -> listOf("git", "push", "--force-with-lease", "--") + args
            GitOperation.TAG_CREATE -> listOf("git", "tag", "--") + args
            GitOperation.TAG_DELETE -> listOf("git", "tag", "-d", "--") + args
            GitOperation.RESET_HARD -> listOf("git", "reset", "--hard", "--") + args
            GitOperation.CLEAN -> listOf("git", "clean", "-fd", "--") + args
            GitOperation.BRANCH_DELETE -> listOf("git", "branch", "-D", "--") + args
        }
    }

    companion object {
        internal fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
    }
}
