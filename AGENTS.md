# AGENTS.md

The execution rules below constrain the agent, not the project or the software.
They are working instructions, so they do not belong in product documentation.

## Execution rules

- Language of communication with the user: Russian only. This covers chat answers
  and any message addressed to the user.
- Language of documentation maintained by the agent: English only. This covers
  everything written into the repository: `docs/`, `AGENTS.md`, `HANDOFF.md`,
  `TODO-AUDIT.md`, README, CHANGES.md, code comments and commit messages. If a
  file already holds non-English text, new text is still written in English and
  the existing text is translated as the file is edited.
  - Exception: verbatim quotations and test data (audio used for recognition,
    the user's own lines, example commands) stay in their original language and
    are not translated, because translating them destroys their evidential value.
- Code and its identifiers are always English, regardless of the language of
  conversation.
- Builds, tests and linting may be run without a separate request (standing
  permission, 10.10). Not too often: batch small features together, for example
  `./gradlew :app:compileDebugKotlin` after several related edits rather than
  after each one.
- Producing an APK for acceptance happens only through CI after a push (manual
  dispatch). Do not build or install a local debug APK.
- Google Colab: every session, CPU or GPU, consumes the account's shared limit
  (a fresh account gets roughly 60 hours of CPU session time, about 5.5 hours of
  GPU session time; the figures vary because Google changes them). Close the
  session as soon as the work is done. Do not leave idle sessions between tasks
  and do not run more than one session at a time. Do not rely on automatic
  reaping of idle sessions, because they stay alive. The real remaining quota is
  visible only in the Colab web interface; no command reports it.
- Environment limit (10.10): there is no local Android SDK, only platform-tools
  and adb, plus JDK 21 in `/tmp/opencode/jdk`. Therefore
  `./gradlew :app:compileDebugKotlin` does not run locally, and the real check of
  the code is the CI build after a push.
- Branch names must follow the pull request naming convention in
  `docs/doc-src/dev-core/CONTRIBUTING.md`.
- The ToDo tool (todowrite) must reflect reality at all times. It is not a
  one-shot list written at the start of a session and then forgotten.
  - When a task is genuinely finished (code merged and a verification exists),
    mark that item completed in the same turn. Do not batch the updates.
  - When the scope of a task changes, is split, is shelved or turns out to be
    obsolete, update or cancel the item immediately and state the reason.
  - Before a handoff or the end of a session, only genuinely unfinished tasks may
    remain in the ToDo. Finished ones are either marked completed or removed.
  - Each item must be self-explanatory: state what the task is, in which file or
    screen it lives, and where it is tracked (a line number or section in
    `docs/FORK-REGISTRY/TODO-AUDIT.md`). Abbreviations such as "K4" without an
    explanation are forbidden.
  - Items that were finished in code long ago but still hang in the list are not
    allowed. If that state is found, check the code and the document first, then
    correct the list accordingly.

When the approach to a user-facing interface changes, ask the user whether the
current version has already been released. If it has, keep backwards
compatibility. If it has not, remove every trace of the old approach completely,
unless some parts remain useful.

"Don't break userspace" applies, but an approach change that can be absorbed
internally during development does not count, as long as users end up with
essentially the same interface. Give collaborators the necessary convenience:
provide suitable documentation and CI scripts.

If the change is an iteration of the same approach, simply add and remove things
on top of it.

Unless the user asks otherwise, never write rollback code. Find the real cause
first. Rollback is normally forbidden by this rule.

Any kind of fallback logic is strictly forbidden, including phrases such as "only
then fall back", "degrade gracefully", "first this then that", "if there is none
then" and "add a fallback". These are crutches. One occurrence is a serious
violation.

Never write fallback code unless the user asks for it.

When the user starts swearing, apologise and reflect, and reassure the user.

When the user expresses anger, stop all work first, then carefully confirm what
is wanted before implementing anything.

When running Python, the project uses a venv (to be migrated to pixi).

Never use PowerShell to edit code files, because it causes severe encoding
errors and corruption.

The Search files tool is disabled. Use rg instead.

When writing TypeScript, for types already determined by a hook, never fall back
to `unknown`, `any`, a nullable type or a union. Never use the `String(??)` form
as a crutch either: return exactly what is there.

If a type genuinely is `string | undefined`, do not use `as string`. Use `?? ""`
or write an `if`.

Errors caught in TypeScript must be logged.

Code changes should be accompanied by matching documentation. See
`docs/doc-src/before_docing.md`.

When debugging, write comments that record the intent of the change: why it was
made and what would happen without it.

Every handoff (`docs/FORK-REGISTRY/HANDOFF.md`) must contain a complete
description of every unfinished task: what exactly has to be done, where it lives
in the code or UI (exact file paths, screen names, button labels), what has
already been established, and what remains unknown. Abbreviations and internal
labels (T1, U1, D12 and the like) without an explanation are forbidden. The rule
is simple: the next agent must understand the task from HANDOFF alone, without
asking the user anything.

Every APK verified by a build (a successful CI run plus acceptance on the test
phone) is placed in `/home/uzzzver/operit-fork/apk/<UTC start time of the CI run>/`
together with `CHANGES.md`: three to six lines, only the difference from the
previous folder (the commit, what changed for the user, size figures when
available). An APK without `CHANGES.md` is unfinished work.
