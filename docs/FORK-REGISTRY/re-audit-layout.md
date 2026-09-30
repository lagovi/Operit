# Re-audit: text fitting its container

## Why English specifically

English strings are two to three times wider than the Chinese originals they
replaced. Every layout defect in this file was invisible under Chinese and
appears the moment the locale is English.

That makes English the worst case, so a layout fixed for English is fixed for
every locale added later. Fix for the widest string, not the average one.

## Two mechanisms cause almost all of it

### Fixed height + a text style that was not given a `lineHeight`

Compose's `Text` with only a `fontSize` and no `style=` inherits
`LocalTextStyle`, which M3 sets to `bodyLarge` — 16 sp **with a 24 sp line
height**. In a `Box(Modifier.height(36.dp))` or a `TextButton(height = 28.dp)`,
the glyphs are 14 sp but the line box is 24 sp, so the container is asked to fit
a 24 sp line and the text is cut vertically.

The project typography (`ui/theme/Type.kt`) overrides only three slots. The other
twelve are M3 defaults, and `bodyLarge` and `labelSmall` carry a `0.5.sp`
letter-spacing that widens every string using them.

### `maxLines` without `overflow`

`maxLines` alone defaults to `TextOverflow.Clip`, which slices the line
horizontally through the glyphs. `TextOverflow.Ellipsis` is the other outcome.
Which one a user sees is a choice that was made implicitly by omission.

## Confirmed defects, now fixed

| screen | file | what was wrong |
|---|---|---|
| Toolbox grid | `ui/features/toolbox/screens/ToolboxScreen.kt` | card was `height(156.dp)`; a two-line title left the description 28 dp of a 32 dp block, so its second line was cut through the middle. Card is now `heightIn(min = 156.dp, max = 200.dp)` with the description as the elastic child at `maxLines = 3`. |
| Chat top bar | `ui/main/components/AppContent.kt` | two unweighted `Text`s in the `TopAppBar` title slot, glued by a `"- "` literal in Kotlin. Now a `chat_title_conversation_suffix` format resource, with the title `weight(1f, fill = false)` and the conversation name taking the remainder and ellipsizing. |
| Model config provider | `ui/features/settings/sections/ModelApiSettingsSection.kt` | `SettingsSelectorRow` capped the value at `weight(0.5f, fill = false)` = 33% of the row while the label could take 66% whether or not it needed it, so "Other Providers" became "Other Pr…" with ~90 dp free. Label is now `fill = false` and the value takes the remainder. |
| Model / thinking selector | `ui/features/chat/components/style/input/agent/AgentChatInputSection.kt`, `.../classic/ClassicChatSettingsBar.kt` | fixed `width(300.dp)`/`width(280.dp)` cards, and `stringResource(...) + ":"` produced "Thinking: mode" with no space. Widths are now ranges, punctuation is in resources, and value `Text`s are `maxLines = 1` so rows stay the same height. |
| Package lists | `ui/features/packages/screens/PluginTabContent.kt`, `components/PackageItem.kt`, `PackageTabContent.kt` | every description was `maxLines = 1`, truncating mid-sentence in a column with room. Now `maxLines = 3`, consistent across all three. |

## String concatenation

Roughly twenty sites built labels as `stringResource(R.string.X) + ":"` or
`+ " (%d)"`. None of them can be reordered or re-spaced by a translator. All are
now format resources. `R.string.model_label` = `Model: %1$s` was the existing
precedent; the new `*_colon` keys follow it.

The last four held out because they joined two independent sentences rather than
a label and its value, and were closed separately: `models_displayed` now has
three complete variants instead of being concatenated with a suffix and a
hardcoded `" • n"`, and the llama download tip and blur radius became format
resources.

```bash
grep -rn 'stringResource([^)]*) *+"' app/src/main/java --include=*.kt   # expect: nothing
```

## Fixed-width menus

`DropdownMenu(Modifier.width(180.dp))` in `ChatArea` and `ChatScreenContent`
left the longest English labels shorter than the menu item's own padding, so
they were clipped through the glyphs. Both are `widthIn(min = 180.dp, max = 280.dp)`.

This is the general shape of the bug worth looking for: a fixed width tuned to
the shortest translation, where the padding is a fixed cost that the text has to
fit inside.

## Font scale

The in-app font scale slider goes to **1.5** and is not clamped, and it
multiplies with the system accessibility scale. Every fixed height in this app is
correct at 1.0 and breaks at 1.5.

`on-device verification must be done at 1.5`, in Settings → theme → font size.
Anything that only looks right at 1.0 is not fixed.

## High-risk patterns still in the tree

Worth checking after a merge, since upstream adds screens faster than they get
audited:

- `DropdownMenuItem(Modifier.height(36.dp))` with no `maxLines`/`overflow` —
  `ChatArea.kt` has 14 of them in a `width(180.dp)` menu; the longest English
  labels need about 136 dp of a ~132 dp row. Same pattern in
  `ChatScreenContent.kt` (4) and `MCPServerScreen`.
- Segmented controls inside a fixed-width card — `AgentPermissionSegmentedControl`
  in the 300 dp agent card and its twin in the 280 dp classic card. "Disabled"
  does not fit its cell.
- `PackageManagerScreen` tabs use `softWrap = false` with no `maxLines`, which
  guarantees a hard cut at the tab boundary.
- `Text(fontSize = …)` with no `style=` in a container shorter than 24 dp. The
  densest files are `CharacterCardDialog.kt` (44 occurrences),
  `ModelPromptsSettingsScreen.kt` (28), and the two chat input bars (~19 each).
- The token-statistics feature has 40+ `maxLines = 1` without `overflow` across
  dense fixed-width metric cells.

## Checklist

```bash
# no site should build a label by concatenation
grep -rn 'stringResource([^)]*) *+ *"' app/src/main/java --include=*.kt
grep -rn 'maxLines = [0-9]' app/src/main/java --include=*.kt | wc -l
```

On device, at font scale 1.5: Toolbox, Model config, chat top bar, model and
thinking selector, package list, drawer, and the settings screens.
