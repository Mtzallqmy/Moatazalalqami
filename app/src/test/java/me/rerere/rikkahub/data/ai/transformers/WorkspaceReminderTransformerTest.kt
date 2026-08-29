package me.rerere.rikkahub.data.ai.transformers

import me.rerere.rikkahub.data.db.entity.WorkspaceEntity
import me.rerere.workspace.WorkspaceShellStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Coverage for the pure workspace-state -> system-prompt selector. */
class WorkspaceReminderTransformerTest {

    private fun workspace(status: WorkspaceShellStatus, name: String = "demo") = WorkspaceEntity(
        id = "id-$name",
        name = name,
        root = "id-$name",
        shellStatus = status.name,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun `bound and READY advertises tools and bundled Linux environment`() {
        val prompt = buildWorkspaceReminder(workspace(WorkspaceShellStatus.READY, "proj"), hasAnyWorkspace = true)
        requireNotNull(prompt)
        assertTrue(prompt.contains("<workspace>"))
        assertTrue(prompt.contains("proj"))
        assertTrue(prompt.contains("workspace_shell"))
        assertTrue(prompt.contains("embedded with the app"))
        assertTrue(prompt.contains("Never ask the user to download a rootfs URL"))
    }

    @Test
    fun `bound but DISABLED directs user to embedded provisioning`() {
        val prompt = buildWorkspaceReminder(workspace(WorkspaceShellStatus.DISABLED), hasAnyWorkspace = true)
        requireNotNull(prompt)
        assertTrue(prompt.contains("<workspace-setup>"))
        assertFalse(prompt.contains("<workspace>"))
        assertTrue(prompt.contains("provision the embedded Linux environment"))
        assertTrue(prompt.contains("No external rootfs URL"))
        assertTrue(prompt.contains(WorkspaceShellStatus.DISABLED.name))
    }

    @Test
    fun `bound but INSTALLING describes local provisioning`() {
        val prompt = buildWorkspaceReminder(workspace(WorkspaceShellStatus.INSTALLING), hasAnyWorkspace = true)
        requireNotNull(prompt)
        assertTrue(prompt.contains("<workspace-setup>"))
        assertTrue(prompt.contains("being provisioned locally"))
    }

    @Test
    fun `bound but BROKEN directs user to embedded repair`() {
        val prompt = buildWorkspaceReminder(workspace(WorkspaceShellStatus.BROKEN), hasAnyWorkspace = true)
        requireNotNull(prompt)
        assertTrue(prompt.contains("<workspace-setup>"))
        assertTrue(prompt.contains("Repair embedded Linux"))
        assertFalse(prompt.contains("paste a rootfs URL"))
    }

    @Test
    fun `unbound workspace guidance mentions bundled rootfs`() {
        val prompt = buildWorkspaceReminder(workspace = null, hasAnyWorkspace = true)
        requireNotNull(prompt)
        assertTrue(prompt.contains("<workspace-setup>"))
        assertFalse(prompt.contains("<workspace>"))
        assertTrue(prompt.contains("+ button"))
        assertTrue(prompt.contains("rootfs is bundled with the app"))
    }

    @Test
    fun `no workspace at all injects nothing`() {
        val prompt = buildWorkspaceReminder(workspace = null, hasAnyWorkspace = false)
        assertNull(prompt)
    }
}
