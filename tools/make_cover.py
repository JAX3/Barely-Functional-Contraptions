"""Makes the 1920x220 profile cover banner.

- Sheep only (the default): a band through the cover_sheep scene's row of eleven resourceful sheep
  (run/screenshots/cover_sheep_raw.png, from -PharnessScene=cover_sheep -PharnessWidth=1920 -PharnessHeight=1080),
  saved as docs/profile_cover.png.
- With --logo: a band through the cover scene (run/screenshots/cover_raw.png, -PharnessScene=cover), with the mod's
  logo banner on the left, saved as docs/profile_cover_logo.png.

Run from the repository root: python3 tools/make_cover.py [--logo] [top of the band, default 380]
"""
import sys
from PIL import Image

WIDTH, HEIGHT = 1920, 220
args = [a for a in sys.argv[1:] if a != "--logo"]
logo = "--logo" in sys.argv[1:]
top = int(args[0]) if args else 380

scene = Image.open("run/screenshots/cover_raw.png" if logo else "run/screenshots/cover_sheep_raw.png").convert("RGBA")
cover = scene.crop((0, top, WIDTH, top + HEIGHT))

if logo:
    # Darken the left, where the logo goes, fading out before the first sheep.
    shade = Image.new("RGBA", (WIDTH, HEIGHT))
    for x in range(WIDTH):
        a = 0 if x > 640 else int(150 * (1 - x / 640) ** 1.4)
        for y in range(HEIGHT):
            shade.putpixel((x, y), (16, 12, 28, a))
    cover.alpha_composite(shade)
    plate = Image.open("docs/banner.png").convert("RGBA")  # 480x160, the pixel-art title plate
    cover.alpha_composite(plate, (56, (HEIGHT - plate.height) // 2))

out = "docs/profile_cover_logo.png" if logo else "docs/profile_cover.png"
cover.convert("RGB").save(out)
print(out, cover.size)
