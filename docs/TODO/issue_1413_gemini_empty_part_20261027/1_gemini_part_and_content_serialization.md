---
title: Gemini Part and Content serialization constraints
status: complete
---

# Gemini Part and Content serialization constraints

## Old implementation

The plain-text branch of `GeminiProvider.buildPartsArray()` unconditionally writes `{"text": text}`. When a history message body is empty this yields `{"text": ""}`, the `Part.data` oneof has no initialized member, and the endpoint returns 400.

Each turn branch of `buildContentsAndCountTokens()` writes parts straight into `contentsArray` without checking the length. An empty body thus becomes a Content with empty parts, hitting another server validation, `contents.parts must not be empty`.

Two further sources of the same empty Part:

- the media branch yields empty parts when the image link is no longer in the image pool, and it is still written into a Content
- `systemInstruction` unconditionally writes `{"text": systemContent}`, which is likewise an empty Part when the system prompt is blank

## New implementation

Parts layer: the plain-text branch writes `text` only when the body is non-blank, matching the media branch's "trim first, then decide" rule.

Content layer: added `putContent(role, parts)`, which drops the whole Content from the request when parts is empty and logs a debug line. Every branch that emits a Content goes through this entry, including `emitQueuedFunctionCallsIfNeeded()` of tool calls, `flushOpenFunctionCallsAsUnmatched()`, ASSISTANT, TOOL_CALL, USER, SUMMARY, TOOL_RESULT, and the fallback branch when closing tool calls.

System prompt layer: blank system messages are removed before joining, and no `systemInstruction` is produced when all are blank.

Request layer: `createRequestBody()` checks whether any usable Content remains in the history before writing `contents`. When none remains it throws `EmptyContentsException` instead of sending a request that is certain to be rejected. Like `PromptBlockedException`, this exception is rethrown directly in `handleRetryableError()` and does not participate in retries, because retrying the same history only yields the same result.

## What stays unchanged

- `thoughtSignature` is still attached to the functionCall Part, carried by `queueFunctionCalls()` and `buildGeminiFunctionCallPart()`
- callers with `preserveThinkInHistory = true` still send the raw ` Thinking` text as a text Part
- functionCall and functionResponse pairing is still guaranteed by the existing open tool call queue; only messages with no Part to send are dropped
- token counting goes through the separate `sanitizedHistoryForTokenCount` and is unaffected by the parts filtering

## Verification

- Added `GeminiContentsSerializationTest`, which walks every Part with invariant assertions: it must carry a `data` member, `text` must not be an empty string, and `parts` must not be an empty array
- Covers a thinking-only reply, plain text, thinking preserved, tool calls and results, a stale image link, a blank system prompt, and the local error for empty contents
- Red/green check: after reverting only the two guards (parts and content) and rerunning the same test class, 6 of the 13 cases fail, with `contents[1].parts[0] has empty text`, an empty-parts content count of `expected:<0> but was:<1>`, and empty contents not being intercepted, matching the three shapes reported in the issue; restoring the guards makes all 13 pass
- Full `:app:testDebugUnitTest`: 1356 passed, 0 failed, 6 skipped
- Gates `check_repo_hygiene.py`, `check_markdown_links.py`, `check_localizations.py`: errors=0 warnings=0

Local unit test runs excluded `:app:syncSttModelAssets` and `:app:syncMainAssets`, because the STT model assets fail download verification on this machine and are unrelated to the serialization change; the native library and the FFmpegKit AAR reuse existing local artifacts. The CI `Android JVM tests` job builds these two prerequisites itself.

[DONE]
