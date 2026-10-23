# In-place help mode

The `?` action in the app bar no longer opens the long help screen directly.
It toggles an in-place mode on the current screen. The mode shows a banner and
turns short taps into element descriptions.

## Data flow

- `HelpModeState` lives in `AppContent` and is provided through `LocalHelpMode`.
- `helpTopicForScreen` maps a screen class to a topic id. The same id is
  provided to each cached screen through `LocalHelpScreenId`.
- `Modifier.helpAnchor(id, scopeOverride)` publishes the control's bounds in
  root coordinates. It does not change the control's click, gesture or scroll
  behavior. `scopeOverride` registers the anchor under another screen id, which
  is how an overlay annotates itself. A `DisposableEffect` removes the anchor
  when the control leaves composition, so tab switches, collapsed sections and
  recycled list rows cannot leave stale rectangles that answer later taps.
- `Modifier.helpTapInterceptor(state)` is attached once per screen wrapper. It
  observes the pointer stream at the initial pass, leaves the press and drag
  untouched, and consumes only the up event of a short tap. That keeps lists
  scrollable while preventing the control under the finger from acting.
- `HelpModeState.tap` picks the smallest registered rectangle that contains the
  tap. A hit opens the element entry from `HelpModeCatalog`; an unannotated tap
  opens the screen guide from `HelpTopics`.

## Adding an entry

1. Add the element text to `HelpModeCatalog` under the screen id.
2. Put `.helpAnchor("element_id")` on the control. Prefer the outer clickable,
   not an inner icon, so the whole touch target is described.
3. The screen id must already exist in `helpTopicForScreen` and in
   `HelpTopics`. A screen without a topic gets no `?` action.

Toolbox cards use their navigation entry id (`toolbox.*`) as the anchor id.
All built-in cards have an entry. A plugin-provided card whose id is absent
from the catalog deliberately falls through to the Toolbox screen guide; add
an entry when a plugin card becomes standard.

Settings rows use `settings.*`. The shared row composables take an optional
`modifier`, so the anchor wraps the whole clickable row rather than a label.

Assistant Config uses two tabs under the same screen id. Anchors on the
visible tab only are live; anchors on the hidden tab are disposed. The Agent
chat input bar (the default composer) publishes `attach`, `model`, `more`,
`composer`, `composer_expand` and `send`; `attach` and `send` are anchored on
the outer touch targets, not the icons. The composer sits at the bottom window
edge, so the lower part of its 36 dp attach target is clipped by the system
navigation bar even though help reads the unclipped Compose bounds.

The Workspace is an overlay inside AI Chat, not a routed screen. Its header
`?` calls `helpMode.toggle("workspace")` and its controls call
`.helpAnchor("workspace.*", "workspace")` while the underlying screen id is
still `aichat`. When the overlay closes, a `LaunchedEffect` in
`WorkspaceManager` disables help if the active id is still `workspace`.

## Current limits

- The terminal lives in the `:terminal` module, which cannot depend on app UI.
  It gets the screen guide and unannotated fallback, but no element anchors yet.
- The Workspace setup screen (no workspace bound yet) still has no `?`; only
  the opened workspace overlay does.
- Dialogs are separate windows and are not observed by the screen interceptor.
- Cached screens publish their own topic id, so hidden screens never overwrite
  the anchors of the visible one.
