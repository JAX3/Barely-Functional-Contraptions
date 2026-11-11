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
    # Body: a deep 55 Hz electrical drone. Two slightly detuned voices beat once every 4 s; the harmonics up to
    # 275 Hz carry the low note on small speakers and earbuds that can't play 55 Hz itself.
    body = (np.sin(two_pi * 55 * t) + 0.6 * np.sin(two_pi * 55.25 * t)
            + 0.7 * np.sin(two_pi * 110 * t + 0.3) + 0.45 * np.sin(two_pi * 165 * t + 0.6)
            + 0.25 * np.sin(two_pi * 220 * t + 0.9) + 0.12 * np.sin(two_pi * 275 * t + 1.2))
    # Throb: a slow, heavy swell of the body, twice every 4 seconds.
    body *= 0.8 + 0.2 * np.sin(two_pi * 0.5 * t)
    # Shimmer: a faint, lower whine that rises and falls every 2 seconds, like charged air.
    shimmer = (np.sin(two_pi * 880 * t) + 0.5 * np.sin(two_pi * 1320.25 * t)) * (0.5 + 0.5 * np.sin(two_pi * 0.5 * t)) ** 2
    # Crackle: a faint wobble on a low-mid tone, four times a second.
    crackle = np.sin(two_pi * 440 * t + 2.0 * np.sin(two_pi * 4.0 * t)) * 0.5
    mix = body + 0.06 * shimmer + 0.04 * crackle
    return mix / np.max(np.abs(mix)) * 0.75


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
