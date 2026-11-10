"""Hand-placed 16x16 item textures."""
from PIL import Image

from .palette import hexc

# Original (realistic) Barely Functional Wrench, used with --style realistic.
WRENCH_PALETTE = {
    "K": hexc("#1d1626"),  # outline
    "s": hexc("#e3dde8"), "S": hexc("#aaa3b6"), "d": hexc("#6f6879"),  # silver light / mid / dark
    "P": hexc("#b45cff"), "p": hexc("#6d2bb0"), "L": hexc("#f4d8ff"),  # purple glow / dark / highlight
    "g": hexc("#7fb045"), "G": hexc("#4a7230"),  # moss
    "b": hexc("#9a6c3c"), "B": hexc("#5e3d20"), "w": hexc("#c9a066"),  # wood / leather
    "h": hexc("#4b3d5c"), "H": hexc("#74608f"),  # handle
    "m": hexc("#e06ccc"), "n": hexc("#ffb6ef"), "t": hexc("#efe4d4"),  # pink mushroom cap / spot / stem
}

WRENCH = [
    ".........KK...K.",
    "........KsSK.KsK",
    ".......KSsgK.KSK",
    ".......KSsSSKSSK",
    "......KSsPLSSgdK",
    "..KKK.KdSpPSdddK",
    ".KmnmKbBdSdGdKK.",
    "..KtKHbBKKKKK...",
    "...KHhgK.KKK....",
    "..KhPhK.KmnmK...",
    ".KHgK....KtK....",
    "KwwbK.....K.....",
    "Kw.bK...........",
    "KbBBK...........",
    ".KKK............",
    "................",
]


def from_grid(grid, palette):
    height, width = len(grid), len(grid[0])
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == width, f"row {y} has {len(row)} pixels, expected {width}"
        for x, key in enumerate(row):
            if key != ".":
                img.putpixel((x, y), palette[key])
    return img


# Cartoon Barely Functional Wrench (default): a stylized crowbar in the blocks' art style. Thick dark outline,
# a shaft in the machines' purple rim paint with one magenta glint, and slate-steel ends: a hooked, notched claw at the
# top and a flattened chisel tip at the bottom.
WRENCH_CARTOON_PALETTE = {
    "O": hexc("#231c35"),  # outline (same as the blocks)
    "s": hexc("#f4f8fc"), "S": hexc("#b9c3d1"), "d": hexc("#6f7a8b"),  # steel highlight / steel / steel shadow
    "L": hexc("#d39bff"), "P": hexc("#9a52f0"), "p": hexc("#5c2aa6"),  # purple paint highlight / paint / shadow
    "g": hexc("#ff6ae6"),  # glint
}

WRENCH_CARTOON = [
"..........OOOO..",
".........OsSSSO.",
"........OsSOOSdO",
"........OSdOOSdO",
"........OLPpOOdO",
".......OLPpO.OdO",
"......OLPpO..ddO",
".....OLgpO...O.O",
"....OLPpO.......",
"...OLPpO........",
"..OLPpO.........",
".OsPpO..........",
".OsSdO..........",
"OsSdO...........",
"OSdO............",
".OO.............",
]


def mycelium_wrench(style="cartoon"):
    if style == "realistic":
        return from_grid(WRENCH, WRENCH_PALETTE)
    return from_grid(WRENCH_CARTOON, WRENCH_CARTOON_PALETTE)
