#!/usr/bin/env python3
"""
Generates every texture used by Resourceful Sheep as original pixel art.

Run from the project root:  python3 tools/generate_textures.py
Requires Pillow. Output goes to src/main/resources/assets/barelyfunctionalcontraptions/textures/.

All art is procedurally drawn here (no vanilla textures are read or copied); only
the UV *layout* of the sheep fur model (64x32) is matched so the overlay wraps
the vanilla model correctly.
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "barelyfunctionalcontraptions", "textures")


def out(path):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    return full


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(c1, c2, t):
    return tuple(int(round(c1[i] * (1 - t) + c2[i] * t)) for i in range(4))


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3])


CLEAR = (0, 0, 0, 0)

# ---------------------------------------------------------------------------
# Resource wool items (16x16), in the same cartoon style as the Mechanical
# Separator / Mycelial Power: a thick dark outline, the flat resource colour
# with one highlight and one shadow tone, yarn-strand arcs, and a chunky
# per-resource accent stamped on top.
#   O outline   H highlight   M base colour   S shadow / strand
# ---------------------------------------------------------------------------
WOOL_OUTLINE = hexc("#231c35")

WOOL_BALL = [
    "................",
    ".....OOOOOO.....",
    "...OOHHHHHHOO...",
    "..OHHHMMMMSHSO..",
    ".OHHMMMMMSMMMSO.",
    ".OHMMMMMSMMMSMSO",
    "OHHMMMMSMMMSMMSO",
    "OHMMMMSMMMSMMMSO",
    "OHMMMSMMMSMMMMSO",
    "OHMMSMMMSMMMMSSO",
    "OMMSMMMSMMMMSMSO",
    ".OSMMMSMMMMSMSO.",
    ".OSMMSMMMMSMSSO.",
    "..OSSMMMMSSSSO..",
    "...OOSSSSSSOO...",
    ".....OOOOOO.....",
]


def wool_palette(base):
    return {
        "O": WOOL_OUTLINE,
        "H": mix(base, (255, 255, 255, 255), 0.38),
        "M": base,
        "S": shade(base, 0.68),
    }


def draw_wool(base):
    img = Image.new("RGBA", (16, 16), CLEAR)
    pal = wool_palette(base)
    for y, row in enumerate(WOOL_BALL):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


def inside(x, y):
    return 0 <= x < 16 and 0 <= y < 16 and WOOL_BALL[y][x] not in ".O"


def put(img, x, y, c):
    if inside(x, y):
        img.putpixel((x, y), c)


def stamp(img, ox, oy, rows, pal):
    """Draws a small character grid at (ox, oy); '.' is skipped, only inside the ball."""
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch in pal:
                put(img, ox + dx, oy + dy, pal[ch])


WHITE = hexc("#ffffff")

# Chunky accent motifs (A = accent colour, a = accent shadow, G = shine, O = outline)
GEM = [".O.", "OGO", "OAa", ".O."]
GEM_BIG = ["..O..", ".OGO.", "OGAaO", ".OaO.", "..O.."]
STAR = ["..G..", ".GAG.", "GAGAG", ".GAG.", "..G.."]
DOT = [".A.", "AGA", ".a."]
CHUNK = ["OO.", "OaO", ".OO"]


def deco_strands():
    """Bright metal threads following the yarn arcs, with glints."""
    def f(img, pal):
        for i in range(8):
            put(img, 10 - i, 3 + i, pal["A"])      # main bright thread
            put(img, 11 - i, 3 + i, pal["a"])
        for i in range(6):
            put(img, 13 - i, 6 + i, pal["A"])      # second thread
        for (x, y) in [(8, 5), (5, 8), (11, 8), (4, 4)]:
            put(img, x, y, pal["G"])
    return f


def deco_sparkles():
    def f(img, pal):
        stamp(img, 3, 4, STAR, pal)
        stamp(img, 8, 8, STAR, pal)
        put(img, 11, 4, pal["G"])
    return f


def deco_gems(big_at, small_at):
    def f(img, pal):
        stamp(img, big_at[0], big_at[1], GEM_BIG, pal)
        for (x, y) in small_at:
            stamp(img, x, y, GEM, pal)
    return f


def deco_dots(positions):
    def f(img, pal):
        for (x, y) in positions:
            stamp(img, x, y, DOT, pal)
    return f


def deco_spots(extra=None):
    """Oxidisation / speckle spots plus an optional second colour."""
    def f(img, pal):
        for (x, y) in [(4, 6), (10, 5), (8, 10)]:
            stamp(img, x, y, ["AA", "Aa"], pal)
        if extra:
            for (x, y) in [(6, 9), (11, 9)]:
                put(img, x, y, extra)
    return f


def deco_chunks():
    def f(img, pal):
        stamp(img, 4, 5, CHUNK, pal)
        stamp(img, 9, 9, CHUNK, pal)
        for (x, y) in [(5, 6), (10, 10), (11, 5)]:
            put(img, x, y, pal["G"])
    return f


def deco_rings():
    def f(img, pal):
        for x in range(3, 13):
            put(img, x, 6, pal["a"])
            put(img, x, 10, pal["a"])
        for (x, y) in [(5, 8), (6, 8), (10, 8), (7, 4)]:
            put(img, x, y, pal["A"])
    return f


BUILTIN = {
    # name: (wool colour, accent colour, decorator). Keep colours in sync with tools/generate_data.py.
    "iron": ("#C8C8C8", "#F4F4F4", deco_strands()),
    "gold": ("#F2C230", "#FFF6A8", deco_sparkles()),
    "copper": ("#D27A4E", "#5FD3B5", deco_spots()),
    "diamond": ("#7FE6E0", "#4ED8E8", deco_gems((5, 5), [(10, 3)])),
    "emerald": ("#3FC46A", "#2FE07A", deco_gems((5, 5), [(10, 3)])),
    "redstone": ("#9E1D1D", "#FF3B2B", deco_dots([(3, 4), (9, 3), (5, 9), (10, 8)])),
    "lapis": ("#2F55B5", "#9DB8FF", deco_spots(hexc("#F2D45C"))),
    "coal": ("#3A3A3E", "#8C8C96", deco_chunks()),
    "netherite": ("#4A3E44", "#B58BD0", deco_strands()),
    "stone": ("#8E8E8E", "#5E5E5E", deco_chunks()),
    "wood": ("#A8834F", "#D2B07A", deco_rings()),
    "quartz": ("#EDE6DC", "#FFFFFF", deco_gems((6, 6), [(10, 3)])),
}


def accent_palette(accent):
    return {"A": accent, "a": shade(accent, 0.7), "G": mix(accent, WHITE, 0.6), "O": WOOL_OUTLINE}


def generate_wool_items():
    for name, (base, accent, deco) in BUILTIN.items():
        img = draw_wool(hexc(base))
        deco(img, accent_palette(hexc(accent)))
        img.save(out(f"item/{name}_wool.png"))
    # Fallback layers for data-pack resources. layer0 (tinted with wool_color) is a light-grey ball;
    # the outline is drawn on layer1 (tinted with accent_color) would recolour it, so the outline stays
    # on layer0 in near-black, which tinting keeps dark.
    grey = draw_wool(hexc("#E6E6E6"))
    for y in range(16):
        for x in range(16):
            if WOOL_BALL[y][x] == "O":
                grey.putpixel((x, y), hexc("#2a2a2a"))
    grey.save(out("item/resource_wool_base.png"))
    acc = Image.new("RGBA", (16, 16), CLEAR)
    deco_sparkles()(acc, {"A": hexc("#e6e6e6"), "a": hexc("#a0a0a0"), "G": WHITE, "O": hexc("#3a3a3a")})
    # stamp() only draws inside the ball shape, which also holds for this transparent layer.
    acc.save(out("item/resource_wool_overlay.png"))


# ---------------------------------------------------------------------------
# Sheep fur overlay (64x32, vanilla SheepFurModel UV layout) - greyscale so the
# renderer can tint it, plus a sparse accent ("glint strands") layer.
# ---------------------------------------------------------------------------
def box_faces(u, v, w, h, d):
    """Returns dict face -> (x0, y0, x1, y1) for a model cube at texOffs(u, v)."""
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "east": (u, v + d, u + d, v + d + h),
        "north": (u + d, v + d, u + d + w, v + d + h),
        "west": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def fur_noise(x, y, rnd):
    # Curly fleece: base 200 with clumps; deterministic per pixel.
    clump = ((x * 7 + y * 13) % 11) / 11.0
    v = 196 + int(40 * clump) + rnd.randint(-14, 14)
    if (x + 2 * y) % 5 == 0:
        v -= 22
    if (2 * x + y) % 7 == 0:
        v += 14
    return max(120, min(255, v))


def generate_sheep_fur():
    rnd = random.Random(1337)
    wool = Image.new("RGBA", (64, 32), CLEAR)
    accent = Image.new("RGBA", (64, 32), CLEAR)

    def fill(rect, mask=None):
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                if mask and not mask(x - x0, y - y0, x1 - x0, y1 - y0):
                    continue
                g = fur_noise(x, y, rnd)
                wool.putpixel((x, y), (g, g, g, 255))
                if rnd.random() < 0.07:
                    a = rnd.choice([255, 230, 200])
                    accent.putpixel((x, y), (255, 255, 255, a))

    head = box_faces(0, 0, 6, 6, 6)
    for face in ("top", "east", "west", "south"):
        fill(head[face])
    # Face: only a fringe of wool across the top two rows so the sheep's face shows.
    fill(head["north"], lambda x, y, w, h: y < 2 or (y < 3 and x in (0, w - 1)))
    body = box_faces(28, 8, 8, 16, 6)
    for face in body:
        fill(body[face])
    legs = box_faces(0, 16, 4, 6, 4)
    fill(legs["top"])
    for face in ("east", "north", "west", "south"):
        fill(legs[face], lambda x, y, w, h: y < 4)

    # A few long glinting strands along the body sides for the "metallic strands" look.
    for (x0, y0, x1, y1) in (body["north"], body["south"], body["east"], body["west"]):
        for sx in range(x0 + 1, x1, 3):
            for y in range(y0 + 2, y1 - 2, 4):
                accent.putpixel((sx, y), (255, 255, 255, 255))
                if y + 1 < y1:
                    accent.putpixel((sx, y + 1), (235, 235, 235, 200))

    wool.save(out("entity/sheep/infused_wool.png"))
    accent.save(out("entity/sheep/infused_wool_accent.png"))


# ---------------------------------------------------------------------------
# Mechanical Separator block textures (16x16), cartoon style shared with the
# sister project Mycelial Power: thick dark outlines, flat slate metal with one
# highlight and one shadow tone, chunky bolts and glowing purple ports. Where
# Mycelial Power grows moss, the separator sheds tufts of wool.
# Each texture is a hand-placed character grid; edit the grids directly.
# ---------------------------------------------------------------------------
CARTOON = {
    "O": hexc("#231c35"),                                            # outline
    "M": hexc("#5d6687"), "H": hexc("#8d97bc"), "S": hexc("#3f4766"),  # metal / highlight / shadow
    "B": hexc("#dfe4f5"), "b": hexc("#9aa3c4"),                      # bolt highlight / bolt
    "P": hexc("#9a52f0"), "p": hexc("#5c2aa6"),                      # port ring / ring shadow
    "G": hexc("#ffd2fb"), "g": hexc("#ff6ae6"),                      # port glow centre / glow
    "W": hexc("#ffffff"), "w": hexc("#e3e1ea"), "v": hexc("#b7b3c6"),  # wool light / wool / wool shadow
    "K": hexc("#141019"), "k": hexc("#2a2238"),                      # dark chamber / chamber edge
    "C": hexc("#f2b24a"), "c": hexc("#b86f22"), "Y": hexc("#ffe58a"),  # brass gear / gear shadow / gear shine
    "R": hexc("#ff4b4b"), "r": hexc("#7d3a44"),                      # status light lit / unlit
    "E": hexc("#ffe14a"), "e": hexc("#c98a12"),                      # energy bolt / bolt shadow
}

SEP_SIDE = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMMMMOOOOMMMMSO",
    "OHMMMOPPPPOMMMSO",
    "OHMMOPpggpPOMMSO",
    "OHMMOPgGGgPOMMSO",
    "OHMMOPgGGgPOMMSO",
    "OHMMOPpggpPOMMSO",
    "OHMMMOPPPPOMMMSO",
    "OHMwMMOOOOMMwMSO",
    "OHwWwMMMMMMwWwSO",
    "OwWWWwMwMMwWWWwO",
    "OvwWwwvWwvwwWwvO",
    "OOOOOOOOOOOOOOOO",
]

SEP_TOP = [  # wool intake hopper with fluff caught in the grate
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbOOOOOOOOOObSO",
    "OHMOkkkkkkkkOMSO",
    "OHMOKwWKKKKKOMSO",
    "OHMOkkkkkkkkOMSO",
    "OHMOKKKKKWwKOMSO",
    "OHMOkkkkkkkkOMSO",
    "OHMOKKwWKKKKOMSO",
    "OHMOkkkkkkkkOMSO",
    "OHMOKKKKKKKKOMSO",
    "OHbOOOOOOOOOObSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

SEP_BOTTOM = [  # output chute
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMMMMMMMMMMMMSO",
    "OHMMMOOOOOOMMMSO",
    "OHMMOKkKkKkKOMSO",
    "OHMMOkKkKkKkOMSO",
    "OHMMOKkKkKkKOMSO",
    "OHMMOkKkKkKkOMSO",
    "OHMMMOOOOOOMMMSO",
    "OHMMMMMMMMMMMMSO",
    "OHMMMMMMMMMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

SEP_BACK = [  # glowing FE input port with an energy bolt
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMOOOOOOMbSSO",
    "OHMMOPPPPPPOMMSO",
    "OHMOPpggggpPOMSO",
    "OHMOPgggEegPOMSO",
    "OHMOPggEegggOMSO",
    "OHMOPgEEEEegOMSO",
    "OHMOPgggEegPOMSO",
    "OHMOPggEeggPOMSO",
    "OHMMOPpggpPOMMSO",
    "OHMMMOPPPPOMMMSO",
    "OHBbMMOOOOMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

# Front: the Mycelial Generator's purple rim; the window is transparent and the model's raised chamber fills it.
SEP_FRONT_FRAME = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHPPPPPPPPPPPPSO",
    "OHPOOOOOOOOOOpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPOOOOOOOOOOpSO",
    "OwppppppppppppvO",
    "OvwWwwvWwvwwWwvO",
    "OOOOOOOOOOOOOOOO",
]

# 8x8 window contents (drawn at 4,4). Two gear poses alternate; wool tufts drift right while active.
GEAR_POSES = [
    ["..C..C..", ".CCCCCC.", "CCcYYcCC", ".CY..YC.", ".CY..YC.", "CCcYYcCC", ".CCCCCC.", "..C..C.."],
    [".C....C.", "CCCCCCCC", ".CcYYcC.", ".cY..Yc.", ".cY..Yc.", ".CcYYcC.", "CCCCCCCC", ".C....C."],
]


def grid(rows, pal, img=None, ox=0, oy=0):
    if img is None:
        img = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((ox + x, oy + y), pal[ch])
    return img


# ---- Front chambers, drawn the same way as the Mycelial Generator's: the front texture is the purple rim with a
# transparent window, and the model's raised glass chamber shows a 10x10 cartoon at pixel 3,3 of a chamber texture.
# Idle: the cartoon sits unlit on dark glass. Running: it glows on a pulsing halo with one sparkle at a time.
CHAMBER_ORIGIN = 3

WOOL_BLOCK = [  # puffy cartoon wool block with scalloped edges
    "..OO..OO..",
    ".OWWOOWWO.",
    "OWLWWWWwWO",
    "OWWLwWWwvO",
    ".OWwWWLvO.",
    ".OWWWLwvO.",
    "OWLwWWWwvO",
    "OWwWwvWvvO",
    ".OvvOOvvO.",
    "..OO..OO..",
]
WOOL_IDLE = {"O": hexc("#120d18"), "L": hexc("#a29cae"), "W": hexc("#8a8496"), "w": hexc("#6c6778"), "v": hexc("#4f4a5b")}
WOOL_LIT = {"O": hexc("#3b1a5c"), "L": hexc("#ffffff"), "W": hexc("#f6f3fb"), "w": hexc("#d9d4e6"), "v": hexc("#ab9fc4")}

SHEARS_ICON = [  # open shears, blades up, finger loops down
    "L........L",
    "lL......Ll",
    ".dl....ld.",
    "..dl..ld..",
    "...dXXd...",
    "...qXXq...",
    "..qQ..Qq..",
    ".QQ....QQ.",
    ".Q.Q..Q.Q.",
    "..QQ..QQ..",
]
SHEARS_IDLE = {"O": hexc("#120d18"), "L": hexc("#8f97a6"), "l": hexc("#6c7482"), "d": hexc("#4a5160"),
               "X": hexc("#8a7a4a"), "Q": hexc("#7a3a3e"), "q": hexc("#552529")}
SHEARS_LIT = {"O": hexc("#3b1a5c"), "L": hexc("#ffffff"), "l": hexc("#d4dce8"), "d": hexc("#8e99ab"),
              "X": hexc("#ffd75e"), "Q": hexc("#ff4b4b"), "q": hexc("#c42a3a")}

CHAMBER_SPARKLES = [(3, 3), (12, 4), (12, 11), (3, 11)]


def _lerp(c1, c2, t):
    return tuple(round(c1[i] + (c2[i] - c1[i]) * t) for i in range(4))


def _chamber_frame(img, top, colour):
    for i in range(16):
        for (x, y) in ((i, top), (0, top + i), (i, top + 15), (15, top + i)):
            img.putpixel((x, y), colour)


def chamber_off(art, pal):
    img = Image.new("RGBA", (16, 16), hexc("#1d1626"))
    grid(art, pal, img, CHAMBER_ORIGIN, CHAMBER_ORIGIN)
    for (x, y) in ((4, 4), (5, 4), (4, 5)):
        if img.getpixel((x, y)) == hexc("#1d1626"):
            img.putpixel((x, y), hexc("#4a3d5c"))
    _chamber_frame(img, 0, hexc("#3a2d4a"))
    return img


def chamber_on(art, pal, inner, outer, frames=8):
    import math
    img = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        pulse = 0.5 + 0.5 * math.sin(f / frames * 2 * math.pi)
        top = f * 16
        for y in range(16):
            for x in range(16):
                r = math.hypot(x - 7.5, y - 7.0) / (7.0 + pulse)
                img.putpixel((x, top + y), _lerp(inner, outer, min(1.0, r)))
        grid(art, pal, img, CHAMBER_ORIGIN, top + CHAMBER_ORIGIN)
        sx, sy = CHAMBER_SPARKLES[(f // 2) % len(CHAMBER_SPARKLES)]
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            if 0 < sx + dx < 15 and 0 < sy + dy < 15:
                img.putpixel((sx + dx, top + sy + dy), hexc("#ffffff") if (dx, dy) == (0, 0) else hexc("#fff3a8"))
        _chamber_frame(img, top, hexc("#f6eeff"))
    return img


def save_chambers(prefix, art, idle, lit, inner, outer):
    chamber_off(art, idle).save(out(f"block/{prefix}_chamber_off.png"))
    chamber_on(art, lit, inner, outer).save(out(f"block/{prefix}_chamber_on.png"))
    with open(out(f"block/{prefix}_chamber_on.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 4,\n    "interpolate": true\n  }\n}\n')


def machine_front_preview(prefix):
    """Front face as it looks running (rim + first lit chamber frame), for banners and docs."""
    img = Image.open(out(f"block/{prefix}_front.png")).convert("RGBA")
    chamber = Image.open(out(f"block/{prefix}_chamber_on.png")).convert("RGBA").crop((3, 3, 13, 13))
    img.alpha_composite(chamber, (3, 3))
    return img


def generate_block():
    grid(SEP_SIDE, CARTOON).save(out("block/mechanical_separator_side.png"))
    grid(SEP_TOP, CARTOON).save(out("block/mechanical_separator_top.png"))
    grid(SEP_BOTTOM, CARTOON).save(out("block/mechanical_separator_bottom.png"))
    grid(SEP_BACK, CARTOON).save(out("block/mechanical_separator_back.png"))
    grid(SEP_FRONT_FRAME, CARTOON).save(out("block/mechanical_separator_front.png"))
    for stale in ("mechanical_separator_front_active.png", "mechanical_separator_front_active.png.mcmeta"):
        if os.path.exists(out("block/" + stale)):
            os.remove(out("block/" + stale))
    # violet halo: the separator pulls the wool apart
    save_chambers("mechanical_separator", WOOL_BLOCK, WOOL_IDLE, WOOL_LIT, hexc("#f1dcff"), hexc("#b77cf5"))


CREATIVE_GEN = [  # creative energy source: magenta-rimmed plate with a big glowing energy core
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHggggggggggggSO",
    "OgBbMMOOOOMMBbgO",
    "OgbSMOPPPPOMbSgO",
    "OgMMOPpGGpPOMMgO",
    "OgMOPpGEEGpPOMgO",
    "OgMOPGEEeGGPOMgO",
    "OgMOPGGEEEGPOMgO",
    "OgMOPpGGEepPOMgO",
    "OgMMOPpGEpPOMMgO",
    "OgbSMOPPPPOMbSgO",
    "OgBbMMOOOOMMBbgO",
    "OHggggggggggggSO",
    "OHbSSSSSSSSSSbSO",
    "OOOOOOOOOOOOOOOO",
]


def generate_creative_generator():
    """4-frame pulsing glow animation, same on every face."""
    strip = Image.new("RGBA", (16, 64), CLEAR)
    pulses = [0.0, 0.35, 0.7, 0.35]
    for f, t in enumerate(pulses):
        pal = dict(CARTOON)
        pal["g"] = mix(hexc("#c23bb8"), hexc("#ff9af0"), t)        # rim glow
        pal["G"] = mix(hexc("#ff6ae6"), hexc("#ffe6fd"), t)        # core glow
        pal["P"] = mix(hexc("#9a52f0"), hexc("#c99bff"), t)
        frame = grid(CREATIVE_GEN, pal)
        strip.paste(frame, (0, f * 16))
    strip.save(out("block/creative_generator.png"))
    with open(out("block/creative_generator.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 4,\n    "interpolate": true\n  }\n}\n')


# ---------------------------------------------------------------------------
# GUI (256x256), dark-slate theme shared with Mycelial Power: outlined panel,
# recessed slots and bars, tick-marked vertical energy gauge, purple glow fills.
# Background is 176x193 at (0,0); sprites live in the free space around it:
#   (176,0)   4x 16x16 redstone mode icons     (176,64) 16x16 button / (192,64) hovered
#   (176,96)  12x64 energy fill                (0,194)  32x16 progress arrow fill
#   (138,194) 4x 12x12 gear frames             (186,194) 6x 8x8 status lamps
# ---------------------------------------------------------------------------
GUI = {
    "bg": hexc("#3a3d45"), "light": hexc("#5f6470"), "shadow": hexc("#202228"), "outline": hexc("#0f1013"),
    "inset_dark": hexc("#17181c"), "inset_light": hexc("#6a6f7c"), "slot": hexc("#2a2c33"),
    "panel": hexc("#1b1c21"), "gauge": hexc("#141519"), "tick": hexc("#2b2d35"),
    "white": hexc("#ffffff"),
}


def rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), c)


def panel(img, x0, y0, x1, y1):
    """Outlined slate window, bounds [x0, x1) x [y0, y1)."""
    rect(img, x0, y0, x1, y1, GUI["bg"])
    for x in range(x0 + 1, x1 - 1):
        img.putpixel((x, y0), GUI["outline"])
        img.putpixel((x, y1 - 1), GUI["outline"])
    for y in range(y0 + 1, y1 - 1):
        img.putpixel((x0, y), GUI["outline"])
        img.putpixel((x1 - 1, y), GUI["outline"])
    for x in range(x0 + 1, x1 - 2):
        img.putpixel((x, y0 + 1), GUI["light"])
    for y in range(y0 + 1, y1 - 2):
        img.putpixel((x0 + 1, y), GUI["light"])
    for x in range(x0 + 2, x1 - 1):
        img.putpixel((x, y1 - 2), GUI["shadow"])
    for y in range(y0 + 2, y1 - 1):
        img.putpixel((x1 - 2, y), GUI["shadow"])
    for (x, y) in [(x0, y0), (x1 - 1, y0), (x0, y1 - 1), (x1 - 1, y1 - 1)]:
        img.putpixel((x, y), CLEAR)


def inset(img, x0, y0, x1, y1, fill):
    """Recessed box, bounds [x0, x1) x [y0, y1)."""
    rect(img, x0, y0, x1, y1, fill)
    for x in range(x0, x1):
        img.putpixel((x, y0), GUI["inset_dark"])
    for y in range(y0, y1):
        img.putpixel((x0, y), GUI["inset_dark"])
    for x in range(x0 + 1, x1):
        img.putpixel((x, y1 - 1), GUI["inset_light"])
    for y in range(y0 + 1, y1):
        img.putpixel((x1 - 1, y), GUI["inset_light"])


def slot(img, item_x, item_y):
    inset(img, item_x - 1, item_y - 1, item_x + 17, item_y + 17, GUI["slot"])


def big_slot(img, item_x, item_y):
    inset(img, item_x - 5, item_y - 5, item_x + 21, item_y + 21, GUI["panel"])
    slot(img, item_x, item_y)


def sheep_icon(img, x, y, wool):
    """Tiny 7x5 sheep, the separator's counterpart to Mycelial Power's mushroom icons."""
    rows = [".www...", "wwwwwhh", "wwwwwhe", ".l.l.l.", ".l.l.l."]
    pal = {"w": wool, "h": hexc("#d9c3a6"), "e": hexc("#1a1a1a"), "l": hexc("#5a4632")}
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x + dx, y + dy), pal[ch])


def arrow_shape(fill, outline):
    """32x16 right-pointing arrow with an outline."""
    a = Image.new("RGBA", (32, 16), CLEAR)
    for y in range(5, 11):
        for x in range(1, 22):
            a.putpixel((x, y), fill)
    for i in range(8):
        for y in range(8 - i - 1, 8 + i + 1):
            if 0 <= y < 16:
                a.putpixel((22 + (7 - i), y), fill)
    px = a.load()
    o = Image.new("RGBA", (32, 16), CLEAR)
    for y in range(16):
        for x in range(32):
            if px[x, y][3] == 0:
                for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 32 and 0 <= ny < 16 and px[nx, ny][3]:
                        o.putpixel((x, y), outline)
                        break
    o.alpha_composite(a)
    return o


def generate_gui():
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)

    # Vertical energy gauge (outer 10..24 x 18..84, fill area 11..23 x 19..83 = 12x64) with tick marks.
    inset(img, 10, 18, 25, 84, GUI["gauge"])
    for ty in range(23, 83, 6):
        for x in range(19, 23):
            img.putpixel((x, ty), GUI["tick"])
    # Small energy bolt above the gauge
    for (x, y) in [(18, 9), (17, 10), (16, 11), (17, 11), (18, 11), (17, 12), (16, 13)]:
        img.putpixel((x, y), CARTOON["E"])

    # Process window behind slots and arrow
    inset(img, 34, 40, 152, 87, GUI["panel"])
    for x in range(36, 150):
        if x % 2 == 0:
            img.putpixel((x, 66), hexc("#22232a"))

    slot(img, 44, 56)          # input
    big_slot(img, 108, 47)     # primary output
    slot(img, 108, 69)         # secondary (string)
    slot(img, 130, 47)         # bonus
    # faint wool hint inside the input slot
    for (x, y) in [(49, 61), (50, 60), (51, 60), (52, 61), (53, 62), (52, 63), (50, 63), (49, 62)]:
        img.putpixel((x, y), hexc("#3d3f48"))
    # empty progress arrow
    img.alpha_composite(arrow_shape(hexc("#2c2e36"), hexc("#17181c")), (66, 56))
    # gear housing and status lamp housing
    inset(img, 75, 71, 89, 85, GUI["gauge"])
    inset(img, 8, 86, 18, 96, GUI["gauge"])

    # Player inventory (8,111) 3x9 and hotbar (8,169)
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)

    # Decorations, like the mushroom icons on the Mycelial generator GUI
    sheep_icon(img, 146, 5, hexc("#f2f2f2"))
    rnd = random.Random(7)
    for x in range(4, 172):
        if rnd.random() < 0.18:
            img.putpixel((x, 190), rnd.choice([hexc("#e3e1ea"), hexc("#b7b3c6"), hexc("#86c25a")]))

    # ---- sprites ----
    # Energy fill (176,96) 12x64, purple glow, brighter at the top
    for y in range(64):
        t = y / 63.0
        c = mix(hexc("#ff6ae6"), hexc("#5c2aa6"), t)
        for x in range(12):
            cc = mix(c, hexc("#ffd2fb"), 0.35) if x in (2, 3) else (shade(c, 0.75) if x >= 10 else c)
            if y % 6 == 4:
                cc = shade(cc, 0.85)
            img.putpixel((176 + x, 96 + y), cc)
    # Progress arrow fill (0,194)
    img.alpha_composite(arrow_shape(hexc("#c27cff"), hexc("#3a1a66")), (0, 194))
    hl = arrow_shape(hexc("#ffd2fb"), CLEAR)
    for y in range(16):
        for x in range(32):
            if y in (6, 7) and hl.getpixel((x, y))[3] and 2 <= x <= 20:
                img.putpixel((x, 194 + y), hexc("#ffd2fb"))
    # Gear frames (138,194) 4x 12x12 in brass
    for f in range(4):
        g = Image.new("RGBA", (12, 12), CLEAR)
        grid(GEAR_POSES[f % 2], CARTOON, g, 2, 2)
        if f >= 2:
            g = g.transpose(Image.FLIP_LEFT_RIGHT)
        img.alpha_composite(g, (138 + f * 12, 194))
    # Status lamps (186,194) 6x 8x8 in MachineStatus order, outlined cartoon bulbs
    lamp_colors = ["#8d97bc", "#86c25a", "#f2b24a", "#ff4b4b", "#ff4b4b", "#9a52f0"]
    bulb = ["..OOOO..", ".OHccSO.", "OHHcccSO", "OHccccSO", "OcccccSO", "OccccSSO", ".OSSSSO.", "..OOOO.."]
    for i, col in enumerate(lamp_colors):
        base = hexc(col)
        pal = {"O": CARTOON["O"], "c": base, "H": mix(base, hexc("#ffffff"), 0.6), "S": shade(base, 0.6)}
        grid(bulb, pal, img, 186 + i * 8, 194)

    # Redstone mode icons (176,0): IGNORED, HIGH, LOW, DISABLED
    def torch(ox, oy, lit):
        for y in range(7, 14):
            img.putpixel((ox + 7, oy + y), hexc("#6b4f2a"))
            img.putpixel((ox + 8, oy + y), hexc("#8a6a3a"))
        head = hexc("#ff4b4b") if lit else hexc("#5a1a14")
        for (x, y) in [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6)]:
            img.putpixel((ox + x, oy + y), head)
        if lit:
            for (x, y) in [(7, 3), (5, 5), (10, 5), (7, 2)]:
                img.putpixel((ox + x, oy + y), hexc("#ffb0a0"))

    for (x, y) in [(5, 10), (6, 9), (7, 9), (8, 9), (9, 9), (10, 10), (4, 11), (11, 11), (6, 10), (7, 10), (8, 10), (9, 10)]:
        img.putpixel((176 + x, y), hexc("#9aa3c4"))
    for (x, y) in [(6, 8), (8, 8), (10, 9)]:
        img.putpixel((176 + x, y), hexc("#dfe4f5"))
    torch(192, 0, True)
    torch(208, 0, False)
    torch(224, 0, False)
    for i in range(3, 13):
        img.putpixel((224 + i, i), hexc("#ff4b4b"))
        if i < 12:
            img.putpixel((224 + i + 1, i), hexc("#ff4b4b"))
    # Button backgrounds (176,64) normal, (192,64) hovered: small slate panels
    for bx, face in [(176, hexc("#4b505b")), (192, hexc("#5d6687"))]:
        rect(img, bx, 64, bx + 16, 80, face)
        for i in range(16):
            img.putpixel((bx + i, 64), GUI["light"] if bx == 176 else hexc("#8d97bc"))
            img.putpixel((bx, 64 + i), GUI["light"] if bx == 176 else hexc("#8d97bc"))
            img.putpixel((bx + i, 79), GUI["outline"])
            img.putpixel((bx + 15, 64 + i), GUI["outline"])
    img.save(out("gui/mechanical_separator.png"))


# ---------------------------------------------------------------------------
# JEI sheet (256x256)
# ---------------------------------------------------------------------------
def sheep_sprite(w, h, wool, face):
    """Side-view sheep of size w x h (32x24 or 16x16 scale)."""
    s = Image.new("RGBA", (32, 24), CLEAR)
    body = [
        "................................",
        "................................",
        "........oooooooooooooo..........",
        "......oo44444434444444oo........",
        ".....o4444344444443444444o......",
        "....o444444443444444444444o.....",
        "....o4434444444444434444444oooo.",
        "....o44444444444444444444444ffo.",
        "...o4444444344444444444444444ffo",
        "...o4434444444443444444444444eff",
        "...o44444444444444444444444ffffo",
        "...o444444444444444444444444ffo.",
        "....o4444443444444444444444fffo.",
        "....o444444444444434444444oooo..",
        ".....o44444444444444444444o.....",
        "......oo4444444444444444oo......",
        "........oooooooooooooooo........",
        ".........ll..ll....ll..ll.......",
        ".........ll..ll....ll..ll.......",
        ".........ll..ll....ll..ll.......",
        ".........hh..hh....hh..hh.......",
        "................................",
        "................................",
        "................................",
    ]
    pal = {"o": shade(wool, 0.55), "4": wool, "3": shade(wool, 0.82), "f": face, "e": hexc("#1A1A1A"),
           "l": hexc("#D9C3A6"), "h": hexc("#5A4632")}
    for y, row in enumerate(body):
        for x, ch in enumerate(row):
            if ch in pal:
                s.putpixel((x, y), pal[ch])
    return s


def generate_jei():
    img = Image.new("RGBA", (256, 256), CLEAR)
    # (0,16) 32x24 sheep illustration with a golden infused fleece and sparkles
    sheep = sheep_sprite(32, 24, hexc("#F2C230"), hexc("#E7D2B5"))
    for (x, y) in [(8, 5), (14, 9), (20, 6), (11, 12), (23, 11)]:
        sheep.putpixel((x, y), hexc("#FFFBE0"))
    img.alpha_composite(sheep, (0, 16))
    # (0,0) 16x16 icon: small sheep head + sparkle
    icon = Image.new("RGBA", (16, 16), CLEAR)
    small = sheep_sprite(32, 24, hexc("#C8C8C8"), hexc("#E7D2B5")).resize((16, 12), Image.NEAREST)
    icon.alpha_composite(small, (0, 3))
    for (x, y) in [(3, 1), (2, 2), (3, 2), (4, 2), (3, 3), (12, 1)]:
        icon.putpixel((x, y), hexc("#FFE45C"))
    img.alpha_composite(icon, (0, 0))
    # (32,16) energy empty 8x40, (40,16) energy full 8x40
    for y in range(40):
        for x in range(8):
            edge = x in (0, 7) or y in (0, 39)
            img.putpixel((32 + x, 16 + y), hexc("#17181c") if edge else hexc("#141519"))
            t = y / 39.0
            c = mix(hexc("#ff6ae6"), hexc("#5c2aa6"), t)
            img.putpixel((40 + x, 16 + y), hexc("#17181c") if edge else shade(c, 1.1 if x in (2, 3) else 1.0))
    jei_slate_sprites(img)
    img.save(out("gui/jei.png"))


def jei_arrow(fill, outline):
    """24x16 right-pointing arrow in the separator GUI's style (body 1..14, head 14..22)."""
    a = Image.new("RGBA", (24, 16), CLEAR)
    for y in range(5, 11):
        for x in range(1, 15):
            a.putpixel((x, y), fill)
    for i in range(8):
        for y in range(8 - i - 1, 8 + i + 1):
            a.putpixel((14 + (7 - i), y), fill)
    px = a.load()
    o = Image.new("RGBA", (24, 16), CLEAR)
    for y in range(16):
        for x in range(24):
            if px[x, y][3] == 0:
                for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 24 and 0 <= ny < 16 and px[nx, ny][3]:
                        o.putpixel((x, y), outline)
                        break
    o.alpha_composite(a)
    return o


