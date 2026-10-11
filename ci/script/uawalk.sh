#!/usr/bin/env bash
# uawalk — generic UI walker for scripted acceptance.
#
# A scenario is a text file, one command per line (`#` comments and blank
# lines skipped). The walker taps through the listed elements and checks
# the screen against expectations; any mismatch aborts with the step
# number, the command, what was expected, and what the screen actually
# shows (plus a saved dump). No silent waiting, no blind taps.
#
# Commands:
#   TAP <text>            tap element by exact text (else content-desc);
#                         abort if the element is not on screen
#   TAP_AT <x> <y>        coordinate tap for icon-only elements (no text node);
#                         prefer TAP whenever text exists
#   EXPECT [secs] <text>  screen must show <text> within secs (default 30)
#   WAIT_PKG [secs] <pkg-substring>
#                         foreground window must belong to <pkg-substring>
#                         (locale-independent screen tracking: the installer
#                         speaks Russian on this phone, the package does not)
#   TYPE <text>           adb `input text` (%s for spaces, as usual)
#   BACK                  KEYCODE_BACK
#   HIDEKB                BACK only when the soft keyboard is shown
#   SWIPE_UP              swipe 360,1200 -> 360,300
#   SWIPE_DOWN            swipe 360,300 -> 360,1200
#   SCROLL_TO [n] <text>  swipe down (reveal content above) until <text> is
#                         visible, max n swipes (default 5); abort otherwise
#   SLEEP <secs>
#   DUMP                  save current dump+texts to the log trail
#
# Requires phone_lab.sh sourced first (uses plab_dump/plab_texts/plab_tap).
# Env: PHONE, PKG (from phone_lab.sh), UA_DUMP_DIR (default /tmp/uaw).

set -uo pipefail

UA_DUMP_DIR="${UA_DUMP_DIR:-/tmp/uaw}"
UA_STEP=0

# List every actionable element on screen: text|desc|bounds, one per line.
# Used to author scenarios (what exists), not during runs.
uaw_elements() {
    plab_dump | python3 -c "
import sys, re
x = sys.stdin.read()
for m in re.finditer(r'<node[^>]*>', x):
    n = m.group(0)
    if 'clickable=\"true\"' not in n and 'scrollable=\"true\"' not in n and 'checkable=\"true\"' not in n:
        continue
    t = re.search(r'text=\"([^\"]*)\"', n)
    d = re.search(r'content-desc=\"([^\"]*)\"', n)
    b = re.search(r'bounds=\"([^\"]*)\"', n)
    c = 'scrollable' if 'scrollable=\"true\"' in n else ('checkable' if 'checkable=\"true\"' in n else 'tap')
    print((t.group(1) if t else '') + '|' + (d.group(1) if d else '') + '|' + (b.group(1) if b else '') + '|' + c)
"
}

# Print "x1 y1 x2 y2" of the first node with exact text, else exact
# content-desc. Return 1 when the element is not on screen.
uaw_find() {
    local want="$1"
    plab_dump | python3 -c "
import sys, re
want = '''$1'''
x = sys.stdin.read()
for m in re.finditer(r'<node[^>]*>', x):
    n = m.group(0)
    t = re.search(r'text=\"([^\"]*)\"', n)
    d = re.search(r'content-desc=\"([^\"]*)\"', n)
    b = re.search(r'bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', n)
    if not b:
        continue
    if (t and t.group(1) == want) or (d and d.group(1) == want):
        print(b.group(1), b.group(2), b.group(3), b.group(4))
        break
"
}

# Focused window package + visible texts: the "where am I" snapshot used in
# every abort message.
uaw_where() {
    local pkg texts
    pkg=$(adb -s "$PHONE" shell "dumpsys window" 2>/dev/null | grep -m1 "mCurrentFocus=" | grep -o "u0 [a-zA-Z0-9._/]*" | head -1)
    texts=$(plab_texts 2>/dev/null | head -25 | tr '\n' '|')
    echo "package=[${pkg:-unknown}] screen=[${texts:-EMPTY}]"
}

# Wait until the foreground window belongs to $1 (substring), up to $2 secs.
uaw_wait_pkg() {
    local want="$1" timeout="${2:-30}" i
    for ((i=0; i<timeout; i+=2)); do
        if adb -s "$PHONE" shell "dumpsys window" 2>/dev/null | grep -m1 "mCurrentFocus=" | grep -qF "$want"; then
            return 0
        fi
        sleep 2
    done
    return 1
}

uaw_abort() {
    local dump="$UA_DUMP_DIR/dump_step${UA_STEP}.xml"
    mkdir -p "$UA_DUMP_DIR"
    plab_dump > "$dump" 2>/dev/null
    {
        echo "UAW ABORT step=$UA_STEP cmd=[$UA_CMD]"
        echo "  expected: $1"
        echo "  actual:   $(uaw_where)"
        echo "  dump:     $dump"
    } >&2
    exit 1
}

