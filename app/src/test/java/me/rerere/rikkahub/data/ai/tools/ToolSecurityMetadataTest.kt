package me.rerere.rikkahub.data.ai.tools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSecurityMetadataTest {
    @Test fun unknownSideEffectingToolIsDeniedHeadless() {
        assertFalse(ToolSecurityRegistry.permits("unknown_mutation", true, emptySet()))
    }

    @Test fun headlessDoesNotInheritInteractiveGithubCapabilities() {
        assertTrue(ToolSecurityRegistry.permits("github_get_repository", false, setOf(me.rerere.workspace.agent.SandboxCapability.GITHUB_READ)))
        assertFalse(ToolSecurityRegistry.permits("github_get_repository", true, emptySet()))
        assertFalse(ToolSecurityRegistry.permits("github_create_file", true, setOf(me.rerere.workspace.agent.SandboxCapability.GITHUB_WRITE)))
    }

    @Test fun gitPushIsHighRiskAndDeniedUnattended() {
        val metadata = requireNotNull(ToolSecurityRegistry.metadata("git_push"))
        assertTrue(metadata.riskLevel == ToolRiskLevel.HIGH)
        assertFalse(ToolSecurityRegistry.permits("git_push", true, setOf(me.rerere.workspace.agent.SandboxCapability.GIT_WRITE)))
    }
}
