# Google Colab and Qwen-Image 2.1

How this fork uses a rented Colab VM for inference that does not fit on a
phone, and which model build is chosen for that job.

## Operating rule

A Colab session costs resources for as long as it exists. Close it with
`colab stop -s <name>` immediately after the work is done. Do not leave an
IDLE session between tasks and do not run two sessions at once. This rule also
lives in `AGENTS.md` so it applies to every agent session, not only to whoever
reads this file.

## Why Colab

The phone cannot run a multi-billion-parameter image model, and the
development host cannot either: it has 7.8 GB RAM, no Android SDK and a nearly
full disk. A Colab VM gives 2 vCPU, about 12.4 GB RAM and 120 GB of disk for
CPU work, or a Tesla T4 with 15.6 GB VRAM for generation. The app never talks
to Colab directly; it talks to an HTTP endpoint that the VM exposes.

## Authentication

The CLI (`google-colab-cli`, installed with `pip install --user
--break-system-packages google-colab-cli`) uses OAuth2 with the public client
that ships inside the package. Version 0.7.5 stores the token as a plain file
at `~/.config/colab-cli/token.json`, not in a keyring, which is why the token
survives moving to another machine while its refresh token stays valid.

The copy-paste flow is the only one that works without a browser on the same
machine. `colab_cli/auth.py` runs it with the redirect
`https://sdk.cloud.google.com/applicationdefaultauthcode.html`, prints a URL,
and reads the code from stdin. Because `colab` itself calls `input()`, an agent
cannot drive it; the helper script `/tmp/opencode/colab_auth.py` does the same
exchange with a URL file and a code file instead, which removal does not
depend on a terminal.

The account used for the fork is `inno0772@gmail.com`. Its compute balance is
0.00 units, which is enough because CPU sessions are free and the GPU quota is
separate.

## Session lifecycle

    colab new -s <name>            # CPU
    colab new -s <name> --gpu T4   # T4, 15.6 GB VRAM
    colab status -s <name>         # hardware, shape, IDLE or BUSY
    colab sessions                 # server-side list
    colab stop -s <name>           # always at the end

Always pass `-s`. Without it the CLI invents a random name. A session can be
revoked by the backend without warning; when `exec` fails with "Session not
found", run `colab sessions` and create a new one.

`~/.config/colab-cli/current` is a convenience file for the SSH wrapper and is
not used by the CLI itself.

## Executing work

    colab exec -s <name> -f local_script.py    # file is read locally and sent
    colab exec -s <name> --timeout 1800 -f x.py

`exec` runs inside the same long-lived kernel each time, so imports and
variables persist between calls. It does not read stdin, so interactive code
does not work. `colab run script.py` provisions a VM, runs once and tears it
down, which is convenient only for one-shot jobs.

Facts measured on this account:

- `colab new` for CPU finishes in about 7 seconds.
- The CPU VM reports Python 3.13.18, torch 2.11.0+cpu, transformers 5.21.0,
  diffusers 0.42.1, sentencepiece 0.2.2, 2 cores, 12.4 GiB RAM, 93 GB free.
- The T4 session reports torch 2.11.0+cu130, SM 7.5, 15.6 GB VRAM, 120 GB free.
- `torch.cuda.is_bf16_supported()` returns True on this T4 even though Turing
  has no native bfloat16, so the flag does not mean what it appears to mean.

## Files

    colab upload -s <name> local remote
    colab download -s <name> remote local
    colab ls -s <name> [path]

`/content` disappears when the session stops. Download artifacts before
`colab stop`. `exec` starts in `/content`; use absolute paths.

## SSH

`colab ssh -s <name> --proxy-mode` works as an OpenSSH ProxyCommand, and
`scp` can go through it. It is a convenience, not a requirement: the API path
above needs no tunnel. If SSH reports `429 another colab ssh is connected`,
an old connection is held on the runtime side and cannot be fixed locally;
fall back to `exec`, `upload` and `download`.

## Qwen-Image 2.1

