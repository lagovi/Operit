# Re-audit: prompt text the user can see

## The rule this fork applies

**Model input and user-visible text are different things.**

| where the text goes | language |
|---|---|
| Sent to the model as context, never rendered | Chinese is acceptable. The user cannot read it, and translating it is a large diff over files upstream actively edits. |
| Rendered or editable anywhere in the app's UI | **English. Always.** |

`usesChineseContent(context)` is now always false, so the `useEnglish: Boolean`
gating in the prompt builders is effectively pinned to `true`. That means the
English branch is what ships. The Chinese branches are dead code that upstream
will keep editing, and deleting them would create permanent merge conflicts for
no user-visible gain. Leaving them is a deliberate trade, not an oversight.

## Surfaces to re-check after every upstream merge

This is the list a script cannot check: places where Chinese prompt text becomes
UI text without any change to the prompt file itself.

Ordered by how much prompt text each one can surface.

1. **`ui/features/settings/components/CharacterCardDialog.kt`** — the richest
   prompt-editing surface. Fields for `characterSetting`, `openingStatement`,
   `otherContentChat`, `otherContentVoice`, `advancedCustomPrompt` and `marks`,
   all backed by `CharacterCardManager` keys and all concatenated into the system
   prompt. **Also** renders tool `categoryName` · `description` as a picker
   subtitle, which comes from `SystemToolPrompts`.
2. **`ui/features/chat/components/style/input/common/ToolPromptManagerDialog.kt`**
   — same source, renders `categoryName`. Reached from `ClassicChatSettingsBar`
   and `AgentChatInputSection`.
3. **`ui/features/workflow/screens/WorkflowDetailScreen.kt`** — the only place
   `SystemToolPrompts.getAllCategoriesCn()` reaches the UI, rendering ~550 tool
   descriptions and their parameter schemas.
4. **`ui/features/settings/screens/TagMarketScreen.kt`** — renders preset tag
   name, description and `promptContent`, and copies the localized prompt into a
   user-editable tag.
5. **`ui/features/settings/screens/ModelPromptsSettingsScreen.kt`** — tag prompt
   preview and `TagDialog` editor; also re-seeds defaults when a character card
   is created.
6. **`ui/features/settings/screens/WaifuModeSettingsScreen.kt`** — the waifu
   custom prompt `TextField`. The default was Chinese; it is now English
   (`WaifuPreferences.DEFAULT_WAIFU_CUSTOM_PROMPT`).
7. **`ui/features/settings/screens/ContextSummarySettingsScreen.kt`** — the
   summary custom-rules field.
8. **`ui/features/settings/screens/UserPreferencesSettingsScreen.kt`** — the
   `user.md` profile editor, injected into the knowledge-graph prompt.
9. **`ui/features/chat/components/style/bubble/BubbleStyleChatMessage.kt`** and
   the cursor variant — the summary-message renderers. Any Chinese appended to
   the summary string in `AIMessageManager` is user-visible.
10. **`ui/features/packages/screens/PackageManagerScreen.kt`** — AI-generated
    package descriptions. The generator instructs the model in Chinese
    (`FunctionalPrompts` CN branch); that text is persisted and displayed.
11. **`ui/features/toolbox/screens/tooltester/ToolTesterScreen.kt`** — renders
    `ToolResult.error`. Exactly one Chinese `ToolResult.error` existed, in
    `StandardUITools`; it is now `R.string.ui_controller_image_capability_required`.
    **This is the pattern to watch**: a tool error message is invisible until
    someone opens the Tool Tester, and it bypasses resources entirely.

## Two locale checks that are not `usesChineseContent`

Both of these read `resources.configuration` directly and are correct for an
English build because they compare against `"zh"`. They will need attention the
moment a second locale ships, and they are a trap when adding one.

- `TagMarketBilingualData.isChineseLocale(context)`
- `CharacterCardBilingualData.isChineseLocale(context)`
- `WorkflowDetailScreen`'s inline `isChinese` check

## Where the two bilingual data files stand

`TagMarketBilingualData.kt` and `CharacterCardBilingualData.kt` already carry
`nameZh`/`nameEn` pairs, so an English build renders English with no change
needed. Verify rather than assume if a merge touches them: a missing `en` field
resolves to the `zh` value and looks exactly like defect 4 above.

## Checklist

```bash
# new Chinese literals anywhere in Kotlin (model prompts included — decide each)
python3 ci/script/fork_audit.py

# spot-check the surfaces above on device, after a data clear
adb shell pm clear com.ai.assistance.operit.debug
```
