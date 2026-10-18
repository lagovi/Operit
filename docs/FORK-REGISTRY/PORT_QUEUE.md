# Port queue: upstream → fork (opened 11.10, user decision)

> **Language rule (user directive, 15.10): talk to the user in Russian.
> All agent-internal docs are in English.**

Practice for a long-lived localization/size fork: NO wholesale merge
(resurrects deleted locales/assets/CN providers), NO rebase of 200
commits (breaks the tested line). Spot ports only. Fork-point: `1ed8687d`
(07.09). Statuses: TODO / PORTED / MANUAL (hand-carried) / REJECTED
(deliberately skipped) / BLOCKED (waits on another port).

## Batch 1 — clean (files don't intersect ours)

| Upstream | What | Status |
|---|---|---|
| `43858cdf` ANR thinking-regex (`ChatUtils.kt`) | PORTED 11.10 |
| `7d1b7274` SQLite bind limit (`ChatContentDao.kt`) | PORTED 11.10 |
| `04eaa397` JS MIME (`WebChatHttpBridge.kt`) | PORTED 11.10 |
| `37696251` xAI draw URL rename (covers `5ffa0cf4`) | PORTED 11.10 |
| `671b8081` video_url Responses (`OpenAIResponsesProvider.kt` + test) | PORTED 11.10 |
| `6152d820` version 51/1.12.2 (by hand, 2 lines) | PORTED 11.10 |
| `af743726` CI `packages: ''` | EQUIVALENT: we already have `packages: platform-tools` with a comment — don't touch |

## Batch 2 — hand-carried (intersects our files)

| Upstream | What | Status |
|---|---|---|
| `45d63331` clipboard-gate (manual + tests) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `1ee5dbb4` tool-result order (cherry-pick -n, clean) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `72c1216b` DB health repair (cherry-pick + AppDatabase resolve + 55 EN lines) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `19b50643` DB health collapse + `907a4c77` weight-import fix (cherry-pick + EN) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `f2414493` config repair (cherry-pick + 14 rewords + 15 EN lines) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `40081a2a` DataStore domain repair (cherry-pick -n, clean) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `62ea0c3e` perf monitors (cherry-pick + both Settings lines + 50 EN lines + terminal-submodule PTY pid) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `4e6ae68a` CPU sampling on top | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `6d5e8ae8` floating crash-protection (cherry-pick -n, clean auto-merge) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `567f245a` group-orchestration dupes (manual, threaded via sendMessageInternal) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `21212530` dialogue review (cherry-pick + 4 EN lines) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `1e89d4f2` composedsl cursor (cherry-pick -n, clean) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `a19d7ef8` unpaired toolcalls (cherry-pick, dead providers dropped) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `6bb2a245` unmatched tool history (wholesale 6 files + EN, via intermediate renames) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `2112033a`+`9cf8283e` copy-preview (cherry-pick + manual conflict resolve) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `72731683` copy-preview read-only field (cherry-pick + manual resolve) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `99830874` tool round markup (manual + test) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `e295a82a` summary sections (cherry-pick + lines by hand into EN) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `863fb0d8` message_persisted (file created by the commit itself; cherry-pick -n, clean) | PORTED 11.10 (CIs green, confirmed by build `20261012_104102Z`) |
| `a7676fa3` (dev) Antigravity OAuth | DONE 13.10 (`92b4cc63`+`d2246757`, CI `38332016760` green, build `20261013_025022Z`) |
## Antigravity acceptance 13.10 (phone, build 20261013_025022Z)
- PASS: `Antigravity (Google)` in the provider dialog; login block
  (Sign in with Google + fixed endpoint + `gemini-3.7-flash` by default);
  OAuth reaches accounts.google.com (Antigravity logo, screenshot);
  loopback+PKCE in logcat; no crashes.