def jei_slate_sprites(img):
    """
    Dark slate pieces so the JEI pages match the machine GUIs:
      (0,64)   200x20 Mechanical Separation row panel
      (0,88)   150x58 Resource Infusion panel
      (176,0)  18x18 slot frame        (196,0) 26x26 big (output) slot frame
      (176,32) 24x16 arrow, empty      (176,48) 24x16 arrow, filled (purple, animated in JEI)
      (204,32) 13x13 plus sign
    """
    panel(img, 0, 64, 200, 84)
    panel(img, 0, 88, 150, 146)
    # Infusion panel decorations: a recessed strip behind the text, and the wool/grass fringe of the machine GUIs.
    inset(img, 4, 119, 146, 144, GUI["panel"])
    rnd = random.Random(11)
    for x in range(3, 147):
        if rnd.random() < 0.18:
            img.putpixel((x, 145), rnd.choice([hexc("#e3e1ea"), hexc("#b7b3c6"), hexc("#86c25a")]))
    slot(img, 177, 1)                                   # 18x18 frame at (176,0)
    inset(img, 196, 0, 222, 26, GUI["panel"])            # 26x26 big frame at (196,0)
    inset(img, 200, 4, 218, 22, GUI["slot"])
    img.alpha_composite(jei_arrow(hexc("#2c2e36"), hexc("#17181c")), (176, 32))
    full = jei_arrow(hexc("#c27cff"), hexc("#3a1a66"))
    for x in range(2, 14):
        for y in (6, 7):
            full.putpixel((x, y), hexc("#ffd2fb"))
    img.alpha_composite(full, (176, 48))
    for i in range(13):                                 # plus sign, slate highlight on outline
        for (x, y) in [(i, 5), (i, 6), (i, 7), (5, i), (6, i), (7, i)]:
            img.putpixel((204 + x, 32 + y), hexc("#8d97bc"))
    for i in range(1, 12):
        img.putpixel((204 + i, 32 + 6), hexc("#dfe4f5"))
        img.putpixel((204 + 6, 32 + i), hexc("#dfe4f5"))


