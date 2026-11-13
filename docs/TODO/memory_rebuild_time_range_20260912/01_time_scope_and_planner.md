# Time range and window planning

Old implementation:

- `ChatMemoryWindowPlanner.plan` received a whole conversation's messages and split them by window size
- `ChatMemoryRebuildManager.start` received only the chat ids and the window size

Intended correction:

- Express the build scope with a sealed type: all messages, or a closed date range in the device zone
- Parse the milliseconds from the DatePicker as a UTC calendar date, then convert to a half-open interval from local-day 0:00 to the day after the end date 0:00
- The planner filters messages by the range first, then keeps the original windowing rules
- User or assistant messages outside the range do not enter a window or get added back as cross-window context

Expected result:

- The same-day selection is valid, and messages on the end day are all included
- A western time zone does not parse UTC midnight as the previous day
- Existing summary exclusion, window boundaries and group-chat slicing are unaffected by the time range

[DONE]
