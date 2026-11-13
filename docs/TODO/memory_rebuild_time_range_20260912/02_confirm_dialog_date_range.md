# Confirm dialog date selection

Old implementation:

- The confirm memory library build dialog only offered the window size
- Starting the build processed every original message of the selected conversations

Intended correction:

- Offer two explicit options in the confirm dialog: all messages and custom time range
- For the custom range, collapse the confirm dialog first, then open a standalone calendar dialog to pick start and end dates on a month grid
- After the calendar dialog is confirmed, return to the confirm dialog showing the chosen dates; cancel returns to the confirm dialog without changing the dates
- Disable start build while a complete start/end date is not chosen
- When the selected range has no extractable user messages, the failure text says the range is empty instead of a generic no-messages

Expected result:

- The user can summarize only a time range of memory
- All messages remain the current selection when the dialog opens, behaving as before
- Progress, cancel and window failure keep their original semantics

[DONE]