# ---------------------------------------------------------------------------
# Mod list icon (square) and info-panel banner, same cartoon palette as the
# blocks. Drawn at low resolution and scaled with nearest-neighbour so they stay
# crisp pixel art (mods.toml sets iconBlur = false).
# ---------------------------------------------------------------------------
def _sheep_head(size=32):
    """Front-facing infused sheep: gold fleece, cream face, outlined."""
    img = Image.new("RGBA", (size, size), CLEAR)
    gold, gold_hi, gold_sh = hexc("#F2C230"), hexc("#FFE58A"), hexc("#B8891A")
    face, face_sh = hexc("#E7D2B5"), hexc("#C9B090")
    shape = {}

    def disc(cx, cy, r, color):
        for y in range(size):
            for x in range(size):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    shape[(x, y)] = color

    for (cx, cy, r) in [(10, 12, 5), (16, 9, 6), (22, 12, 5), (8, 18, 5), (24, 18, 5), (16, 21, 7), (11, 23, 4), (21, 23, 4)]:
        disc(cx, cy, r, gold)
    # ears
    for (cx, cy) in [(6, 15), (26, 15)]:
        for y in range(cy - 1, cy + 2):
            for x in range(cx - 2, cx + 3):
                shape[(x, y)] = face_sh
    # face (rounded rectangle)
    for y in range(13, 26):
        for x in range(12, 21):
            if (y in (13, 25)) and x in (12, 20):
                continue
            shape[(x, y)] = face
    for (x, y), c in shape.items():
        img.putpixel((x, y), c)
    # fleece shading: highlight up-left edges, shadow down-right edges
    for (x, y), c in shape.items():
        if c != gold:
            continue
        if (x - 1, y - 1) not in shape:
            img.putpixel((x, y), gold_hi)
        elif (x + 1, y + 1) not in shape:
            img.putpixel((x, y), gold_sh)
    # curls on the fleece
    for (x, y) in [(9, 10), (15, 6), (22, 9), (7, 18), (25, 20), (14, 27), (19, 27)]:
        if (x, y) in shape and shape[(x, y)] == gold:
            img.putpixel((x, y), gold_sh)
            if (x + 1, y) in shape and shape[(x + 1, y)] == gold:
                img.putpixel((x + 1, y), gold_hi)
    # face shading, eyes, nose
    for y in range(14, 25):
        img.putpixel((20, y), face_sh)
    for (ex, ey) in [(13, 17), (18, 17)]:
        img.putpixel((ex, ey), hexc("#1a1220"))
        img.putpixel((ex + 1, ey), hexc("#1a1220"))
        img.putpixel((ex, ey + 1), hexc("#1a1220"))
        img.putpixel((ex + 1, ey + 1), hexc("#1a1220"))
        img.putpixel((ex, ey), hexc("#ffffff"))
    for (x, y) in [(15, 21), (16, 21), (15, 22), (16, 22)]:
        img.putpixel((x, y), hexc("#d98a8a"))
    img.putpixel((15, 23), hexc("#8a4a4a"))
    img.putpixel((16, 23), hexc("#8a4a4a"))
    # outline around the whole silhouette
    outline = []
    for y in range(size):
        for x in range(size):
            if (x, y) in shape:
                continue
            if any((x + dx, y + dy) in shape for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]):
                outline.append((x, y))
    for p in outline:
        if 0 <= p[0] < size and 0 <= p[1] < size:
            img.putpixel(p, CARTOON["O"])
    return img


