"""Makes the 800x800 profile picture to match the cover banner: the cover scene's diamond sheep close up
(run/screenshots/pfp_raw.png, from -PharnessScene=cover -PharnessWidth=1920 -PharnessHeight=1080), with the mod's
emblem badge low on the right, inside the circle many sites crop a profile picture to.

Run from the repository root: python3 tools/make_profile_picture.py
"""
from PIL import Image, ImageDraw

SIZE, CROP = 800, 1080
scene = Image.open("run/screenshots/pfp_raw.png").convert("RGBA")
cx, cy = 1090, 540  # right of the sheep, so it stands left of the middle and the badge has grass
picture = scene.crop((cx - CROP // 2, cy - CROP // 2, cx + CROP // 2, cy + CROP // 2)).resize((SIZE, SIZE), Image.LANCZOS)

# The emblem (the mod's icon, 144x144 pixel art) on a soft dark disc, two-thirds of the way out towards the lower right.
icon = Image.open("src/main/resources/icon.png").convert("RGBA")
bx = by = int(SIZE / 2 + SIZE / 2 * 0.62 * 0.707) - icon.width // 2
disc = Image.new("RGBA", (SIZE, SIZE))
ImageDraw.Draw(disc).ellipse((bx - 14, by - 14, bx + icon.width + 14, by + icon.height + 14), fill=(16, 12, 28, 120))
picture.alpha_composite(disc)
picture.alpha_composite(icon, (bx, by))
# Check it sits inside the circle crop.
corner = ((bx + icon.width - SIZE / 2) ** 2 * 2) ** 0.5
assert corner < SIZE / 2, "the badge would be cut off by a round crop"
picture.convert("RGB").save("docs/profile_picture.png")

# A preview of how it looks cropped round.
mask = Image.new("L", (SIZE, SIZE), 0)
ImageDraw.Draw(mask).ellipse((0, 0, SIZE - 1, SIZE - 1), fill=255)
round_ = Image.new("RGBA", (SIZE, SIZE), (13, 13, 13, 255))
round_.paste(picture, (0, 0), mask)
round_.convert("RGB").save("run/screenshots/profile_picture_round_preview.png")
print("docs/profile_picture.png", picture.size)
