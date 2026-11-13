---
fork: https://github.com/luojiaping/Operit
status: device-smoke-passed
related_pr: https://github.com/AAswordman/Operit/pull/1420
---

# Browser click hits and navigation outcomes

[Back to the TODO notes](../README.md)

## Prior state and intent

The upstream PR's ref-click fix moved the WebView size reads back onto the main thread.
Later device feedback also found duplicate selector activations, touch points that fell
outside the viewport after scrolling, links whose click did not navigate, and a displayed
address that changed before the document commit.

A static check confirmed that the selector path dispatched an event and then also called
`target.click()`, so one call could activate the target several times; a plain single
click on a ref and on a selector used different paths. Requesting WebView focus after the
coordinates were computed could also scroll an already-focused element again, invalidating
the earlier target geometry.

`shouldOverrideUrlLoading` already returns false for ordinary HTTP/HTTPS. This change fixes
target preparation, touch dispatch and outcome observation, keeping the existing tool names,
arguments and result shapes.

## Final behaviour

1. Refs and selectors share one target-resolution entry point, and a plain left click uses
   the same native touch path. Special mouse gestures share one JS dispatch implementation;
   double clicks and modifier clicks produce only the corresponding number of activations.
2. Request WebView focus first, then scroll instantly and wait for stable target geometry.
   The tap point must be inside the visible viewport and pass the hit test; CSS viewport
   coordinates are converted once, document scrollY is not added on top, and an out-of-range
   point is not clamped to the edge.
3. Native events use the TOUCHSCREEN source and the FINGER tool type, send DOWN and UP
   separately with real event times, and require both to be accepted. A dispatch failure is
   reported as an error directly.
4. A native JavaScript bridge records the real DOM click target, count, trusted state,
   coordinates and cancellation state. The observer is managed and cleaned up by the
   top-level document so timers from a removed iframe do not keep holding the target.
5. An HTTP/HTTPS link whose default behaviour is not cancelled and whose target URL differs
   must observe navigation; no navigation returns `clicked_but_not_navigated`, a main-frame
   load failure returns `navigation_failed`, and a timeout waiting for the document to be
   ready reports a failure explicitly. Buttons, downloads, dialogs and page-driven
   cancellation are judged by their actual outcomes.
6. `pendingNavigationUrl` is kept separate from the committed `currentUrl`. The displayed
   address updates on document commit or finish, keeping same-page history updates; the
   complete state of the previous document cannot satisfy the readiness check of a new
   navigation.

The iframe hit test includes border and scale mapping, and observes navigation in the target
browsing context. After a cross-origin crossing, the frame load event is used as lifecycle
evidence; no claim is made about reading the restricted document URL.

## Re-verification steps

- Click the same counter button once through a selector and once through a fresh snapshot
  ref: each plain single click adds 1, a double click adds 2, and a modifier click no longer
  dispatches an extra unmodified click.
- Click an element outside the viewport and check the final touch point against the DOM event
  coordinates. A 720-tall viewport should reject an 818 coordinate, and an overlay covering
  the target should report `element_obscured`.
- Start each of Learn more, About and Contact Us from a fresh page and snapshot, and check the
  click acknowledgement, the displayed address and `location.href`.
- Check preventDefault, an ordinary button, download and alert/confirm/prompt, plus hash,
  pushState, same-origin iframe and cross-origin iframe outcomes.

The temporary probe below keeps the page's smooth-scroll setting to check whether the tool
performs an instant scroll and hits accurately. Re-navigating clears the probe.

```javascript
() => {
    document.documentElement.style.scrollBehavior = "smooth";
    const button = document.createElement("button");
    button.id = "operit-offscreen-probe";
    button.textContent = "Offscreen click probe";
    button.style.position = "absolute";
    button.style.top = "1600px";
    button.style.left = "40px";
    const events = [];
    button.addEventListener("click", event => {
        const rect = button.getBoundingClientRect();
        events.push({
            trusted: event.isTrusted,
            x: event.clientX,
            y: event.clientY,
            centerX: (rect.left + rect.right) / 2,
            centerY: (rect.top + rect.bottom) / 2,
            viewportHeight: window.visualViewport.height
        });
    });
    document.body.appendChild(button);
    window.__operitOffscreenClicks = events;
    return { selector: "#operit-offscreen-probe" };
}
```

## Reading the logs

The log tag is `BrowserNavigation`, recording the session, WebView instance, navigation
sequence and click id. The JSON top-level `lastClickId` is the most recently initiated click;
the `clickId` in a DOM record is the id the observer bound to. Combine `clickAgeMs` with the
lifecycle records to correlate the action.

- `native-tap`: touch dispatch record; `dom-click-capture` and `dom-click-final`: the actual
  hit and the final cancellation state
- `matchesTarget=false`: the element receiving the event differs from the expected target
- `defaultPrevented=true`: the page cancelled the default behaviour; page teardown may happen
  before the delayed record
- `page-started`, `page-commit-visible`, `page-finished`: request start, document commit and
  finish
- `main-frame-error`, `main-frame-http-error`: main-frame load errors
- `popup-created`, `frame-load`: lifecycle evidence for a new browsing context or an iframe

## Verification record

2026-11-04, user report: "Everything else passed apart from waifu and external http, which
were not tested." The browser fix is recorded as passing based on the device scenarios the
user executed; the re-verification steps above are for reproducing similar problems and do
not widen the tested scope.

`NativeTapCoordinatesTest` contains 10 coordinate regression cases covering out-of-viewport
points, non-integer density, invalid dimensions and non-finite coordinates. The main app and
all JVM unit-test sources compiled in the local integration build; the previous round of
filtered commands did not run this test class. This commit clean-up did not rebuild or rerun
the tests.

[DONE]