def _star(img, x, y, c, glow):
    img.putpixel((x, y), c)
    for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
        img.putpixel((x + dx, y + dy), glow)


def _plate(w, h):
    """Outlined slate plate with highlight/shadow rims and corner bolts, like the block faces."""
    img = Image.new("RGBA", (w, h), CARTOON["M"])
    for x in range(w):
        img.putpixel((x, 0), CARTOON["O"]); img.putpixel((x, h - 1), CARTOON["O"])
        img.putpixel((x, 1), CARTOON["H"]); img.putpixel((x, h - 2), CARTOON["S"])
    for y in range(h):
        img.putpixel((0, y), CARTOON["O"]); img.putpixel((w - 1, y), CARTOON["O"])
        img.putpixel((1, y), CARTOON["H"]); img.putpixel((w - 2, y), CARTOON["S"])
    for (x, y) in [(3, 3), (w - 5, 3), (3, h - 5), (w - 5, h - 5)]:
        img.putpixel((x, y), CARTOON["B"]); img.putpixel((x + 1, y), CARTOON["b"])
        img.putpixel((x, y + 1), CARTOON["b"]); img.putpixel((x + 1, y + 1), CARTOON["S"])
    for (x, y) in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
        img.putpixel((x, y), CLEAR)
    return img


def _glow(img, cx, cy, r):
    for y in range(img.height):
        for x in range(img.width):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                img.putpixel((x, y), mix(CARTOON["P"], CARTOON["M"], min(1.0, d / r) ** 1.5))


