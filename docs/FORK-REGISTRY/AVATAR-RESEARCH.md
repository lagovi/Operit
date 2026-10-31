# Avatar presets and creation — research

Status: proposal only. Nothing is implemented until the user picks an option.

## What exists today

Avatars are a per-entity image URI, stored in DataStore, shown by Coil in every
avatar slot.

| Where | Key | Set from |
| --- | --- | --- |
| Character card (AI portrait) | `<card prefix>custom_ai_avatar_uri` | `CharacterCardDialog` → `CompactAvatarPicker`, `ThemeSettingsChatTab` AI picker |
| User portrait per character card | `custom_user_avatar_uri` in the card theme | Theme → Chat → Avatar customization |
| Global user portrait | `custom_user_avatar_uri` in display settings | Theme → Chat → Avatar customization |
| Character group | `<group prefix>custom_ai_avatar_uri` | `GroupCardDialog` → `CompactAvatarPicker` |
| Assistant | same `custom_ai_avatar_uri` of the active card | Assistant → Avatar tab |

The default card gets `file:///android_asset/operit.png` (1024×1024 PNG with
alpha, 1.3 MB) on first run and on reset. `resetDefaultCharacterCard` restores
the same file. That is the only bundled portrait. `<GroupCardDialog>` does not
have a bundled default; a group with no custom avatar gets a generated grid.

Current flow for a character card or group:

1. Tap the round avatar in the card dialog.
2. `ActivityResultContracts.GetContent()` opens the system document picker.
3. `CropImageContract` opens the cropper locked to 1:1, PNG quality 90.
4. The crop result is copied into `filesDir` as
   `avatar_<cardId>_<uuid>.png` and its `file://` URI is saved.
5. Reset removes the key (card) or deletes the managed file (group).

Group cards already have one piece of self-creation: when a managed group
avatar file exists and the member roster changes, `buildDefaultGroupAvatar`
composes a 512×512 grid of member avatars on a light background with an
initial-letter placeholder (`CharacterGroupCardManager.kt` around line 379).

## Why the current flow feels unclear

- The tappable avatar has no label or picker icon, and it is the only way in.
  In `CharacterCardDialog` the “Add/Reset” text below it is 10 sp and easy to
  miss.
- The button says “Add” but nothing suggests that the action is “choose a
  picture from the device”.
- There are no presets at all, so a new user either imports a file or keeps
  the generic Operit portrait.
- “User portrait”, “global user portrait” and “AI portrait” live in a different
  screen with a long explanation, so there is no single place to decide how
  the character looks.
- A crop result is final: there is no way to see the previous avatar, compare
  options, or regenerate.
- Assistant avatar silently follows the active card; changing one changes the
  other.

## Options

Every option keeps the stored contract (a URI in the existing keys), so old
data, backups and web-chat snapshots keep working.

### A. Bundled preset gallery (no network)

Ship 12–24 square portraits as WebP in `assets/avatars/`, plus a small catalog
file (id, label, tags, license, sha256). The picker becomes a bottom sheet with
three tabs: Presets, From device, Create.

- Privacy: nothing leaves the device.
- Licensing: must be original art or CC0/public-domain; each entry needs a
  documented source. 24 WebP at ~80 KB is about 2 MB of APK.
- Storage: no runtime storage; presets are read from assets.
- Risk: taste is subjective, and the set ages. Adding presets means an app
  update.
- APK size: about +2 MB.

### B. Fork-hosted downloadable preset packs

GitHub Releases of `lagovi/Operit` host signed zips (catalog JSON + images).
The app fetches the catalog on demand, shows previews, downloads a pack into
`filesDir`, verifies sha256 and license metadata.

- Privacy: one network request to GitHub; the request reveals the user’s IP to
  GitHub, no account data.
- Licensing: each pack carries its license and attribution; the app shows them
  before download.
- Storage: user-controlled, 1–20 MB per pack; packs can be removed.
- Risk: needs a release pipeline, a versioned catalog schema, and offline
  behaviour that is still useful when GitHub is unreachable.
- APK size: about +50 KB of UI/code.

### C. Optional third-party image service

Add a preset source that queries an external image API (for example an
open-license art search or a generation API). Requires keys or OAuth.

- Privacy: prompts and possibly a portrait go to a third party; needs explicit
  consent and a privacy notice, and must be off by default.
- Licensing: depends entirely on the service; some terms forbid storing the
  result or using it as an avatar.
- Network/storage: unpredictable size, possible rate limits and costs.
- Risk: highest maintenance and the only option with a real legal surface.
- Recommendation: not for the first iteration.

### D. Better local import and crop

Keep “from device” but make it obvious: labelled “Choose image”, a camera
option, an in-app review step with the previous avatar, a “restore previous”
action, and an explicit hint about the 1:1 crop. Optionally remember the last
three choices per entity.

- Privacy: none.
- Licensing: user responsibility, shown as a one-line reminder.
- Storage: keep the last three managed files per entity; cleanup on replace.
- Risk: the least work, but does not solve “I have no picture”.

### E. User-provided images only

No presets, no generation; just the improved import flow from D.

- Cheapest and safest, but a new user still has no starting point. It reads as
  a regression next to the other options.

### F. Self-creation of a new avatar (mandatory option)

A single “Create” entry with three independent tools that all end in the same
1:1 PNG and the same save path:

1. Draw. A small Compose canvas: brush size, colour, eraser, background fill,
   undo, 512×512 output. No extra dependency.
2. Generate. Build a prompt from the card name, description and a handful of
   style chips (anime, realistic, ink, pixel, flat), call the user’s currently
   bound image-capable model, and show 2–4 candidates. The app already parses
   image outputs from OpenAI-compatible providers (`b64_json` in
   `OpenAIProvider.kt`), so this reuses existing plumbing instead of adding a
   service.
3. Compose. A local generator that makes identity-free portraits: background
   gradient plus a geometric figure or animal silhouette, chosen from a small
   palette, seeded by the card id so it is stable. This is the only fully
   offline “new avatar” path and costs no licence.

- Privacy for draw/compose: none. For generate: the prompt goes to the model
  endpoint the user already configured; the image request is no broader than
  normal chat. It must be labelled as such and stay opt-in per tap.
- Licensing: draw and compose are the user’s own output; generate is subject
  to the model provider’s terms.
- Storage: one 512×512 PNG per saved result, about 100–300 KB.
- Risk: draw needs touch ergonomics; generate needs a model that actually
  supports image output, and must report a clear error when it does not.

## Suggested shape

A base that combines D with F, then presets layered on top:

- Picker sheet with three tabs: Presets, Device, Create.
- Presets tab: option A first; option B later if the bundled set feels small.
- Device tab: D with previous-avatar preview and restore.
- Create tab: F, with Compose available offline, Draw next, Generate only when
  the bound model reports image support.
- Option C is left out of the first iteration.

## Decisions from the user

1. Presets are hosted on the fork, not embedded in the APK. They live in the
   separate public repository `lagovi/OperitAvatarAssets`.
2. Generate, Draw and Compose are all in the first iteration.
3. The gallery applies to every avatar: character card, group card, per-card
   user portrait, and the global user portrait.
4. Two preset sets: neutral assistant portraits and stylised characters.

Asset weight is not a selection constraint because the assets live in the
repository, not in the APK.

Animated Assistant avatars are a separate system; see `AVATAR-3D-RESEARCH.md`.

## Open questions

1. For generation, only OpenAI-compatible image endpoints are planned. Is that
   enough, or must other providers be covered in the first iteration?
2. Who reviews preset licences before a pack is published?
