"""Emit the fixture that the Kotlin unit test asserts against.

Runs the reference log-mel on a fixed pseudo-random int16 buffer, writes a small
binary fixture, and prints the values the Kotlin test will hardcode. The buffer is
generated from a fixed seed so both sides see identical input without committing
audio to the repository.

Usage:  python3 tools/gigaam/gen_fixture.py <out-dir>
"""

from __future__ import annotations

import struct
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from reference_frontend import log_mel_spectrogram  # noqa: E402

SAMPLE_COUNT = 16000  # exactly 1.00 s at 16 kHz


def make_pcm() -> np.ndarray:
    """A deterministic, speech-like-enough signal: a sum of partials plus noise.

    Any fixed input works for a numerical comparison; using a harmonic stack
    rather than pure noise gives the mel filterbank something with structure to
    bite on, so a wrong filterbank shape is unlikely to pass by accident.
    """
    rng = np.random.default_rng(20260930)
    t = np.arange(SAMPLE_COUNT, dtype=np.float64) / 16000.0
    signal = np.zeros(SAMPLE_COUNT)
    for freq, amp in ((110.0, 0.30), (220.0, 0.18), (440.0, 0.10), (1200.0, 0.05)):
        signal += amp * np.sin(2 * np.pi * freq * t)
    signal += 0.02 * rng.standard_normal(SAMPLE_COUNT)
    peak = np.abs(signal).max()
    signal = signal / peak * 0.7
    return np.clip(signal * 32768.0, -32768, 32767).astype(np.int16)


def main() -> None:
    out_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/gigaam-fixture")
    out_dir.mkdir(parents=True, exist_ok=True)

    pcm = make_pcm()
    feats = log_mel_spectrogram(pcm)

    # int16 PCM, little endian — the exact bytes Android's AudioRecord produces.
    (out_dir / "fixture.pcm").write_bytes(struct.pack("<%dh" % pcm.size, *pcm.tolist()))
    # float32 log-mel, mel-major, C order — what GigaAMFeatureExtractor must produce.
    feats.astype(np.float32).tofile(out_dir / "fixture_logmel.f32")

    print(f"  pcm отсчётов : {pcm.size}")
    print(f"  log-mel shape: {feats.shape}  (n_mels, n_frames)")
    print(f"  всего float32: {feats.size}")
    print(f"  записано в    : {out_dir}")

    # A handful of checksum-style values for a quick human diff.
    for m in (0, 8, 32, 63):
        col = feats[m]
        print(f"  mel[{m:2d}]: first={col[0]:.6f} mid={col[len(col) // 2]:.6f} last={col[-1]:.6f}")
    print(f"  global min={feats.min():.6f} max={feats.max():.6f}")


if __name__ == "__main__":
    main()
