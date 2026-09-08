package me.rerere.workspace.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import me.rerere.workspace.WorkspaceManager

class GitRepositoryManagerTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun forcePushIsCriticalAndUsesLease() {
        assertEquals(GitRiskLevel.CRITICAL, GitOperation.FORCE_PUSH.risk)
        val command = GitRepositoryManager(WorkspaceManager(temporaryFolder.root))
            .commandFor(GitOperation.FORCE_PUSH, listOf("origin", "feature"))
        assertTrue("--force-with-lease" in command)
        assertTrue("--force" !in command)
    }

    @Test fun shellArgumentsAreQuoted() {
        assertEquals("'a'\\''b'", GitRepositoryManager.shellQuote("a'b"))
    }

    @Test fun embeddedCredentialsAreRejected() {
        val manager = GitRepositoryManager(WorkspaceManager(temporaryFolder.root))
        assertThrows(IllegalArgumentException::class.java) {
            manager.commandFor(GitOperation.CLONE, listOf("https://secret@github.com/owner/repo.git"))
        }
    }
}