- En route: our `GeminiProvider` held `modelName/apiEndpoint/apiKeyProvider/JSON/customHeaders` private (upstream protected) + had no `unwrapStreamingPayload` — widened to upstream shape, both parse sites wrapped in the hook.
- Deliberately skipped drift (provider-groups, DeepSeek/Moonshot, isEmbeddedLocalProvider, unit tests — their CI doesn't run them).
- Phone returned as-was: Default Config = OPENAI_GENERIC + 192.168.1.55 + key intact (save in logcat).
- USER MANUAL STEP (user does it alone later, decision 15.10): finish Google sign-in, check quotas/model list/chat via Antigravity.
## Backlog (user decisions 12.10)
- **Antigravity**: DONE 13.10 (acceptance above). Only the user manual
  step left (finish Google sign-in — user does it alone later, decision
  15.10).
- **PaddleOCR**: DONE 14.10 (`96e27adb`+fixes, CI `38499450049` green,
  build `20261014_025827Z`, APK 163082823 B = −5.0 MB: ML Kit .so out).
  26 SDK files copied to `com.paddle.ocr` (3 fork points: models/yml
  from disk, OpenCV `System.load`), engine `PaddleOcrEngine` + registry
  `OfflinePayload.PADDLE_OCR` (release `ocr-paddleocr-v1`, ~36 MB).
  Acceptance via Tool Tester (same method): clean/small/noisy/rotated/
  darkui + Russian — 12/12 lines letter-exact (Russian was garbage).
  FALSE ALARM 14.10: "clipped tails" (`123456`, `бул`) — not the model,
  the pictures are wider than the canvas (visible by eye); Paddle reads
  exactly the visible pixels, truth files fixed. detUnclipRatio hypothesis
  dropped. Cold 1.2 s, run ~3–4 s. En route: quickbird `.so` demanded
  `__sfp_handle_exceptions` (in no libc++ on device) → official 4.5.5,
  era-matched (all 377 UND symbols resolve); R8 ate the JNI ctor of
  OrtException → keep; det IR 10 needs ORT ≥1.19 → 1.21.1 for all.
- **Provider proxy** (user decision 13.10): DONE 14.10
  (`2c245bf4`+fix `bd47afae`, CI `38520434647` green, build
  `20261014_062241Z` 163102271 B = +19 KB).
  Done: `proxyMode/proxyHost/proxyPort/proxyUsername/proxyPassword` fields
  in `ModelConfigData` (DIRECT default + baked lab host/ports);
  `data/model/ProviderProxy.kt` (native HTTP + 407-auth; SOCKS5 no-auth
  native, with-auth — `Socks5TunnelSocketFactory` RFC 1928/1929; cache
  per base client); `AIServiceFactory.buildService` — all chat via
  `ProviderProxyClients.forConfig`; Codex/Antigravity
  `fetchUsage/fetchQuota/fetchModels` — optional `proxy: ProxySettings?
  = null` (called from the section with the edited config); login/refresh
  stay direct deliberately (account-level, no config context); UI block +
  mode dialog in `ModelApiSettingsSection` (hidden for MNN/llama, type
  switch swaps the port only while it holds the other baked default);
  10 EN strings `provider_proxy_*`.
  Eyes-on acceptance (Test Connection → gateway .55:20128): DIRECT
  Success; HTTP Success (`route=...:18888`); SOCKS5 Success; SOCKS5+auth
  Success (`SOCKS5 tunnel ... established`, forwarder: `auth ok` +
  CONNECT); dead proxy — clean `Failed: failed to connect to ...:11080`
  within connectTimeout. Default Config restored as-was (0 proxy fields).
  IMPORTANT: lab proxies 192.168.1.55:18080/10808 BLOCK LAN targets by
  policy (curl: outside 200, to gateway — empty reply/000) — end-to-end
  ran via permissive forwarders `/tmp/opencode/testproxy/` (never commit
  to repo, session test infra) on the same box.
  Remainder (CLOSED 16.10, `6594bc2c` — user directive "everything
  through proxy when configured"): `AntigravityProvider.getModelsList`
  and `CodexProvider.getModelsList` carry the config proxy from the
  factory; generic `ModelListFetcher.getModelsList` takes the section
  proxy; token exchange/refresh, email/project discovery, onboarding,
  quota/usage, Codex revocation and both login dialogs are proxied.
  Only the OAuth loopback callback servers stay direct (phone-local).
  (Spec below — executed verbatim, kept as the decision record.)

  **What the user wants (verbatim):** each API provider's settings can say
  the connection goes through a proxy; types — SOCKS5 and HTTP/HTTPS
  (both — with and without login/password).

  **Code map (verified 14.10, line numbers refreshed 15.10; the spec is
  DONE — below is the verbatim plan it was built from):**
  - Chat providers hang off ONE singleton `SharedHttpClient.instance`
    (`api/chat/llmprovider/AIServiceFactory.kt:198`; pool/timeouts/
    listener there). `AIServiceFactory.createService` (`:272`, build —
    `:286`, client — `:301`) hands it to all providers. Client is
    immutable — per-config variant via `instance.newBuilder()` (pool and
    dispatcher shared — good) + `.proxy(...)` (+ `.proxyAuthenticator`
    for HTTP 407), cache per base client (implemented with a
    `Pair<base, ProxySettings>` key, not a bare `ProxyKey`).
  - OAuth/model-list/quota build THEIR OWN clients, not Shared (numbers
    already include the proxy edits: `data/api/CodexAuthManager.kt:24` +
    `clientFor` `:31`, `data/api/AntigravityAuthManager.kt:39` +
    `clientFor` `:49`,
    `ui/features/antigravity/AntigravityOAuthCoordinator.kt:27` —
    coordinator login-time client, direct deliberately).
    Managers are per-user singletons, proxy is per-config: semantics —
    fetchModels/fetchQuota/login calls take the EDITED config's proxy
    (optional param from `ModelApiSettingsSection`, default = direct).
  - Storage: `ModelConfigData` (`data/model/ModelConfigData.kt:80`, proxy
    fields `:98+`, `@Serializable`, all fields defaulted — DataStore
    survives new fields silently, and defaults are NEVER written
    (`encodeDefaults=false` — missing key = default). Fields: `proxyMode`
    (`DIRECT`/`HTTP`/`SOCKS5`), `proxyHost`, `proxyPort`, `proxyUsername`,
    `proxyPassword` (sensitive like apiKey — lives in the same config,
    deliberate).
  - Save: `ModelConfigManager.updateApiSettingsFull` (`:798` — number
    held) — EXPLICIT params per setting: add + `copy()` + update ALL
    callers (exactly two places: `ModelApiSettingsSection.kt` —
    `persist`/`buildAutoSaveState`/`ApiAutoSaveState` (`:307+`) — and
    `ModelConfigManager.kt` itself).
  - UI: block in `ModelApiSettingsSection.kt` (mode selector + host/port/
    user/pass), HIDE for local (`isMnnProvider || isLlamaProvider` —
    vars already in file, `:511–512`; MNN/llama talk to localhost,
    proxy is pointless). Strings — new EN keys in `values/strings.xml`
    (format args `%1$s`-style only, LABEL-CONCAT gate).
  - SOCKS5 with auth OkHttp CANNOT do natively (no-auth only via
    `Proxy(Proxy.Type.SOCKS, addr)`) → own `SocketFactory`: handshake
    RFC1928 (greeting + method; `0x00` no-auth / `0x02` user-pass) +
    on `0x02` RFC1929 (user/pass) + CONNECT to target, return socket;
    plant via `.socketFactory(...)`. No-auth — bare Proxy(SOCKS).
    HTTP/HTTPS — `Proxy(Proxy.Type.HTTP, addr)` (+ Authenticator on 407).
  - OUT of scope (deliberately, not "forgotten"): `RemoteAssetFetcher`
    (payloads must download even when the provider proxy is mistyped),
    STT/TTS/translation/tools. Wider only as a separate task on request.

  **Step 0 (historical: no infra on 14.10; UPDATE: lab HAS proxies —
  192.168.1.55:18080/10808 from the user — but they block LAN by policy,
  see E22 in HANDOFF; end-to-end ran via our permissive forwarders
  `/tmp/opencode/testproxy/`, restore recipe — memo (N35) in HANDOFF):**
  Raise on the notebook (phone on the same LAN 192.168.1.x, reaches it):
  minimal python CONNECT-proxy for HTTP/HTTPS tests (port 8888) and
  minimal SOCKS5 (port 1080: RFC1928 no-auth + RFC1929 user/pass).
  ~80 lines together, run by hand for acceptance, NEVER commit to repo
  (session test infra) — or to `/tmp/opencode/`, not the repo.

  **DoD (eyes on the phone):** chat reply via HTTP proxy, via SOCKS5
  no-auth, via SOCKS5 with auth (target — LAN gateway
  `192.168.1.55:20128`, same LAN, reachable via proxy); negative: crooked
  proxy (dead port) gives a clean error within connectTimeout, not an
   eternal hang. OAuth login via proxy — only if it fits the same scheme
   (don't demand an account specially). Regression: direct mode (DIRECT)
   works as before.

## Backlog — added 18.10

- **Model-down detection in the test scripts (user, 18.10).** The API model
  we use for chat (Gemini Flash Latest) can be unavailable/erroring; the
  app surfaces this as a popup/toast message, but the harness keeps
  treating an empty assistant reply as a pass. The UI-automation harness
  (`ci/script/uawalk.sh` + `ci/script/scen/*.uaw`) and any chat smoke test
  must DETECT that popup/error text and FAIL the run instead of passing on
  a blank response. Discovered during the 18.10 phone-side
  `windows_control` end-to-end test: a chat request to invoke
  `windows_exec` returned an empty assistant body because the model was
  down (the same symptom `Handoff N60b` records as "inconclusive").

## Not taking (REJECTED, deliberate)

- All `values*/strings.xml` upstream additions — only via manual port into
  our `values/` (pull strings along with the ports above, never in bulk).
- `ea819096` README layout (contradicts ours; our README stays).
- woff2 Material Symbols bundle (`f9eded98`, `808c76d2`) — against SIZE goals.
- `83acf075`/`c2333185` apk_reverse toolkit, `33f8ff7c` apktool artifacts — our deleted tool.
- `92327665` proxy history (touches deleted Deepseek/Kimi + model_logos-svg; take only when porting neighboring provider fixes; our own proxy built from scratch 14.10 — don't confuse, that's about upstream code).
- i18n commits (`a51f23b4`, `a46e0c62`, `d1a28473`, `98241209`, `f72b20f6`, `2f31888b`) — nothing to localize, fork is English-only.
- Dev draw packages (`29d67812`, `ef1bf4b6`, `14c8afdc`) — don't touch draw.

## Debt: terminal submodule (non-blocking)
- Pointer `26d4964a`→`04586898` (`pty.pid` needed for the perf monitor)
  brought 49 hardcoded CN strings (SSH screens, mirror names). It's
  someone else's repo (submodule) — can't edit there (CI rebuilds clean).
  Allowlisted in `cjk-allowlist.json` as upstream debt. Full EN pass of
  terminal — separate task, when we fork terminal (upstream started
  values-en itself).