Current open model for image generation and editing from the Qwen team:
Qwen-Image-2.1, released 20 September 2026, a 7B visual generator with a
Qwen3-VL 8B text encoder, up to 10 reference images for editing, native RGBA,
and up to 2K output. Qwen-Image-2.1-Turbo, released 9 October 2026, is the same
architecture in 8 steps instead of about 40. Licence is Qwen Research, which is
non-commercial. Qwen-Image-3.0 is API-only.

Colab already ships `diffusers` 0.42.1 with `QwenImage21Pipeline`,
`QwenImage21TurboAutoBlocks` and the related classes, so no upgrade is needed
for the diffusers route.

### Sizes that decide the layout

Reported by the repositories themselves, in GB.

| Component | bf16 | int8 convrot | w4a8 |
| --- | --- | --- | --- |
| Text encoder, Qwen3-VL 8B | 17.53 | 9.35 | 6.31 |
| Transformer, Turbo | 14.23 | 7.26 | published for the base model only |
| VAE | 0.68 | 0.68 | 0.68 |
| Total | 32.44 | 17.29 | 13.30 |

The T4 has 15.6 GB of VRAM. A full bf16 run does not fit anywhere on this
account. INT8 fits only if the text encoder is offloaded after the prompt is
encoded, which is exactly what the model card recommends. w4a8 for the encoder
plus INT8 for the transformer fits with about 1.3 GB to spare.

### Chosen build: INT8 convrot through ComfyUI

Decision: use the Comfy-Org INT8 convrot weights and ComfyUI, not bf16, and not
GGUF.

- The Comfy-Org repository publishes the INT8 weights in the ComfyUI layout,
  and ComfyUI loads them natively. `diffusers` has no reader for these files.
- On SM 7.5 the INT8 tensor-core kernels come from `comfy-kitchen`, which is
  the package the AIQUEST notebook installs and which explicitly targets T4.
- ComfyUI exposes a workflow JSON API and returns a finished PNG, so the agent
  sends one stable request instead of writing Python around a pipeline. Fewer
  moving parts per call.
- GGUF is aimed at GPUs without tensor cores and needs the third-party
  `ComfyUI-GGUF` node. On a T4 it is slower and adds a dependency.

Notebooks consulted while writing the setup:

- `TeamAIQ/Colab-notebooks`, `Qwen_Image_2_1_Uncensored_INT8_Colab_by_AIQUEST.ipynb`.
  Closest match to this account: it checks SM 7.5 and explains why INT8
  tensor cores need a T4 or newer, installs `comfy-kitchen`, and pins every
  package version.
- `Isi-dev/Google-Colab_Notebooks`, `Qwen_Image_2_1_Headless.ipynb` and the
  ComfyUI variant.
- `AICHUCKY/Ai-with-Chucky-Colab-Notebooks`, `Qwen_Image_2_1.ipynb` and the
  editing variant. Useful for the ComfyUI launch flags and the
  `aria2c` download loop.
- `fendimaharsi0-sketch/sdcpp-cuda-linux`, the `stable-diffusion.cpp` T4
  notebook. Interesting for the lightest path, not chosen here.

The AIQUEST notebook is the reference for package pinning and for the
statement that INT8 tensor-core kernels require SM 7.5, which is the basis of
the decision above.

### Practical notes

`hf_hub_download` without a token prints a rate-limit warning but works. A
7 GB file takes a few minutes. The kernel is single-threaded for `exec` calls,
so a second `exec` while a download runs waits; use `colab restart-kernel -s
<name>` to abandon a stuck call, which keeps the VM but wipes the kernel state.

## Verified end-to-end on a T4

Measured on this account on 1 November 2026.

- `colab new -s qwen --gpu T4` on `gpu-t4-s-euw4c0-...`, SM 7.5, 15.6 GB VRAM,
  120 GB free.
- ComfyUI 0.43.0 cloned from `comfyanonymous/ComfyUI`, requirements installed
  from the repository, `comfy-kitchen` 0.2.35 present.
- Weights from `Comfy-Org/Qwen-Image-2.1`:
  `diffusion_models/qwen_image_2.1_turbo_int8_convrot.safetensors` (7.26 GB),
  `text_encoders/qwen3vl_8b_int8_convrot.safetensors` (9.35 GB),
  `vae/qwen_image_2.1_vae_bf16.safetensors` (0.68 GB). Download took a few
  minutes each.
