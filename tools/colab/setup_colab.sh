#!/usr/bin/env bash
# Prepare a Colab VM for Qwen-Image 2.1 Turbo INT8 and start the OpenAI-compatible
# images endpoint in front of ComfyUI.
#
# Usage, from the fork root, after `colab new -s <name> --gpu T4`:
#   colab exec -s <name> -f tools/colab/qwen_openai_server.py
# is not enough on its own, because the server needs ComfyUI and the weights.
# Run this instead:
#   colab exec -s <name> -f tools/colab/setup_colab.sh
#
# Stop the session when done: colab stop -s <name>
set -euo pipefail

COMFY_DIR=/content/ComfyUI
REPO=Comfy-Org/Qwen-Image-2.1

python -m pip install -q --no-warn-conflicts comfy-kitchen huggingface_hub

if [ ! -d "$COMFY_DIR" ]; then
  git clone -q --depth 1 https://github.com/comfyanonymous/ComfyUI.git "$COMFY_DIR"
  python -m pip install -q --no-warn-conflicts -r "$COMFY_DIR/requirements.txt"
fi

mkdir -p "$COMFY_DIR/models/diffusion_models" \
         "$COMFY_DIR/models/text_encoders" \
         "$COMFY_DIR/models/vae"

python - <<'PY'
import os
from huggingface_hub import hf_hub_download
jobs = [
    ("diffusion_models/qwen_image_2.1_turbo_int8_convrot.safetensors", "diffusion_models"),
    ("text_encoders/qwen3vl_8b_int8_convrot.safetensors", "text_encoders"),
    ("vae/qwen_image_2.1_vae_bf16.safetensors", "vae"),
]
for filename, target in jobs:
    destination = os.path.join("/content/ComfyUI/models", target)
    if os.path.exists(os.path.join(destination, os.path.basename(filename))):
        print("present", filename)
        continue
    path = hf_hub_download(repo_id="Comfy-Org/Qwen-Image-2.1", filename=filename, local_dir=destination)
    print("downloaded", filename, round(os.path.getsize(path) / 1e9, 2), "GB")
PY

echo "weights ready; start the server from inside the VM:"
echo "  python /content/qwen_openai_server.py &"