uaw_run() {
    local scenario="$1" line cmd arg rest
    [ -f "$scenario" ] || { echo "uaw_run: no scenario $scenario" >&2; exit 2; }
    # NOTE: the scenario is read on fd 3, NOT stdin: `adb shell` consumes
    # stdin and would otherwise swallow the remaining steps (lost a full
    # run to this: only step 1 executed, silently).
    while IFS= read -r line <&3 || [ -n "$line" ]; do
        line="${line%%#*}"
        [ -z "${line//[[:space:]]/}" ] && continue
        UA_STEP=$((UA_STEP+1))
        cmd="${line%% *}"
        rest="${line#* }"
        [ "$cmd" = "$line" ] && rest=""
        UA_CMD="$line"
        case "$cmd" in
            TAP)
                [ -n "$rest" ] || uaw_abort "TAP needs an element"
                local b c
                b=$(uaw_find "$rest") || uaw_abort "element '$rest' not on screen"
                [[ "$b" =~ ^[0-9]+[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]][0-9]+$ ]] \
                    || uaw_abort "element '$rest' has unparsable bounds '$b'"
                c=$(echo "$b" | plab_center)
                echo "  tap($UA_STEP) '$rest' @ $c | $(uaw_where)"
                plab_tap $c
                sleep 2
                ;;
            TAP_AT)
                [[ "$rest" =~ ^[0-9]+[[:space:]][0-9]+$ ]] || uaw_abort "TAP_AT needs 'x y'"
                echo "  tap_at($UA_STEP) $rest | $(uaw_where)"
                plab_tap $rest
                sleep 2
                ;;
            EXPECT)
                local want secs first
                want="$rest"; secs=30
                first="${rest%% *}"
                if [[ "$first" =~ ^[0-9]+$ ]]; then
                    secs="$first"; want="${rest#* }"
                fi
                [ -n "$want" ] || uaw_abort "EXPECT needs text"
                plab_wait_text "$want" "$secs" \
                    || uaw_abort "text '$want' not visible within ${secs}s"
                echo "  expect($UA_STEP) '$want' ok"
                ;;
            WAIT_PKG)
                local wpkg wsecs wfirst
                wpkg="$rest"; wsecs=30
                wfirst="${rest%% *}"
                if [[ "$wfirst" =~ ^[0-9]+$ ]]; then
                    wsecs="$wfirst"; wpkg="${rest#* }"
                fi
                [ -n "$wpkg" ] || uaw_abort "WAIT_PKG needs a package"
                uaw_wait_pkg "$wpkg" "$wsecs" \
                    || uaw_abort "package '$wpkg' not foreground within ${wsecs}s"
                echo "  wait_pkg($UA_STEP) '$wpkg' ok"
                ;;
            TYPE)
                [ -n "$rest" ] || uaw_abort "TYPE needs text"
                adb -s "$PHONE" shell "input text '$rest'"
                echo "  type($UA_STEP) ${#rest} chars"
                sleep 2
                ;;
            BACK)
                adb -s "$PHONE" shell "input keyevent KEYCODE_BACK"
                echo "  back($UA_STEP)"
                sleep 2
                ;;
            HIDEKB)
                if adb -s "$PHONE" shell "dumpsys input_method" 2>/dev/null | grep -q "mInputShown=true"; then
                    adb -s "$PHONE" shell "input keyevent KEYCODE_BACK"
                    echo "  hidekb($UA_STEP) keyboard was open"
                else
                    echo "  hidekb($UA_STEP) no keyboard"
                fi
                sleep 1
                ;;
            SWIPE_UP)
                adb -s "$PHONE" shell "input swipe 360 1200 360 300 400"
                echo "  swipe_up($UA_STEP)"
                sleep 2
                ;;
            SWIPE_DOWN)
                adb -s "$PHONE" shell "input swipe 360 300 360 1200 400"
                echo "  swipe_down($UA_STEP)"
                sleep 2
                ;;
            SCROLL_TO)
                local stext smax sfirst s i
                stext="$rest"; smax=5
                sfirst="${rest%% *}"
                if [[ "$sfirst" =~ ^[0-9]+$ ]]; then
                    smax="$sfirst"; stext="${rest#* }"
                fi
                [ -n "$stext" ] || uaw_abort "SCROLL_TO needs text"
                for ((s=0; s<smax; s++)); do
                    if plab_texts 2>/dev/null | grep -qF "$stext"; then
                        echo "  scroll_to($UA_STEP) '$stext' ok after $s swipes"
                        break
                    fi
                    adb -s "$PHONE" shell "input swipe 360 300 360 1200 400" < /dev/null
                    sleep 2
                done
                plab_texts 2>/dev/null | grep -qF "$stext" \
                    || uaw_abort "text '$stext' not visible after $smax swipes"
                ;;
            SLEEP)
                sleep "${rest:-2}"
                ;;
            DUMP)
                echo "  dump($UA_STEP) $(uaw_where)"
                ;;
            *)
                uaw_abort "unknown command '$cmd'"
                ;;
        esac
    done 3< "$scenario"
    echo "uawalk: $UA_STEP steps done, scenario $scenario"
}
