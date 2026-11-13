---
fork: https://github.com/luojiaping/Operit
status: device-smoke-passed
last_reviewed: 2026-11-05
---

# Character card prompt editor completion

[Back to the main plan](index.md)

## Feedback and current state

User feedback from a real device: the character card's character setting, opening statement, other content, custom prompt and marks still have scroll issues, and PR #1411 did not cover the full editing experience.
The edit entry of the home character selector eventually leads into the same character card settings and editing components.

The original patch only replaced the scroll state of the outer `Column`. The `rememberScrollState()` of the current Foundation 1.10.4 already uses `rememberSaveable`, so that swap cannot fix the cursor, selection, scroll and keyboard layout changes inside a multi-line input.
The compact input and the full-screen editor still use the old string-value callback interface; the outer form did not hold each field's input and internal scroll state separately, and the full-screen draft did not carry over the source field's selection.

## Scope and steps

1. Hold a stable `TextFieldState` and an input-internal `ScrollState` for every character card field, using the state-based text input interface supported by the project's existing dependencies.
2. Uniformly cover character setting, opening statement, other content (chat/voice), custom prompt, marks, and the name and description in the shared component; update the tag editor and the callers of the shared input component at the same time.
3. Full-screen editing uses an independent draft and an independent viewport, preserving the source field's selection; saving writes back the text and selection, cancel keeps the original field content.
4. Review the boundary between the input field and the parent scroll window and the keyboard layout, without adding delayed position restoration or code that forces a jump back to an old position.
5. Add regression cases for real Compose editing behavior covering long text, focus switching, re-layout, expand, save and cancel; add a real-device checklist.

## Implementation result

- Added `PromptEditorState`, which holds the text, selection and internal scroll state separately per character card or tag ID and field ID.
- Character setting, opening statement, other content (chat/voice), custom prompt and marks all use the same state-based input; the callers of the shared input for name, description and character group are updated in sync.
- Field state is owned by the form, so hiding advanced fields does not destroy their input and scroll state. Saving the character card reads the current field state directly, avoiding capturing the previous frame's text.
- The full-screen editor creates an independent draft and an independent `ScrollState`, initialized with the source field's selection. Saving writes back the text and selection, cancel does not write back, and reopening uses the source field's current content.
- The tag's name, description and prompt inputs likewise use independent state.
- No delayed position restoration, no forced scroll back to an old position and no extra exception handling paths were added.

## Verification record

- The state-based `OutlinedTextField`, `TextFieldState.Saver`, `ScrollState` and line limit interfaces were checked against the Material3 1.4.0 and Foundation 1.10.4 sources used by the project.
- The callers of the shared input component and the write-back paths of save, translate, full-screen save and cancel were checked item by item.
- `git diff --check` passes; no trailing whitespace was found in the new source, test or this document.
- Added `PromptEditorAndroidTest`, covering the parent-form and input nested scrolling of the six prompt field groups, tapping to type at a visible position, shrinking the viewport, hiding and showing again, full-screen save and cancel, and isolation across character switches.
- The integrated build before export completed `:app:assembleRelease`, with Gradle reporting `BUILD SUCCESSFUL in 13m 2s`, and the APK passed V2 and V3 signature verification. The build was not re-run while organizing this contribution branch.
- The user reported on 2026-11-05 that real-device acceptance passed; this records the actual feedback, and the new Android instrumentation cases have still not been executed.

## Real-device checklist

1. Enter editing from the home character selector, and fill the character setting, opening statement, other content (chat/voice), custom prompt and marks with enough long multi-line content.
2. For each field, first scroll the parent window, then scroll inside the input field, tap, type and delete at the currently visible position, and confirm that neither the text nor the parent window is pulled back to the top.
3. Toggle the keyboard and switch field focus, confirm that the line being edited is still visible; IME composition input, newlines and selection operations work normally.
4. Collapse and re-expand the advanced settings, confirm that the content, selection and internal scroll position of the custom prompt and marks are preserved.
5. Expand the full-screen editor of the six field groups separately, scroll and modify middle content; after saving, the source field's text and selection update, after cancel the source content is unchanged, and reopening does not show the canceled draft.
6. Save the character card and reopen to confirm the content is complete, then switch to another character card and confirm that the previous card's draft does not leak in.
7. Run one long-text editing regression on the tag prompt and the character group description.

[DONE] Field state, compact and full-screen editing, shared callers and regression cases are complete; the integrated release build and signature passed, the user passed real-device acceptance, and the Android instrumentation cases remain to be executed.
