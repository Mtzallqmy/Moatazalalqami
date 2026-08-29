# Moataz Alaqami 3.0 — Modernization Plan

This document is the implementation contract for the 3.0 product migration.

## Product identity

The user-facing product identity is **Moataz Alaqami**. Legacy RikkaHub/RikkaHub Agent branding is removed from normal product surfaces, navigation labels, onboarding, launcher presentation, update links and promotional UI.

Upstream attribution is not erased. Copyright notices, AGPL obligations and third-party notices remain in source/legal files and are exposed to users through **About → Credits & Licenses**.

## Compatibility baseline

- Android 8.0 / API 26 minimum.
- Android target/compile SDK tracks the current project toolchain while preserving API-26 runtime guards.
- Release APK is arm64-first (`arm64-v8a`).
- Java/Kotlin toolchain: JVM 17 for builds; no assumption that Android runtime itself provides Java 17 APIs.
- RTL is a first-class layout requirement, including Arabic.

## 1. Design system

Create a dedicated Moataz design system instead of continuing to accumulate screen-local styling.

### Foundation

- Semantic color tokens: primary, secondary, accent, surface, elevated surface, outline, success, warning, danger and information.
- Light and dark schemes with contrast validation.
- Dynamic color is optional and must never destroy brand identity.
- Unified shape scale and spacing scale.
- Typography scale optimized for Arabic and Latin rendering.
- Consistent icon sizes and touch targets.

### Components

- App bars and contextual top bars.
- Navigation rail / bar / drawer behavior.
- Agent cards, tool cards and permission states.
- Settings rows and grouped settings surfaces.
- Empty/error/loading states.
- Confirmation and approval sheets.
- Input composer and attachment surfaces.
- Code, terminal and browser surfaces.

## 2. Adaptive navigation

Replace fixed width checks and screen-specific branching with adaptive window logic.

- Compact: bottom navigation or focused single-pane flows.
- Medium: rail where it improves density; optional supporting pane.
- Expanded: navigation rail/drawer plus multi-pane content.
- Fold posture and safe display features must not obscure controls.
- Preserve state when moving between size classes and orientation changes.
- Back behavior must be predictable and Android-native.

## 3. Motion and interaction

- Navigation transitions communicate hierarchy rather than decoration.
- Shared/container transforms only where they improve spatial continuity.
- Respect reduced-motion/accessibility preferences.
- Eliminate layout jumps during streaming responses and tool execution.
- Consistent haptic policy for destructive/confirmed actions.

## 4. Embedded Linux runtime

The Linux workspace must be available without requiring the user to download a RootFS after installing the APK.

### Build pipeline

- Pin a stable aarch64 Linux minirootfs version.
- Verify its checksum during the build pipeline.
- Package the compressed root filesystem into application assets or an install-time packaged payload.
- Provision it into app-private storage on first use/first launch.
- Provisioning must be resumable and atomic: an interrupted extraction cannot leave a runtime marked as ready.
- Use `/bin/sh` as the baseline shell; optional packages can add Bash later.

### Runtime lifecycle

- Detect runtime schema/version separately from app version.
- Support in-place runtime migrations where safe.
- Support repair/reprovision without deleting user workspace data.
- Keep user home/workspace separate from replaceable system RootFS content.
- Expose storage consumption and runtime health in settings.

## 5. Agent UX

- Distinguish model reasoning/status, tool execution, approvals, results and errors visually.
- Show which capability is about to act on the device before sensitive execution.
- Long-running work uses a persistent activity surface rather than transient toasts.
- Provide cancel/stop controls where execution can safely be interrupted.
- Improve trace readability without exposing secrets.

## 6. Permissions and safety

- Request Android permissions at point of use, not as a blanket onboarding wall.
- Keep tool enablement separate from OS permission grant state.
- Sensitive tools retain explicit approval boundaries.
- Secrets/API keys must not appear in logs, backups, exports or agent-visible prompts by default.
- Network fetchers keep private-network/SSRF protections unless the user explicitly enables an isolated trusted workflow.

## 7. Performance

- Profile startup, chat scrolling, long conversations and tool-stream rendering.
- Remove unnecessary recomposition hotspots.
- Lazy render large histories and logs.
- Bound screenshots, browser captures and large tool payloads.
- Keep background receivers/workers demand-driven.

## 8. Packaging and CI

- Reproducible debug build on CI.
- arm64 release APK build.
- Lint and unit tests on pull requests.
- APK artifact produced by GitHub Actions.
- Release signing material stays outside the repository.
- Generate third-party license inventory for the About screen.

## 9. Migration order

1. Import upstream source without changing behavior.
2. Establish build green baseline.
3. Apply package/product identity changes.
4. Introduce design tokens and new app shell/navigation.
5. Migrate high-traffic screens: onboarding, chat, assistants, tools, settings, Linux workspace.
6. Integrate embedded Linux provisioning.
7. Audit all legacy links/branding/product copy.
8. Performance/accessibility/RTL pass.
9. Release automation and signed beta.

## Definition of done for 3.0

3.0 is not complete because the colors changed. It is complete when the product can be installed on a supported arm64 Android 8+ device, opens under the Moataz Alaqami identity, adapts across common window sizes, provides a coherent modern navigation model, provisions its Linux runtime without a post-install RootFS download, preserves required upstream/legal attribution, and passes a repeatable release build pipeline.
