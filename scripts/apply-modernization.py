from pathlib import Path

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

print("Moataz Alaqami modernization patches applied")
