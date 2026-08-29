from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def replace(path: str, old: str, new: str, required: bool = False) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if old not in text:
        if required:
            raise RuntimeError(f"Expected pattern not found in {path}: {old!r}")
        return
    p.write_text(text.replace(old, new), encoding="utf-8")
    print(f"updated {path}")


# Alpine minirootfs guarantees /bin/sh, while bash is an optional package.
replace(
    "app/src/main/java/me/rerere/rikkahub/ui/pages/extensions/workspace/WorkspaceTerminalSession.kt",
    '        "SHELL=/bin/bash",\n        "/bin/bash",',
    '        "SHELL=/bin/sh",\n        "/bin/sh",',
)

# Use Material/Android-style expanded breakpoint instead of the old landscape-only 1100dp gate.
replace(
    "app/src/main/java/me/rerere/rikkahub/ui/pages/chat/ChatPage.kt",
    "    val isBigScreen =\n        windowAdaptiveInfo.width > windowAdaptiveInfo.height && windowAdaptiveInfo.width >= 1100.dp",
    "    val isBigScreen = windowAdaptiveInfo.width >= 840.dp",
)

# Product-facing terminal session identity.
replace(
    "workspace/src/main/java/me/rerere/workspace/ProotShellRunner.kt",
    '            "rikkahub",',
    '            "moataz-alaqami",',
)

# Embedded Linux repository path uses java.io.File; make the import explicit and idempotent.
workspace_repo = ROOT / "app/src/main/java/me/rerere/rikkahub/data/repository/WorkspaceRepository.kt"
text = workspace_repo.read_text(encoding="utf-8")
if "import java.io.File\n" not in text:
    marker = "import java.io.ByteArrayOutputStream\n"
    if marker not in text:
        raise RuntimeError("WorkspaceRepository import insertion point not found")
    text = text.replace(marker, marker + "import java.io.File\n", 1)
    workspace_repo.write_text(text, encoding="utf-8")
    print("updated WorkspaceRepository.kt import")

# AGP 9 rejects configuring the same ABI in both defaultConfig.ndk.abiFilters and splits.abi.
# Keep the simpler arm64-only ndk filter so debug/release each produce one 64-bit APK.
app_gradle = ROOT / "app/build.gradle.kts"
text = app_gradle.read_text(encoding="utf-8")
text, count = re.subn(
    r'\n    splits \{\n        abi \{\n            val isBuildingBundle = gradle\.startParameter\.taskNames\.any \{ it\.lowercase\(\)\.contains\("bundle"\) \}\n            isEnable = !isBuildingBundle\n            reset\(\)\n            include\("arm64-v8a"\)\n            isUniversalApk = false\n        \}\n    \}\n',
    '\n',
    text,
    count=1,
)
if count:
    app_gradle.write_text(text, encoding="utf-8")
    print("updated app/build.gradle.kts arm64 packaging")

# Keep root project branding stable after upstream syncs.
settings = ROOT / "settings.gradle.kts"
text = settings.read_text(encoding="utf-8")
text2 = re.sub(r'rootProject\.name\s*=\s*"[^"]+"', 'rootProject.name = "MoatazAlaqami"', text, count=1)
if text2 != text:
    settings.write_text(text2, encoding="utf-8")
    print("updated settings.gradle.kts branding")

# Visible Android strings: preserve resource identifiers for binary/source compatibility,
# but remove upstream product branding from human-facing translated text.
for strings_file in (ROOT / "app/src/main/res").glob("values*/strings.xml"):
    original = strings_file.read_text(encoding="utf-8")

    def clean_string(match: re.Match[str]) -> str:
        prefix, body, suffix = match.groups()
        body = re.sub(r'RikkaHub|Rikka Hub|rikkahub', 'Moataz Alaqami', body)
        return prefix + body + suffix

    updated = re.sub(r'(<string\s+name="[^"]+"[^>]*>)(.*?)(</string>)', clean_string, original)
    if updated != original:
        strings_file.write_text(updated, encoding="utf-8")
        print(f"updated visible branding in {strings_file.relative_to(ROOT)}")

