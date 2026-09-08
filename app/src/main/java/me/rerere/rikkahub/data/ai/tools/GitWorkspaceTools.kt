package me.rerere.rikkahub.data.ai.tools

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.repository.WorkspaceRepository
import me.rerere.rikkahub.reliability.SecretRedactor
import me.rerere.workspace.git.GitOperation
import me.rerere.workspace.git.GitRiskLevel

private val gitToolOperations = linkedMapOf(
    "git_init" to GitOperation.INIT, "git_clone" to GitOperation.CLONE,
    "git_status" to GitOperation.STATUS, "git_diff" to GitOperation.DIFF,
    "git_log" to GitOperation.LOG, "git_fetch" to GitOperation.FETCH,
    "git_pull" to GitOperation.PULL, "git_add" to GitOperation.ADD,
    "git_commit" to GitOperation.COMMIT, "git_branch" to GitOperation.BRANCH_LIST,
    "git_checkout" to GitOperation.CHECKOUT, "git_switch" to GitOperation.SWITCH,
    "git_merge" to GitOperation.MERGE, "git_rebase" to GitOperation.REBASE,
    "git_push" to GitOperation.PUSH, "git_tag" to GitOperation.TAG_CREATE,
)

fun createGitWorkspaceTools(workspaceId: String, repository: WorkspaceRepository): List<Tool> =
    gitToolOperations.map { (name, operation) ->
        Tool(
            name = name,
            description = "Typed Git ${operation.name.lowercase()} in the bound workspace. Arguments are passed as argv, never interpolated as raw shell. High-risk operations require approval. Tokens in remote URLs are rejected.",
            parameters = { InputSchema.Obj(buildJsonObject {
                put("args", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("type", "string") })
                    put("description", "Arguments only; do not include the word git")
                })
            }) },
            needsApproval = { operation.risk >= GitRiskLevel.MEDIUM },
            execute = { input ->
                val args = input.jsonObject["args"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                val effectiveOperation = when {
                    name == "git_branch" && args.isNotEmpty() -> GitOperation.BRANCH_CREATE
                    name == "git_tag" && args.firstOrNull() in setOf("-d", "--delete") -> GitOperation.TAG_DELETE
                    name == "git_push" && args.any { it in setOf("-f", "--force", "--force-with-lease") } -> GitOperation.FORCE_PUSH
                    else -> operation
                }
                val safeArgs = if (effectiveOperation == GitOperation.TAG_DELETE) args.drop(1) else args
                val result = repository.executeGit(workspaceId, effectiveOperation, safeArgs, approved = effectiveOperation.risk >= GitRiskLevel.HIGH)
                listOf(UIMessagePart.Text(buildJsonObject {
                    put("operation", effectiveOperation.name)
                    put("command", buildJsonArray { result.command.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                    put("exitCode", result.result.exitCode)
                    put("stdout", SecretRedactor.redact(result.result.stdout))
                    put("stderr", SecretRedactor.redact(result.result.stderr))
                    put("timedOut", result.result.timedOut)
                }.toString()))
            },
        )
    }
