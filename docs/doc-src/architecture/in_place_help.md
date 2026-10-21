# In-place help mode

The `?` action in the app bar no longer opens the long help screen directly.
It toggles an in-place mode on the current screen. The mode shows a banner and
turns short taps into element descriptions.

## Data flow

- `HelpModeState` lives in `AppContent` and is provided through `LocalHelpMode`.
- `helpTopicForScreen` maps a screen class to a topic id. The same id is
  provided to each cached screen through `LocalHelpScreenId`.
- `Modifier.helpAnchor(id)` publishes the control's bounds in root coordinates.
  It does not change the control's click, gesture or scroll behavior.
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

## Current limits

- The terminal lives in the `:terminal` module, which cannot depend on app UI.
  It gets the screen guide and unannotated fallback, but no element anchors yet.
- Workspace is an overlay inside the chat, not a routed screen, so it needs a
  separate entry point before it can have its own help mode.
- Dialogs are separate windows and are not observed by the screen interceptor.
- Cached screens publish their own topic id, so hidden screens never overwrite
  the anchors of the visible one.