def generate_logo():
    # Square icon: 36x36 art, scaled x4 -> 144x144
    icon = _plate(36, 36)
    _glow(icon, 18, 18, 15)
    icon.alpha_composite(_sheep_head(32), (2, 2))
    _star(icon, 5, 6, hexc("#ffffff"), hexc("#ffe58a"))
    _star(icon, 30, 29, hexc("#ffffff"), hexc("#ff9af0"))
    icon.resize((144, 144), Image.NEAREST).save(os.path.join(ROOT, "..", "..", "..", "icon.png"))

    # Banner: 120x40 art, scaled x4 -> 480x160. Sheep, an arrow of wool, and the separator.
    banner = _plate(120, 40)
    _glow(banner, 22, 20, 17)
    banner.alpha_composite(_sheep_head(32), (6, 4))
    tex = os.path.join(ROOT)
    woolx = 44
    for i, name in enumerate(["iron", "gold", "diamond"]):
        w = Image.open(os.path.join(tex, "item", f"{name}_wool.png")).convert("RGBA")
        banner.alpha_composite(w, (woolx + i * 13, 12 + (4 if i == 1 else 0)))
    # little arrow toward the machine
    for x in range(85, 91):
        banner.putpixel((x, 20), CARTOON["G"])
    for i in range(3):
        banner.putpixel((90 - i, 20 - i - 1), CARTOON["G"]); banner.putpixel((90 - i, 20 + i + 1), CARTOON["G"])
    front = machine_front_preview("mechanical_separator")
    banner.alpha_composite(front.resize((24, 24), Image.NEAREST), (93, 8))
    for (x, y) in [(40, 7), (78, 31), (112, 6)]:
        _star(banner, x, y, hexc("#ffffff"), hexc("#ff9af0"))
    banner.resize((480, 160), Image.NEAREST).save(os.path.join(ROOT, "..", "..", "..", "banner.png"))


