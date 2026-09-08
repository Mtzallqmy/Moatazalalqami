package me.rerere.rikkahub.data.ai.tools

import me.rerere.workspace.agent.SandboxCapability

enum class ToolRiskLevel { READ_ONLY, LOW, MEDIUM, HIGH, CRITICAL }
enum class SideEffect { NONE, LOCAL_WRITE, NETWORK_WRITE, DEVICE_CONTROL, CODE_EXECUTION }
enum class DataSensitivity { PUBLIC, INTERNAL, PRIVATE, SECRET }
enum class ApprovalPolicy { NONE, CONFIGURABLE, ALWAYS_ASK, NO_ALWAYS_ALLOW }
enum class UnattendedPolicy { DENY, READ_ONLY, EXPLICIT_CAPABILITY }

data class ToolSecurityMetadata(
    val riskLevel: ToolRiskLevel,
    val sideEffect: SideEffect,
    val dataSensitivity: DataSensitivity = DataSensitivity.INTERNAL,
    val approvalPolicy: ApprovalPolicy,
    val unattendedPolicy: UnattendedPolicy,
    val requiredCapabilities: Set<SandboxCapability>,
)

object ToolSecurityRegistry {
    private val entries = mutableMapOf<String, ToolSecurityMetadata>()
    init {
        val githubReads = setOf(
            "github_list_repositories", "github_get_repository", "github_get_repository_info",
            "github_get_file", "github_list_directory", "github_list_branches", "github_list_commits",
            "github_get_commit", "github_list_pull_requests", "github_get_pull_request",
            "github_list_issues", "github_get_issue", "github_list_workflows",
            "github_list_workflow_runs", "github_get_workflow_run", "github_get_workflow_logs",
            "github_list_releases", "github_get_release",
        )
        githubReads.forEach { entries[it] = ToolSecurityMetadata(
            ToolRiskLevel.READ_ONLY, SideEffect.NONE, DataSensitivity.PRIVATE,
            ApprovalPolicy.NONE, UnattendedPolicy.EXPLICIT_CAPABILITY, setOf(SandboxCapability.GITHUB_READ),
        ) }
        val githubWrites = setOf(
            "github_create_file", "github_update_file", "github_create_branch",
            "github_create_pull_request", "github_update_pull_request", "github_create_issue",
            "github_comment_issue", "github_dispatch_workflow", "github_create_release",
        )
        githubWrites.forEach { entries[it] = ToolSecurityMetadata(
            ToolRiskLevel.HIGH, SideEffect.NETWORK_WRITE, DataSensitivity.PRIVATE,
            ApprovalPolicy.ALWAYS_ASK, UnattendedPolicy.DENY, setOf(SandboxCapability.GITHUB_WRITE),
        ) }
        setOf("github_delete_file", "github_delete_branch", "github_merge_pull_request").forEach {
            entries[it] = ToolSecurityMetadata(
                ToolRiskLevel.CRITICAL, SideEffect.NETWORK_WRITE, DataSensitivity.PRIVATE,
                ApprovalPolicy.NO_ALWAYS_ALLOW, UnattendedPolicy.DENY, setOf(SandboxCapability.GITHUB_WRITE),
            )
        }
        setOf("git_status", "git_diff", "git_log").forEach {
            entries[it] = ToolSecurityMetadata(
                ToolRiskLevel.READ_ONLY, SideEffect.NONE, approvalPolicy = ApprovalPolicy.NONE,
                unattendedPolicy = UnattendedPolicy.EXPLICIT_CAPABILITY,
                requiredCapabilities = setOf(SandboxCapability.GIT_READ),
            )
        }
        setOf("git_init", "git_add", "git_branch").forEach {
            entries[it] = ToolSecurityMetadata(
                ToolRiskLevel.LOW, SideEffect.LOCAL_WRITE, approvalPolicy = ApprovalPolicy.CONFIGURABLE,
                unattendedPolicy = UnattendedPolicy.EXPLICIT_CAPABILITY,
                requiredCapabilities = setOf(SandboxCapability.GIT_WRITE),
            )
        }
        setOf("git_commit", "git_checkout", "git_switch", "git_tag").forEach {
            entries[it] = ToolSecurityMetadata(
                ToolRiskLevel.MEDIUM, SideEffect.LOCAL_WRITE, approvalPolicy = ApprovalPolicy.CONFIGURABLE,
                unattendedPolicy = UnattendedPolicy.DENY,
                requiredCapabilities = setOf(SandboxCapability.GIT_WRITE),
            )
        }
        setOf("git_clone", "git_fetch", "git_pull").forEach {
            entries[it] = ToolSecurityMetadata(
                ToolRiskLevel.MEDIUM, SideEffect.LOCAL_WRITE, approvalPolicy = ApprovalPolicy.CONFIGURABLE,
                unattendedPolicy = UnattendedPolicy.DENY,
                requiredCapabilities = setOf(SandboxCapability.GIT_WRITE, SandboxCapability.NETWORK_READ),
            )
        }
        setOf("git_merge", "git_rebase", "git_push").forEach {
            entries[it] = ToolSecurityMetadata(
                ToolRiskLevel.HIGH, if (it == "git_push") SideEffect.NETWORK_WRITE else SideEffect.LOCAL_WRITE,
                approvalPolicy = ApprovalPolicy.ALWAYS_ASK, unattendedPolicy = UnattendedPolicy.DENY,
                requiredCapabilities = if (it == "git_push") {
                    setOf(SandboxCapability.GIT_WRITE, SandboxCapability.NETWORK_WRITE)
                } else setOf(SandboxCapability.GIT_WRITE),
            )
        }
    }
    @Synchronized fun register(toolName: String, metadata: ToolSecurityMetadata) { entries[toolName] = metadata }
    @Synchronized fun metadata(toolName: String): ToolSecurityMetadata? = entries[toolName]

    /** Side-effecting tools without typed metadata are denied in unattended execution. */
    fun permits(toolName: String, headless: Boolean, capabilities: Set<SandboxCapability>): Boolean {
        val meta = metadata(toolName) ?: return !headless && !ToolApprovalDefaults.requiresApproval(toolName)
        if (!capabilities.containsAll(meta.requiredCapabilities)) return false
        return !headless || meta.unattendedPolicy != UnattendedPolicy.DENY
    }
}

data class UnattendedExecutionPolicy(val capabilities: Set<SandboxCapability> = emptySet()) {
    fun permits(toolName: String): Boolean = ToolSecurityRegistry.permits(toolName, headless = true, capabilities)
}
