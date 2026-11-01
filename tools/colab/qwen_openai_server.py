#!/usr/bin/env python3
"""OpenAI-compatible images endpoint in front of ComfyUI running Qwen-Image 2.1 Turbo INT8.

Run this on a Colab VM after setup.sh has prepared ComfyUI and the weights.
It starts ComfyUI on a private port, waits for it, then serves
POST /v1/images/generations on 0.0.0.0:8000 in the shape the Operit avatar
generator expects: {"prompt", "n", "size", "model", "response_format"} in, and
{"data": [{"b64_json": ...}]} or {"data": [{"url": ...}]} out.

Port 8080 belongs to the Colab frontend proxy, so the public port defaults to
8000 and can be changed with PUBLIC_PORT.

Why a separate process instead of a ComfyUI custom node: the app speaks the
OpenAI images API, ComfyUI speaks its own graph protocol, and keeping the
translation in one plain HTTP file means it can be read, replaced or run
elsewhere without touching ComfyUI's plugin system.
"""

import base64
import json
import os
import subprocess
import sys
import threading
import time
import urllib.parse
import urllib.request
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

COMFY_DIR = "/content/ComfyUI"
COMFY_PORT = 8188
PUBLIC_PORT = int(os.environ.get("PUBLIC_PORT", "8000"))

UNET = "diffusion_models/qwen_image_2.1_turbo_int8_convrot.safetensors"
CLIP = "text_encoders/qwen3vl_8b_int8_convrot.safetensors"
VAE = "vae/qwen_image_2.1_vae_bf16.safetensors"

STEPS = int(os.environ.get("QWEN_STEPS", "8"))
CFG = float(os.environ.get("QWEN_CFG", "1.0"))
SHIFT = float(os.environ.get("QWEN_SHIFT", "3.0"))

_queue_lock = threading.Lock()
_comfy = None


def comfy_url(path):
    return f"http://127.0.0.1:{COMFY_PORT}{path}"