# ---------------------------------------------------------------------------
# Automatic Shears: same cartoon metal casing, a pair of open shears behind the front window.
# ---------------------------------------------------------------------------
SHEARS_PAL = dict(CARTOON)
SHEARS_PAL.update({
    "L": hexc("#f4f8fc"), "l": hexc("#b9c3d1"), "d": hexc("#6f7a8b"),  # blade shine / blade / blade edge
    "Q": hexc("#e0443e"), "q": hexc("#8e2028"),                      # handle / handle shadow
    "X": hexc("#ffd75e"),                                            # brass pivot
})

SHEARS_SIDE = [  # storage hatch with a latch
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMOOOOOOOOOOMSO",
    "OHMOHHHHHHHHOMSO",
    "OHMOHMMMMMMSOMSO",
    "OHMOHMMXXMMSOMSO",
    "OHMOHMMXXMMSOMSO",
    "OHMOHMMMMMMSOMSO",
    "OHMOSSSSSSSSOMSO",
    "OHMOOOOOOOOOOMSO",
    "OHwWwMMMMMMwWwSO",
    "OwWWWwMwMMwWWWwO",
    "OvwWwwvWwvwwWwvO",
    "OOOOOOOOOOOOOOOO",
]

SHEARS_TOP = [  # lid with a blade emblem
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMMMMMMMMMMMMSO",
    "OHMMLMMMMMMLMMSO",
    "OHMMlLMMMMLlMMSO",
    "OHMMMdlMMldMMMSO",
    "OHMMMMdXXdMMMMSO",
    "OHMMMMqXXqMMMMSO",
    "OHMMMqQMMQqMMMSO",
    "OHMMqQMMMMQqMMSO",
    "OHMMMMMMMMMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]


def generate_automatic_shears():
    grid(SEP_FRONT_FRAME, CARTOON).save(out("block/automatic_shears_front.png"))
    # pink halo, like the generator's
    save_chambers("automatic_shears", SHEARS_ICON, SHEARS_IDLE, SHEARS_LIT, hexc("#ffd6de"), hexc("#ff7a92"))
    grid(SHEARS_SIDE, SHEARS_PAL).save(out("block/automatic_shears_side.png"))
    grid(SHEARS_TOP, SHEARS_PAL).save(out("block/automatic_shears_top.png"))
    grid(SEP_BOTTOM, CARTOON).save(out("block/automatic_shears_bottom.png"))   # output chute, like the separator
    grid(SHEARS_SIDE, SHEARS_PAL).save(out("block/automatic_shears_back.png"))


# ---------------------------------------------------------------------------
# Automatic Shears GUI (256x256), same slate theme and sprite positions as the separator's sheet.
# ---------------------------------------------------------------------------
SHEARS_OPEN = [
    "................",
    ".L............L.",
    ".lL..........Ll.",
    "..lL........Ll..",
    "...dL......Ld...",
    "....dL....Ld....",
    ".....dl..ld.....",
    "......dXXd......",
    "......qXXq......",
    ".....qQ..Qq.....",
    "....qQ....Qq....",
    "...QQq....qQQ...",
    "..Q..Q....Q..Q..",
    "..Q..Q....Q..Q..",
    "...QQ......QQ...",
    "................",
]
SHEARS_CLOSED = [
    "................",
    ".......LL.......",
    ".......Ll.......",
    ".......ll.......",
    ".......ld.......",
    ".......ld.......",
    ".......dd.......",
    "......dXXd......",
    "......qXXq......",
    ".....qQ..Qq.....",
    "....qQ....Qq....",
    "...QQq....qQQ...",
    "..Q..Q....Q..Q..",
    "..Q..Q....Q..Q..",
    "...QQ......QQ...",
    "................",
]


def generate_shears_gui():
    for g in (SHEARS_OPEN, SHEARS_CLOSED):
        assert len(g) == 16 and all(len(r) == 16 for r in g)
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)

    # Energy gauge, exactly like the separator's (fill area 11..23 x 19..83), with its bolt.
    inset(img, 10, 18, 25, 84, GUI["gauge"])
    for ty in range(23, 83, 6):
        for x in range(19, 23):
            img.putpixel((x, ty), GUI["tick"])
    for (x, y) in [(18, 9), (17, 10), (16, 11), (17, 11), (18, 11), (17, 12), (16, 13)]:
        img.putpixel((x, y), CARTOON["E"])

    # Machine window: shears icon, next-sweep bar and energy readout on the left, 3x3 storage on the right.
    inset(img, 32, 16, 152, 78, GUI["panel"])
    inset(img, 39, 21, 59, 41, GUI["gauge"])      # icon box (icon drawn at 41,23)
    inset(img, 38, 46, 90, 52, GUI["gauge"])      # next-sweep bar (fill 39,47 50x4)
    for x in range(61, 89, 3):                     # little "3x3 area" dots beside the icon
        for y in (27, 31, 35):
            if x < 70:
                img.putpixel((x, y), hexc("#8d97bc"))
    for row in range(3):
        for col in range(3):
            slot(img, 96 + col * 18, 20 + row * 18)
    inset(img, 8, 86, 18, 96, GUI["gauge"])       # status lamp housing

    # Player inventory (8,111) 3x9 and hotbar (8,169)
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)

    # Decorations shared with the separator: the little sheep and the wool/grass fringe.
    sheep_icon(img, 146, 5, hexc("#f2f2f2"))
    rnd = random.Random(7)
    for x in range(4, 172):
        if rnd.random() < 0.18:
            img.putpixel((x, 190), rnd.choice([hexc("#e3e1ea"), hexc("#b7b3c6"), hexc("#86c25a")]))

    # ---- sprites ----
    # Shared with the separator: redstone icons + button faces + energy fill (x 176+), status lamps (186,194).
    img.paste(sep.crop((176, 0, 256, 194)), (176, 0))
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))
    # Shears icon frames (0,194) open, (16,194) closed.
    grid(SHEARS_OPEN, SHEARS_PAL, img, 0, 194)
    grid(SHEARS_CLOSED, SHEARS_PAL, img, 16, 194)
    # Next-sweep bar fill (0,212) 50x4: brushed steel with a bright top edge.
    for x in range(50):
        for y in range(4):
            c = mix(hexc("#8d97bc"), hexc("#f4f8fc"), x / 49.0 * 0.6 + (0.4 if y == 0 else 0.0))
            if y == 3:
                c = shade(c, 0.7)
            img.putpixel((x, 212 + y), c)
    img.save(out("gui/automatic_shears.png"))


