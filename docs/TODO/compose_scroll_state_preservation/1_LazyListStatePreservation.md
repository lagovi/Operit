# LazyListState persistence

## Old implementation

In `ToolPkgComposeDslGeneratedRenderers.kt`:

- `renderLazyColumnNode` used `rememberLazyListState()`.
- `renderLazyRowNode` had no state parameter and used the default internal state of `LazyRow`.

Although `rememberLazyListState()` is itself saveable, saveable only works while the composition is still alive or the host has saved the composition state. The Compose DSL tree is rebuilt by JS after every state update, and conditional branches, tabs and dialog rebuilds make the list leave composition.

The `when (currentTab)` of `ModelPromptsSettingsScreen` likewise replaces the whole tab content. The outer `rememberScrollState()` of `CharacterCardDialog` and `TagDialog` is itself saveable, and swapping its Saver alone cannot handle the scroll, selection and focus changes inside the input fields; the follow-up fix for that part is covered in [Character card prompt editor completion](2_CharacterCardEditors.md).

## New implementation

- Added `ComposeDslScrollStateStore`, owned by the DSL page, the dialog host and the embedded render entry; its lifetime is longer than the UI tree.
- Added `rememberComposeDslLazyListState(nodePath, nodeType, key)`: it prefers to get the state from the store by a stable identity, and uses `rememberSaveable(LazyListState.Saver)` when there is no host.
- The stable identity prefers the DSL node `key`, otherwise the node path and the normalized type; worldbook declares a key for the list page, the edit page, the group page and the import page separately, so different views do not share scroll positions.
- `LazyRow` also receives the same state explicitly.
- The character card editor and the tag editor now use `rememberSaveable(ScrollState.Saver)`; the character card list and the tag list get `key = { it.id }`.
- The three settings tabs are uniformly wrapped in `SaveableStateHolder.SaveableStateProvider(currentTab)`, so the list state is restored when returning to a tab.

## Verification

- JVM unit test: `ComposeDslScrollStateStoreTest`.
- Manual acceptance scope: switching item state in the worldbook list, typing in the edit page, typing in the character card and tag editors, round-tripping the settings tabs, and the scroll position after a list reload.
- 2026-11-05 user feedback: the character card's character setting, opening statement, other content, custom prompt and marks still have issues, so the original patch did not fully cover the editor's internal state and these windows cannot be recorded as verified.
