package me.rerere.rikkahub.data.ai.tools

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Coverage for issue #42: launch_activity had no approval gate while its sibling launch_app
 * did. Asserts the fix without re-testing the whole [ToolApprovalDefaults] set.
 */
class ToolApprovalDefaultsTest {

    @Test
    fun `launch_activity requires approval, same as launch_app`() {
        assertTrue(ToolApprovalDefaults.requiresApproval("launch_activity"))
        assertTrue(ToolApprovalDefaults.allowsAlwaysAllow("launch_activity"))
    }

    @Test fun `critical GitHub operations cannot be always allowed`() {
        assertTrue(ToolApprovalDefaults.requiresApproval("github_merge_pull_request"))
        assertTrue(!ToolApprovalDefaults.allowsAlwaysAllow("github_merge_pull_request"))
        assertTrue(ToolApprovalDefaults.requiresApproval("github_delete_file"))
    }
}
