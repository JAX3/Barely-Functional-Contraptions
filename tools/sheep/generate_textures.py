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


MACHINE_BLOCK = [  # bare blue-steel casing: the machines' frame and corner bolts around an empty, unlit port socket
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMMSOOOOOOSMMSO",
    "OHMMOkkkkkkOMMSO",
    "OHMMOkpPPpkOMMSO",
    "OHMMOkPKKPkOMMSO",
    "OHMMOkPKKPkOMMSO",
    "OHMMOkpPPpkOMMSO",
    "OHMMOkkkkkkOMMSO",
    "OHMMHOOOOOOHMMSO",
    "OHMMMMMMMMMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]


def generate_machine_block():
    assert len(MACHINE_BLOCK) == 16 and all(len(r) == 16 for r in MACHINE_BLOCK)
    grid(MACHINE_BLOCK, CARTOON).save(out("block/functional_machine_block.png"))


# ---------------------------------------------------------------------------
# Zero Point Energy Module: a 4px blue-steel base plate with a crystal emitter on top. Each texture comes "off"
# (dull, unlinked) and "on" (bright blue, linked; the emitter pulses). The model only uses parts of each 16x16:
#   plate: whole texture is the plate's top face (the emitter hides the middle 8x8)
#   plate_side: rows 12-15 are the plate's edge
#   emitter: rows 0-3, columns 4-11 are the emitter's sides; the middle 8x8 is its top
# ---------------------------------------------------------------------------
ZPE_PLATE = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSXXXXXXXXbSSO",
    "OHMXOOOOOOOOXMSO",
    "OHMXOkkkkkkOXMSO",
    "OHMXOkkkkkkOXMSO",
    "OHMXOkkkkkkOXMSO",
    "OHMXOkkkkkkOXMSO",
    "OHMXOkkkkkkOXMSO",
    "OHMXOkkkkkkOXMSO",
    "OHMXOOOOOOOOXMSO",
    "OHMSXXXXXXXXSMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]
ZPE_PLATE_SIDE = ["OHHHHHHHHHHHHHMO"] * 12 + [
    "OOOOOOOOOOOOOOOO",
    "OBbMMMXXXXMMMBbO",
    "OSSSSSSSSSSSSSSO",
    "OOOOOOOOOOOOOOOO",
]
ZPE_EMITTER = [
    "....OOOOOOOO....",
    "....OnQQQQnO....",
    "....ONnnnnNO....",
    "....OOOOOOOO....",
    "....OOOOOOOO....",
    "....ONnnnnNO....",
    "....OnQQQQnO....",
    "....OnQqqQnO....",
    "....OnQqqQnO....",
    "....OnQQQQnO....",
    "....ONnnnnNO....",
    "....OOOOOOOO....",
    "................",
    "................",
    "................",
    "................",
]
ZPE_OFF = {"X": hexc("#2a2238"), "N": hexc("#2a2238"), "n": hexc("#3f4766"), "Q": hexc("#5d6687"), "q": hexc("#8d97bc")}
ZPE_ON = {"X": hexc("#3fc8ff"), "N": hexc("#1b5fa8"), "n": hexc("#3fc8ff"), "Q": hexc("#a8ecff"), "q": hexc("#ffffff")}


def generate_zero_point_module():
    for rows in (ZPE_PLATE, ZPE_PLATE_SIDE, ZPE_EMITTER):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows)
    base = "block/zero_point_energy_module/"
    for state, extra in (("off", ZPE_OFF), ("on", ZPE_ON)):
        pal = dict(CARTOON, **extra)
        grid(ZPE_PLATE, pal).save(out(base + f"plate_{state}.png"))
        grid(ZPE_PLATE_SIDE, pal).save(out(base + f"plate_side_{state}.png"))
    grid(ZPE_EMITTER, dict(CARTOON, **ZPE_OFF)).save(out(base + "emitter_off.png"))
    # Linked: the emitter pulses between bright blue and white.
    strip = Image.new("RGBA", (16, 64), CLEAR)
    for f, t in enumerate([0.0, 0.4, 0.8, 0.4]):
        pal = dict(CARTOON, **ZPE_ON)
        pal["n"] = mix(hexc("#3fc8ff"), hexc("#a8ecff"), t)
        pal["Q"] = mix(hexc("#a8ecff"), hexc("#ffffff"), t)
        strip.paste(grid(ZPE_EMITTER, pal), (0, f * 16))
    strip.save(out(base + "emitter_on.png"))
    with open(out(base + "emitter_on.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": true\n  }\n}\n')


