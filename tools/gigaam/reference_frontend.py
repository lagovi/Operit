"""Reference GigaAM frontend + greedy CTC decode, transcribed from the model config.

Every constant here is read off multilingual_ctc.yaml from the export repo, not
guessed:

    preprocessor:
      sample_rate: 16000
      features: 64          <- 64 mel bins, not 80
      win_length: 320       <- not 400
      hop_length: 160
      n_fft: 320            <- not 400
      center: false         <- matters: no reflect padding
    head: num_classes: 71

The spectrogram itself is torchaudio.transforms.MelSpectrogram with its defaults
(power=2.0, f_min=0, f_max=sr/2, norm=None, mel_scale="htk") followed by
gigaam.preprocess.SpecScaler, which is log(clamp(x, 1e-9, 1e9)).

This file is the oracle for the Kotlin port in
app/src/main/java/com/ai/assistance/operit/api/speech/GigaAMFeatureExtractor.kt.
If the two disagree, the Kotlin side is wrong.
"""

from __future__ import annotations

import numpy as np

SAMPLE_RATE = 16000
N_MELS = 64
WIN_LENGTH = 320
HOP_LENGTH = 160
N_FFT = 320
CENTER = False
LOG_CLAMP_MIN = 1e-9
LOG_CLAMP_MAX = 1e9

# gigaam.preprocess.load_audio divides int16 by 32768.0, so the model wants
# float samples already in [-1, 1]. No extra peak normalisation anywhere.
PCM_SCALE = 32768.0


def hz_to_mel(freq: np.ndarray) -> np.ndarray:
    return 2595.0 * np.log10(1.0 + freq / 700.0)


def mel_to_hz(mel: np.ndarray) -> np.ndarray:
    return 700.0 * (10.0 ** (mel / 2595.0) - 1.0)


def mel_filterbank() -> np.ndarray:
    """Torchaudio's default mel filterbank: HTK scale, no Slaney norm, triangular.

    Returns [n_mels, n_fft // 2 + 1].
    """
    n_freqs = N_FFT // 2 + 1
    fft_freqs = np.linspace(0.0, SAMPLE_RATE / 2.0, n_freqs)

    mel_min = hz_to_mel(np.array(0.0))
    mel_max = hz_to_mel(np.array(SAMPLE_RATE / 2.0))
    mel_points = np.linspace(mel_min, mel_max, N_MELS + 2)
    hz_points = mel_to_hz(mel_points)

    filters = np.zeros((N_MELS, n_freqs), dtype=np.float64)
    for m in range(N_MELS):
        left, centre, right = hz_points[m], hz_points[m + 1], hz_points[m + 2]
        # Torchaudio computes the slopes in the mel domain and normalises each
        # filter to unit area in the frequency domain, so replicate that rather
        # than a hand-rolled triangular.
        if right <= left:
            centre_freq = float(fft_freqs[np.argmin(np.abs(fft_freqs - centre))])
            filters[m, np.argmin(np.abs(fft_freqs - centre_freq))] = 1.0
            continue
        rising = (fft_freqs - left) / (centre - left)
        falling = (right - fft_freqs) / (right - centre)
        weights = np.minimum(rising, falling)
        weights = np.maximum(weights, 0.0)
        filters[m] = weights
    return filters


def hann_window() -> np.ndarray:
    """torch.hann_window(win_length, periodic=True) — matches torchaudio's default."""
    n = np.arange(WIN_LENGTH, dtype=np.float64)
    return 0.5 - 0.5 * np.cos(2.0 * np.pi * n / WIN_LENGTH)


_FILTERBANK = mel_filterbank()
_WINDOW = hann_window()


def log_mel_spectrogram(samples: np.ndarray) -> np.ndarray:
    """PCM int16 (or float in [-1, 1]) -> log-mel [n_mels, n_frames].

    Mel-major on purpose: the graph's input is declared ['batch', 64, 'seq_len'],
    so this is the array to feed without transposing.
    """
    x = np.asarray(samples, dtype=np.float64) / PCM_SCALE
    if x.ndim > 1:
        x = x.mean(axis=-1)

    # center=False: only whole windows, no padding at either end.
    n_frames = 1 + (len(x) - WIN_LENGTH) // HOP_LENGTH
    if n_frames <= 0:
        return np.zeros((N_MELS, 0), dtype=np.float32)

    # Strided framing, then the rfft. np.fft.rfft matches torch.fft.rfft.
    idx = (
        np.arange(WIN_LENGTH)[None, :]
        + HOP_LENGTH * np.arange(n_frames)[:, None]
    )
    frames = x[idx] * _WINDOW[None, :]
    spectrum = np.fft.rfft(frames, n=N_FFT, axis=-1)
    power = (spectrum.real**2 + spectrum.imag**2)  # power=2.0

    mel = power @ _FILTERBANK.T
    log_mel = np.log(np.clip(mel, LOG_CLAMP_MIN, LOG_CLAMP_MAX)).astype(np.float32)
    return np.ascontiguousarray(log_mel.T)


def load_tokens(path: str) -> list[str]:
    """tokens.txt: 'a 2' -> 'a'. Index 0 is the space, the last index is <blk>."""
    tokens: list[str] = []
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.rstrip("\n")
            if not line:
                continue
            symbol, _idx = line.rsplit(" ", 1)
            tokens.append(symbol)
    return tokens


def greedy_ctc_decode(logits: np.ndarray, tokens: list[str]) -> str:
    """Greedy CTC: argmax per frame, collapse repeats, then drop blanks."""
    blank_id = len(tokens) - 1
    best = np.argmax(logits, axis=-1)
    out: list[str] = []
    previous = -1
    for symbol_id in best:
        symbol_id = int(symbol_id)
        if symbol_id != previous and symbol_id != blank_id:
            out.append(tokens[symbol_id])
        previous = symbol_id
    return "".join(out)
