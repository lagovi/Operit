<h1 align="center">
  <img src="app/src/main/assets/logo.svg" width="48" height="48" align="absmiddle" alt=""> Operit AI — English-only fork
</h1>

> **Fork notice. This fork is 100% vibe-coded (written with AI assistance), and every release here is ALPHA: expect rough edges and breaking changes. For the stable upstream project, see [AAswordman/Operit](https://github.com/AAswordman/Operit).**

<div align="center">
  <img src="https://img.shields.io/github/last-commit/lagovi/Operit" alt="last commit">
  <img src="https://img.shields.io/badge/Platform-Android_8.0%2B-brightgreen.svg" alt="platform">
  <a href="https://github.com/lagovi/Operit/releases/latest"><img src="https://img.shields.io/github/v/release/lagovi/Operit" alt="latest release"></a>
  <br>
  <img src="docs/assets/readme/operit-ai-banner-en.webp" width="100%" alt="Operit AI">
</div>

## What this fork is

Operit AI is an open-source AI agent platform for Android: it connects cloud or local models to the terminal, browser, files and project workspace. This fork makes it English-only and shrinks it — heavy assets download on demand from our releases instead of bloating the APK.

## What works

- Task chat with tools, workspace context and multi-round jobs
- Cloud models, custom endpoints, local GGUF via llama.cpp
- File, web and terminal control; UI automation (accessibility, Shizuku, root)
- Ubuntu user space, SSH/SFTP, project templates, export to APK
- Memory, roles, workflows, marketplace (ToolPkg, MCP, Skill)
- Voice, themes, floating window

## Quick start

| | |
|---|---|
| Requirements | Android 8.0+, ARM64 |
| Install | Download the APK from [our Releases](https://github.com/lagovi/Operit/releases) (ALPHA builds) |
| Setup | Open the app, follow onboarding, configure a model and permissions |

> **Safety:** install only from [our Releases](https://github.com/lagovi/Operit/releases). Unknown APKs may be tampered with. Chat and settings stay on your device; cloud requests go straight to your configured provider.

## Fork differences

- English-only UI and runtime (Chinese strings removed, audit-gated)
- Heavy payloads (export templates, toolkits) download on first use, verified by sha256
- Minified builds with R8, including debug
- Change registry: `docs/FORK-REGISTRY/HANDOFF.md`

Upstream: [AAswordman/Operit](https://github.com/AAswordman/Operit) (LGPL-3.0-only, same license here).

## Contribute / build

- [Contributing](docs/doc-src/dev-core/CONTRIBUTING.md) (branch rules apply to this fork too)
- [Building](docs/doc-src/dev-core/BUILDING.md)
