package me.rerere.rikkahub.github

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.reliability.SecretRedactor
import java.net.URLEncoder

private data class Endpoint(val method: String, val path: (String, String, String) -> String, val write: Boolean = false)

private val endpoints = mapOf(
    "github_list_repositories" to Endpoint("GET", { _, _, _ -> "/user/repos?per_page=100&sort=updated" }),
    "github_get_repository" to Endpoint("GET", { r, _, _ -> "/repos/$r" }),
    "github_get_repository_info" to Endpoint("GET", { r, _, _ -> "/repos/$r" }),
    "github_get_file" to Endpoint("GET", { r, p, _ -> "/repos/$r/contents/${encodePath(p)}" }),
    "github_list_directory" to Endpoint("GET", { r, p, _ -> "/repos/$r/contents/${encodePath(p)}" }),
    "github_create_file" to Endpoint("PUT", { r, p, _ -> "/repos/$r/contents/${encodePath(p)}" }, true),
    "github_update_file" to Endpoint("PUT", { r, p, _ -> "/repos/$r/contents/${encodePath(p)}" }, true),
    "github_delete_file" to Endpoint("DELETE", { r, p, _ -> "/repos/$r/contents/${encodePath(p)}" }, true),
    "github_list_branches" to Endpoint("GET", { r, _, _ -> "/repos/$r/branches?per_page=100" }),
    "github_create_branch" to Endpoint("POST", { r, _, _ -> "/repos/$r/git/refs" }, true),
    "github_delete_branch" to Endpoint("DELETE", { r, p, _ -> "/repos/$r/git/refs/heads/${encodePath(p)}" }, true),
    "github_list_commits" to Endpoint("GET", { r, _, _ -> "/repos/$r/commits?per_page=100" }),
    "github_get_commit" to Endpoint("GET", { r, p, _ -> "/repos/$r/commits/${encodePath(p)}" }),
    "github_list_pull_requests" to Endpoint("GET", { r, _, _ -> "/repos/$r/pulls?state=all&per_page=100" }),
    "github_get_pull_request" to Endpoint("GET", { r, p, _ -> "/repos/$r/pulls/${number(p)}" }),
    "github_create_pull_request" to Endpoint("POST", { r, _, _ -> "/repos/$r/pulls" }, true),
    "github_update_pull_request" to Endpoint("PATCH", { r, p, _ -> "/repos/$r/pulls/${number(p)}" }, true),
    "github_merge_pull_request" to Endpoint("PUT", { r, p, _ -> "/repos/$r/pulls/${number(p)}/merge" }, true),
    "github_list_issues" to Endpoint("GET", { r, _, _ -> "/repos/$r/issues?state=all&per_page=100" }),
    "github_get_issue" to Endpoint("GET", { r, p, _ -> "/repos/$r/issues/${number(p)}" }),
    "github_create_issue" to Endpoint("POST", { r, _, _ -> "/repos/$r/issues" }, true),
    "github_comment_issue" to Endpoint("POST", { r, p, _ -> "/repos/$r/issues/${number(p)}/comments" }, true),
    "github_list_workflows" to Endpoint("GET", { r, _, _ -> "/repos/$r/actions/workflows?per_page=100" }),
    "github_list_workflow_runs" to Endpoint("GET", { r, _, _ -> "/repos/$r/actions/runs?per_page=100" }),
    "github_get_workflow_run" to Endpoint("GET", { r, p, _ -> "/repos/$r/actions/runs/${number(p)}" }),
    "github_get_workflow_logs" to Endpoint("GET", { r, p, _ -> "/repos/$r/actions/runs/${number(p)}/logs" }),
    "github_dispatch_workflow" to Endpoint("POST", { r, p, _ -> "/repos/$r/actions/workflows/${encodePath(p)}/dispatches" }, true),
    "github_list_releases" to Endpoint("GET", { r, _, _ -> "/repos/$r/releases?per_page=100" }),
    "github_get_release" to Endpoint("GET", { r, p, _ -> "/repos/$r/releases/${number(p)}" }),
    "github_create_release" to Endpoint("POST", { r, _, _ -> "/repos/$r/releases" }, true),
)

fun githubRepositoryTools(client: GitHubApiClient, preferences: GitHubPreferences): List<Tool> = endpoints.map { (name, endpoint) ->
    Tool(
        name = name,
        description = "GitHub REST operation $name. Repository-scoped calls require the local repository allowlist. Secrets are injected by the credential vault and never accepted as arguments.",
        parameters = { InputSchema.Obj(buildJsonObject {
            put("repository", buildJsonObject { put("type", "string"); put("description", "owner/repository") })
            put("path", buildJsonObject { put("type", "string"); put("description", "File path, id, number, branch, or workflow id, depending on operation") })
            put("body", buildJsonObject { put("type", "string"); put("description", "JSON request body for write operations") })
        }) },
        execute = { input -> executeGithub(name, endpoint, client, preferences, input) },
    )
}
private suspend fun executeGithub(name: String, endpoint: Endpoint, client: GitHubApiClient, prefs: GitHubPreferences, input: JsonElement): List<UIMessagePart> {
    val obj = input.jsonObject
    val repo = obj["repository"]?.jsonPrimitive?.contentOrNull.orEmpty()
    if (name != "github_list_repositories") {
        val normalized = runCatching { GitHubPreferences.normalizeRepository(repo) }.getOrElse { return errorPart("invalid_repository") }
        if (!prefs.isAllowed(normalized)) return errorPart("repository_not_allowed")
    }
    val path = obj["path"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val body = obj["body"]?.jsonPrimitive?.contentOrNull
    if (endpoint.write && body.isNullOrBlank() && name !in setOf("github_delete_branch")) return errorPart("missing_json_body")
    return runCatching {
        val result = client.request(endpoint.method, endpoint.path(repo, path, body.orEmpty()), body)
        UIMessagePart.Text(SecretRedactor.redact(result.body))
    }.fold({ listOf(it) }, { errorPart(SecretRedactor.redact(it.message ?: "github_request_failed")) })
}

private fun errorPart(message: String) = listOf(UIMessagePart.Text(buildJsonObject { put("error", message) }.toString()))
private fun encodePath(value: String): String = value.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
private fun number(value: String): Long = value.toLongOrNull()?.takeIf { it > 0 } ?: error("positive numeric id required")
