package me.rerere.rikkahub.github

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GitHubPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("github_integration", Context.MODE_PRIVATE)
    private val mutableAllowed = MutableStateFlow(prefs.getStringSet(KEY_ALLOWED, emptySet()).orEmpty().map(String::lowercase).toSet())
    val allowedRepositories: StateFlow<Set<String>> = mutableAllowed.asStateFlow()

    fun isAllowed(fullName: String): Boolean = fullName.lowercase() in mutableAllowed.value
    fun setAllowed(fullNames: Set<String>) {
        val safe = fullNames.map { normalizeRepository(it) }.toSet()
        check(prefs.edit().putStringSet(KEY_ALLOWED, safe).commit())
        mutableAllowed.value = safe
    }
    fun clear() = setAllowed(emptySet())

    companion object {
        private const val KEY_ALLOWED = "allowed_repositories"
        fun normalizeRepository(value: String): String {
            val normalized = value.trim().lowercase()
            require(normalized.matches(Regex("[a-z0-9_.-]+/[a-z0-9_.-]+"))) { "Invalid GitHub repository" }
            return normalized
        }
    }
}

