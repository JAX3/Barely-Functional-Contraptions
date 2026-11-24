"""Item textures for the upgrade cards: a Blue Steel circuit card with gold edge contacts, a wool strip in the
card's colour down its left side and its mark on the right (none on the blank Upgrade Base).

    python3 tools/sheep/upgrade_textures.py
"""
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/barelyfunctionalcontraptions/textures/item"
OUTLINE = (35, 28, 53, 255)
STEEL, STEEL_LIGHT, STEEL_DARK = (93, 102, 135, 255), (141, 151, 188, 255), (63, 71, 102, 255)
GOLD, GOLD_DARK = (236, 190, 64, 255), (170, 120, 30, 255)
CHIP, CHIP_LIGHT = (42, 44, 56, 255), (78, 82, 100, 255)


def card(wool, wool_dark, mark=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # The card: 14 x 11, rows 2..12, outlined.
    for x in range(1, 15):
        for y in range(2, 13):
            edge = x in (1, 14) or y in (2, 12)
            px[x, y] = OUTLINE if edge else STEEL
    for x in range(2, 14):
        px[x, 3] = STEEL_LIGHT
    for x in range(2, 14):
        px[x, 11] = STEEL_DARK
    for y in range(3, 11):
        px[2, y] = STEEL_LIGHT if y < 6 else STEEL
    # Gold edge contacts along the bottom, below the card.
    for x in range(3, 13):
        px[x, 13] = OUTLINE
        if x % 2 == 1:
            px[x, 13] = GOLD
            px[x, 14] = GOLD_DARK
    # The wool strip down the left.
    for y in range(4, 11):
        px[3, y] = wool
        px[4, y] = wool_dark if y % 2 else wool
    # A little chip in the middle.
    for x in range(6, 10):
        for y in range(5, 9):
            px[x, y] = CHIP
    px[6, 5] = CHIP_LIGHT
    for x in (6, 9):
        px[x, 4] = STEEL_LIGHT
        px[x, 9] = STEEL_DARK
    if mark:
        for (x, y), colour in mark.items():
            px[x, y] = colour
    return img


YELLOW, YELLOW_DARK = (255, 226, 80, 255), (214, 160, 30, 255)
COPPER, COPPER_LIGHT = (196, 106, 64, 255), (238, 152, 102, 255)
# Speed: a lightning bolt.
SPEED = {}
for y, xs in {4: (12,), 5: (11, 12), 6: (10, 11), 7: (10, 11, 12, 13), 8: (12, 13), 9: (11, 12), 10: (11,)}.items():
    for x in xs:
        SPEED[(x, y)] = YELLOW if y <= 7 else YELLOW_DARK
# Efficiency: a copper coil (three loops).
EFFICIENCY = {}
for y in (5, 7, 9):
    EFFICIENCY[(11, y)] = COPPER_LIGHT
    EFFICIENCY[(12, y)] = COPPER_LIGHT
    EFFICIENCY[(10, y + 1)] = COPPER
    EFFICIENCY[(13, y + 1)] = COPPER
EFFICIENCY.pop((10, 10), None)
EFFICIENCY.pop((13, 10), None)

OUT.mkdir(parents=True, exist_ok=True)
card((176, 176, 186, 255), (140, 140, 152, 255)).save(OUT / "upgrade_base.png")
card((214, 58, 58, 255), (160, 36, 40, 255), SPEED).save(OUT / "speed_upgrade.png")
card((112, 190, 56, 255), (74, 140, 36, 255), EFFICIENCY).save(OUT / "efficiency_upgrade.png")
print("wrote", OUT / "upgrade_base.png", OUT / "speed_upgrade.png", OUT / "efficiency_upgrade.png")
