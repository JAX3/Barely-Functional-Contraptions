"""Makes the 1920x220 profile cover banner: a band through the cover scene's sheep (run/screenshots/cover_raw.png,
from -PharnessScene=cover -PharnessWidth=1920 -PharnessHeight=1080) with the mod's logo banner on the left.

Run from the repository root: python3 tools/make_cover.py [top of the band, default 380]
"""
import sys
from PIL import Image

WIDTH, HEIGHT = 1920, 220
top = int(sys.argv[1]) if len(sys.argv) > 1 else 380

scene = Image.open("run/screenshots/cover_raw.png").convert("RGBA")
cover = scene.crop((0, top, WIDTH, top + HEIGHT))

# Darken the left, where the logo goes, fading out before the first sheep.
shade = Image.new("RGBA", (WIDTH, HEIGHT))
for x in range(WIDTH):
    a = 0 if x > 640 else int(150 * (1 - x / 640) ** 1.4)
    for y in range(HEIGHT):
        shade.putpixel((x, y), (16, 12, 28, a))
cover.alpha_composite(shade)

logo = Image.open("docs/banner.png").convert("RGBA")  # 480x160, the pixel-art title plate
cover.alpha_composite(logo, (56, (HEIGHT - logo.height) // 2))
cover.convert("RGB").save("docs/profile_cover.png")
print("docs/profile_cover.png", cover.size)
