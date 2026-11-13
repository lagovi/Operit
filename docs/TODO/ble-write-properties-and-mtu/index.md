---
fork: https://github.com/mvanhorn/Operit
issue: https://github.com/AAswordman/Operit/issues/1431
status: implemented-awaiting-verification
---

# BLE write properties and complete transfers

## Previous behavior and scope

On Android 33 and newer writes always used `WRITE_TYPE_DEFAULT`, and older versions did not set the
characteristic's `writeType`, so a printer characteristic that only advertises `write_no_response`
could not use the correct write mode. Large payloads were submitted as one write, with no MTU
chunking and no protection against concurrent writes to the same session.

This change touches only `BluetoothSessionManager.kt`, the matching JVM regression tests, this record
and the [tool return type reference](../../doc-src/package-dev/tool-types.md). Standalone writes and
write-then-read keep their existing parameters and result structures, never request a larger MTU and
add no retries.

## Implementation steps

1. Write property correction [DONE]

   Both Android API branches use the write mode declared by the characteristic. When
   `write_no_response` is advertised, an unacknowledged write is used; when only `write` is
   advertised, the acknowledged write is kept; when neither is advertised, the write is rejected
   before submission.

2. MTU and write ordering [DONE]

   Each session starts with an MTU of 23, and only a successful MTU callback updates that state. An
   unacknowledged write uses at most MTU minus three bytes per chunk, each chunk has its own
   completion waiter, and the next chunk is sent 15 ms after a successful callback. A session mutex
   covers the whole transfer; a failed submission, a failed callback, a timeout or a cancellation
   stops the transfer, cleans up the waiter and releases the lock. An empty payload still performs
   one write, and acknowledged writes keep the single-submission behavior.

3. Regression and acceptance

   JVM regression tests use Mockito and the coroutine test dispatcher and set the private session and
   completion callbacks through reflection inside the test. They cover the mode and complete content
   of an 11-byte command, and the ordered 20/20/1 chunking of 41 bytes at the initial MTU, the
   per-chunk waiter, no submission before the callback, full reassembly and the final `bytesWritten`.

   They also cover the 244-byte ceiling after a successful MTU callback of 247, a failed MTU callback
   keeping the previous state, an invalid MTU rejecting submission, whole-chunk boundaries,
   single-byte and empty payloads, and acknowledged, dual-property and non-writable characteristics.
   Failure paths cover submission failure, status 3, timeout, no further chunks after cancellation
   while waiting for a callback or between chunks, waiter cleanup and mutex release. The same-session
   concurrency test checks that chunks do not interleave and waiters are not replaced; the
   different-session test uses the same service and characteristic UUIDs and checks that while one
   session waits for its callback another can finish all its chunks with independent content, waiters
   and cleanup.

   Static check: `git diff --check` passes. Both API branches were inspected to use the same write
   mode, the waiter is cleaned up in `finally`, and the mutex covers the chunking and the delay. The
   existing dependencies already include Mockito 5 and `kotlinx-coroutines-test`, so the test
   configuration was not changed. Compilation, build and tests have not been run yet. To run them
   once authorized:

   ```bash
   ./gradlew :app:testDebugUnitTest --tests com.ai.assistance.operit.core.tools.defaultTool.standard.BluetoothSessionManagerTest
   ```

   The JVM scenarios above have tests, but their results are still unverified.

   Device acceptance has not been performed: on Android 33 and newer, check the new overridden
   `writeType` and the chunk contents, and verify older Android versions if possible. With a
   controlled peripheral, confirm that AE01 receives the 11-byte self-test command and several KB of
   data completely, then verify standalone write and write-then-read through
   `StandardSystemOperationTools`. An Android write callback only means the Bluetooth stack finished
   the write; it does not prove the printer application has consumed the data.

## Collaboration notes

The upstream branch was `fix/1431-ble-write-properties-and-mtu`, with no commit or PR created at the
time of writing. The planned baseline was `main`, and follow-up PRs target `dev` per the repository
contribution guide, using the existing PR template.