# ---------------------------------------------------------------------------
# Energy Cable: a 4px blue-steel tube with purple collars, in the machines' colours: a 6px junction box in the middle
# with a purple port ring and pink glow like the machines' ports, and an 8px plug plate (the same port) where it meets
# a machine. The models use automatic UVs, so each texture is drawn where its element sits in the block:
#   cable: the tube runs along columns 6-9 and rows 6-9 (arms on any axis land on one of the bands); the 6x6 junction
#          box is at 5..10. Collars at 2 and 13 mark the arm ends.
#   cable_plug: the plate's face is the middle 8x8 (4..11); its 2px edges are the strips round the border.
# ---------------------------------------------------------------------------
CABLE_CORE = [
    "OOOOOO",
    "OHPPMO",
    "OPGgpO",
    "OPggpO",
    "OMppSO",
    "OOOOOO",
]
CABLE_PLUG_FACE = [
    "OOOOOOOO",
    "OHHHHHMO",
    "OHPPPPSO",
    "OHPGgpSO",
    "OHPggpSO",
    "OHppppSO",
    "OMSSSSSO",
    "OOOOOOOO",
]


def generate_conduit(name, pal):
    img = Image.new("RGBA", (16, 16), CLEAR)
    across = "OHSO"
    collar = "OPpO"
    for i in range(16):
        pattern = collar if i in (2, 13) else across
        for k, ch in enumerate(pattern):
            img.putpixel((6 + k, i), pal[ch])   # vertical band
            img.putpixel((i, 6 + k), pal[ch])   # horizontal band
    grid(CABLE_CORE, pal, img, 5, 5)
    img.save(out(f"block/{name}/{name.split('_')[1]}.png"))
    plug = Image.new("RGBA", (16, 16), CLEAR)
    edge = "OMMMMMMO"
    for k, ch in enumerate(edge):
        for d in (0, 1, 14, 15):
            c = pal["O"] if d in (0, 15) else pal[ch]
            plug.putpixel((4 + k, d), c)  # top and bottom strips
            plug.putpixel((d, 4 + k), c)  # left and right strips
    grid(CABLE_PLUG_FACE, pal, plug, 4, 4)
    plug.save(out(f"block/{name}/{name.split('_')[1]}_plug.png"))



# Fluid Pipe: the same tube in blue steel, with blue collars and a blue port ring with a white-blue glow instead of the
# machines' purple and pink.
PIPE_BLUE = {"P": hexc("#3fc8ff"), "p": hexc("#1b5fa8"), "G": hexc("#ffffff"), "g": hexc("#a8ecff")}


# Item Pipe: the same tube with yellow collars and a yellow port ring with a pale gold glow.
PIPE_YELLOW = {"P": hexc("#ffe14a"), "p": hexc("#c98a12"), "G": hexc("#ffffff"), "g": hexc("#ffe58a")}


def generate_energy_cable():
    generate_conduit("energy_cable", CARTOON)
    generate_conduit("fluid_pipe", dict(CARTOON, **PIPE_BLUE))
    generate_conduit("item_pipe", dict(CARTOON, **PIPE_YELLOW))

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


# 5x7 pixel font for the banner title (only the letters it needs).
FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    " ": ["000", "000", "000", "000", "000", "000", "000"],
}


def _text(img, x, y, text, scale, top, bottom, shadow=None):
    """Pixel-font text with a dark outline, a vertical colour gradient and an optional drop shadow."""
    w = sum((len(FONT[c][0]) + 1) * scale for c in text) - scale
    h = 7 * scale
    layer = Image.new("RGBA", (w + 4, h + 4), CLEAR)
    cx = 2
    for c in text:
        glyph = FONT[c]
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    for sy in range(scale):
                        for sx in range(scale):
                            layer.putpixel((cx + gx * scale + sx, 2 + gy * scale + sy),
                                           mix(top, bottom, (gy * scale + sy) / max(1, h - 1)))
        cx += (len(glyph[0]) + 1) * scale
    filled = {(px, py) for py in range(layer.height) for px in range(layer.width) if layer.getpixel((px, py))[3]}
    for (px, py) in [(px + dx, py + dy) for (px, py) in filled for dx in (-1, 0, 1) for dy in (-1, 0, 1)]:
        if (px, py) not in filled and 0 <= px < layer.width and 0 <= py < layer.height:
            layer.putpixel((px, py), CARTOON["O"])
    if shadow:
        sh = Image.new("RGBA", layer.size, CLEAR)
        for py in range(layer.height):
            for px in range(layer.width):
                if layer.getpixel((px, py))[3]:
                    sh.putpixel((px, py), shadow)
        img.alpha_composite(sh, (x - 2 + 1, y - 2 + 2))
    img.alpha_composite(layer, (x - 2, y - 2))
    return w


