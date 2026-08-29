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
    required=True,
)

# Use Material/Android-style expanded breakpoint instead of the old landscape-only 1100dp gate.
replace(
    "app/src/main/java/me/rerere/rikkahub/ui/pages/chat/ChatPage.kt",
    "    val isBigScreen =\n        windowAdaptiveInfo.width > windowAdaptiveInfo.height && windowAdaptiveInfo.width >= 1100.dp",
    "    val isBigScreen = windowAdaptiveInfo.width >= 840.dp",
    required=True,
)

# Product-facing terminal session identity.
replace(
    "workspace/src/main/java/me/rerere/workspace/ProotShellRunner.kt",
    '            "rikkahub",',
    '            "moataz-alaqami",',
)

print("Moataz Alaqami modernization patches applied")