- Server started with `--disable-pinned-memory --mmap-torch-files --fp16-vae
  --preview-method none` and answered `/system_stats`.
- Result: a 1024x1024 PNG in 90 seconds including weight loading, so the
  subsequent images in the same session are faster. The file is 1.48 MB and
  contains a real image, not noise.

### Working workflow

The `TextEncodeQwenImage21` node carries both prompts and the resolution, so no
separate negative encoder is needed. Minimal graph:

    UNETLoader (turbo int8) -> ModelSamplingAuraFlow (shift 3.0) -> KSampler
    CLIPLoader (type qwen_image) -> TextEncodeQwenImage21 (prompt, negative_prompt, resolution 1024)
    VAELoader + EmptySD3LatentImage -> KSampler (steps 8, cfg 1.0, euler, simple) -> VAEDecode -> SaveImage

`KSampler` takes the positive and negative conditioning from outputs 0 and 1 of
the same `TextEncodeQwenImage21` node. A request without a `SaveImage` node is
rejected with `prompt_no_outputs`.

### Pitfalls met

A second `exec` while one is running waits for the kernel and then fails
confusingly. Use `colab restart-kernel -s <name>` to abandon it; the VM and
the downloaded weights survive because they live on disk.
`colab download` of a single PNG worked without SSH.
The session was revoked once between two of my calls; nothing in `/content`
survived, and a fresh `colab new` was needed.

## Where this fits in the app

The avatar generator in `AvatarImageGenerator.kt` sends `POST` to an
OpenAI-compatible `/v1/images/generations` with
`{"prompt": ..., "n": 1, "size": "512x512", "model": ..., "response_format":
"b64_json"}` and reads `data[0].b64_json` or `data[0].url`. ComfyUI speaks a
workflow graph instead, so `tools/colab/qwen_openai_server.py` bridges the two.

### The adapter

    tools/colab/setup_colab.sh          # ComfyUI + the three INT8 files
    tools/colab/qwen_openai_server.py   # ComfyUI on :8188, images API on :8000

The adapter starts ComfyUI itself, waits for `/system_stats`, then serves:

    GET  /health                     readiness, backend name, model file
    GET  /v1/models                  a single qwen-image-2.1-turbo entry
    POST /v1/images/generations      the OpenAI images shape
    GET  /v1/images/content/<name>   only for response_format "url"

It builds the verified graph, queues it on `/prompt`, polls `/history`, and
fetches the PNG from `/view`. Requests are serialised with a lock because a T4
cannot run two generations at once and interleaving them only produces
out-of-memory errors.

Verified end-to-end on a fresh T4 session on 1 November 2026: setup.sh
downloaded 7.26 + 9.35 + 0.68 GB into an empty VM, the adapter answered
`/health`, and a request identical to the app's (`512x512`, `b64_json`)
returned a valid 512x512 RGBA PNG of 210800 bytes in 67 seconds, matching the
`elapsed_seconds` reported by the server.

### Pointing the app at it

In Settings → Avatar generation → Backend, the endpoint is
`http://<vm-address>:8000/v1/images/generations`. The address is the VM's own;
Colab does not proxy this port publicly, so the phone and the VM must be
reachable from each other. API key is unused by the adapter and can be left
empty. Model name is optional and is ignored, because the adapter always runs
the single downloaded model.

### Ports

8080 belongs to the Colab frontend proxy and is already listening on a fresh
VM, so binding it fails with `Address already in use`. 8188 is ComfyUI,
2222 is sshd, 9000 is jupyter-server, 6000 is the kernel manager, 3453 is the
file shim. The adapter uses 8000, overridable with `PUBLIC_PORT`.

### Running it on the VM

`colab exec -f` sends a Python file and executes it as Python, so a shell
script there fails with `SyntaxError`. Either wrap the shell script in Python
with `subprocess.run(["bash", path])`, or upload it with `colab upload` and run
it the same way. The server must be started from inside the VM, not from the
host, because only the VM can reach ComfyUI on loopback.
