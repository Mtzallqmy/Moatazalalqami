package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.db.entity.WorkspaceEntity
import me.rerere.rikkahub.data.repository.WorkspaceRepository
import me.rerere.workspace.WorkspaceShellStatus

/**
 * Injects workspace state into the system prompt so the model knows when Linux tools are ready
 * and how to guide the user when the embedded environment needs provisioning or repair.
 */
class WorkspaceReminderTransformer(
    private val workspaceRepository: WorkspaceRepository,
) : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val workspaceId = ctx.assistant.workspaceId?.toString()
        val workspace = workspaceId?.let { workspaceRepository.getById(it) }
        val hasAnyWorkspace = workspace != null || workspaceRepository.getAll().isNotEmpty()

        val prompt = buildWorkspaceReminder(workspace, hasAnyWorkspace, ctx.workspaceCwd)
            ?: return messages

        val systemIndex = messages.indexOfFirst { it.role == MessageRole.SYSTEM }
        return if (systemIndex >= 0) {
            messages.toMutableList().apply {
                this[systemIndex] = this[systemIndex].appendText("\n\n$prompt")
            }
        } else {
            listOf(UIMessage.system(prompt)) + messages
        }
    }
}

internal fun buildWorkspaceReminder(
    workspace: WorkspaceEntity?,
    hasAnyWorkspace: Boolean,
    cwd: String? = null,
): String? = when {
    workspace != null && workspace.shellStatus == WorkspaceShellStatus.READY.name ->
        buildWorkspacePrompt(workspace, cwd)

    workspace != null -> buildWorkspaceNotReadyPrompt(workspace)

    hasAnyWorkspace -> buildWorkspaceUnboundPrompt()

    else -> null
}

private fun buildWorkspacePrompt(workspace: WorkspaceEntity, cwd: String? = null): String = buildString {
    appendLine("<workspace>")
    appendLine("You have access to a persistent Linux workspace named \"${workspace.name}\", running in a sandboxed PRoot Alpine Linux environment embedded with the app.")
    appendLine("- The Linux rootfs is packaged inside the installed app and is provisioned locally. Never ask the user to download a rootfs URL or fetch a Linux image from the internet.")
    appendLine("- The workspace files area is mounted at `/workspace`. Use it as your working directory; files written there persist across turns of this conversation.")
    appendLine("- All paths passed to workspace tools must be absolute and inside the Rootfs (for example `/workspace/notes.md`).")
    appendLine("- Available tools:")
    appendLine("  - `workspace_read_file`: read file contents.")
    appendLine("  - `workspace_write_file` / `workspace_edit_file`: create files, or make precise edits to existing files.")
    appendLine("  - `workspace_create_folder`: create a directory (and missing parents).")
    appendLine("  - `workspace_read_folder`: recursively list a directory as an indented tree.")
    appendLine("  - `workspace_shell`: run shell commands (the files area is mounted at /workspace).")
    appendLine("  - `workspace_run_background`: start a long-running command that persists across tool calls and survives after the call returns (dev servers, long installs, file watchers, batch jobs); returns a task id. The command runs in the FOREGROUND of its own persistent process, so do NOT append `&`.")
    appendLine("  - `workspace_background_status`: check status and recent output of background tasks (all, or one by task id).")
    appendLine("  - `workspace_background_kill`: stop a background task by task id.")
    appendLine("- Prefer `workspace_shell` for tasks that standard Unix tools handle well, and prefer `workspace_edit_file` for targeted edits over rewriting whole files.")
    appendLine("- To preview web files in the browser, start a static server with `workspace_run_background` (for example `python3 -m http.server 8000`) and then open it, since file:// breaks ES modules and fetch.")
    appendLine("- The skills directory is mounted at `/skills`. Each skill is a subdirectory `/skills/<skill-name>/` containing a `SKILL.md` plus any supporting files. Read a skill's `SKILL.md` before using it, and follow its instructions.")
    appendLine("- Files the user uploaded are mounted at `/upload`. Treat `/upload` as READ-ONLY. If you need to change an uploaded file, copy it into `/workspace` first and edit the copy.")
    if (!cwd.isNullOrBlank()) {
        appendLine("- Current working directory: `$cwd`. Use this as the default context for file operations and shell commands.")
    }
    append("</workspace>")
}

private fun buildWorkspaceNotReadyPrompt(workspace: WorkspaceEntity): String = buildString {
    appendLine("<workspace-setup>")
    appendLine("A workspace named \"${workspace.name}\" is bound to this assistant, but its embedded Linux shell is not ready (status: ${workspace.shellStatus}), so workspace tools are NOT available right now.")
    appendLine("The Alpine Linux rootfs ships inside the app. No external rootfs URL, manual Linux image download, or network provisioning is required.")
    val howto = when (workspace.shellStatus) {
        WorkspaceShellStatus.INSTALLING.name ->
            "the embedded Linux environment is being provisioned locally; ask them to let that operation finish, then send a new message."
        WorkspaceShellStatus.BROKEN.name ->
            "the embedded environment needs repair; ask them to open Extensions > Workspace, open this workspace, and choose Repair embedded Linux until the shell status shows Ready."
        else ->
            "ask them to open Extensions > Workspace, open this workspace, and provision the embedded Linux environment until the shell status shows Ready."
    }
    appendLine("If the user wants to use the workspace, explain in the user's language how to make it ready: $howto")
    appendLine("Never instruct the user to paste a rootfs URL. Do not claim to have workspace tools or attempt to call them until the shell status is Ready.")
    append("</workspace-setup>")
}

private fun buildWorkspaceUnboundPrompt(): String = buildString {
    appendLine("<workspace-setup>")
    appendLine("The user has a workspace, but none is bound to this assistant, so workspace tools are NOT available.")
    appendLine("If the user asks to save files or run shell / Linux commands in a workspace, explain in the user's language how to enable it:")
    appendLine("1. Tap the + button in the chat input bar and select a workspace to bind it to this assistant.")
    appendLine("2. If that workspace's shell is not Ready, open Extensions > Workspace, open the workspace, and provision or repair the embedded Linux environment. The rootfs is bundled with the app; no URL or separate download is needed.")
    appendLine("Do not claim to have workspace tools or attempt to call them until a workspace is bound and its shell is Ready.")
    append("</workspace-setup>")
}

private fun UIMessage.appendText(extra: String): UIMessage {
    val updatedParts = parts.toMutableList()
    val firstTextIndex = updatedParts.indexOfFirst { it is UIMessagePart.Text }
    if (firstTextIndex >= 0) {
        val text = updatedParts[firstTextIndex] as UIMessagePart.Text
        updatedParts[firstTextIndex] = text.copy(text = text.text + extra)
    } else {
        updatedParts.add(UIMessagePart.Text(extra))
    }
    return copy(parts = updatedParts)
}
