---
fork: https://github.com/yoruuuchan/Operit
issue: 709
---

# Preserving scroll position in Compose DSL and settings pages

## Old implementation

- The `LazyColumn` of the `ToolPkg Compose DSL` called `rememberLazyListState()` directly inside the renderer. The whole tree is rebuilt after a JS state change, so once the list node leaves composition the scroll state is lost with it.
- `LazyRow` did not pass an explicit `LazyListState` at all.
- The character card editor and the tag editor used `rememberScrollState()`; the tab content is replaced by a `when` branch, so the list jumps back to the top after leaving composition.
- The `items` of the character card list and the tag list had no stable key, so a data refresh rebound entries by position.

## Intent

Make ordinary state updates, tab switches and dialog re-layout stop dropping the scroll position back to the top, while keeping normal navigation, explicit scrolling and list data update scrolling behavior.

## Scope

- `app/src/main/java/com/ai/assistance/operit/ui/common/composedsl/`
- `app/src/main/java/com/ai/assistance/operit/ui/features/settings/`
- the list state keys of `examples/worldbook`
- the `tools/compose_dsl/` generator and template
- the corresponding JVM unit tests

Pages that already manage their own scroll state separately, such as the main chat screen and the file manager, are not changed.

## Steps

1. [LazyListState persistence](1_LazyListStatePreservation.md) [DONE]
