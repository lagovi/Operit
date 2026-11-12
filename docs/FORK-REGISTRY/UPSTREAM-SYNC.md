# Upstream sync: the port ledger

This page explains `docs/FORK-REGISTRY/upstream-port-ledger.json`, the
machine-readable record of what was taken from upstream and what was not.
The ledger covers upstream (`AAswordman/Operit`) commits that landed after the
fork point.

## Why the ledger exists

The fork never merges `upstream/*`. Every port is done by hand (cherry-pick,
manual copy, or a rewrite), and the merge-base with upstream stays pinned at
`1ed8687d` (2026-09-07) because upstream is never merged back. Once that base is
fixed, a plain `git log upstream/main --oneline` shows hundreds of commits with
no way to tell which ones are already inside the fork.

Without a record, "did we already port commit X?" means reading both codebases
and diffing them in your head. The ledger removes that work: one entry per
upstream commit, written the day the commit is reviewed.

## What the ledger contains

`docs/FORK-REGISTRY/upstream-port-ledger.json` is a JSON document with a header
(`schema_version`, `fork`, `upstream`, `upstream_ref`, `fork_point`, `note`) and
an `entries` array. Each entry has:

| Key | Meaning |
|---|---|
| `upstream_sha` | Upstream commit sha, 7 or more hex characters. |
| `upstream_subject` | The upstream commit subject, for human reading. |
| `status` | One of `ported`, `skipped`, `deferred`, `superseded`. |
| `fork_commit` | Short sha of our commit when ported; `null` otherwise. |
| `reason` | One line: why this status was chosen. |
| `decided` | Date the decision was made (`YYYY-MM-DD`). |

The four statuses:

- **ported** — the change is in the fork. `fork_commit` points at our commit,
  so the port can be inspected or reverted without searching the history.
- **skipped** — deliberately not taken (dropped feature, i18n bulk that an
  English-only fork does not need, size or policy decision). The `reason` field
  carries the justification.
- **deferred** — worth taking, not taken yet. This is the review backlog.
- **superseded** — upstream replaced this commit with a later one; the later
  commit carries its own entry.

An upstream commit with no entry is **UNCLASSIFIED**: nobody has decided on it.

## Using the report

```
python3 ci/script/fork_audit.py --ports
python3 ci/script/fork_audit.py --ports --no-color
```

The script reads the ledger, enumerates upstream commits after the merge-base,
and prints:

- **UNCLASSIFIED** — commits in the upstream range with no ledger entry. Each
  one still needs a decision. This is the work list for a sync.
- **PORTED** — every ported entry with its `upstream_sha -> fork_commit` link.
- **SKIPPED**, **DEFERRED**, **SUPERSEDED** — the other three buckets, listed
  by sha and subject so the state of the backlog is visible at a glance.
- **STALE-ENTRY** — a ledger sha that matches no commit in the range. Almost
  always a typo or an entry written for a commit that was later rewritten; fix
  the sha or drop the entry.
- **Summary** — counts: commits in range, classified, UNCLASSIFIED, ledger
  entries, stale entries.

The report always enumerates from the git merge-base, not from the ledger's
`fork_point`. If the two disagree, the report prints a warning and trusts git.

## Workflow for a new upstream batch

1. `git fetch upstream` so `upstream/main` is current.
2. Run `python3 ci/script/fork_audit.py --ports --no-color`.
3. For each commit in UNCLASSIFIED, read the upstream commit and decide its
   status. Do not batch commits under one entry: one entry per upstream commit,
   keyed by its sha.
4. If the status is `ported`, apply the change to the fork (cherry-pick or by
   hand) and note the resulting fork commit sha.
5. Add or update the ledger entry **in the same change** as the port, with
   `status`, `fork_commit`, `reason` and `decided`. The ledger must never drift
   from reality: a port without its entry is unfinished work.
6. Re-run the report. UNCLASSIFIED should shrink and STALE-ENTRY should be
   empty.

## Relationship to the other documents

- **`registry.json`** records the fork's *own* changes: files touched, rationale,
  and anchors that must survive later ports. The default `fork_audit.py` run
  (without `--ports`) checks those anchors. The ledger is unrelated to it.
- **`PORT_QUEUE.md`** holds the human narrative: batches, acceptance notes,
  phone checks, decisions with their context, links by sha.
- **`upstream-port-ledger.json`** is the machine-readable status of upstream
  commits. `PORT_QUEUE.md` links to shas; the ledger is the matrix of their
  state.

Source of truth: for the question "has upstream commit X been handled?", the
ledger answers. For "why, and what happened during acceptance?", read
`PORT_QUEUE.md` and `HANDOFF.md`.

## Limitations

- The ledger does not automate the port. Conflicts in strings, in resources and
  in English text are resolved by hand, and the result is whatever the fork
  needs, not a patch of upstream.
- It does not use `git cherry-pick -x` or `git patch-id` to detect ports.
  Manual porting changes the patch, so patch identity does not survive; the
  record has to be explicit. That is precisely why every port must write its
  entry.
- The ledger does not prove a port still works. That is the job of the build,
  the default audit run, and phone acceptance.
