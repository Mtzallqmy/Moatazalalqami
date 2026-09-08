package me.rerere.rikkahub.github

import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubPreferencesTest {
    @Test fun repositoryNamesAreNormalized() {
        assertEquals("owner/repo", GitHubPreferences.normalizeRepository(" Owner/Repo "))
    }
    @Test(expected = IllegalArgumentException::class) fun malformedRepositoryIsRejected() {
        GitHubPreferences.normalizeRepository("https://github.com/owner/repo")
    }
}

