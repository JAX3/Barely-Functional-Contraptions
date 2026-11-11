#!/usr/bin/env python3
"""
Synthesises the mod's sound effects and encodes them to Ogg Vorbis with ffmpeg.

Run from the project root:  python3 tools/sheep/generate_sounds.py   (needs numpy and ffmpeg with libvorbis)

zero_point_hum.ogg: the Zero Point Energy Module's energy hum, a 4-second seamless loop. Every frequency and
modulation rate is a whole multiple of 1/4 Hz, so the waveform ends exactly where it began and loops without a click.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

RATE = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets",
                   "barelyfunctionalcontraptions", "sounds", "block")


def zero_point_hum(seconds=4.0):
    t = np.arange(int(RATE * seconds)) / RATE
    two_pi = 2 * np.pi
    # Body: a low electrical drone with odd harmonics, two slightly detuned voices beating twice every 4 s.
    body = (np.sin(two_pi * 110 * t) + 0.55 * np.sin(two_pi * 110.5 * t)
            + 0.35 * np.sin(two_pi * 220 * t) + 0.22 * np.sin(two_pi * 330 * t + 0.6)
            + 0.10 * np.sin(two_pi * 550 * t + 1.1))
    # Throb: a slow swell of the body, once a second.
    body *= 0.85 + 0.15 * np.sin(two_pi * 1.0 * t)
    # Shimmer: a soft high whine that rises and falls every 2 seconds, like charged air.
    shimmer = (np.sin(two_pi * 1760 * t) + 0.5 * np.sin(two_pi * 2640.25 * t)) * (0.5 + 0.5 * np.sin(two_pi * 0.5 * t)) ** 2
    # Crackle: a faint wobble on a mid tone, four times a second.
    crackle = np.sin(two_pi * 880 * t + 2.0 * np.sin(two_pi * 4.0 * t)) * 0.5
    mix = body + 0.12 * shimmer + 0.06 * crackle
    return mix / np.max(np.abs(mix)) * 0.7


def write_ogg(samples, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = (np.clip(samples, -1, 1) * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = os.path.join(tmp, "sound.wav")
        with wave.open(wav_path, "wb") as w:
            w.setnchannels(1)  # mono, so Minecraft plays it positionally
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "5",
                        "-map_metadata", "-1", "-fflags", "+bitexact", path], check=True)


if __name__ == "__main__":
    write_ogg(zero_point_hum(), os.path.join(OUT, "zero_point_hum.ogg"))
    print("Sounds written to", os.path.abspath(OUT))
