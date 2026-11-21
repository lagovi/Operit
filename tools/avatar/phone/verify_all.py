"""Drive the avatar preview on the phone for every preset and measure the
figure height at rest and during a clip.

Run: python3 verify_all.py <outdir> [model:clip ...]
Default set: xbot:walk soldier:walk fox:Walk rose:happy

This is the durable copy of the throwaway driver used during development. It
restarts the app for every pair and skips the cloud download when the model
already sits in /sdcard/Android/data/<pkg>/files/avatars/<Name>. See HANDOFF.md
for the device quirks (DPAD navigation, the large-files chip).
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import drive  # noqa: E402
from PIL import Image  # noqa: E402
import numpy as np  # noqa: E402


def card_bbox(path):
    """Model extent inside the preview card, in pixels of the 720x1560 screen."""
    im = np.array(Image.open(path).convert("RGB"))[270:640, 30:690].astype(int)
    d = np.sqrt(((im - np.array([240, 235, 245])) ** 2).sum(2))
    ys, xs = np.nonzero(d > 40)
    if len(ys) == 0:
        return None
    return int(ys.max() - ys.min() + 1), int(xs.max() - xs.min() + 1)


def open_avatar():
    for _ in range(14):
        time.sleep(1)
        if drive.find("Enable virtual avatar"):
            return
        if drive.find("Avatar Configuration") or drive.find("Current Model"):
            drive.swipe(360, 900, 360, 1400, 400)
            time.sleep(0.8)
            continue
        if drive.find("AI Chat"):
            drive.tap(50, 108)
            time.sleep(2)
            if drive.find("Assistant Config"):
                drive.tap_text("Assistant Config")
                time.sleep(4)
            continue
        time.sleep(2)
    raise RuntimeError("avatar screen did not open")


def collapse_moods():
    for _ in range(3):
        pos = drive.find("Collapse")
        if not pos:
            return
        x, y = pos if isinstance(pos, tuple) else (610, 1350)
        drive.tap(x, y)
        time.sleep(1.5)


def scroll_to_cloud():
    collapse_moods()
    if not drive.find("Enable virtual avatar"):
        drive.back()
        time.sleep(1)
    for _ in range(10):
        if drive.find("Enable virtual avatar"):
            return
        drive.swipe(360, 900, 360, 1400, 400)
        time.sleep(0.7)
    raise RuntimeError("could not scroll the avatar card to the top")


def find_ci(name):
    low = name.lower()
    for t, a, b, c, d in drive.ui():
        if t.strip().lower() == low:
            return (a + c) // 2, (b + d) // 2
    return None


PKG = "com.ai.assistance.operit.debug"


def reset_app():
    drive.run(["shell", "am", "force-stop", PKG])
    drive.run(["shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1"])
    for _ in range(30):
        time.sleep(1)
        if drive.find("AI Chat"):
            time.sleep(1)
            return
    raise RuntimeError("app did not reach AI Chat")


def has_model(name):
    path = f"/sdcard/Android/data/{PKG}/files/avatars/{name}/model.glb"
    try:
        return "model.glb" in drive.run(["shell", "ls", path])
    except Exception:
        return False


def download(name, out):
    if has_model(name):
        return
    scroll_to_cloud()
    if find_ci(name):
        drive.tap(*find_ci(name))
        time.sleep(7)
        return
    drive.tap(551, 1040)
    time.sleep(7)
    if not drive.find("Import from cloud"):
        raise RuntimeError("cloud import did not open")
    for _ in range(8):
        pos = find_ci(name)
        if pos:
            drive.tap(*pos)
            time.sleep(8)
            return
        drive.swipe(360, 1100, 360, 600, 350)
        time.sleep(1)
    raise RuntimeError("cloud list: no " + name)


def select_model(name, out):
    scroll_to_cloud()
    drive.tap_text("Current Model")
    time.sleep(3)
    pos = find_ci(name)
    if not pos:
        for _ in range(4):
            drive.swipe(360, 1200, 360, 800, 350)
            time.sleep(1)
            pos = find_ci(name)
            if pos:
                break
    if not pos:
        raise RuntimeError("model list: no " + name)
    drive.tap(*pos)
    time.sleep(8)


def play(clip, out, tag):
    drive.scroll_to("Mood Trigger Mapping", down=True)
    time.sleep(1)
    if drive.find("Expand"):
        drive.tap_text("Expand")
        time.sleep(2)
    drive.scroll_to("Preview", down=True)
    time.sleep(1)
    pos = drive.find("Mapped Action")
    if not pos:
        raise RuntimeError("no Mapped Action row")
    drive.tap(600, pos[1])
    time.sleep(2)
    pos = drive.find(clip)
    if not pos:
        raise RuntimeError("no clip " + clip)
    drive.tap(*pos)
    time.sleep(2)
    drive.tap_text("Preview")
    time.sleep(0.3)
    frames = []
    for i in range(6):
        p = os.path.join(out, f"{tag}_f{i}.png")
        drive.shot(p)
        frames.append((p, card_bbox(p)))
        time.sleep(0.8)
    return frames


def main():
    out = sys.argv[1]
    pairs = sys.argv[2:] or ["xbot:walk", "soldier:walk", "fox:Walk", "rose:happy"]
    os.makedirs(out, exist_ok=True)
    for pair in pairs:
        model, clip = pair.split(":")
        print(f"=== {model}:{clip}", flush=True)
        reset_app()
        open_avatar()
        download(model, out)
        select_model(model, out)
        rest = os.path.join(out, f"{model}_rest.png")
        drive.shot(rest)
        print(f"  rest {card_bbox(rest)}", flush=True)
        for p, box in play(clip, out, model):
            print(f"  {os.path.basename(p)} {box}", flush=True)


if __name__ == "__main__":
    main()
