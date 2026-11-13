---
fork_repository: https://github.com/yoruuuchan/Operit.git
branch: fix/issue-1307-a2a-streaming-final
issue: https://github.com/AAswordman/Operit/issues/1307
---

# Remove the schema-external final member from A2A streaming events

## Previous state

`A2aHttpHandler.statusUpdateEvent()` wrote a `final` boolean into `TaskStatusUpdateEvent` to tell streaming clients "this is the last frame", and the SSE loop also exited on it. A2A 1.0's `lf.a2a.v1.TaskStatusUpdateEvent` only has `taskId`, `contextId`, `status` and `metadata`. The official `a2a-sdk` parses strictly with `json_format.ParseDict` and throws `ParseError` on reading `final`, so `SendStreamingMessage` and `SubscribeToTask` were completely unusable on official SDK clients. The non-streaming path does not go through this function, so `SendMessage`, `GetTask` and `ListTasks` were always fine; the defect only surfaced in streaming.

`docs/doc-src/feature-protocol/external_a2a_server.md` at the time documented this behavior as part of the contract; the implementation and the document were consistent with each other but both deviated from the specification.

## Intent

The terminal state is expressed by `status.state`, and the stream closes after the terminal status frame; the member set of a frame is strictly equal to the A2A 1.0 definition, so an official SDK can parse frame by frame until the stream ends.

Non-streaming responses are unchanged: the `Task` member set, the conditions that produce `artifacts`, and `status.message` in a failed status are untouched.

## Scope

- `integrations/a2a/A2aJsonSerialization.kt`: new protocol serialization unit concentrating `Task`, `TaskStatusUpdateEvent`, `TaskArtifactUpdateEvent` and the JSON-RPC envelope, and reporting "does this frame end the stream" when a frame is generated.
- `integrations/a2a/A2aHttpHandler.kt`: the streaming loop exits on the frame's end marker; serialization calls move to the new unit, and `statusUpdateEvent()`, `taskToJson()` and the duplicated envelope construction are removed.
- `integrations/a2a/A2aTaskManager.kt`: `A2aTaskEvent.Status` drops the `final` field, so the event itself only describes a state change.
- `docs/doc-src/feature-protocol/external_a2a_server.md`: the event examples and end condition now decide by `status.state`.
- `app/src/test/.../integrations/a2a/A2aJsonSerializationTest.kt`: regression test.

## Verification

- The unit test `A2aJsonSerializationTest` asserts that the key sets of `statusUpdate`, `artifactUpdate` and `Task` equal the 1.0 definition, recursively asserts that no frame contains `final`, and asserts that four terminal states end the stream while four non-terminal states do not. The full `:app:testDebugUnitTest` passes.
- Handing the fixed serialized output to the official `a2a-sdk` 1.1.5: the field set of `lf.a2a.v1.TaskStatusUpdateEvent` is confirmed via `DESCRIPTOR.fields` to be `task_id / context_id / status / metadata`; 20 samples covering the eight task states, failed status messages, artifact chunks and the non-streaming `Task` all pass `json_format.ParseDict`, and `WhichOneof("payload")` lands on the correct member each time.
- The same check on the old frame (with `final: true`) reproduces the original error from the issue, `ParseError: Message type "lf.a2a.v1.TaskStatusUpdateEvent" has no field named "final"`, confirming that the removed failure point is exactly this one.

[DONE]