# ---------------------------------------------------------------------------
# Fluid Reservoir: the same cartoon casing, with a glass window on each side showing how full it is (fill 0..4),
# a purple-ringed water port on top and a water intake grate underneath.
# ---------------------------------------------------------------------------
RES_PAL = dict(CARTOON)
RES_PAL.update({
    "A": hexc("#bff0ff"),   # water surface highlight
    "U": hexc("#3f8cf0"),   # water
    "u": hexc("#2a5fc0"),   # deep water
    "Z": hexc("#1b2233"),   # empty glass
    "z": hexc("#4a5a78"),   # glass glint
})

RES_SIDE = [  # '.' is the 8x8 window, filled in by reservoir_side()
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbOOOOOOOOOObSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHbOOOOOOOOOObSO",
    "OHBbPPPPPPPPBbSO",
    "OHbSppppppppbSSO",
    "OOOOOOOOOOOOOOOO",
]

RES_TOP = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMOOOOMMbSSO",
    "OHMMMOPPPPOMMMSO",
    "OHMMOPpUUpPOMMSO",
    "OHMOPpUAUUpPOMSO",
    "OHMOPUAUUUuPOMSO",
    "OHMOPUUUUUuPOMSO",
    "OHMOPpUUUupPOMSO",
    "OHMMOPpuupPOMMSO",
    "OHMMMOPPPPOMMMSO",
    "OHMMMMOOOOMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

RES_BOTTOM = [row.replace("K", "U").replace("k", "u") for row in SEP_BOTTOM]  # intake grate over the water


def reservoir_side(fill):
    img = grid(RES_SIDE, RES_PAL)
    water_rows = fill * 2                      # 0, 2, 4, 6 or 8 of the 8 window rows
    for y in range(4, 12):
        for x in range(4, 12):
            depth = y - (12 - water_rows)      # 0 at the surface row
            if depth < 0:
                c = RES_PAL["Z"]
            elif depth == 0:
                c = RES_PAL["A"] if (x + fill) % 3 else RES_PAL["U"]
            elif y == 11:
                c = RES_PAL["u"]
            else:
                c = RES_PAL["U"] if (x * 3 + y) % 7 else RES_PAL["u"]
            img.putpixel((x, y), c)
    # glass glint in the top-left corner (over glass or water alike)
    for (x, y) in ((5, 5), (4, 6), (6, 5)):
        if water_rows < 12 - y:
            img.putpixel((x, y), RES_PAL["z"])
        else:
            img.putpixel((x, y), RES_PAL["A"])
    return img


def generate_fluid_reservoir():
    for rows in (RES_SIDE, RES_TOP, RES_BOTTOM):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows)
    for fill in range(5):
        reservoir_side(fill).save(out(f"block/fluid_reservoir_side_{fill}.png"))
    grid(RES_TOP, RES_PAL).save(out("block/fluid_reservoir_top.png"))
    grid(RES_BOTTOM, RES_PAL).save(out("block/fluid_reservoir_bottom.png"))


WATER_DROP = [  # 7x9 GUI icon
    "...O...",
    "..OAO..",
    "..OUO..",
    ".OAUUO.",
    ".OAUUO.",
    "OAUUUuO",
    "OUUUUuO",
    ".OUuuO.",
    "..OOO..",
]