def _logo_emblem():
    """64x64: the four contraptions in a 2x2 machine grid with the crowbar wrench across them."""
    tex = ROOT
    def face(front, chamber, frame=0):
        img = Image.open(os.path.join(tex, "block", front + ".png")).convert("RGBA").crop((0, 0, 16, 16))
        ch = Image.open(os.path.join(tex, "block", chamber + ".png")).convert("RGBA").crop((3, 3 + 16 * frame, 13, 13 + 16 * frame))
        img.alpha_composite(ch, (3, 3))
        return img
    tiles = [
        face("generator_front", "generator_chamber_on", 2),
        face("mechanical_separator_front", "mechanical_separator_chamber_on", 2),
        face("automatic_shears_front", "automatic_shears_chamber_on", 2),
        Image.open(os.path.join(tex, "block", "hume_energy_cube", "side_0000_6.png")).convert("RGBA").crop((0, 0, 16, 16)),
    ]
    emblem = Image.new("RGBA", (64, 64), CLEAR)
    for i, t in enumerate(tiles):
        emblem.alpha_composite(t.resize((32, 32), Image.NEAREST), ((i % 2) * 32, (i // 2) * 32))
    # The crowbar across the middle, where the four machines meet, so each machine's window stays visible.
    crowbar = Image.open(os.path.join(tex, "item", "mycelium_wrench.png")).convert("RGBA").resize((32, 32), Image.NEAREST)
    halo = Image.new("RGBA", (36, 36), CLEAR)   # a dark halo so it reads against the machines
    for y in range(32):
        for x in range(32):
            if crowbar.getpixel((x, y))[3]:
                for dx in range(-2, 3):
                    for dy in range(-2, 3):
                        halo.putpixel((x + 2 + dx, y + 2 + dy), (35, 28, 53, 170))
    emblem.alpha_composite(halo, (14, 14))
    emblem.alpha_composite(crowbar, (16, 16))
    return emblem


def _spark(img, x, y):
    """A little electrical spark: a bright plus with yellow arms and a stray pixel."""
    img.putpixel((x, y), hexc("#ffffff"))
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        img.putpixel((x + dx, y + dy), CARTOON["E"])
    img.putpixel((x + 2, y - 2), CARTOON["e"])
    img.putpixel((x - 2, y + 2), CARTOON["e"])


def _barely_functional_plate(w, h):
    """The slate plate, but not quite right: one corner bolt has fallen out (and lies loose), a hairline crack runs
    in from an edge, and a strip of tape holds the opposite corner on."""
    img = _plate(w, h)
    # missing top-right bolt: an empty hole where it was
    hx, hy = w - 5, 3
    for (x, y) in ((hx, hy), (hx + 1, hy), (hx, hy + 1), (hx + 1, hy + 1)):
        img.putpixel((x, y), CARTOON["k"])
    # the loose bolt, lying near the bottom edge
    bx, by = w - 14, h - 5
    for (x, y, c) in ((bx, by, "B"), (bx + 1, by, "b"), (bx, by + 1, "b"), (bx + 1, by + 1, "S"), (bx + 2, by + 1, "O")):
        img.putpixel((x, y), CARTOON[c])
    # crack from the bottom edge
    x, y = w // 3, h - 2
    for step in range(7):
        img.putpixel((x, y), CARTOON["O"])
        y -= 1
        x += (1, 0, -1, 0)[step % 4]
    # tape over the bottom-left corner
    for i in range(9):
        for t in range(4):
            px, py = 1 + i + t, h - 2 - i + t - 3
            if 0 <= px < w and 0 <= py < h:
                img.putpixel((px, py), hexc("#c9c3b4") if t in (1, 2) else hexc("#9e9886"))
    return img


def generate_logo():
    # Square icon: 72x72 art, scaled x2 -> 144x144. The emblem on a slightly broken plate, with sparks.
    icon = _barely_functional_plate(72, 72)
    _glow(icon, 36, 36, 30)
    icon.alpha_composite(_logo_emblem(), (4, 4))
    _spark(icon, 62, 10)
    _spark(icon, 9, 60)
    icon.resize((144, 144), Image.NEAREST).save(os.path.join(ROOT, "..", "..", "..", "icon.png"))

    # Banner: 240x80 art, scaled x2 -> 480x160. Emblem on the left, the mod title on the right.
    banner = _barely_functional_plate(240, 80)
    _glow(banner, 40, 40, 34)
    banner.alpha_composite(_logo_emblem(), (8, 8))
    _text(banner, 88, 17, "BARELY FUNCTIONAL", 1, hexc("#f4f8fc"), hexc("#b9c3d1"), shadow=(35, 28, 53, 255))
    _text(banner, 85, 34, "CONTRAPTIONS", 2, hexc("#ff9af0"), hexc("#7b3be0"), shadow=(35, 28, 53, 255))
    # underline "wire" with a spark where it has come loose
    for x in range(86, 212):
        banner.putpixel((x, 56), CARTOON["p"])
        banner.putpixel((x, 57), CARTOON["P"] if x % 6 else CARTOON["g"])
    for i, x in enumerate(range(212, 220)):
        banner.putpixel((x, 57 + i // 3), CARTOON["P"])
    _spark(banner, 222, 61)
    _spark(banner, 76, 12)
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


def _cube_mask_name(up, down, left, right):
    """Connection mask as used in file names: '1' where a cube touches that edge (so it has no frame)."""
    return "".join("1" if c else "0" for c in (up, down, left, right))


def _cube_frame(img, up, down, left, right, pal=None):
    """Frame strips on the unconnected edges, and the bolted corners where two framed edges meet."""
    CARTOON = pal or globals()["CARTOON"]
    src = CUBE_SIDE
    for i in range(16):
        if not up:
            for y in range(4):
                img.putpixel((i, y), CARTOON[src[y][6]])
        if not down:
            for y in range(12, 16):
                img.putpixel((i, y), CARTOON[src[y][6]])
        if not left:
            for x in range(4):
                img.putpixel((x, i), CARTOON[src[6][x]])
        if not right:
            for x in range(12, 16):
                img.putpixel((x, i), CARTOON[src[6][x]])
    for (framed_v, framed_h, ys, xs) in ((not up, not left, range(4), range(4)), (not up, not right, range(4), range(12, 16)),
                                         (not down, not left, range(12, 16), range(4)), (not down, not right, range(12, 16), range(12, 16))):
        if framed_v and framed_h:
            for y in ys:
                for x in xs:
                    img.putpixel((x, y), CARTOON[src[y][x]])


def cube_side_frame(level, frame, up=False, down=False, left=False, right=False):
    """One animation frame of a side face. Unconnected edges get the frame; the window runs to connected edges."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    top_i, bottom_i = (0 if up else 4), (15 if down else 11)
    rows = bottom_i - top_i + 1
    filled = round(level / CUBE_LEVELS * rows)
    surface = bottom_i - filled + 1                       # first filled row
    for y in range(16):
        for x in range(16):
            if y < surface or y < top_i:
                c = CARTOON["K"] if (x + y) % 5 else CARTOON["k"]
            elif y == surface and not (up and level == CUBE_LEVELS):
                # the shimmering surface line (not at the top of a full cube whose energy carries on above)
                c = ENERGY_SPARK if (x + frame * 2) % 8 == 0 else ENERGY_SURFACE
            else:
                c = (ENERGY_LIGHT, ENERGY_MID, ENERGY_MID, ENERGY_DEEP)[(x + y + frame) % 4]
            img.putpixel((x, y), c)
    if not up and not left:
        for (x, y) in ((5, 5), (4, 6), (6, 5)):
            if y < surface:
                img.putpixel((x, y), hexc("#4a3d5c"))
    _cube_frame(img, up, down, left, right)
    return img


def cube_end(up=False, down=False, left=False, right=False):
    """
    Top or bottom face: a plain metal plate framed on unconnected edges, so a group's top is one seamless plate.
    Only a face with no neighbours at all (a lone cube, or the top of a one-wide tower) shows the purple bolt port.
    """
    img = Image.new("RGBA", (16, 16), CARTOON["M"])
    for y in range(3, 13) if not (up or down or left or right) else ():
        for x in range(3, 13):
            ch = CUBE_TOP[y][x]
            if ch not in "HMBbS":
                img.putpixel((x, y), CARTOON[ch])
    _cube_frame(img, up, down, left, right)
    return img


def cube_inner_corner(corner, pal=None):
    """
    Transparent overlay with a 4x4 inside-corner frame piece, drawn where a face connects on two adjacent edges but
    the diagonal block between them is not a cube (the inside of an L). The two frame strips meeting there turn the
    corner as nested L-shaped bands: band k = min(depth into one strip, depth into the other).
    """
    CARTOON = pal or globals()["CARTOON"]
    img = Image.new("RGBA", (16, 16), CLEAR)
    top = [CUBE_SIDE[y][6] for y in (3, 2, 1, 0)]          # top strip, inner to outer
    bottom = [CUBE_SIDE[y][6] for y in (12, 13, 14, 15)]   # bottom strip, inner to outer
    left = [CUBE_SIDE[6][x] for x in (3, 2, 1, 0)]         # left strip, inner to outer
    right = [CUBE_SIDE[6][x] for x in (12, 13, 14, 15)]    # right strip, inner to outer
    vertical_strip, horizontal_strip = (left if "l" in corner else right), (top if "t" in corner else bottom)
    for v in range(4):          # depth into the horizontal strip (0 = inner)
        for u in range(4):      # depth into the vertical strip
            x = (3 - u) if "l" in corner else (12 + u)
            y = (3 - v) if "t" in corner else (12 + v)
            k = min(u, v)
            ch = vertical_strip[k] if u <= v else horizontal_strip[k]
            img.putpixel((x, y), CARTOON[ch])
    return img


def generate_energy_cube():
    for rows in (CUBE_SIDE, CUBE_TOP):
        assert len(rows) == 16 and all(len(r) == 16 for r in rows)
    for name in os.listdir(out("block")):
        if name.startswith("hume_energy_cube") and os.path.isfile(out("block/" + name)):
            os.remove(out("block/" + name))
    os.makedirs(out("block/hume_energy_cube"), exist_ok=True)
    for name in os.listdir(out("block/hume_energy_cube")):
        os.remove(out("block/hume_energy_cube/" + name))
    for corner in ("tl", "tr", "bl", "br"):
        cube_inner_corner(corner).save(out(f"block/hume_energy_cube/inner_{corner}.png"))
    for m in range(16):
        up, down, left, right = bool(m & 8), bool(m & 4), bool(m & 2), bool(m & 1)
        mask = _cube_mask_name(up, down, left, right)
        cube_end(up, down, left, right).save(out(f"block/hume_energy_cube/end_{mask}.png"))
        for level in range(CUBE_LEVELS + 1):
            strip = Image.new("RGBA", (16, 16 * CUBE_FRAMES), CLEAR)
            for f in range(CUBE_FRAMES):
                strip.paste(cube_side_frame(level, f, up, down, left, right), (0, f * 16))
            name = f"block/hume_energy_cube/side_{mask}_{level}.png"
            strip.save(out(name))
            with open(out(name + ".mcmeta"), "w") as fh:
                fh.write('{\n  "animation": {\n    "frametime": 4,\n    "interpolate": true\n  }\n}\n')


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


# ---------------------------------------------------------------------------
# Zero Point Energy Module GUI: the Hume Energy Cube's layout with a blue energy gauge, a beam diagram between the
# gauge and the info window, and no slots. Sprites (beside the 176x193 background):
#   (176,0)  30x64 energy fill          (176,66) 4 frames of 30x2 surface shimmer
#   (208,0)  16x8 module pointing up, off / (208,8) on      (208,16) pointing down, off / (208,24) on
#   (208,32) 5x7 "this module" pointer  (208,40) 12x12 blocking block   (208,56) 16x8 missing module (dashed)
#   (228,0)  4 frames of 6x64 beam      (186,194) status lamps shared with the other machines
# ---------------------------------------------------------------------------
ZPE_GUI_DEEP, ZPE_GUI_MID, ZPE_GUI_LIGHT = hexc("#1b5fa8"), hexc("#2f8fe0"), hexc("#3fc8ff")
ZPE_GUI_SURFACE, ZPE_GUI_SPARK = hexc("#a8ecff"), hexc("#ffffff")
ZPE_MODULE_UP = [
    "....OOOOOOOO....",
    "....OnQQQQnO....",
    "....ONnnnnNO....",
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OBbMMMXXXXMMMBbO",
    "OSSSSSSSSSSSSSSO",
    "OOOOOOOOOOOOOOOO",
]
ZPE_POINTER = ["W....", "WW...", "WWW..", "WWWW.", "WWW..", "WW...", "W...."]
ZPE_BLOCKER = [
    "OOOOOOOOOOOO",
    "OHHHHHHHHHMO",
    "OHRMMMMMMRSO",
    "OHMRMMMMRMSO",
    "OHMMRMMRMMSO",
    "OHMMMRRMMMSO",
    "OHMMMRRMMMSO",
    "OHMMRMMRMMSO",
    "OHMRMMMMRMSO",
    "OHRMMMMMMRSO",
    "OHSSSSSSSSSO",
    "OOOOOOOOOOOO",
]
ZPE_MISSING = [
    "O.O.O.O.O.O.O.O.",
    "................",
    "O......vv......O",
    ".......v.v......",
    "O........v.....O",
    "........v.......",
    "O...............",
    ".O.O.O.Ov.O.O.O.",
]


def generate_zero_point_gui():
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # energy gauge, fill 11..40 x 19..82
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 46, 16, 76, 84, GUI["gauge"])          # beam diagram, 47..74 x 17..82
    inset(img, 80, 16, 168, 78, GUI["panel"])         # info window
    for x in range(84, 164):                          # divider between energy and generation
        img.putpixel((x, 44), hexc("#2c2e36"))
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    rnd = random.Random(29)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice([ZPE_GUI_SURFACE, ZPE_GUI_LIGHT, ZPE_GUI_MID]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))
    for y in range(64):                                         # energy fill
        for x in range(30):
            c = mix(ZPE_GUI_LIGHT, ZPE_GUI_DEEP, y / 63.0)
            if (x + y) % 4 == 0:
                c = mix(c, ZPE_GUI_SURFACE, 0.25)
            img.putpixel((176 + x, y), c)
    for k in range(1, 8):
        ty = round(64 * k / 8)
        for x in range(25, 30):
            img.putpixel((176 + x, ty), mix(img.getpixel((176 + x, ty)), ZPE_GUI_SURFACE, 0.5))
    for f in range(4):                                          # surface shimmer
        for x in range(30):
            img.putpixel((176 + x, 66 + f * 2), ZPE_GUI_SPARK if (x + f * 2) % 8 == 0 else ZPE_GUI_SURFACE)
            img.putpixel((176 + x, 67 + f * 2), ZPE_GUI_LIGHT)
    down = list(reversed(ZPE_MODULE_UP))
    for i, (rows, extra) in enumerate(((ZPE_MODULE_UP, ZPE_OFF), (ZPE_MODULE_UP, ZPE_ON), (down, ZPE_OFF), (down, ZPE_ON))):
        grid(rows, dict(CARTOON, **extra), img, 208, i * 8)
    grid(ZPE_POINTER, {"W": hexc("#dfe4f5")}, img, 208, 32)
    grid(ZPE_BLOCKER, dict(CARTOON, R=hexc("#ff4b4b")), img, 208, 40)
    grid(ZPE_MISSING, {"O": hexc("#6a6f7c"), "v": hexc("#9aa0a6")}, img, 208, 56)
    for f in range(4):                                          # beam: white core, blue glow, bright bands moving up
        for y in range(64):
            band = ((y + f * 4) % 16) < 3
            row = [ZPE_GUI_DEEP, ZPE_GUI_LIGHT, ZPE_GUI_SPARK, ZPE_GUI_SPARK, ZPE_GUI_LIGHT, ZPE_GUI_DEEP]
            if band:
                row = [ZPE_GUI_MID, ZPE_GUI_SURFACE, ZPE_GUI_SPARK, ZPE_GUI_SPARK, ZPE_GUI_SURFACE, ZPE_GUI_MID]
            for x, c in enumerate(row):
                img.putpixel((228 + f * 6 + x, y), c)
    img.save(out("gui/zero_point_energy_module.png"))


ADVANCEMENT_BACKGROUND = [  # dim blue-steel floor plate, tiled behind the advancement tab
    "SMMMMMMMMMMMMMMS",
    "MbMMMMMMMMMMMMbM",
    "MMMMMMMMMMMMMMMM",
    "MMMMMMMMMMMMMMMM",
    "MMMMMMSSSSMMMMMM",
    "MMMMMSMMMMSMMMMM",
    "MMMMSMMMMMMSMMMM",
    "MMMMSMMMMMMSMMMM",
    "MMMMSMMMMMMSMMMM",
    "MMMMSMMMMMMSMMMM",
    "MMMMMSMMMMSMMMMM",
    "MMMMMMSSSSMMMMMM",
    "MMMMMMMMMMMMMMMM",
    "MMMMMMMMMMMMMMMM",
    "MbMMMMMMMMMMMMbM",
    "SMMMMMMMMMMMMMMS",
]


def generate_advancement_background():
    pal = {"M": shade(CARTOON["M"], 0.45), "S": shade(CARTOON["S"], 0.45), "b": shade(CARTOON["b"], 0.45)}
    grid(ADVANCEMENT_BACKGROUND, pal).save(out("gui/advancements/backgrounds/contraptions.png"))


# ---------------------------------------------------------------------------
# Fluid Tank: the Hume Energy Cube's casing in blue (blue rings instead of purple) with a glass window instead of
# the energy window. The glass is mostly see-through, with faint diagonal streaks lined up across neighbouring tanks;
# the fluid behind it is drawn by FluidTankRenderer. The same edge masks and inside corners as the cube.
# A lone top shows a blue valve with a water drop.
# ---------------------------------------------------------------------------
TANK_PAL = dict(CARTOON, P=hexc("#3fc8ff"), p=hexc("#1b5fa8"))
TANK_TOP = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMOOOOMMbSSO",
    "OHMMMOPPPPOMMMSO",
    "OHMMOPpKXKpPOMSO",
    "OHMOPpKXQXKpPOSO",
    "OHMOPKXQQQXKPOSO",
    "OHMOPKXQWQXKPOSO",
    "OHMOPKXXQXXKPOSO",
    "OHMMOPKXXXKPOMSO",
    "OHMMMOPPPPOMMMSO",
    "OHMMMMOOOOMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]
TANK_TOP_PAL = dict(TANK_PAL, K=hexc("#0f2a4a"), X=hexc("#2f9fe0"), Q=hexc("#a8ecff"), W=hexc("#ffffff"))
GLASS = (200, 232, 255, 38)
GLASS_STREAK = (235, 248, 255, 120)
GLASS_STREAK_SOFT = (225, 244, 255, 75)


def tank_side(up=False, down=False, left=False, right=False):
    img = Image.new("RGBA", (16, 16), GLASS)
    for y in range(16):
        for x in range(16):
            d = (x + y) % 12
            if d == 0:
                img.putpixel((x, y), GLASS_STREAK)
            elif d == 1:
                img.putpixel((x, y), GLASS_STREAK_SOFT)
    # A dark rim just inside the frame, so the window reads as recessed glass.
    for i in range(16):
        if not up:
            img.putpixel((i, 4), (20, 28, 48, 150))
        if not left:
            img.putpixel((4, i), (20, 28, 48, 150))
    _cube_frame(img, up, down, left, right, TANK_PAL)
    return img


def tank_end(up=False, down=False, left=False, right=False):
    img = Image.new("RGBA", (16, 16), TANK_PAL["M"])
    for y in range(3, 13) if not (up or down or left or right) else ():
        for x in range(3, 13):
            ch = TANK_TOP[y][x]
            if ch not in "HMBbS":
                img.putpixel((x, y), TANK_TOP_PAL[ch])
    _cube_frame(img, up, down, left, right, TANK_PAL)
    return img


def generate_fluid_tank():
    assert len(TANK_TOP) == 16 and all(len(r) == 16 for r in TANK_TOP)
    folder = "block/fluid_tank"
    os.makedirs(out(folder), exist_ok=True)
    for name in os.listdir(out(folder)):
        os.remove(out(folder + "/" + name))
    for corner in ("tl", "tr", "bl", "br"):
        cube_inner_corner(corner, TANK_PAL).save(out(f"{folder}/inner_{corner}.png"))
    for m in range(16):
        up, down, left, right = bool(m & 8), bool(m & 4), bool(m & 2), bool(m & 1)
        mask = _cube_mask_name(up, down, left, right)
        tank_end(up, down, left, right).save(out(f"{folder}/end_{mask}.png"))
        tank_side(up, down, left, right).save(out(f"{folder}/side_{mask}.png"))


TANK_DROP_ICON = [  # 9x9 GUI icon
    "....O....",
    "...OQO...",
    "..OQXXO..",
    ".OQXXXXO.",
    "OQXXXXXXO",
    "OQXXXXXXO",
    "OXXXXXXKO",
    ".OXXXXKO.",
    "..OOOOO..",
]


def generate_tank_gui():
    """Fluid Tank GUI: the Hume Energy Cube's layout; the gauge is filled with the fluid's own texture in game."""
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # fluid gauge (11..40 x 19..82, 30x64)
    inset(img, 48, 16, 168, 78, GUI["panel"])        # info window
    for x in range(52, 164):
        img.putpixel((x, 54), hexc("#2c2e36"))
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    grid(TANK_DROP_ICON, TANK_TOP_PAL, img, 152, 4)
    rnd = random.Random(17)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice([hexc("#3fc8ff"), hexc("#a8ecff"), hexc("#2f9fe0")]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))      # status lamps shared with the other machines
    # Glass over the fluid (176,0) 30x64: a highlight down the left, and a mark for every eighth on the right.
    for y in range(64):
        img.putpixel((176 + 1, y), (255, 255, 255, 70))
        img.putpixel((176 + 2, y), (255, 255, 255, 40))
    for k in range(1, 8):
        ty = round(64 * k / 8)
        for x in range(25, 30):
            img.putpixel((176 + x, ty), (230, 245, 255, 160))
    img.save(out("gui/fluid_tank.png"))


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


# ---------------------------------------------------------------------------
# Themed sheep (resource "appearance"): a hay-bale fleece, a skull-and-bones
# overlay and the sheet of extra body parts (SheepExtrasModel, 64x64). Parts
# that a resource tints with "feature_color" are drawn in light greys.
# ---------------------------------------------------------------------------
def generate_hay_wool():
    """Hay-bale fleece in the vanilla sheep wool layout: golden straw with two binding bands round the body."""
    rnd = random.Random(4242)
    img = Image.new("RGBA", (64, 32), CLEAR)
    straw = [hexc("#E3C252"), hexc("#D1AE3C"), hexc("#B8952C"), hexc("#F0D774")]
    band = [hexc("#8C5A26"), hexc("#734820")]

    def fill(rect, mask=None, banded=False):
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                if mask and not mask(x - x0, y - y0, x1 - x0, y1 - y0):
                    continue
                # Straw runs in short streaks: the colour changes every few pixels along a row.
                c = straw[(x // 3 + y * 7 + rnd.randint(0, 1)) % 4]
                if (x * 5 + y * 3) % 9 == 0:
                    c = shade(c, 0.82)
                if banded and (y - y0) in (4, 5, 11, 12):
                    c = band[(x + y) % 2]
                img.putpixel((x, y), c)

    head = box_faces(0, 0, 6, 6, 6)
    for face in ("top", "east", "west", "south"):
        fill(head[face])
    fill(head["north"], lambda x, y, w, h: y < 2 or (y < 3 and x in (0, w - 1)))
    body = box_faces(28, 8, 8, 16, 6)
    for face in ("east", "west", "north", "south"):
        fill(body[face], banded=True)
    for face in ("top", "bottom"):
        fill(body[face])
    legs = box_faces(0, 16, 4, 6, 4)
    fill(legs["top"])
    for face in ("east", "north", "west", "south"):
        fill(legs[face], lambda x, y, w, h: y < 4)
    img.save(out("entity/sheep/hay_wool.png"))


def generate_sheep_bones():
    """Skull face and bony legs in the vanilla sheep body layout (64x32); tinted by feature_color."""
    rnd = random.Random(777)
    img = Image.new("RGBA", (64, 32), CLEAR)
    bone = [(232, 230, 222, 255), (214, 211, 200, 255), (244, 243, 238, 255)]
    dark = (30, 26, 24, 255)
    crack = (168, 164, 152, 255)

    def fill(rect, colour=None):
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                img.putpixel((x, y), colour or rnd.choice(bone))

    head = box_faces(0, 0, 6, 6, 8)
    for face in head.values():
        fill(face)
    fx, fy = head["north"][0], head["north"][1]
    for x, y in ((0, 2), (1, 2), (0, 3), (1, 3), (4, 2), (5, 2), (4, 3), (5, 3), (2, 4), (3, 4)):
        img.putpixel((fx + x, fy + y), dark)
    for x in range(6):
        img.putpixel((fx + x, fy + 5), dark if x % 2 else crack)
    # Legs: a thin bone down the middle of each side, with knuckles.
    legs = box_faces(0, 16, 4, 12, 4)
    for name, face in legs.items():
        x0, y0, x1, y1 = face
        for y in range(y0, y1):
            for x in range(x0, x1):
                edge = x in (x0, x1 - 1) and name not in ("top", "bottom")
                c = rnd.choice(bone)
                if edge:
                    c = crack
                if name not in ("top", "bottom") and (y - y0) in (5, 10):
                    c = bone[2]
                img.putpixel((x, y), c)
    # Ribs on the body (only seen on a sheared sheep... which never shows features, but keeps the texture complete).
    body = box_faces(28, 8, 8, 16, 6)
    for name, face in body.items():
        x0, y0, x1, y1 = face
        for y in range(y0, y1):
            for x in range(x0, x1):
                img.putpixel((x, y), crack if (y - y0) % 3 == 2 else rnd.choice(bone))
    img.save(out("entity/sheep/sheep_bones.png"))


def generate_sheep_extras():
    """The sheet of extra body parts for SheepExtrasModel (64x64)."""
    rnd = random.Random(9001)
    img = Image.new("RGBA", (64, 64), CLEAR)

    def grey(v, a=255):
        v = max(0, min(255, v))
        return (v, v, v, a)

    def fill(rect, fn):
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = fn(x - x0, y - y0, x1 - x0, y1 - y0)
                if c:
                    img.putpixel((x, y), c)

    # (0,0)-(22,11) is free: the Wither sheep's extra heads use the game's own Wither Skeleton Skull texture.

    # Wing bone (texOffs 0,12; 16x1x1).
    fill((0, 12, 34, 14), lambda x, y, w, h: grey(214 if x % 4 else 170))
    # Wing membrane (texOffs 0,14; 16x0x12): veins fanning back from the bone, a scalloped trailing edge.
    def membrane(x, y, w, h):
        scallop = (x % 4) in (0, 3)
        if y == h - 1 and scallop:
            return None
        if y == h - 2 and x % 4 == 0:
            return None
        vein = x % 4 == 0
        v = 128 if vein else 176 + rnd.randint(-10, 10) - y * 3
        return grey(v)
    fill((12, 14, 28, 26), membrane)
    fill((28, 14, 44, 26), membrane)

    # Spider legs (texOffs 0,26; 14x2x2): bristly, with darker joints.
    fill((0, 26, 32, 30), lambda x, y, w, h: grey(110 if x % 7 in (0, 1) else 150 + rnd.randint(-18, 18)))

    # Ghast tentacles (texOffs 0,30; up to 2x11x2): white, greying towards the tips.
    fill((0, 30, 8, 43), lambda x, y, w, h: grey(246 - y * 4 + rnd.randint(-6, 6)))

    # Sculk tendrils (texOffs 40,0; 6x7 planes): already coloured, deep teal with glowing cyan tips.
    def tendril(x, y, w, h):
        cx = abs(x - (w - 1) / 2.0)
        if cx > 1.6 + y * 0.35:   # a frond, narrow at the top and widening down to the head
            return None
        if y <= 1 and cx < 1.5:
            return hexc("#5FF5F5")
        if cx < 0.6:
            return hexc("#29DFEB")
        return hexc("#0E5A63") if (x + y) % 3 else hexc("#0A3F47")
    fill((40, 0, 46, 7), tendril)
    fill((46, 0, 52, 7), tendril)

    # Glowing eyes (texOffs 40,8; 6x6 plane on the face): the vanilla sheep's eye pixels, white so the accent tints them.
    for x, y in ((0, 2), (1, 2), (4, 2), (5, 2)):
        img.putpixel((40 + x, 8 + y), (255, 255, 255, 255))
    img.save(out("entity/sheep/sheep_extras.png"))


def main():
    generate_wool_items()
    generate_sheep_fur()
    generate_hay_wool()
    generate_sheep_bones()
    generate_sheep_extras()
    generate_block()
    generate_creative_generator()
    generate_automatic_shears()
    generate_gui()
    generate_shears_gui()
    generate_fluid_reservoir()
    generate_machine_block()
    generate_zero_point_module()
    generate_energy_cable()
    generate_reservoir_gui()
    generate_energy_cube()
    generate_cube_gui()
    generate_fluid_tank()
    generate_tank_gui()
    generate_zero_point_gui()
    generate_advancement_background()
    generate_jei()
    generate_logo()
    print("Textures written to", os.path.abspath(ROOT))


if __name__ == "__main__":
    main()
