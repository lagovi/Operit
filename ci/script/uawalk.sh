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
#   MAYBE_TAP <text>      tap only when the element is on screen, else skip
#                         (for genuinely optional UI like an empty-chat
#                         "New Chat" button — NOT an error softener);
#   EXPECT [secs] <text>  screen must show <text> within secs (default 30)
#   REPLY_BASE            record the current number of assistant reply body
#                         rows (chat); use right before sending a message
#   EXPECT_REPLY [secs]   a NEW assistant reply body row must appear within
#                         secs (default 120) since the last REPLY_BASE. The
#                         model-down case (turn ends with only the "Operit"
#                         label and no body) is NOT fatal: it is recorded and
#                         printed in the run's notes/report at the end
#   NOTE_IF_PRESENT <text>
#                         if <text> is visible right now, record it in the
#                         notes/report (does not abort). Use for error
#                         surfaces that should be reported, not crash the run
#   WAIT_PKG [secs] <pkg-substring>
#                         foreground window must belong to <pkg-substring>
#                         (locale-independent screen tracking: the installer
#                         speaks Russian on this phone, the package does not)
#   TYPE <text>           adb `input text` (plain spaces auto-encoded
#                         to %s, as usual)
#   BACK                  KEYCODE_BACK
#   HIDEKB                BACK only when the soft keyboard is shown
#   SWIPE_UP              swipe 360,1200 -> 360,300
#   SWIPE_DOWN            swipe 360,300 -> 360,1200
#   SCROLL_TO [n] [UP|DOWN] <text>
#                         swipe until <text> is visible, max n swipes
#                         (default 5 DOWN = reveal content above; UP reveals
#                         content below); abort otherwise
#   SLEEP <secs>
#   DUMP                  save current dump+texts to the log trail
#
# Requires phone_lab.sh sourced first (uses plab_dump/plab_texts/plab_tap).
# Env: PHONE, PKG (from phone_lab.sh), UA_DUMP_DIR (default /tmp/uaw).

set -uo pipefail

UA_DUMP_DIR="${UA_DUMP_DIR:-/tmp/uaw}"
UA_STEP=0
UA_REPLY_BASE=
UA_NOTES=()

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

# Record a note for the end-of-run report. Notes are conditions that must
# be surfaced to the operator but must NOT abort the walk (e.g. the chat
# model being unavailable): the run finishes and the note is printed with
# the summary, so a green run with a note reads as "UI ok, model not usable"
# instead of a crash.
uaw_note() {
    UA_NOTES+=("$1")
    echo "  note($UA_STEP) $1"
}

# Print all notes collected in this run (called by uaw_run at the end).
uaw_notes() {
    local n
    if [ "${#UA_NOTES[@]}" -eq 0 ]; then
        echo "uawalk notes: none"
        return 0
    fi
    echo "uawalk notes (${#UA_NOTES[@]}):"
    for n in "${UA_NOTES[@]}"; do
        echo "  - $n"
    done
}