def comfy_json(path, payload=None, timeout=60):
    data = None if payload is None else json.dumps(payload).encode()
    request = urllib.request.Request(
        comfy_url(path), data=data, headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.load(response)


def wait_for_comfy(timeout=300):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(comfy_url("/system_stats"), timeout=3) as r:
                if r.status == 200:
                    return True
        except Exception:
            pass
        time.sleep(1)
    return False


def start_comfy():
    global _comfy
    if wait_for_comfy(timeout=2):
        print("ComfyUI already answering", flush=True)
        return
    os.environ["PYTORCH_CUDA_ALLOC_CONF"] = "expandable_segments:True"
    log = open("/content/comfy.log", "w")
    _comfy = subprocess.Popen(
        [
            sys.executable, "main.py",
            "--listen", "127.0.0.1",
            "--port", str(COMFY_PORT),
            "--disable-pinned-memory",
            "--mmap-torch-files",
            "--fp16-vae",
            "--preview-method", "none",
        ],
        cwd=COMFY_DIR,
        stdout=log,
        stderr=subprocess.STDOUT,
    )
    if not wait_for_comfy():
        raise RuntimeError("ComfyUI did not start; see /content/comfy.log")


def build_workflow(prompt, width, height, seed):
    """The TextEncodeQwenImage21 node carries both prompts and the reference
    resolution, so a separate negative encoder would be redundant. KSampler
    reads positive from output 0 and negative from output 1 of that node."""
    return {
        "1": {"class_type": "UNETLoader",
              "inputs": {"unet_name": UNET, "weight_dtype": "default"}},
        "2": {"class_type": "CLIPLoader",
              "inputs": {"clip_name": CLIP, "type": "qwen_image"}},
        "3": {"class_type": "VAELoader", "inputs": {"vae_name": VAE}},
        "4": {"class_type": "ModelSamplingAuraFlow",
              "inputs": {"model": ["1", 0], "shift": SHIFT}},
        "5": {"class_type": "TextEncodeQwenImage21",
              "inputs": {"clip": ["2", 0], "prompt": prompt,
                         "negative_prompt": "", "resolution": max(width, height)}},
        "7": {"class_type": "EmptySD3LatentImage",
              "inputs": {"width": width, "height": height, "batch_size": 1}},
        "8": {"class_type": "KSampler",
              "inputs": {"model": ["4", 0], "positive": ["5", 0], "negative": ["5", 1],
                         "latent_image": ["7", 0], "seed": seed, "steps": STEPS,
                         "cfg": CFG, "sampler_name": "euler", "scheduler": "simple",
                         "denoise": 1.0}},
        "9": {"class_type": "VAEDecode", "inputs": {"samples": ["8", 0], "vae": ["3", 0]}},
        "10": {"class_type": "SaveImage",
               "inputs": {"images": ["9", 0], "filename_prefix": "api"}},
    }


def generate(prompt, width, height, seed):
    workflow = build_workflow(prompt, width, height, seed)
    with _queue_lock:
        queued = comfy_json("/prompt", {"prompt": workflow, "client_id": str(uuid.uuid4())})
        prompt_id = queued["prompt_id"]
        deadline = time.time() + 600
        while time.time() < deadline:
            history = comfy_json(f"/history/{prompt_id}")
            if prompt_id in history:
                entry = history[prompt_id]
                if entry.get("status", {}).get("status_str") != "success":
                    raise RuntimeError(str(entry.get("status"))[:400])
                outputs = entry.get("outputs", {})
                for node in outputs.values():
                    for image in node.get("images", []):
                        query = urllib.parse.urlencode(
                            {"filename": image["filename"],
                             "subfolder": image.get("subfolder", ""),
                             "type": image.get("type", "output")})
                        with urllib.request.urlopen(
                                comfy_url(f"/view?{query}"), timeout=60) as r:
                            return r.read()
                raise RuntimeError("workflow finished without an image")
            time.sleep(1)
    raise RuntimeError("generation timed out")


def parse_size(size, model_width):
    if not size:
        return model_width, model_width
    parts = str(size).lower().replace(" ", "").split("x")
    if len(parts) != 2:
        raise ValueError(f"size must look like 1024x1024, got {size!r}")
    width, height = int(parts[0]), int(parts[1])
    if width <= 0 or height <= 0 or width % 32 or height % 32:
        raise ValueError("size must be positive multiples of 32")
    return width, height


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        sys.stderr.write("[%s] %s\n" % (time.strftime("%H:%M:%S"), fmt % args))

    def _send(self, code, payload, content_type="application/json"):
        body = payload if isinstance(payload, bytes) else json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path.startswith("/v1/images/content/"):
            name = self.path.rsplit("/", 1)[-1]
            path = os.path.join(COMFY_DIR, "output", os.path.basename(name))
            if not os.path.isfile(path):
                self._send(404, {"error": {"message": "no such image"}})
                return
            with open(path, "rb") as fh:
                self._send(200, fh.read(), "image/png")
        elif self.path in ("/", "/health", "/healthz"):
            self._send(200, {"status": "ok", "backend": "ComfyUI", "model": UNET})
        elif self.path == "/v1/models":
            self._send(200, {"object": "list", "data": [
                {"id": "qwen-image-2.1-turbo", "object": "model"}]})
        else:
            self._send(404, {"error": {"message": f"unknown path {self.path}"}})

    def do_POST(self):
        if self.path.rstrip("/") not in ("/v1/images/generations", "/images/generations"):
            self._send(404, {"error": {"message": f"unknown path {self.path}"}})
            return
        length = int(self.headers.get("Content-Length") or 0)
        try:
            payload = json.loads(self.rfile.read(length) or b"{}")
        except json.JSONDecodeError as exc:
            self._send(400, {"error": {"message": f"invalid JSON: {exc}"}})
            return
        prompt = (payload.get("prompt") or "").strip()
        if not prompt:
            self._send(400, {"error": {"message": "prompt is required"}})
            return
        width, height = parse_size(payload.get("size"), 1024)
        count = int(payload.get("n") or 1)
        if count < 1 or count > 4:
            self._send(400, {"error": {"message": "n must be between 1 and 4"}})
            return
        seed = payload.get("seed")
        seed = int(seed) if seed is not None else int(time.time() * 1000) % (2**31)
        fmt = payload.get("response_format") or "b64_json"
        started = time.time()
        try:
            images = [generate(prompt, width, height, seed + i) for i in range(count)]
        except Exception as exc:
            print("generation failed:", repr(exc), flush=True)
            self._send(500, {"error": {"message": str(exc)[:500]}})
            return
        elapsed = round(time.time() - started, 1)
        print(f"generated {count} image(s) {width}x{height} in {elapsed}s", flush=True)
        if fmt == "b64_json":
            data = [{"b64_json": base64.b64encode(raw).decode()} for raw in images]
        elif fmt == "url":
            data = []
            for raw in images:
                name = f"api_{uuid.uuid4().hex}.png"
                path = os.path.join(COMFY_DIR, "output", name)
                with open(path, "wb") as fh:
                    fh.write(raw)
                data.append({"url": f"/v1/images/content/{name}"})
        else:
            self._send(400, {"error": {"message": f"unsupported response_format {fmt}"}})
            return
        self._send(200, {"created": int(started), "data": data, "elapsed_seconds": elapsed})

def main():
    start_comfy()
    server = ThreadingHTTPServer(("0.0.0.0", PUBLIC_PORT), Handler)
    print(f"listening on 0.0.0.0:{PUBLIC_PORT}, backend ComfyUI :{COMFY_PORT}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
