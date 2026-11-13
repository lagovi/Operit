---
fork_repository: https://github.com/AAswordman/Operit.git
---

# Build chat memory over a selected time range

## Previous state

The batch chat operation in settings could only pick whole conversations and then choose a window size before rebuilding memory. Once a long conversation was selected, every original user and assistant message in it was sent to the memory model, with no way to summarize only a certain span of days.

## Intent

When confirming a memory library build, let the user explicitly choose one of two scopes: all messages, or a closed date range. With a time range chosen, only messages within that range are handed to the existing window planner; window size, summary exclusion, ordered rebuild and progress-cancel semantics stay unchanged.

## Scope

- Add a unit-testable memory time range type that converts the date picker's UTC-midnight milliseconds into a half-open interval in the device zone
- Filter messages by that range in the window planner before slicing windows
- The confirm dialog offers both all-messages and custom-time-range entries, opening a date range picker for the custom case
- Provide default-language resources for the new strings and for the existing memory rebuild strings

## Acceptance

- The user can choose all messages, behaving as before
- The user can choose start and end dates and only messages within that closed range are processed
- Start and end on the same day is valid; messages on the end day are all included
- With the custom range selected but no start/end dates yet chosen, the build cannot start
- When the selected range has no extractable user messages, the task fails with the matching explanation
- Cancel, continue-on-window-failure, and memory space binding rules are unaffected by the time range

Steps:

- [01 Time range and window planning](01_time_scope_and_planner.md)
- [02 Confirm dialog date selection](02_confirm_dialog_date_range.md)
