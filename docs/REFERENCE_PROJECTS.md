# AL Agent reference projects

These open-source projects are reference material for the Moataz Alaqami / AL Agent modernization work. They are used to study interaction patterns, provider integrations, mobile-agent architecture, automation, and embedded development environments.

## References

| Project | Repository | Primary inspiration area |
|---|---|---|
| Maid | https://github.com/Mobile-Artificial-Intelligence/maid | Mobile LLM UX, local/remote model integration, speech |
| ChatterUI | https://github.com/Vali-98/ChatterUI | Provider abstraction, chat UX, configurable APIs |
| OpenClaw Android | https://github.com/8crsk/openclaw-android | Android agent/runtime concepts and terminal-oriented workflows |
| Agentic Nexus | https://github.com/niki914/agentic-nexus | Agent orchestration and multi-step task patterns |
| ClosePaw | https://github.com/imoonkey/closepaw | Agent interaction and automation concepts |
| MobileClaw | https://github.com/ChenKuanSun/MobileClaw | Mobile-agent architecture and device workflows |
| Phone Native Agent | https://github.com/tianhao789456/phone-native-agent | Native phone-agent interaction patterns |
| Termux AI | https://github.com/DioNanos/termux-ai | Terminal, coding, and Android/Linux workflow integration |
| MobiAgent | https://github.com/IPADS-SAI/MobiAgent | Research-oriented mobile-agent design |
| MobileRun | https://github.com/droidrun/mobilerun | Android automation and agent execution |
| Droid MC | https://github.com/stixez/droid-mc | Mobile control / agent integration concepts |
| OpenRouter Android Client | https://github.com/Nu11Object/open-router-android-client | OpenRouter provider/model handling on Android |

## Source-use policy

The list above is an **inspiration and research index**, not a statement that source code from those repositories is incorporated here.

Before adapting or copying any implementation from a reference project, this project must:

1. inspect the exact repository revision and its license;
2. confirm that the license is compatible with this project's GNU AGPL-3.0 obligations;
3. preserve copyright, license, and attribution notices required by the source license;
4. document the adapted file or component and upstream revision in this repository's notices;
5. prefer clean-room reimplementation of an idea when license compatibility or provenance is uncertain.

The existing RikkaHub and RikkaHub Agent upstream rights and notices remain preserved separately in the project's legal/about material.

## Modernization targets derived from the review

The reference set informs the following architectural direction without depending on any single codebase:

- capability-aware provider models instead of assuming text-only chat;
- first-class text, image, audio, video, and document message parts where providers support them;
- tool calling, reasoning, file search, web access, and code execution exposed through a common capability layer;
- an agent profile that can inspect files, browse, run JavaScript/code workflows, delegate to sub-agents, and operate the embedded Linux workspace;
- explicit approval boundaries for sensitive phone/device actions;
- adaptive Android UI for phone, tablet, foldable, landscape, and desktop-like layouts;
- reproducible, test-gated arm64 packaging and embedded Linux provisioning.
