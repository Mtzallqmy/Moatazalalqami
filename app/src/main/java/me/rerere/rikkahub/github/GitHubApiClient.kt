package me.rerere.rikkahub.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.security.AndroidCredentialVault
import me.rerere.rikkahub.security.CredentialVault
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class GitHubApiResult(val status: Int, val body: String, val requestId: String?)

class GitHubApiClient(private val http: OkHttpClient, private val vault: CredentialVault) {
    suspend fun request(method: String, path: String, body: String? = null): GitHubApiResult = withContext(Dispatchers.IO) {
        require(path.startsWith("/") && !path.contains(".."))
        val token = vault.decrypt(AndroidCredentialVault.GITHUB_PAT) ?: error("GitHub is not connected")
        try {
            val request = Request.Builder().url("https://api.github.com$path")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Authorization", "Bearer ${token.concatToString()}")
                .method(method, if (method == "GET" || method == "HEAD") null else (body ?: "{}").toRequestBody(JSON))
                .build()
            http.newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                if (!response.isSuccessful) error("GitHub API ${response.code}: ${responseBody.take(1000)}")
                GitHubApiResult(response.code, responseBody, response.header("x-github-request-id"))
            }
        } finally { token.fill('\u0000') }
    }

    companion object { private val JSON = "application/json; charset=utf-8".toMediaType() }
}

