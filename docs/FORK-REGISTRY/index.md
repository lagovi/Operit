# Fork registry

**Starting fresh? Read [`HANDOFF.md`](HANDOFF.md) first.** It says what state
this work is in, what is deliberately unfinished, and which two topics are next:
further distribution-size reduction, and replacing the local speech recogniser.

This directory is the fork's answer to "what did we change, and what has to be
re-done after the next upstream merge?"

Everything the fork does to `AAswordman/Operit` is recorded here in two forms:

- **`registry.json`** — machine-readable. One entry per fork change, with the
  files it touches, a rationale, and *anchors*: literal strings that must still
  be present in the tree for the change to still be in effect.
- **`cjk-allowlist.json`** — every Chinese string literal in Kotlin that was
  examined and judged acceptable (model prompts, logs, parsers, wire values),
  keyed by content rather than line number.

Both are consumed by `ci/script/fork_audit.py`, which is the single command to
run after merging upstream.

## Why this exists

Upstream ships new UI strings in Chinese, in whatever order features land. A
plain merge therefore reintroduces Chinese into an English-only build without
any error, and nothing in the build fails. The only reliable defence is a
checked-in list of what the fork changed plus a check that each change is still
present.

## Running the audit

```bash
python3 ci/script/fork_audit.py
```

Optional flags:

| flag | effect |
|---|---|
| `--no-hotspots` | skip the upstream-overlap report (faster, and works offline) |
| `--no-color` | plain output |
| `--base <ref>` | compare against a specific merge base instead of `merge-base HEAD upstream/dev` |

Exit status is non-zero when a check fails, so it can gate CI.

### What it reports

| check | meaning | action |
|---|---|---|
| `ANCHOR-LOST` | a fork change is no longer applied — its anchor string is gone from the tree | re-apply the change, or confirm it is no longer needed and drop the entry |
| `STRING-UNDEFINED` | an `R.string.x` referenced from Kotlin has no definition in the default resource bucket | this is upstream having added a string nobody translated. Add the English text. **This is the check that catches an untranslated upstream string before it ships.** |
| `CJK-NEW` | a new Chinese string literal in Kotlin that is not on the reviewed allowlist | decide whether it is user-visible. If it is, extract it into a resource. If not, add it to the allowlist. |
| `LABEL-CONCAT` | a label is built by joining two resources in code instead of being one format resource | merge into a single format resource with a placeholder |
| `UNREGISTERED` | the shipped `values-*` directories do not match `registry.json`'s `shipped_locales` | keep `registry.json`, `res/xml/locales_config.xml` and `LocaleUtils.getSupportedLanguages()` in agreement |

The audit also prints **merge hotspots**: files the fork has touched since the
merge base that upstream has *also* touched. Those are where a re-merge will
conflict and the first place to look when deciding whether a fork change is
still required.

## After every upstream merge

1. `git fetch upstream && python3 ci/script/fork_audit.py`
2. Work the `ANCHOR-LOST` and `STRING-UNDEFINED` findings first. Everything else is noise until those are clear.
3. Read the re-audit checklists, which cover the changes a script cannot see:
   - [`re-audit-l10n.md`](re-audit-l10n.md) — the resource and language machinery
   - [`re-audit-prompts.md`](re-audit-prompts.md) — prompt text the user can see
   - [`re-audit-layout.md`](re-audit-layout.md) — text that must fit its container
4. On-device pass at the maximum in-app font scale (1.5). English is the widest
   locale this fork ships, so anything that clips in English clips in every
   locale added later.
5. Update `registry.json`: set `status` to `applied` on entries you landed, and
   re-record anchors for entries whose code you had to re-do.

## Adding an entry

Add to `registry.json`:

```json
{
  "id": "SIZE-005",
  "area": "size",
  "title": "One line describing the change",
  "commit": null,
  "status": "planned",
  "rationale": "Why this change exists. What was observed, and what the cost of not doing it is.",
  "files": ["path/one.kt", "path/two.kt"],
  "anchors": [
    {
      "file": "path/one.kt",
      "contains": "a literal that will not change unless this feature is removed",
      "why": "what this anchor identifies"
    }
  ],
  "reverify": "the command or manual check that proves it still works"
}
```

`status` is `planned` until the change lands, then `applied`. Planned entries are
counted but not checked for lost anchors — the anchor in a planned entry is the
contract to satisfy, not a claim about the current tree.

An anchor must be something whose disappearance would mean the fork change is
gone. A good anchor is a distinctive literal string, a DSL call, or a symbol
name. A bad anchor is a common word or a line number.

## Layout

```
docs/FORK-REGISTRY/
  HANDOFF.md            start here: state, open questions, rules that break the build
  index.md              this file
  registry.json         every fork change
  cjk-allowlist.json    reviewed Chinese literals in Kotlin
  re-audit-l10n.md      what to re-check after an upstream merge: resources
  re-audit-prompts.md   what to re-check: prompt text the user can read
  re-audit-layout.md    what to re-check: text fitting its container
```

## Relationship to the rest of the docs

`docs/TODO/` holds the working history and per-change narrative, following
`docs/TODO/README.md`. This directory is the *operational* index: what to run,
what to look at, what breaks if a change is lost. The older
`docs/TODO/fork-l10n-runbook.md` remains useful for the CI and device-testing
mechanics but its diagnosis of the leftover Chinese text is superseded — see
`re-audit-l10n.md` for the corrected account.
