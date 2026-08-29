# Moataz Alaqami

**A modern Android AI agent for real device automation.**

Moataz Alaqami is a major product-level evolution of the RikkaHub Agent codebase. The project is being rebuilt around a new visual identity, adaptive navigation, improved responsiveness, a cleaner product architecture, and an embedded Linux workspace that is ready from first launch.

## Product direction

- New Moataz Alaqami visual identity and application branding.
- Android 8.0+ support (`minSdk 26`).
- Primary APK target: `arm64-v8a`.
- Modern Jetpack Compose / Material 3 UI with adaptive layouts for phones, foldables and tablets.
- Improved navigation hierarchy, motion, information architecture and accessibility.
- Integrated on-device agent tools for device control, workflows, browser automation, SSH, MCP, Telegram, files and more.
- Embedded Linux workspace packaged with the application instead of requiring a post-install RootFS download.
- Local LLM support alongside cloud providers.
- Explicit approval boundaries for sensitive tools.

## Current modernization track

The 3.0 modernization is being developed as a controlled migration rather than a cosmetic fork. Work is grouped into the following tracks:

1. **Brand system** — new name, iconography, typography, colors, surfaces and component rules.
2. **Adaptive UX** — compact / medium / expanded window behavior, improved navigation and better landscape/tablet layouts.
3. **Linux runtime** — bundled `aarch64` Linux root filesystem with automatic first-run provisioning.
4. **Android packaging** — arm64-first APK, Android 8+ compatibility, release signing and reproducible CI builds.
5. **Architecture cleanup** — remove obsolete product integrations and isolate legacy naming from user-facing product identity.
6. **Safety & permissions** — retain explicit approvals for high-impact device and network actions.
7. **Quality** — build validation, UI testing, performance checks and release automation.

See [`docs/MODERNIZATION.md`](docs/MODERNIZATION.md) for the implementation roadmap.

## Building

The source tree is an Android/Kotlin Gradle project and requires a current JDK plus the Android SDK. Web assets used by the application also require Bun/pnpm where applicable.

```bash
./gradlew :app:assembleDebug
```

Release builds will target the supported 64-bit Android ABI and are expected to package the Linux runtime as part of the APK build pipeline.

## Attribution and license

This project contains modified work derived from **RikkaHub** and **RikkaHub Agent**. Original authors and upstream project attribution are preserved in the application's **About / Credits & Licenses** area and in the source/legal notices where required.

The project is distributed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**. Rebranding does not remove upstream copyright or license obligations.

## Status

Active 3.0 modernization. The repository is being populated in staged, reviewable commits so the new codebase remains traceable and buildable throughout the migration.
