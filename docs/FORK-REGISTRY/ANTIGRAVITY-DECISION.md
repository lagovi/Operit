# Antigravity provider: kept in the fork on purpose

Status: decided 2026-11-16. Owner decision: the fork keeps the Antigravity
provider. Upstream removal commit `b6dc07d31633999d6c5c791e2c1a302bf332c12e`
is not ported. Ledger status for it is `skipped`, not `deferred`.

## What upstream did

- `a7676fa3` (PR #1257) added Antigravity OAuth login, fingerprint spoofing,
  quota query, model wiring and settings UI. The same PR also added a separate
  Vertex AI login that was removed inside the PR series itself
  (`docs/TODO/antigravity_oauth_repair_20260928/01_remove_vertex_ai_login.md`).
- `b6dc07d3` (PR #1450) is a mechanical `git revert` of `a7676fa3`: 40 files,
  +4671/-7302, 17 Antigravity-specific files deleted.

## Why upstream reverted it (from the commit and PR text)

The revert message and PR #1450 give release-hygiene reasons, in this order:

1. The feature never shipped in a release: no release tag contains `a7676fa3`,
   so removing it now costs the least it will ever cost.
2. PR #1257 mixed unrelated fixes into the same change; a revert was the
   cheapest way to drop the mixed-in parts.
3. Local credentials saved by the removed code become invalid.

The PR text also states the original limitation: the author of #1257 had no
real Antigravity or Vertex account and no test environment, so login, quota
query and chat were never verified end to end. No policy, licensing or
terms-of-service justification appears in the revert, the PR, or its review
comments. The removal is about release hygiene and unverified status, not about
a documented prohibition.

## What the code in this fork says

The implementation is self-documented at the fingerprints it copies; these
comments are the closest thing to a "why" that exists in either repository:

- `app/src/main/java/com/ai/assistance/operit/data/api/AntigravityOAuthClient.kt:40`
  — the client version and platform fingerprint is spoofed because Cloud Code
  rejects clients below 2.9.0 for new models and flags other traffic as
  anomalous.
- `AntigravityOAuthClient.kt:200` — token exchange/refresh sends the same
  `Host` and `Go-http-client/2.0` user agent as the native client so the
  gateway does not fingerprint a third-party caller.
- `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/AntigravityProvider.kt:72`
  — the request id loses the `operit-` prefix because it is trivially
  fingerprinted upstream.
- `AntigravityProvider.kt:79` — `safetySettings` is stripped because the
  gateway rejects unknown safety fields.
- `AntigravityProvider.kt:84` — a stable session id is derived from the first
  user message to mimic native session tracking.

In other words, the provider works by presenting itself as the native
`antigravity/hub/2.9.1 darwin/arm64` client. That is the design, not an
accident.

## Risks the fork accepts

- The provider depends on a private gateway of a third party. Availability,
  model list and request shape can change without notice.
- Impersonating the native client and removing `safetySettings` is outside
  anything Google documents for third-party use. The fork accepts this as the
  only way the provider works at all.
- The provider is cloud-only; it does not contradict rule K4 (no bundled
  cloud models in the APK).
- Credentials are stored locally and survive app updates only while the
  signature stays the same; a `adb uninstall` wipes them.

## Consequences for the port workflow

- Do not port `b6dc07d3`. Keep the four `Antigravity*.kt` files under
  `data/api/`, `AntigravityProvider.kt` and the settings/model wiring.
- If upstream later re-adds Antigravity, that new commit becomes the port
  target and this document must be revisited.
- The ledger entry for `b6dc07d3` carries status `skipped` with a pointer to
  this file, so `fork_audit.py --ports` does not report it as deferred work.