def generate_reservoir_gui():
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)

    # Water tank (fill area 11..40 x 19..82, 30x64) with a tick mark for every bucket of the 10.
    inset(img, 10, 18, 42, 84, GUI["gauge"])
    for k in range(1, 10):
        ty = 19 + round(64 * k / 10)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    # Info window, bucket slots with an arrow between them, status lamp housing.
    inset(img, 48, 16, 124, 78, GUI["panel"])
    slot(img, 134, 22)
    slot(img, 134, 56)
    for i, y in enumerate(range(42, 52)):
        for x in range(140, 146):
            img.putpixel((x, y), hexc("#8d97bc"))
    for i in range(5):
        for x in range(138 + i, 148 - i):
            img.putpixel((x, 50 + i), hexc("#8d97bc"))
    for y in range(42, 50):
        img.putpixel((139, y), GUI["outline"]); img.putpixel((146, y), GUI["outline"])
    inset(img, 8, 86, 18, 96, GUI["gauge"])

    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)

    grid(WATER_DROP, RES_PAL, img, 150, 4)
    rnd = random.Random(11)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice([RES_PAL["A"], RES_PAL["U"], RES_PAL["u"]]))

    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))      # status lamps, shared with the other machines
    # Water fill (176,0) 30x64: bright near the top, deeper below, with the bucket ticks showing through.
    for y in range(64):
        for x in range(30):
            t = y / 63.0
            c = mix(hexc("#5fb0ff"), hexc("#1f4fae"), t)
            if (x * 5 + y * 3) % 23 == 0:
                c = mix(c, hexc("#bff0ff"), 0.35)
            img.putpixel((176 + x, y), c)
    for k in range(1, 10):
        ty = round(64 * k / 10)
        for x in range(25, 30):
            img.putpixel((176 + x, ty), mix(img.getpixel((176 + x, ty)), hexc("#dff6ff"), 0.45))
    # Surface shimmer (176,66), 4 frames of 30x2: a light band with sparkles drifting right.
    for f in range(4):
        for x in range(30):
            top = mix(hexc("#bff0ff"), hexc("#ffffff"), 1.0 if (x + f * 2) % 8 == 0 else 0.0)
            img.putpixel((176 + x, 66 + f * 2), top)
            img.putpixel((176 + x, 67 + f * 2), hexc("#7cc8ff"))
    img.save(out("gui/fluid_reservoir.png"))


# ---------------------------------------------------------------------------
# Hume Energy Cube: like the Fluid Reservoir, a glass window on each side shows how full the group is, here in
# eight steps (one per window row) with the energy animated: it swirls upward and a bright surface line shimmers.
# The top and bottom carry a purple port with the energy bolt.
# ---------------------------------------------------------------------------
CUBE_LEVELS = 8
CUBE_FRAMES = 4

CUBE_SIDE = [  # '.' is the 8x8 window
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbPPPPPPPPBbSO",
    "OHbOOOOOOOOOObSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHPO........OpSO",
    "OHbOOOOOOOOOObSO",
    "OHBbppppppppBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

CUBE_TOP = [  # purple port with the energy bolt
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMOOOOMMbSSO",
    "OHMMMOPPPPOMMMSO",
    "OHMMOPpgEepPOMSO",
    "OHMOPpgEEgpPOMSO",
    "OHMOPgEEEEEPOMSO",
    "OHMOPggeEEgPOMSO",
    "OHMOPpgEEgpPOMSO",
    "OHMMOPpEegpPOMSO",
    "OHMMMOPPPPOMMMSO",
    "OHMMMMOOOOMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

ENERGY_DEEP, ENERGY_MID, ENERGY_LIGHT = hexc("#5c2aa6"), hexc("#9a52f0"), hexc("#ff6ae6")
ENERGY_SURFACE, ENERGY_SPARK = hexc("#ffd2fb"), hexc("#ffffff")


def cube_side_frame(level, frame):
    img = grid(CUBE_SIDE, CARTOON)
    for y in range(4, 12):
        for x in range(4, 12):
            depth = y - (12 - level)          # 0 at the surface row
            if depth < 0:
                c = CARTOON["K"] if (x + y) % 5 else CARTOON["k"]
            elif depth == 0:
                c = ENERGY_SPARK if (x + frame * 2) % 8 == 0 else ENERGY_SURFACE
            else:
                # diagonal bands that drift upward one row per frame
                band = (x + y + frame) % 4
                c = (ENERGY_LIGHT, ENERGY_MID, ENERGY_MID, ENERGY_DEEP)[band]
            img.putpixel((x, y), c)
    # glass glint, over the empty glass only
    for (x, y) in ((5, 5), (4, 6), (6, 5)):
        if y < 12 - level:
            img.putpixel((x, y), hexc("#4a3d5c"))
    return img


def generate_energy_cube():
    for rows in (CUBE_SIDE, CUBE_TOP):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows)
    for name in os.listdir(out("block")):
        if name.startswith("hume_energy_cube_"):
            os.remove(out("block/" + name))
    for level in range(CUBE_LEVELS + 1):
        strip = Image.new("RGBA", (16, 16 * CUBE_FRAMES), CLEAR)
        for f in range(CUBE_FRAMES):
            strip.paste(cube_side_frame(level, f), (0, f * 16))
        strip.save(out(f"block/hume_energy_cube_side_{level}.png"))
        with open(out(f"block/hume_energy_cube_side_{level}.png.mcmeta"), "w") as fh:
            fh.write('{\n  "animation": {\n    "frametime": 4,\n    "interpolate": true\n  }\n}\n')
    grid(CUBE_TOP, CARTOON).save(out("block/hume_energy_cube_top.png"))


def generate_cube_gui():
    """Hume Energy Cube GUI: the Fluid Reservoir's layout with an energy gauge instead of water, and no slots."""
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    # Energy gauge (fill 11..40 x 19..82, 30x64) with a mark for every eighth, like the cube's window rows.
    inset(img, 10, 18, 42, 84, GUI["gauge"])
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 48, 16, 168, 78, GUI["panel"])        # info window
    for x in range(52, 164):                          # divider between totals and flow
        img.putpixel((x, 54), hexc("#2c2e36"))
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    grid(CUBE_BOLT_ICON, CARTOON, img, 152, 4)
    rnd = random.Random(13)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice([ENERGY_SURFACE, ENERGY_LIGHT, ENERGY_MID]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))      # status lamps shared with the other machines
    for y in range(64):                                         # energy fill (176,0) 30x64
        for x in range(30):
            c = mix(ENERGY_LIGHT, ENERGY_DEEP, y / 63.0)
            if (x + y) % 4 == 0:
                c = mix(c, ENERGY_SURFACE, 0.25)
            img.putpixel((176 + x, y), c)
    for k in range(1, 8):
        ty = round(64 * k / 8)
        for x in range(25, 30):
            img.putpixel((176 + x, ty), mix(img.getpixel((176 + x, ty)), ENERGY_SURFACE, 0.5))
    for f in range(4):                                          # surface shimmer (176,66), 4 frames of 30x2
        for x in range(30):
            img.putpixel((176 + x, 66 + f * 2), ENERGY_SPARK if (x + f * 2) % 8 == 0 else ENERGY_SURFACE)
            img.putpixel((176 + x, 67 + f * 2), ENERGY_LIGHT)
    img.save(out("gui/hume_energy_cube.png"))


CUBE_BOLT_ICON = [  # 9x9 GUI icon
    "....OOO..",
    "...OEEO..",
    "..OEEO...",
    ".OEEEOOO.",
    "OEEEEEEO.",
    ".OOOEEO..",
    "...OEO...",
    "..OEO....",
    "..OO.....",
]


def main():
    generate_wool_items()
    generate_sheep_fur()
    generate_block()
    generate_creative_generator()
    generate_automatic_shears()
    generate_gui()
    generate_shears_gui()
    generate_fluid_reservoir()
    generate_reservoir_gui()
    generate_energy_cube()
    generate_cube_gui()
    generate_jei()
    generate_logo()
    print("Textures written to", os.path.abspath(ROOT))


if __name__ == "__main__":
    main()
