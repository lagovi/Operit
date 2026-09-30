# Re-audit: resources and language machinery

What to re-check after merging upstream. This supersedes the diagnosis in
`docs/TODO/fork-l10n-runbook.md`, which blamed leftover Chinese on hardcoded
Kotlin literals. That diagnosis was wrong for the symptoms the user actually saw.

## The corrected account of the four defects

`values-en` was already complete (7659/7659) and the hardcoded literals in
`:app` had already been extracted. The Chinese the user saw had four separate
causes, none of them a missing translation.

### 1. A fresh install started in Chinese

`LocaleUtils.resolveSupportedLanguageCode` returned an unsupported system locale
verbatim. On a `ru-RU` phone the app therefore configured itself for `ru-RU`,
Android found no `values-ru`, and fell back to the unqualified `values/` bucket —
which was Chinese. The language *setting* never came into it; the phone simply
never had one.

Anchors: `LocaleUtils.kt` — `resolveSupportedLanguageCode`, `supportedLanguages`;
`UserPreferencesManager.kt` — `DEFAULT_LANGUAGE`.

### 2. A notification stayed Chinese after switching to English

`AIForegroundService` is a service, so `this` is the Application context, whose
`Resources` keep whatever locale the process started with. Below Android 13
`setAppLanguage` reconfigures the calling Activity only and the process is not
restarted, so a language change never reached it. The visible symptom was an
English chat next to a Chinese notification in the same shade.

Anchors: `AIForegroundService.kt` — `val localized = LocaleUtils.getLocalizedContext(this)`
inside `createNotification()`; `FloatingChatService.kt` — the same pattern.

**If you touch either file, keep the localized context.** Replacing it with
`getString(...)` on `this` reintroduces defect 2 and it will not reproduce in
any English-only test that does not switch language mid-session.

### 3. Values written to DataStore on first launch stayed Chinese

`ModelConfigManager.createFreshDefaultConfig` and the `WakeWordPreferences`
defaults persist a *localized* string on first run. On a device whose first run
was Chinese — which is exactly what defect 1 produced — those stay Chinese
forever, because they are user settings, not resources.

This is not a bug to fix; it is the reason the symptom persisted. It resolves
itself once defect 1 is fixed, but an existing installation will keep its stored
values. **When verifying, clear app data** (`adb shell pm clear`) or the old
values will still be there and the test will look like a failure.

### 4. A JS plugin rendered Chinese inside an English UI

`examples/thinking_guidance` is built by CI and packed into the APK. Its
`preferredLanguage(event)` reads `event.eventPayload.useEnglish`, but
`ToolPkgCommonBridgePlugin` built the `toolpkg_input_menu_toggle` payload without
that key, so the plugin fell back to `zh`.

Anchors: `ToolPkgCommonBridgePlugin.kt` — `"useEnglish" to
!LocaleUtils.usesChineseContent(params.context)` in **both** payload sites.

**Both** sites matter: one builds the toggle, the other refreshes the list. A
merge that re-adds one of them brings the bug straight back.

## Resource layout

| bucket | contents |
|---|---|
| `app/src/main/res/values/strings.xml` | **English**, and the single source of truth for resource keys |
| `app/src/main/res/values/colors.xml`, `themes.xml` | unchanged |
| `app/src/main/res/values-night/themes.xml` | night qualifier, not a locale |
| `terminal/src/main/res/values/strings.xml` | **English**, same rule for the terminal module |

There is no `values-zh/` and no `values-en/`. The default bucket holds English on
purpose: it is what Android falls back to for any unresolved reference, and what
the system resolves launcher labels and app shortcuts in.

Adding a locale later means: create `values-<code>/strings.xml`, add a `Language`
entry to `LocaleUtils.supportedLanguages`, add a line to
`res/xml/locales_config.xml`, add the tag to `registry.json`'s `shipped_locales`,
and widen `localeFilters` in `app/build.gradle.kts`. `ci/script/fork_audit.py`
fails if the last two disagree.

## Things a script cannot check

- `usesChineseContent(context)` is now always false. It is kept because the
  prompt builders are still bilingual. Do not assume a prompt is English because
  the app is — see `re-audit-prompts.md`.
- Chinese text fetched from `operit.app` (announcements, market package names)
  is not a resource and is not affected by any of the above. See
  `docs/TODO/remote_content_translation/`.

## Notification channels are the one thing resources cannot fix

A channel's name and description are captured by the system the first time the
channel is created and cannot be changed afterwards. An install whose first
launch was in another language therefore keeps that language's channel name in
the system settings permanently, no matter what the app does. Observed on device
before the fix:

```
NotificationChannel{mId='AI_SERVICE_CHANNEL', mName=Operit 正在运行, ...}
```

A string resource cannot help — it only ever applies to a fresh install. The
channel name is a plain constant and the channel id carries a version suffix,
which is the only mechanism that makes the system create a new channel. If you
add a channel, give it a version-suffixed id and a constant name, not a resource.

## Tooling that used to be wrong

`tools/string/check_strings.py` and `ci/script/check_localizations.py` both
hardcoded the source locale as `zh` and treated `values/` as the Chinese origin.
Once `values/` became English they compared every other bucket against English
and called the result a "missing translation" — silently, because with a single
locale shipped there was nothing to compare and the gate reported zero errors.
The source is now identified by being the unqualified bucket (`SOURCE_LOCALE`).

Both tools are only meaningfully exercised once a second locale exists. If you
add one, that is the moment to trust them.

## Commands

```bash
python3 ci/script/fork_audit.py
python3 -c "import xml.etree.ElementTree as ET; ET.parse('app/src/main/res/values/strings.xml')"
adb shell pm clear com.ai.assistance.operit.debug   # then relaunch and read the shade
```
