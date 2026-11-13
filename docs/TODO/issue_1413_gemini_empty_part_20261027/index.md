---
title: Issue 1413 Gemini empty Part fix
fork: https://github.com/yoruuuchan/Operit
status: complete
---

# Issue 1413 Gemini empty Part fix

## Current state

The Gemini native endpoint validates the request body against proto: `Part.data` is a oneof, and when the body is an empty string the oneof has no initialized member, so the whole request is rejected with HTTP 400 `required oneof field 'data' must have one initialized field`; when `Content.parts` is an empty array it returns `contents.parts must not be empty`.

The plain-text branch of `GeminiProvider.buildPartsArray()` unconditionally writes `{"text": text}`. A model message in the history can become empty after the thinking body is stripped, so `{"text": ""}` is written. With thinking enabled, the model more often outputs only thinking and no body, so one turn carrying this history makes the whole conversation fail.

Two more paths produce the same empty Part: when an image link is no longer in the image pool, the media branch yields empty parts; and when the system prompt is blank, `systemInstruction` also gets an empty text Part.

## Expected result

- An empty body produces no Part, and the request contains no Part with an uninitialized `data`
- A history message with no Part produces no Content entry, and the request contains no empty `parts`
- A request with only a system prompt and an all-empty body reports a readable error locally instead of sending empty `contents` into the server's 400
- Thinking metadata and the existing serialization of tool calls and tool results stay unchanged

## Official and community basis

A Gemini `Content` requires at least one `Part`, and `Part.data` must have one initialized member. The ecosystem has reached the same conclusion for the same issue: CLIProxyAPI skips empty text Parts and empty-parts messages ([issue #1293](https://github.com/router-for-me/CLIProxyAPI/issues/1293), [PR #1294](https://github.com/router-for-me/CLIProxyAPI/pull/1294)), agno skips a Content whose parts are empty ([issue #2649](https://github.com/agno-agi/agno/issues/2649)), and langchain-google throws on empty contents ([PR #1298](https://github.com/langchain-ai/langchain-google/pull/1298)).

- [Gemini REST Content reference](https://ai.google.dev/api/generate-content)

## Steps

1. [Part and Content serialization constraints](./1_gemini_part_and_content_serialization.md) [DONE]

## Out of scope

The empty `optionId` error of a thinking tier mentioned in the issue notes is a tier-alignment issue at config save time, already covered by PR #1391, and is not changed again here.
