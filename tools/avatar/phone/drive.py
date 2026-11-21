import subprocess, re, sys, time, os

DEV = "192.168.1.69:5555"
ADB = ["adb", "-s", DEV]

def run(args, tries=8, timeout=180):
    last = ""
    for _ in range(tries):
        try:
            p = subprocess.run(ADB + args, capture_output=True, text=True, timeout=timeout)
        except subprocess.TimeoutExpired:
            subprocess.run(["adb", "connect", DEV], capture_output=True)
            time.sleep(4)
            last = "timeout"
            continue
        last = p.stdout + p.stderr
        if "device offline" in last or "device '" + DEV + "' not found" in last:
            subprocess.run(["adb", "connect", DEV], capture_output=True)
            time.sleep(4)
            continue
        if p.returncode == 0 or "adb:" not in last:
            return last
        time.sleep(2)
    raise RuntimeError(last.strip())

def ensure():
    for _ in range(8):
        out = subprocess.run(["adb", "devices"], capture_output=True, text=True).stdout
        if DEV + "\tdevice" in out:
            return True
        subprocess.run(["adb", "connect", DEV], capture_output=True)
        time.sleep(4)
    raise RuntimeError("no device")

def ui_xml():
    ensure()
    raw = run(["exec-out", "uiautomator", "dump", "/dev/tty"])
    i = raw.find("<hierarchy")
    return raw[i:] if i >= 0 else ""

def ui():
    x = ui_xml()
    items = []
    for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x):
        t, a, b, c, d = m.groups()
        items.append((t, int(a), int(b), int(c), int(d)))
    return items

def show():
    for t, a, b, c, d in ui():
        if t.strip():
            print(repr(t), (a, b, c, d))

def find(text, exact=True):
    for t, a, b, c, d in ui():
        if (t == text) if exact else (text in t):
            return (a + c) // 2, (b + d) // 2
    return None

def tap_text(text, exact=True):
    p = find(text, exact)
    if not p:
        raise RuntimeError("not found: " + text)
    tap(*p)
    return p

def tap(x, y):
    run(["shell", "input", "tap", str(x), str(y)])

def swipe(x1, y1, x2, y2, ms=400):
    run(["shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms)])

def back():
    run(["shell", "input", "keyevent", "4"])

def shot(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    for _ in range(6):
        try:
            p = subprocess.run(ADB + ["exec-out", "screencap", "-p"], capture_output=True, timeout=180)
            if p.returncode == 0 and len(p.stdout) > 5000:
                open(path, "wb").write(p.stdout)
                return
        except subprocess.TimeoutExpired:
            pass
        subprocess.run(["adb", "connect", DEV], capture_output=True)
        time.sleep(3)
    raise RuntimeError("shot failed " + path)

def series(prefix, n=8):
    os.makedirs(os.path.dirname(prefix) + "/" + os.path.basename(prefix), exist_ok=True)
    for i in range(1, n + 1):
        shot(f"{prefix}/f{i:02d}.png")

def scroll_top():
    for _ in range(6):
        swipe(360, 900, 360, 1450, 150)

def scroll_to(text, down=True, tries=12):
    for _ in range(tries):
        if find(text):
            return
        swipe(360, 1300, 360, 900, 400) if down else swipe(360, 900, 360, 1300, 400)
        time.sleep(1)
    raise RuntimeError("scroll: not found " + text)

def open_config():
    for attempt in range(4):
        if find("Current Model"):
            return
        if find("AI Chat"):
            if find("Assistant Config"):
                tap_text("Assistant Config")
                time.sleep(3)
                continue
            tap(50, 108)
            time.sleep(2)
            continue
        back()
        time.sleep(1)
    if not find("Current Model"):
        raise RuntimeError("cannot open config")