# Count assistant reply body rows on the chat screen. A rendered reply body
# is a non-clickable full-width View whose text is exposed via content-desc
# (N51): bounds start at the left gutter (x1<=60) and span the list width
# (x2>=500). Tool-call chips are the same width but clickable=true, so they
# are excluded; the empty-reply failure mode (model down) renders only the
# "Operit" label and no such row.
uaw_count_reply_rows() {
    plab_dump | python3 -c "
import sys, re
x = sys.stdin.read()
n = 0
for m in re.finditer(r'<node[^>]*>', x):
    s = m.group(0)
    d = re.search(r'content-desc=\"([^\"]*)\"', s)
    c = re.search(r'clickable=\"([^\"]*)\"', s)
    b = re.search(r'bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', s)
    if not (d and d.group(1).strip() and b):
        continue
    if c and c.group(1) == 'true':
        continue
    x1, y1, x2, y2 = (int(v) for v in b.groups())
    if x1 <= 60 and x2 >= 500:
        n += 1
print(n)
"
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
    local run_start step_start step_end step_ms
    run_start=$(date +%s)
    UA_STEP=0
    UA_NOTES=()
    # NOTE: the scenario is read on fd 3, NOT stdin: `adb shell` consumes
    # stdin and would otherwise swallow the remaining steps (lost a full
    # run to this: only step 1 executed, silently).
    while IFS= read -r line <&3 || [ -n "$line" ]; do
        line="${line%%#*}"
        [ -z "${line//[[:space:]]/}" ] && continue
        UA_STEP=$((UA_STEP+1))
        step_start=$(date +%s%N)
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
            MAYBE_TAP)
                [ -n "$rest" ] || uaw_abort "MAYBE_TAP needs an element"
                local mb mc
                # NOTE: uaw_find always exits 0 (empty output = absent),
                # so test the string, not $?.
                mb=$(uaw_find "$rest")
                if [[ "$mb" =~ ^[0-9]+[[:space:]][0-9]+[[:space:]][0-9]+[[:space:]][0-9]+$ ]]; then
                    mc=$(echo "$mb" | plab_center)
                    echo "  maybe_tap($UA_STEP) '$rest' @ $mc | $(uaw_where)"
                    plab_tap $mc
                    sleep 2
                else
                    echo "  maybe_tap($UA_STEP) '$rest' absent, skipped"
                fi
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
            NOTE_IF_PRESENT)
                [ -n "$rest" ] || uaw_abort "NOTE_IF_PRESENT needs text"
                if plab_texts 2>/dev/null | grep -qF "$rest"; then
                    uaw_note "unexpected text present: '$rest'"
                else
                    echo "  note_if_present($UA_STEP) '$rest' absent, ok"
                fi
                ;;
            REPLY_BASE)
                UA_REPLY_BASE=$(uaw_count_reply_rows)
                echo "  reply_base($UA_STEP) body rows=$UA_REPLY_BASE"
                ;;
            EXPECT_REPLY)
                local rsecs rfirst rnow
                rsecs=120
                rfirst="${rest%% *}"
                [[ "$rfirst" =~ ^[0-9]+$ ]] && rsecs="$rfirst"
                [ -n "$UA_REPLY_BASE" ] || uaw_abort "EXPECT_REPLY needs a preceding REPLY_BASE"
                local rdeadline
                rdeadline=$(( $(date +%s) + rsecs ))
                while :; do
                    rnow=$(uaw_count_reply_rows)
                    if [ "$rnow" -gt "$UA_REPLY_BASE" ]; then
                        echo "  expect_reply($UA_STEP) body rows $UA_REPLY_BASE -> $rnow"
                        break
                    fi
                    if [ "$(date +%s)" -ge "$rdeadline" ]; then
                        # NOT fatal (user directive 18.10): a blank reply means
                        # the model is unavailable, not that the UI is broken.
                        # Record it so the final report tells the operator.
                        uaw_note "model unavailable: no assistant reply body appeared within ${rsecs}s (blank reply)"
                        break
                    fi
                    sleep 3
                done
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
                # `input text` wants %s for spaces (%20 stays literal —
                # burned once 16.10); encode plain spaces automatically.
                adb -s "$PHONE" shell "input text '${rest// /%s}'"
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
                local stext smax sdir sfirst s i
                stext="$rest"; smax=5; sdir="DOWN"
                for _t in 1 2; do
                    sfirst="${stext%% *}"
                    if [[ "$sfirst" =~ ^[0-9]+$ ]]; then
                        smax="$sfirst"; stext="${stext#* }"
                    elif [[ "$sfirst" == "UP" || "$sfirst" == "DOWN" ]]; then
                        sdir="$sfirst"; stext="${stext#* }"
                    else
                        break
                    fi
                done
                [ -n "$stext" ] || uaw_abort "SCROLL_TO needs text"
                for ((s=0; s<smax; s++)); do
                    if plab_texts 2>/dev/null | grep -qF "$stext"; then
                        echo "  scroll_to($UA_STEP) '$stext' ok after $s swipes"
                        break
                    fi
                    if [ "$sdir" = "UP" ]; then
                        adb -s "$PHONE" shell "input swipe 360 1200 360 300 400" < /dev/null
                    else
                        adb -s "$PHONE" shell "input swipe 360 300 360 1200 400" < /dev/null
                    fi
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
            SAVEDUMP)
                [[ "$rest" =~ ^[A-Za-z0-9_-]+$ ]] \
                    || uaw_abort "SAVEDUMP needs an alphanumeric name"
                mkdir -p "$UA_DUMP_DIR"
                plab_dump > "$UA_DUMP_DIR/${rest}.xml" 2>/dev/null \
                    || uaw_abort "dump failed"
                echo "  savedump($UA_STEP) $rest.xml"
                sleep 1
                ;;
            *)
                uaw_abort "unknown command '$cmd'"
                ;;
        esac
        step_end=$(date +%s%N)
        step_ms=$(( (step_end - step_start) / 1000000 ))
        echo "  time($UA_STEP) ${step_ms}ms [$cmd]"
    done 3< "$scenario"
    echo "uawalk: $UA_STEP steps done, scenario $scenario, total $(( $(date +%s) - run_start ))s"
    uaw_notes
}
