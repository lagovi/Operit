package com.ai.assistance.operit.api.speech

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal strict WAV reader for feeding audio files into the on-device STT.
 *
 * Accepts only PCM (`AudioFormat=1`), mono, 16-bit files — anything else
 * fails loudly instead of decoding noise. Extra sub-chunks (`fact`, `bext`,
 * …) are skipped by scanning, not by assuming a 44-byte header.
 */
data class WavPcm(val sampleRateHz: Int, val samples: ShortArray)

object WavFileReader {

    fun read(path: String): WavPcm {
        val file = File(path)
        require(file.isFile) { "audio file not found: $path" }
        val bytes = file.readBytes()
        require(bytes.size >= 44) { "file too small to be WAV: $path (${bytes.size} bytes)" }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF") { "not a RIFF file: $path" }
        require(String(bytes, 8, 4, Charsets.US_ASCII) == "WAVE") { "not a WAVE file: $path" }

        var audioFormat = -1
        var channels = -1
        var sampleRate = -1
        var bitsPerSample = -1
        var dataOffset = -1
        var dataSize = -1

        var pos = 12
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4, Charsets.US_ASCII)
            val size = buf.getInt(pos + 4)
            require(size >= 0) { "corrupt WAV chunk '$id' in $path" }
            val body = pos + 8
            when (id) {
                "fmt " -> {
                    require(size >= 16) { "truncated fmt chunk in $path" }
                    audioFormat = buf.getShort(body).toInt()
                    channels = buf.getShort(body + 2).toInt()
                    sampleRate = buf.getInt(body + 4)
                    bitsPerSample = buf.getShort(body + 14).toInt()
                }
                "data" -> {
                    dataOffset = body
                    dataSize = size
                }
            }
            pos = body + size + (size and 1)
        }

        require(audioFormat == 1) { "only PCM WAV is supported, got format $audioFormat: $path" }
        require(channels == 1) { "only mono WAV is supported, got $channels channels: $path" }
        require(bitsPerSample == 16) { "only 16-bit WAV is supported, got $bitsPerSample-bit: $path" }
        require(dataOffset >= 0) { "WAV has no data chunk: $path" }
        require(dataOffset + dataSize <= bytes.size) { "WAV data chunk overruns file: $path" }

        val count = dataSize / 2
        val samples = ShortArray(count) { buf.getShort(dataOffset + it * 2) }
        return WavPcm(sampleRate, samples)
    }
}
