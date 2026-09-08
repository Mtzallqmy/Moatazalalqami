package me.rerere.workspace.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import me.rerere.workspace.WorkspaceManager

class SandboxPolicyTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun policyIsFailClosed() {
        val policy = SandboxPolicy(setOf(SandboxCapability.READ_WORKSPACE))
        assertTrue(policy.allows(SandboxCapability.READ_WORKSPACE))
        assertFalse(policy.allows(SandboxCapability.WRITE_WORKSPACE))
        runCatching { policy.require(SandboxCapability.EXECUTE_COMMAND) }
            .onSuccess { error("missing capability must fail") }
    }

    @Test fun lifecycleAndLimitsAreEnforced() {
        val manager = SandboxManager(
            temporaryFolder.newFolder("sandboxes"),
            WorkspaceManager(temporaryFolder.newFolder("workspaces")),
            clock = { 100L },
        )
        val session = manager.createSandbox(
            SandboxConfig("conversation", limits = SandboxResourceLimits(maxToolCalls = 1)),
        )
        assertEquals(SandboxStatus.ACTIVE, session.status)
        manager.consume(session.sessionId, SandboxOperation.TOOL_CALL)
        assertThrows(SandboxLimitExceeded::class.java) {
            manager.consume(session.sessionId, SandboxOperation.TOOL_CALL)
        }
        assertEquals(SandboxStatus.SUSPENDED, manager.suspendSandbox(session.sessionId).status)
        assertEquals(SandboxStatus.ACTIVE, manager.resumeSandbox(session.sessionId).status)
        assertTrue(manager.destroySandbox(session.sessionId))
        assertFalse(session.workingDirectory.parentFile.exists())
    }
}