# Web client branding. Internal storage/event keys intentionally stay stable so upgrades retain sessions/settings.
for rel in ["web-ui/app/locales/en-US/page.json", "web-ui/app/locales/zh-CN/page.json"]:
    p = ROOT / rel
    if p.exists():
        original = p.read_text(encoding="utf-8")
        updated = original.replace("RikkaHub Web", "Moataz Alaqami Web").replace("RikkaHub web client", "Moataz Alaqami web client").replace("RikkaHub 网页客户端", "Moataz Alaqami 网页客户端")
        if updated != original:
            p.write_text(updated, encoding="utf-8")
            print(f"updated {rel}")

# Reliability/update surfaces must point at this product repository, not upstream release feeds.
for rel in [
    "app/src/main/java/me/rerere/rikkahub/reliability/GitHubReleaseChecker.kt",
    "app/src/main/java/me/rerere/rikkahub/reliability/ReliabilityTools.kt",
    "app/src/main/java/me/rerere/rikkahub/reliability/BugReportBuilder.kt",
    "app/src/main/java/me/rerere/rikkahub/skills/SkillUrlImporter.kt",
]:
    p = ROOT / rel
    if not p.exists():
        continue
    original = p.read_text(encoding="utf-8")
    updated = original
    updated = updated.replace("https://api.github.com/repos/ExTV/rikkahub-agent/releases/latest", "https://api.github.com/repos/Mtzallqmy/Moatazalalqami/releases/latest")
    updated = updated.replace("ExTV/rikkahub-agent", "Mtzallqmy/Moatazalalqami")
    updated = updated.replace("rikkahub-agent/skill-importer", "moataz-alaqami/skill-importer")
    updated = updated.replace("rikkahub-agent/${BuildConfig.VERSION_NAME}", "moataz-alaqami/${BuildConfig.VERSION_NAME}")
    updated = updated.replace("rikkahub-agent-bug-", "moataz-alaqami-bug-")
    updated = updated.replace("App: rikkahub-agent", "App: Moataz Alaqami")
    updated = updated.replace("rikkahub-agent bug report", "Moataz Alaqami bug report")
    updated = updated.replace("newer version of rikkahub-agent", "newer version of Moataz Alaqami")
    if updated != original:
        p.write_text(updated, encoding="utf-8")
        print(f"updated {rel}")

# Public documentation is a Moataz Alaqami surface. Legal upstream attribution remains in About/NOTICE/LICENSE.
docs_index = ROOT / "docs/index.html"
if docs_index.exists():
    original = docs_index.read_text(encoding="utf-8")
    updated = original
    updated = updated.replace("https://extv.github.io/rikkahub-agent/", "https://github.com/Mtzallqmy/Moatazalalqami")
    updated = updated.replace("https://raw.githubusercontent.com/ExTV/rikkahub-agent/master/docs/icon.png", "https://raw.githubusercontent.com/Mtzallqmy/Moatazalalqami/main/docs/icon.png")
    updated = updated.replace('"author": { "@type": "Person", "name": "ExTV", "url": "https://github.com/ExTV" }', '"author": { "@type": "Person", "name": "Moataz Alaqami", "url": "https://github.com/Mtzallqmy" }')
    updated = updated.replace("cd</span> rikkahub-agent", "cd</span> Moatazalalqami")
    updated = updated.replace("© 2026 ExTV · AGPLv3", "© 2026 Moataz Alaqami · AGPLv3")
    # Remove old developer-profile promotional links from the public product page.
    updated = re.sub(r'\s*<li><a href="https://github\.com/ExTV(?:/[^"]*)?"[^>]*>.*?</a></li>', '', updated)
    if updated != original:
        docs_index.write_text(updated, encoding="utf-8")
        print("updated docs/index.html")

print("Moataz Alaqami modernization patches applied")
