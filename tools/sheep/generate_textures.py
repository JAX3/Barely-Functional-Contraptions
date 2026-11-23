#!/usr/bin/env python3
"""
Generates every texture used by Resourceful Sheep as original pixel art.

Run from the project root:  python3 tools/generate_textures.py
Requires Pillow. Output goes to src/main/resources/assets/barelyfunctionalcontraptions/textures/.

All art is procedurally drawn here (no vanilla textures are read or copied); only
the UV *layout* of the sheep fur model (64x32) is matched so the overlay wraps
the vanilla model correctly.
"""
import math
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


# Pipe sides set to Pull (orange ring) or Push (green ring): the same plug face in the mode's colour.
PLUG_PULL = {"P": hexc("#ff8a2a"), "p": hexc("#b5501a"), "G": hexc("#fff1d6"), "g": hexc("#ffc27a")}
PLUG_PUSH = {"P": hexc("#4be37a"), "p": hexc("#1f8a45"), "G": hexc("#f0fff4"), "g": hexc("#a8f5c2")}


def generate_pipe_plug_modes(name, pal):
    for mode, colours in (("pull", PLUG_PULL), ("push", PLUG_PUSH)):
        mode_pal = dict(pal, **colours)
        plug = Image.new("RGBA", (16, 16), CLEAR)
        # The plate's thin edges (what you see from the side or above) are painted in the mode's colour too.
        edge = "pPPPPPPp"
        for k, ch in enumerate(edge):
            for d in (0, 1, 14, 15):
                c = mode_pal["O"] if d in (0, 15) else mode_pal[ch]
                plug.putpixel((4 + k, d), c)
                plug.putpixel((d, 4 + k), c)
        grid(CABLE_PLUG_FACE, mode_pal, plug, 4, 4)
        plug.save(out(f"block/{name}/{name.split('_')[1]}_plug_{mode}.png"))


def generate_energy_cable():
    generate_conduit("energy_cable", CARTOON)
    generate_conduit("fluid_pipe", dict(CARTOON, **PIPE_BLUE))
    generate_conduit("item_pipe", dict(CARTOON, **PIPE_YELLOW))
    generate_pipe_plug_modes("fluid_pipe", dict(CARTOON, **PIPE_BLUE))
    generate_pipe_plug_modes("item_pipe", dict(CARTOON, **PIPE_YELLOW))

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
      (0,150)  150x86 Infusion Altar panel: a rune ring behind a cross of five slots, a text strip at the bottom
    """
    panel(img, 0, 64, 200, 84)
    panel(img, 0, 88, 150, 146)
    # Infusion panel decorations: a recessed strip behind the text, and the wool/grass fringe of the machine GUIs.
    inset(img, 4, 119, 146, 144, GUI["panel"])
    rnd = random.Random(11)
    for x in range(3, 147):
        if rnd.random() < 0.18:
            img.putpixel((x, 145), rnd.choice([hexc("#e3e1ea"), hexc("#b7b3c6"), hexc("#86c25a")]))
    panel(img, 0, 150, 150, 236)
    for a in range(0, 360, 4):                          # rune ring behind the cross (centre slot at 30,28)
        x = round(38 + 27 * math.cos(math.radians(a)))
        y = round(150 + 36 + 27 * math.sin(math.radians(a)))
        img.putpixel((x, y), hexc("#3b2a5c"))
    for (ix, iy) in [(30, 6), (8, 28), (30, 28), (52, 28), (30, 50)]:
        slot(img, ix, 150 + iy)
    for i in range(-2, 20):                             # amethyst rim on the centre slot
        for (px, py) in [(30 + i, 176), (30 + i, 197), (28, 178 + i - 2), (49, 178 + i - 2)]:
            img.putpixel((px, py), hexc("#7b44c9"))
    inset(img, 4, 220, 146, 234, GUI["panel"])
    rnd2 = random.Random(23)
    for x in range(3, 147):
        if rnd2.random() < 0.18:
            img.putpixel((x, 235), rnd2.choice([hexc("#e3e1ea"), hexc("#b7b3c6"), hexc("#c27cff")]))
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


def generate_blaze_furnace_gui():
    """Blaze Sheep Furnace window: energy gauge, heat column (fire rising with the heat), info window, boost slot."""
    shears = Image.open(out("gui/automatic_shears.png")).convert("RGBA")
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # energy gauge, fill 11..40 x 19..82
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 46, 16, 76, 84, GUI["gauge"])          # heat column, 47..74 x 17..82
    inset(img, 80, 16, 168, 78, GUI["panel"])         # info window
    for x in range(84, 164):                          # divider between energy and heat
        img.putpixel((x, 44), hexc("#2c2e36"))
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    slot(img, 152, 87)                                # boost slot
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    rnd = random.Random(31)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice(FIRE[2:5]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    img.paste(shears.crop((176, 64, 208, 80)), (176, 146))          # button background, normal and hovered
    for y in range(64):                                             # energy fill: amber, light at the top
        for x in range(30):
            c = mix(FIRE[4], FIRE[1], y / 63.0)
            if (x + y) % 4 == 0:
                c = mix(c, FIRE[5], 0.2)
            img.putpixel((176 + x, y), c)
    for k in range(1, 8):
        ty = round(64 * k / 8)
        for x in range(25, 30):
            img.putpixel((176 + x, ty), mix(img.getpixel((176 + x, ty)), FIRE[5], 0.5))
    for f in range(4):                                              # energy surface shimmer
        for x in range(30):
            img.putpixel((176 + x, 66 + f * 2), FIRE[5] if (x + f * 2) % 8 == 0 else FIRE[4])
            img.putpixel((176 + x, 67 + f * 2), FIRE[3])
    # Heat column: 3 frames of 26x64 at (176 + 26f, 80). Flame tongues along the top, a hot core below.
    for f in range(3):
        ox = 176 + f * 26
        for x in range(26):
            tip = 5 + 3.5 * math.sin(f * 2.1 + x * 0.9) + 1.5 * math.sin(f * 1.3 + x * 2.3)
            for y in range(64):
                d = y - (9 - tip)  # depth below the flame's edge
                if d < 0:
                    continue
                centre = 1 - abs(x - 12.5) / 13.0
                heat = min(1.0, d / 40.0) * 0.55 + centre * 0.4 + 0.05 * math.sin(y * 0.7 + f * 2 + x)
                c = FIRE[1 + max(0, min(3, int(heat * 4.2)))] if d > 2 else FIRE[3]
                if d > 6 and (x * 7 + y * 3 + f * 5) % 23 == 0:
                    c = FIRE[5]
                img.putpixel((ox + x, 80 + y), c)
    img.save(out("gui/blaze_furnace.png"))


BURNER_FLAME = [  # 14x14 flame for the Wool Burner window: lit sprite, and a dim outline in the background
    "......F.......",
    ".....FF.......",
    ".....FFF......",
    "....FFFF..F...",
    "...FFFYFF.FF..",
    "...FFYYFFFFF..",
    "..FFYYYYFFFF..",
    "..FFYYWYYFFF..",
    ".FFYYWWWYYFFF.",
    ".FFYWWWWWYYFF.",
    ".FFYWWWWWWYFF.",
    ".FFYYWWWWYYFF.",
    "..FFYYYYYYFF..",
    "...FFFFFFFF...",
]


def generate_wool_burner_gui():
    """Wool Burner window: energy gauge, fuel box (flame over the fuel slot), info window."""
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # energy gauge, fill 11..40 x 19..82
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 46, 16, 88, 84, GUI["gauge"])          # fuel box
    grid(BURNER_FLAME, {"F": GUI["tick"], "Y": GUI["tick"], "W": GUI["tick"]}, img, 60, 24)
    slot(img, 59, 56)                                 # fuel slot
    inset(img, 92, 16, 168, 78, GUI["panel"])         # info window
    for x in range(96, 164):
        img.putpixel((x, 44), hexc("#2c2e36"))
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    rnd = random.Random(37)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice([hexc("#e3e1ea"), hexc("#b7b3c6"), FIRE[3]]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    for y in range(64):                                             # energy fill
        for x in range(30):
            c = mix(FIRE[4], FIRE[1], y / 63.0)
            if (x + y) % 4 == 0:
                c = mix(c, FIRE[5], 0.2)
            img.putpixel((176 + x, y), c)
    for k in range(1, 8):
        ty = round(64 * k / 8)
        for x in range(25, 30):
            img.putpixel((176 + x, ty), mix(img.getpixel((176 + x, ty)), FIRE[5], 0.5))
    for f in range(4):                                              # energy surface shimmer
        for x in range(30):
            img.putpixel((176 + x, 66 + f * 2), FIRE[5] if (x + f * 2) % 8 == 0 else FIRE[4])
            img.putpixel((176 + x, 67 + f * 2), FIRE[3])
    grid(BURNER_FLAME, {"F": FIRE[2], "Y": FIRE[4], "W": FIRE[5]}, img, 176, 80)   # lit flame
    img.save(out("gui/wool_burner.png"))


WOOL_BURNER_TOP = [  # top: casing around a round chimney
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMMMMOOOOMMMMSO",
    "OHMMMOkkkkOMMMSO",
    "OHMMOkKKKKkOMMSO",
    "OHMMOkKVVKkOMMSO",
    "OHMMOkKVVKkOMMSO",
    "OHMMOkKKKKkOMMSO",
    "OHMMMOkkkkOMMMSO",
    "OHMMMMOOOOMMMMSO",
    "OHMMMMMMMMMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

WOOL_BURNER_SIDE = [  # side: casing with a wool bale emblem
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMMMOOOOOOMMMSO",
    "OHMMOWWwWWwOMMSO",
    "OHMMOwWWwWWOMMSO",
    "OHMMOWwWWwWOMMSO",
    "OHMMOvwvvwvOMMSO",
    "OHMMMOOOOOOMMMSO",
    "OHMOVVVVVVVVOMSO",
    "OHMOOOOOOOOOOMSO",
    "OHMMMMMMMMMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]


def generate_wool_burner():
    """Wool Burner: front (cold, and animated fire with a tuft of wool in it), sides and chimney top (cold and lit)."""
    pal = dict(CARTOON)
    front = grid(BLAZE_FURNACE_FRONT, pal)
    _fire_mouth(front, 0, 0, False)
    for (x, y) in [(6, 9), (7, 9), (8, 9), (7, 8), (9, 10), (6, 10)]:   # a tuft of wool on the grate
        front.putpixel((x, y), hexc("#b7b3c6"))
    front.save(out("block/wool_burner_front.png"))
    frames = 8
    anim = Image.new("RGBA", (16, 16 * frames), CLEAR)
    for t in range(frames):
        grid(BLAZE_FURNACE_FRONT, pal, anim, 0, 16 * t)
        _fire_mouth(anim, 16 * t, t, True)
        for (x, y) in [(6, 10), (7, 10), (8, 10), (7, 9)]:                 # the wool glowing as it burns
            anim.putpixel((x, 16 * t + y), FIRE[4] if (t + x) % 3 else FIRE[5])
    anim.save(out("block/wool_burner_front_on.png"))
    with open(out("block/wool_burner_front_on.png") + ".mcmeta", "w") as f:
        f.write('{\n  "animation": {\n    "frametime": 3\n  }\n}\n')
    cold = dict(CARTOON)
    cold["V"] = hexc("#2a2238")
    lit = dict(CARTOON)
    lit["V"] = hexc("#ff8a2a")
    grid(WOOL_BURNER_SIDE, cold).save(out("block/wool_burner_side.png"))
    grid(WOOL_BURNER_SIDE, lit).save(out("block/wool_burner_side_lit.png"))
    grid(WOOL_BURNER_TOP, cold).save(out("block/wool_burner_top.png"))
    grid(WOOL_BURNER_TOP, lit).save(out("block/wool_burner_top_lit.png"))


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


def generate_snow_wool():
    """Snow fleece in the vanilla sheep wool layout: bright, slightly blue-white snow with soft drifts and glints."""
    rnd = random.Random(1212)
    img = Image.new("RGBA", (64, 32), CLEAR)
    snow = [hexc("#FFFFFF"), hexc("#F4FBFF"), hexc("#E6F2FB"), hexc("#D8E8F5")]

    def fill(rect, mask=None, drift=False):
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                if mask and not mask(x - x0, y - y0, x1 - x0, y1 - y0):
                    continue
                c = snow[min(3, int(rnd.random() * 2.2) + ((x * 3 + y * 5) % 11 == 0))]
                if drift and (y - y0) >= (y1 - y0) - 2 - ((x // 3) % 2):
                    c = hexc("#C9DEEF")  # the drift's shaded underside
                if rnd.random() < 0.03:
                    c = hexc("#BFE3FF")  # an icy glint
                img.putpixel((x, y), c)

    head = box_faces(0, 0, 6, 6, 6)
    for face in ("top", "east", "west", "south"):
        fill(head[face])
    fill(head["north"], lambda x, y, w, h: y < 2 or (y < 3 and x in (0, w - 1)))
    body = box_faces(28, 8, 8, 16, 6)
    for face in ("east", "west", "north", "south"):
        fill(body[face], drift=True)
    for face in ("top", "bottom"):
        fill(body[face])
    legs = box_faces(0, 16, 4, 6, 4)
    fill(legs["top"])
    for face in ("east", "north", "west", "south"):
        fill(legs[face], lambda x, y, w, h: y < 4)
    img.save(out("entity/sheep/snow_wool.png"))


def generate_melon_wool():
    """Melon-rind fleece in the vanilla sheep wool layout: green rind with dark stripes running along the body."""
    rnd = random.Random(6262)
    img = Image.new("RGBA", (64, 32), CLEAR)
    light = [hexc("#8CC63F"), hexc("#7DB536"), hexc("#9AD14A")]
    dark = [hexc("#3E6E1E"), hexc("#355F19")]
    speck = hexc("#B9E06A")

    def fill(rect, along_x, mask=None):
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                if mask and not mask(x - x0, y - y0, x1 - x0, y1 - y0):
                    continue
                # Stripes run along the body: across x on the sides, across y on the top and bottom.
                k = (y - y0) if along_x else (x - x0)
                wobble = 1 if ((x + y) * 7 + rnd.randint(0, 3)) % 9 == 0 else 0
                c = dark[(x + y) % 2] if (k + wobble) % 4 == 0 else rnd.choice(light)
                if c in light and rnd.random() < 0.05:
                    c = speck
                img.putpixel((x, y), c)

    head = box_faces(0, 0, 6, 6, 6)
    for face in ("top", "east", "west", "south"):
        fill(head[face], face != "top")
    fill(head["north"], True, lambda x, y, w, h: y < 2 or (y < 3 and x in (0, w - 1)))
    body = box_faces(28, 8, 8, 16, 6)
    for face in ("east", "west", "north", "south"):
        fill(body[face], False)
    for face in ("top", "bottom"):
        fill(body[face], True)
    legs = box_faces(0, 16, 4, 6, 4)
    fill(legs["top"], True)
    for face in ("east", "north", "west", "south"):
        fill(legs[face], False, lambda x, y, w, h: y < 4)
    img.save(out("entity/sheep/melon_wool.png"))

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


METER_SIDE = [  # Energy Meter side: steel casing around a dark screen (the live FE/t is drawn on it in game)
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSOOOOOOOOMbSO",
    "OMMOKKKKKKKKOMSO",
    "OMMOKkkkkkkkOMSO",
    "OMMOKkkkkkkkOMSO",
    "OMMOKkkkkkkkOMSO",
    "OMMOKkkkkkkkOMSO",
    "OMMOKkkkkkkkOMSO",
    "OMMOKKKKKKKKOMSO",
    "OHMSOOOOOOOOSMSO",
    "OHMPPPPPPPPPPMSO",
    "OHBbSSSSSSSSBbSO",
    "OHbSSSSSSSSSSbSO",
    "OOOOOOOOOOOOOOOO",
]

METER_PORT = [  # Energy Meter in (dark core) and out (glowing core) faces
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMOOOOMMbSSO",
    "OMMMMOPPPPOMMMSO",
    "OMMMOPpccpPOMMSO",
    "OMMOPpcCCcpPOMSO",
    "OMMOPcCCCCcPOMSO",
    "OMMOPcCCCCcPOMSO",
    "OMMOPpcCCcpPOMSO",
    "OMMMOPpccpPOMMSO",
    "OMMMMOPPPPOMMMSO",
    "OHbSMMOOOOMMbSSO",
    "OHBbSSSSSSSSBbSO",
    "OHbSSSSSSSSSSbSO",
    "OOOOOOOOOOOOOOOO",
]


def generate_energy_meter():
    """Energy Meter: screen sides, an unlit input port (back) and a glowing output port (front)."""
    pal = dict(CARTOON)
    pal["k"] = hexc("#10141c")
    grid(METER_SIDE, pal).save(out("block/energy_meter_side.png"))
    dark = dict(CARTOON)
    dark["C"], dark["c"] = hexc("#141019"), hexc("#2a2238")
    grid(METER_PORT, dark).save(out("block/energy_meter_in.png"))
    # Plain casing for the faces with neither a port nor the screen: the side panel with its screen window shut.
    casing = [row.replace("K", "S").replace("k", "M") for row in METER_SIDE]
    grid(casing, dict(CARTOON)).save(out("block/energy_meter_casing.png"))
    lit = dict(CARTOON)
    lit["C"], lit["c"] = hexc("#ffd2fb"), hexc("#ff6ae6")
    grid(METER_PORT, lit).save(out("block/energy_meter_out.png"))


GLOW_PANEL_TOP = [  # Glow Panel top: steel frame around four light cells
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHOOOOOOOOOOOOSO",
    "OHOcccccOcccccOO",
    "OHOcCCCcOcCCCcOO",
    "OHOcCCCcOcCCCcOO",
    "OHOcCCCcOcCCCcOO",
    "OHOcccccOcccccOO",
    "OHOOOOOOOOOOOOOO",
    "OHOcccccOcccccOO",
    "OHOcCCCcOcCCCcOO",
    "OHOcCCCcOcCCCcOO",
    "OHOcCCCcOcCCCcOO",
    "OHOcccccOcccccOO",
    "OMOOOOOOOOOOOOSO",
    "OOOOOOOOOOOOOOOO",
]

GLOW_PANEL_SIDE = [  # Glow Panel side: the bottom 6 rows are the panel's edge
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHPPPPPPPPPPPPSO",
    "OMbSMMMMMMMMbSSO",
    "OMSSSSSSSSSSSSSO",
    "OOOOOOOOOOOOOOOO",
]

GLOW_LAMP = [  # Glow Lamp: a glass frame around swirling glow ink
    "OOOOOOOOOOOOOOOO",
    "OwwwwwwwwwwwwwvO",
    "OwCCCCcCCCCCCcvO",
    "OwCYYCCcCCYCCcvO",
    "OwCYCCCCcCCCCcvO",
    "OwCCCcCCCCcCCCvO",
    "OwcCCCCYYCCCcCvO",
    "OwCCcCYGGYCcCCvO",
    "OwCCCcYGGYCCCCvO",
    "OwCcCCCYYCCcCCvO",
    "OwCCCCcCCCCCcCvO",
    "OwCCYCCCcCCCYCvO",
    "OwCcCCCCCcCYYCvO",
    "OwCCCcCCCCCCCCvO",
    "OvvvvvvvvvvvvvvO",
    "OOOOOOOOOOOOOOOO",
]


def generate_glow_panel():
    """Glow Panel top (idle and lit), its edge, and the Glow Lamp."""
    idle = dict(CARTOON)
    idle["C"], idle["c"] = hexc("#1b2a44"), hexc("#2c4468")
    grid(GLOW_PANEL_TOP, idle).save(out("block/glow_panel_top.png"))
    lit = dict(CARTOON)
    lit["C"], lit["c"] = hexc("#9ffbe8"), hexc("#3fcfc0")
    grid(GLOW_PANEL_TOP, lit).save(out("block/glow_panel_top_lit.png"))
    # In between, for when it makes less than its best (dusk, rain, a Glow Lamp at night): the cells part lit.
    for name, t in (("dim", 0.35), ("half", 0.68)):
        part = dict(CARTOON)
        part["C"], part["c"] = mix(idle["C"], lit["C"], t), mix(idle["c"], lit["c"], t)
        grid(GLOW_PANEL_TOP, part).save(out(f"block/glow_panel_top_{name}.png"))
    grid(GLOW_PANEL_SIDE, dict(CARTOON)).save(out("block/glow_panel_side.png"))
    lamp = dict(CARTOON)
    lamp["C"], lamp["c"], lamp["Y"], lamp["G"] = hexc("#5ee8d6"), hexc("#2fb7b0"), hexc("#b8fff2"), hexc("#ffffff")
    grid(GLOW_LAMP, lamp).save(out("block/glow_lamp.png"))


BLAZE_FURNACE_FRONT = [  # Blaze Sheep Furnace front: steel casing, brass-rimmed fire mouth (filled in per state)
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMOOOOOOOOOOMSO",
    "OHMOCCCCCCCCOMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMO........OMSO",
    "OHMOCCCCCCCCOMSO",
    "OHMOOOOOOOOOOMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

BLAZE_FURNACE_SIDE = [  # side: steel casing with three heat vents
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMMMMMMMMMMMMSO",
    "OHMOOOOOOOOOOMSO",
    "OHMOVVVVVVVVOMSO",
    "OHMOOOOOOOOOOMSO",
    "OHMOVVVVVVVVOMSO",
    "OHMOOOOOOOOOOMSO",
    "OHMOVVVVVVVVOMSO",
    "OHMOOOOOOOOOOMSO",
    "OHMMMMMMMMMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

BLAZE_FURNACE_TOP = [  # top: an iron grate over the embers, where the sheep stand
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHOOOOOOOOOOOOSO",
    "OHOHVHVHVHVHVOSO",
    "OHOHVHVHVHVHVOSO",
    "OHOMVMVMVMVMVOSO",
    "OHOHVHVHVHVHVOSO",
    "OHOHVHVHVHVHVOSO",
    "OHOMVMVMVMVMVOSO",
    "OHOHVHVHVHVHVOSO",
    "OHOHVHVHVHVHVOSO",
    "OHOMVMVMVMVMVOSO",
    "OHOSVSVSVSVSVOSO",
    "OHOOOOOOOOOOOOSO",
    "OMSSSSSSSSSSSSSO",
    "OOOOOOOOOOOOOOOO",
]

FIRE = [hexc("#5a1408"), hexc("#a3260c"), hexc("#e8561a"), hexc("#ff9a2e"), hexc("#ffd25a"), hexc("#fff4c2")]


def _fire_mouth(img, oy, t, lit):
    """The 8x5 fire mouth at (4, 6): dull embers when cold, flickering flames (frame t of 8) when lit."""
    for x in range(8):
        for y in range(5):
            px, py = 4 + x, oy + 6 + y
            depth = 4 - y  # 0 at the bottom row
            if not lit:
                c = hexc("#141019") if depth > 0 else (hexc("#4a1a12") if x % 3 == 1 else hexc("#2a2238"))
            else:
                # Flame height per column, wavering over the 8 frames.
                h = 2.6 + 1.6 * math.sin(t * math.pi / 4 + x * 1.9) + 0.8 * math.sin(t * math.pi / 2 + x * 0.7)
                heat = h - depth  # >0 inside the flame, bigger is hotter
                if heat <= -0.5:
                    c = hexc("#2b0a06")
                else:
                    c = FIRE[max(0, min(len(FIRE) - 1, int(heat + 1.5)))]
            img.putpixel((px, py), c)


def generate_blaze_furnace():
    """Blaze Sheep Furnace: front (cold, and an 8-frame animated fire), sides and top grate (cold and lit)."""
    pal = dict(CARTOON)
    front = grid(BLAZE_FURNACE_FRONT, pal)
    _fire_mouth(front, 0, 0, False)
    front.save(out("block/blaze_furnace_front.png"))
    frames = 8
    anim = Image.new("RGBA", (16, 16 * frames), CLEAR)
    for t in range(frames):
        grid(BLAZE_FURNACE_FRONT, pal, anim, 0, 16 * t)
        _fire_mouth(anim, 16 * t, t, True)
    anim.save(out("block/blaze_furnace_front_on.png"))
    with open(out("block/blaze_furnace_front_on.png") + ".mcmeta", "w") as f:
        f.write('{\n  "animation": {\n    "frametime": 3\n  }\n}\n')
    cold = dict(CARTOON)
    cold["V"] = hexc("#2a2238")
    lit = dict(CARTOON)
    lit["V"] = hexc("#ff8a2a")
    grid(BLAZE_FURNACE_SIDE, cold).save(out("block/blaze_furnace_side.png"))
    grid(BLAZE_FURNACE_SIDE, lit).save(out("block/blaze_furnace_side_lit.png"))
    grid(BLAZE_FURNACE_TOP, cold).save(out("block/blaze_furnace_top.png"))
    top = grid(BLAZE_FURNACE_TOP, lit)
    for y in range(3, 13):  # embers glow unevenly between the bars
        for x in range(4, 13, 2):
            k = (x * 7 + y * 3) % 6
            top.putpixel((x, y), FIRE[4] if k == 0 else FIRE[3] if k < 3 else FIRE[2])
    top.save(out("block/blaze_furnace_top_lit.png"))


MANUAL = [  # Contraptions Manual: a purple book with a brass gear on the cover
    "................",
    "..OOOOOOOOOOO...",
    "..OPPPPPPPPPpO..",
    "..OPPPPCCPPPpO..",
    "..OPPPCYYCPPpO..",
    "..OPPCYccYCPpO..",
    "..OPPCcOOcCPpO..",
    "..OPPCcOOcCPpO..",
    "..OPPCYccYCPpO..",
    "..OPPPCYYCPPpO..",
    "..OPPPPCCPPPpO..",
    "..OPPPPPPPPPpO..",
    "..OwwwwwwwwwwO..",
    "..OvvvvvvvvvvO..",
    "...OOOOOOOOOO...",
    "................",
]


def generate_manual():
    grid(MANUAL, dict(CARTOON)).save(out("item/contraptions_manual.png"))


# ---------------------------------------------------------------------------
# Wool Bale: a greyscale wool block (tinted with the resource's wool colour in game) under untinted steel straps,
# laid like a hay bale. Wool Packer: the machine casing with a press in its front and nine tufts on its sides.
# ---------------------------------------------------------------------------
def _grey(v, a=255):
    v = max(0, min(255, int(v)))
    return (v, v, v, a)


def generate_wool_bale():
    rnd = random.Random(91)
    # Side: pressed fleece in puffy rows, each lock lit on top and shaded underneath.
    side = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            row = y // 4
            lx = (x + (2 if row % 2 else 0)) % 4      # locks staggered like brickwork
            ly = y % 4
            v = 240 - ly * 14 - (8 if lx == 3 else 0) + rnd.randint(-5, 5)
            if ly == 3 and lx in (0, 3):
                v -= 18
            side.putpixel((x, y), _grey(v))
    side.save(out("block/wool_bale_side.png"))

    # Ends: the cut face of the bale, a swirl of compressed fleece inside a darker rim.
    end = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            ring = int(d * 1.15 + math.atan2(y - 7.5, x - 7.5) * 0.6) % 3
            v = 240 - ring * 14 + rnd.randint(-4, 4)
            if x in (0, 15) or y in (0, 15):
                v = 172 + rnd.randint(-6, 6)
            end.putpixel((x, y), _grey(v))
    end.save(out("block/wool_bale_end.png"))

    # Two thin steel straps round the sides, each with a small brass buckle.
    straps = Image.new("RGBA", (16, 16), CLEAR)
    for band in (3, 12):
        for x in range(16):
            straps.putpixel((x, band), CARTOON["H"])
            straps.putpixel((x, band + 1), CARTOON["S"])
        for (x, y) in [(7, 0), (8, 0), (7, 1), (8, 1)]:
            straps.putpixel((x, band + y), CARTOON["C"] if y == 0 else CARTOON["c"])
    straps.save(out("block/wool_bale_side_straps.png"))


WOOL_PACKER_SIDE = [  # side: casing with nine tufts, three by three
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMOOOOOOOOOOMSO",
    "OHMOWwOWwOWwOMSO",
    "OHMOwvOwvOwvOMSO",
    "OHMOOOOOOOOOOMSO",
    "OHMOWwOWwOWwOMSO",
    "OHMOwvOwvOwvOMSO",
    "OHMOOOOOOOOOOMSO",
    "OHMOWwOWwOWwOMSO",
    "OHMOwvOwvOwvOMSO",
    "OHBOOOOOOOOOOBSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]

WOOL_PACKER_TOP = [  # top: the press's ram housing, bolted down
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMMOOOOOOOOMMSO",
    "OHMMOCCCCCCOMMSO",
    "OHMMOCYYYYcOMMSO",
    "OHMMOCYKKccOMMSO",
    "OHMMOCYKKccOMMSO",
    "OHMMOCcccccOMMSO",
    "OHMMOCCCCCCOMMSO",
    "OHMMOOOOOOOOMMSO",
    "OHMMMMMMMMMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]


def _press_mouth(img, oy, drop, glow):
    """The 8x5 press mouth at (4, 6): a brass ram {drop} pixels down onto a tuft of wool."""
    for x in range(8):
        for y in range(5):
            img.putpixel((4 + x, oy + 6 + y), hexc("#141019"))
    for x in range(8):                                    # the ram
        for y in range(drop + 1):
            c = CARTOON["c"] if y < drop else CARTOON["C"]
            img.putpixel((4 + x, oy + 6 + y), c)
        img.putpixel((4 + x, oy + 6 + drop), CARTOON["Y"] if x in (2, 3) else CARTOON["C"])
    wool_top = 6 + drop + 1
    for y in range(max(wool_top, 8), 11):                 # the wool, squashed flatter as the ram comes down
        for x in range(1, 7):
            c = CARTOON["w"] if (x + y) % 3 else CARTOON["v"]
            if glow and y == 10:
                c = CARTOON["W"]
            img.putpixel((4 + x, oy + y), c)


def generate_wool_packer():
    pal = dict(CARTOON)
    front = grid(BLAZE_FURNACE_FRONT, pal)
    _press_mouth(front, 0, 0, False)
    front.save(out("block/wool_packer_front.png"))
    drops = [0, 1, 2, 2, 1, 0, 0, 0]
    anim = Image.new("RGBA", (16, 16 * len(drops)), CLEAR)
    for t, drop in enumerate(drops):
        grid(BLAZE_FURNACE_FRONT, pal, anim, 0, 16 * t)
        _press_mouth(anim, 16 * t, drop, drop == 2)
    anim.save(out("block/wool_packer_front_on.png"))
    with open(out("block/wool_packer_front_on.png") + ".mcmeta", "w") as f:
        f.write('{\n  "animation": {\n    "frametime": 3\n  }\n}\n')
    grid(WOOL_PACKER_SIDE, pal).save(out("block/wool_packer_side.png"))
    grid(WOOL_PACKER_TOP, pal).save(out("block/wool_packer_top.png"))


def generate_wool_packer_gui():
    """Wool Packer window: energy gauge, wool slot with 3x3 pips, press arrow, bale slot, info lines."""
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # energy gauge, fill 11..40 x 19..82
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 46, 16, 168, 84, GUI["panel"])         # work window
    slot(img, 62, 31)                                 # wool
    big_slot(img, 126, 31)                            # bale
    img.alpha_composite(arrow_shape(hexc("#2c2e36"), hexc("#17181c")), (86, 31))
    for i in range(9):                                # 3x3 pips at (61, 53), 5x5 on a 7 grid
        px, py = 61 + i % 3 * 7, 53 + i // 3 * 7
        inset(img, px, py, px + 5, py + 5, hexc("#3a3c46"))
    for x in range(88, 164):
        if x % 2 == 0:
            img.putpixel((x, 51), hexc("#22232a"))
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    sheep_icon(img, 146, 5, hexc("#f2f2f2"))
    rnd = random.Random(53)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice([hexc("#e3e1ea"), hexc("#b7b3c6"), CARTOON["C"]]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    for y in range(64):                                             # energy fill, the separator's purple
        t = y / 63.0
        c = mix(hexc("#ff6ae6"), hexc("#5c2aa6"), t)
        for x in range(30):
            cc = mix(c, hexc("#ffd2fb"), 0.35) if x in (3, 4, 5) else (shade(c, 0.75) if x >= 26 else c)
            img.putpixel((176 + x, y), cc)
    for k in range(1, 8):
        ty = round(64 * k / 8)
        for x in range(25, 30):
            img.putpixel((176 + x, ty), mix(img.getpixel((176 + x, ty)), hexc("#ffd2fb"), 0.5))
    for f in range(4):                                              # energy surface shimmer
        for x in range(30):
            img.putpixel((176 + x, 66 + f * 2), hexc("#ffffff") if (x + f * 2) % 8 == 0 else hexc("#ffd2fb"))
            img.putpixel((176 + x, 67 + f * 2), hexc("#ff6ae6"))
    img.alpha_composite(arrow_shape(CARTOON["C"], hexc("#5a3510")), (176, 80))   # press arrow fill, brass
    for x in range(178, 197):
        if img.getpixel((x, 86))[3]:
            img.putpixel((x, 86), CARTOON["Y"])
    pip = ["WWWwv", "WWwwv", "Wwwwv", "wwwvv", "vvvvv"]                         # lit pip: a little wool tuft
    grid(pip, {"W": hexc("#ffffff"), "w": hexc("#e3e1ea"), "v": hexc("#b7b3c6")}, img, 176, 100)
    img.save(out("gui/wool_packer.png"))


# ---------------------------------------------------------------------------
# Infusion Altar: a pedestal (wide base, column, wide top) of blue steel with an amethyst rune ring on top that glows
# while it infuses, and its window: energy gauge, a cross of five slots, progress bar and tier readout.
# ---------------------------------------------------------------------------
ALTAR_TOP = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMOOOOOOMbSSO",
    "OHMMOPpPPpPOMMSO",
    "OHMOPpggggpPOMSO",
    "OHMOPgGGGGgPOMSO",
    "OHMOPgGWWGgPOMSO",
    "OHMOPgGWWGgPOMSO",
    "OHMOPgGGGGgPOMSO",
    "OHMOPpggggpPOMSO",
    "OHMMOPpPPpPOMMSO",
    "OHbSMOOOOOOMbSSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSSSSO",
    "OOOOOOOOOOOOOOOO",
]
ALTAR_SIDE = [  # used for the base and the top slab edges
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OMBbMMPPPPMMBbSO",
    "OSSSSSSSSSSSSSSO",
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHMMMMMMMMMMMMSO",
    "OHMMPpMMMMPpMMSO",
    "OHMMpPMMMMpPMMSO",
    "OHMMMMMMMMMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHMMMMMMMMMMMMSO",
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]
ALTAR_COLUMN = [
    "OOOOOOOOOOOOOOOO",
    "OSMMMHHHHHHMMMSO",
    "OSMMHPPPPPPHMMSO",
    "OSMMHPggggPHMMSO",
    "OSMMHPgGGgPHMMSO",
    "OSMMHPgGGgPHMMSO",
    "OSMMHPggggPHMMSO",
    "OSMMHPPPPPPHMMSO",
    "OSMMMHHHHHHMMMSO",
    "OSMMMMMMMMMMMMSO",
    "OSMMMMbMMbMMMMSO",
    "OSMMMMMMMMMMMMSO",
    "OSMMMMMMMMMMMMSO",
    "OSMMMMbMMbMMMMSO",
    "OSSSSSSSSSSSSSSO",
    "OOOOOOOOOOOOOOOO",
]


def generate_infusion_altar():
    dull = dict(CARTOON)
    dull["g"], dull["G"], dull["W"] = hexc("#5c2aa6"), hexc("#7b44c9"), hexc("#a77ce6")
    grid(ALTAR_TOP, dull).save(out("block/infusion_altar_top.png"))
    frames = 8
    anim = Image.new("RGBA", (16, 16 * frames), CLEAR)
    for t in range(frames):
        pulse = 0.5 + 0.5 * math.sin(t * math.pi / 4)
        lit = dict(CARTOON)
        lit["g"] = mix(hexc("#ff6ae6"), hexc("#c27cff"), pulse)
        lit["G"] = mix(hexc("#ffd2fb"), hexc("#ff9af0"), pulse)
        lit["W"] = WHITE
        grid(ALTAR_TOP, lit, anim, 0, 16 * t)
    anim.save(out("block/infusion_altar_top_active.png"))
    with open(out("block/infusion_altar_top_active.png") + ".mcmeta", "w") as f:
        f.write('{\n  "animation": {\n    "frametime": 3\n  }\n}\n')
    grid(ALTAR_SIDE, dict(CARTOON)).save(out("block/infusion_altar_side.png"))
    grid(ALTAR_COLUMN, dull).save(out("block/infusion_altar_column.png"))
    lit = dict(CARTOON)
    lit["g"], lit["G"] = hexc("#ff6ae6"), hexc("#ffd2fb")
    grid(ALTAR_COLUMN, lit).save(out("block/infusion_altar_column_active.png"))


def generate_infusion_altar_gui():
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    shears = Image.open(out("gui/automatic_shears.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # energy gauge, fill 11..40 x 19..82
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 46, 16, 168, 86, GUI["panel"])          # work window
    # A faint rune ring behind the cross of slots.
    for a in range(0, 360, 4):
        r = 27
        x = round(82 + r * math.cos(math.radians(a)))
        y = round(47 + r * math.sin(math.radians(a)))
        img.putpixel((x, y), hexc("#3b2a5c"))
    for (ix, iy) in [(74, 39), (74, 18), (95, 39), (74, 60), (53, 39)]:
        slot(img, ix, iy)
    for (ix, iy) in [(74, 39)]:                         # the centre slot gets an amethyst rim
        for i in range(-2, 20):
            for (px, py) in [(ix + i, iy - 2), (ix + i, iy + 19), (ix - 2, iy + i), (ix + 19, iy + i)]:
                img.putpixel((px, py), hexc("#7b44c9"))
    inset(img, 49, 79, 117, 84, GUI["gauge"])          # progress bar, fill 50..115 x 80..82
    for x in range(120, 165):
        if x % 2 == 0:
            img.putpixel((x, 33), hexc("#22232a"))
    inset(img, 8, 86, 18, 96, GUI["gauge"])            # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    sheep_icon(img, 146, 5, hexc("#f2f2f2"))
    rnd = random.Random(61)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice([hexc("#e3e1ea"), hexc("#b7b3c6"), hexc("#c27cff")]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    for y in range(64):
        t = y / 63.0
        c = mix(hexc("#ff6ae6"), hexc("#5c2aa6"), t)
        for x in range(30):
            img.putpixel((176 + x, y), mix(c, hexc("#ffd2fb"), 0.35) if x in (3, 4, 5) else (shade(c, 0.75) if x >= 26 else c))
    for f in range(4):
        for x in range(30):
            img.putpixel((176 + x, 66 + f * 2), hexc("#ffffff") if (x + f * 2) % 8 == 0 else hexc("#ffd2fb"))
            img.putpixel((176 + x, 67 + f * 2), hexc("#ff6ae6"))
    for x in range(66):                                             # progress fill (176,80) 66x3
        c = mix(hexc("#7b44c9"), hexc("#ff9af0"), x / 65.0)
        img.putpixel((176 + x, 80), mix(c, WHITE, 0.4))
        img.putpixel((176 + x, 81), c)
        img.putpixel((176 + x, 82), shade(c, 0.7))
    img.paste(shears.crop((176, 64, 208, 80)), (176, 146))          # show-area button background, normal and hovered
    img.save(out("gui/infusion_altar.png"))


ICE = [hexc("#0f2a4a"), hexc("#1f5a8c"), hexc("#3f8fcf"), hexc("#7fc8f0"), hexc("#bfe8ff"), hexc("#f2fbff")]


def generate_chiller_gui():
    """Snow Sheep Chiller window: water gauge, cold column (frost rising with the cold), info window, Ice slot."""
    shears = Image.open(out("gui/automatic_shears.png")).convert("RGBA")
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # water gauge, fill 11..40 x 19..82
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 46, 16, 76, 84, GUI["gauge"])          # cold column, 47..74 x 17..82
    inset(img, 80, 16, 168, 78, GUI["panel"])         # info window
    slot(img, 134, 41)                                # Ice slot
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    rnd = random.Random(77)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice(ICE[3:6]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    img.paste(shears.crop((176, 64, 208, 80)), (176, 146))          # area button background, normal and hovered
    for y in range(64):                                             # water fill: deep blue, lighter at the top
        for x in range(30):
            c = mix(hexc("#2f6fd0"), hexc("#163a8a"), y / 63.0)
            if (x * 3 + y) % 11 == 0:
                c = mix(c, hexc("#7fb8ff"), 0.35)
            img.putpixel((176 + x, y), c)
    for f in range(4):                                              # water surface ripple
        for x in range(30):
            img.putpixel((176 + x, 66 + f * 2), hexc("#bfe0ff") if (x + f * 3) % 8 < 2 else hexc("#5f9bf0"))
            img.putpixel((176 + x, 67 + f * 2), hexc("#2f6fd0"))
    # Cold column: 3 frames of 26x64 at (176 + 26f, 80): frost crystals rising, icicle points along the top.
    for f in range(3):
        ox = 176 + f * 26
        for x in range(26):
            tip = 4 + 3.0 * abs(math.sin(f * 1.7 + x * 0.8)) + 1.5 * math.sin(f * 0.9 + x * 2.1)
            for y in range(64):
                d = y - (8 - tip)
                if d < 0:
                    continue
                centre = 1 - abs(x - 12.5) / 13.0
                cold = min(1.0, d / 40.0) * 0.5 + centre * 0.35 + 0.08 * math.sin(y * 0.6 + f * 2 + x * 1.3)
                c = ICE[1 + max(0, min(3, int(cold * 4.2)))] if d > 2 else ICE[4]
                if d > 4 and (x * 5 + y * 7 + f * 3) % 19 == 0:
                    c = ICE[5]  # a frost glint
                img.putpixel((ox + x, 80 + y), c)
    # Freezing arrow (empty and full) at (176, 162) and (176, 172), 24x9.
    arrow = ["....................#...", "....................##..", "#####################.#.", "#####################..#",
             "#####################.#.", "....................##..", "....................#...", "........................", "........................"]
    for i, (fill, edge) in enumerate(((GUI["gauge"], GUI["inset_light"]), (ICE[3], ICE[5]))):
        for y, row in enumerate(arrow):
            for x, ch in enumerate(row):
                if ch == "#":
                    img.putpixel((176 + x, 162 + i * 10 + y), edge if y in (2, 4) or x in (20, 21, 22, 23) else fill)
    img.save(out("gui/chiller.png"))


def generate_chiller_block():
    """Snow Sheep Chiller block: frosted blue-steel casing, an ice window at the front (frosting over while it
    freezes), and a vent grate with icicles on top."""
    STEEL, STEEL_L, STEEL_D, DARK = hexc("#4f6a8a"), hexc("#7f9cbf"), hexc("#2f4560"), hexc("#16202e")

    def casing():
        img = Image.new("RGBA", (16, 16), STEEL)
        rnd = random.Random(5)
        for y in range(16):
            for x in range(16):
                c = STEEL
                if rnd.random() < 0.12:
                    c = mix(STEEL, ICE[4], 0.35)  # frost spots
                img.putpixel((x, y), c)
        for i in range(16):
            img.putpixel((i, 0), STEEL_L); img.putpixel((0, i), STEEL_L)
            img.putpixel((i, 15), STEEL_D); img.putpixel((15, i), STEEL_D)
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            img.putpixel((x, y), ICE[5]); img.putpixel((x + 1, y + 1), STEEL_D)
        return img

    side = casing()
    for x in range(1, 15):  # a frosty band
        side.putpixel((x, 6), ICE[3]); side.putpixel((x, 7), ICE[2])
    side.save(out("block/chiller_side.png"))
    top = casing()
    for y in range(4, 12):
        for x in range(4, 12):
            top.putpixel((x, y), DARK if (y % 2 == 0) else STEEL_D)  # vent grate
    top.save(out("block/chiller_top.png"))

    def front(active):
        img = casing()
        for y in range(3, 13):
            for x in range(3, 13):
                if x in (3, 12) or y in (3, 12):
                    img.putpixel((x, y), STEEL_D)  # window frame
                else:
                    d = abs(x - 7.5) + abs(y - 7.5)
                    if active:
                        c = ICE[5] if d < 2 else ICE[4] if d < 4 else ICE[3]
                        if (x + y) % 4 == 0:
                            c = ICE[5]
                    else:
                        c = ICE[2] if d < 3 else ICE[1]
                    img.putpixel((x, y), c)
        # An ice crystal in the window.
        for i in range(4, 12):
            img.putpixel((i, 7), ICE[5] if active else ICE[3])
            img.putpixel((7, i), ICE[5] if active else ICE[3])
        return img

    front(False).save(out("block/chiller_front.png"))
    front(True).save(out("block/chiller_front_active.png"))


def generate_powered_furnace():
    """Powered Furnace: block (the machine casing with a furnace mouth at the front, glowing while it smelts) and its
    window (energy gauge, input slot with a flame that lights while Blaze sheep heat it, arrow, output slot, info)."""
    casing = Image.open(out("block/functional_machine_block.png")).convert("RGBA")
    casing.save(out("block/powered_furnace_side.png"))
    top = casing.copy()
    for y in range(5, 11):  # a chimney grate
        for x in range(5, 11):
            top.putpixel((x, y), hexc("#17181c") if y % 2 else hexc("#3a3d45"))
    top.save(out("block/powered_furnace_top.png"))

    def front(lit):
        img = casing.copy()
        for y in range(6, 13):
            for x in range(3, 13):
                if y == 6 or x in (3, 12):
                    img.putpixel((x, y), hexc("#2a2c33"))  # the mouth's frame
                elif lit:
                    d = abs(x - 7.5) + (12 - y) * 0.8
                    img.putpixel((x, y), FIRE[5] if d < 2 else FIRE[4] if d < 3.5 else FIRE[3] if d < 5 else FIRE[2])
                else:
                    img.putpixel((x, y), hexc("#17181c"))
        for x in range(4, 12):  # the grate bars
            img.putpixel((x, 9), hexc("#2a2c33"))
        for x, y in ((5, 3), (7, 3), (9, 3)):  # a vent row above the mouth
            img.putpixel((x, y), hexc("#17181c"))
        return img

    front(False).save(out("block/powered_furnace_front.png"))
    front(True).save(out("block/powered_furnace_front_lit.png"))

    shears = Image.open(out("gui/automatic_shears.png")).convert("RGBA")
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # energy gauge, fill 11..40 x 19..82
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    slot(img, 56, 22)                                 # input
    grid(BURNER_FLAME, {"F": GUI["tick"], "Y": GUI["tick"], "W": GUI["tick"]}, img, 57, 42)   # flame, unlit
    img.alpha_composite(arrow_shape(hexc("#2c2e36"), hexc("#17181c")), (82, 31))
    slot(img, 124, 31)                                # output
    inset(img, 46, 60, 168, 84, GUI["panel"])         # info window
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    rnd = random.Random(53)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice(FIRE[2:5]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    img.paste(shears.crop((176, 64, 208, 80)), (176, 146))          # area button background
    for y in range(64):                                             # energy fill: amber
        for x in range(30):
            c = mix(FIRE[4], FIRE[1], y / 63.0)
            if (x + y) % 4 == 0:
                c = mix(c, FIRE[5], 0.2)
            img.putpixel((176 + x, y), c)
    for f in range(4):                                              # energy surface shimmer
        for x in range(30):
            img.putpixel((176 + x, 66 + f * 2), FIRE[5] if (x + f * 2) % 8 == 0 else FIRE[4])
            img.putpixel((176 + x, 67 + f * 2), FIRE[3])
    img.alpha_composite(arrow_shape(FIRE[3], FIRE[1]), (176, 80))   # arrow fill
    grid(BURNER_FLAME, {"F": FIRE[2], "Y": FIRE[4], "W": FIRE[5]}, img, 176, 100)  # lit flame
    img.save(out("gui/powered_furnace.png"))


def generate_hume_field_generator():
    """Hume Field Generator (WIP preview): a steel base and cap on four pillars, two glowing field coils, a glass
    shell round a core that flickers between "on" (cyan) and "off" (magenta), and an observing lens on top."""
    # Dark blue steel with purple trim, like the Hume Energy Cube.
    DARK, STEEL, STEEL_L, STEEL_D, STEEL_DD = hexc("#1a1528"), hexc("#3f4766"), hexc("#5d6687"), hexc("#2c324c"), hexc("#231c35")
    PURPLE, PURPLE_D, HI = hexc("#9a52f0"), hexc("#5c2aa6"), hexc("#8d97bc")
    CYAN, CYAN_L, CYAN_D, MAGENTA, MAGENTA_D = hexc("#69d6ff"), hexc("#cbf4ff"), hexc("#1b5fa8"), hexc("#ff6ae6"), hexc("#9c2f8f")

    def panel(rivets=True):
        img = Image.new("RGBA", (16, 16), STEEL)
        for i in range(16):
            img.putpixel((i, 0), STEEL_L); img.putpixel((0, i), STEEL_L)
            img.putpixel((i, 15), STEEL_DD); img.putpixel((15, i), STEEL_DD)
        if rivets:
            for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
                img.putpixel((x, y), HI); img.putpixel((x + 1, y + 1), STEEL_D)
        return img

    # Base: side (bottom 3 px used) with a purple trim line; top a plate with an inset ring under the coils.
    side = panel(False)
    for x in range(16):
        side.putpixel((x, 13), PURPLE); side.putpixel((x, 12), PURPLE_D)
    for x in (2, 13):
        side.putpixel((x, 14), HI)
    side.save(out("block/hume_field_generator/base_side.png"))
    top = panel()
    for i in range(4, 12):
        for j in (4, 11):
            top.putpixel((i, j), PURPLE_D); top.putpixel((j, i), PURPLE_D)
    for i in range(5, 11):
        for j in range(5, 11):
            top.putpixel((i, j), DARK)
    top.save(out("block/hume_field_generator/base_top.png"))

    # Pillar: a steel strut with a purple groove.
    pillar = Image.new("RGBA", (16, 16), STEEL)
    for y in range(16):
        pillar.putpixel((0, y), STEEL_L); pillar.putpixel((1, y), PURPLE if y % 4 else HI)
        for x in range(2, 16):
            pillar.putpixel((x, y), STEEL_D if x > 2 else STEEL)
    pillar.save(out("block/hume_field_generator/pillar.png"))

    # Coil: glowing purple windings.
    coil = Image.new("RGBA", (16, 16), PURPLE_D)
    for y in range(16):
        for x in range(16):
            coil.putpixel((x, y), PURPLE if (x + y) % 3 else MAGENTA)
    coil.save(out("block/hume_field_generator/coil.png"))

    # Glass shell: mostly clear, a faint tint and bright edges.
    glass = Image.new("RGBA", (16, 16), (200, 220, 255, 16))
    for i in range(16):
        for c in ((i, 0), (0, i), (i, 15), (15, i)):
            glass.putpixel(c, (223, 228, 245, 110))
    for i in range(3, 7):
        glass.putpixel((i, 9 - i), (255, 255, 255, 120))
    glass.save(out("block/hume_field_generator/glass.png"))

    # Core: animated; each frame is a mix of "is" (cyan) and "isn't" (magenta), never settling.
    import random as _r
    rnd = _r.Random(1776)
    frames = []
    states = [0.0, 0.15, 0.5, 0.85, 1.0, 0.7, 0.3, 0.9, 0.05, 0.6, 1.0, 0.4]
    for t in states:
        f = Image.new("RGBA", (16, 16), DARK)
        for y in range(16):
            for x in range(16):
                on = rnd.random() < t
                d = abs(x - 7.5) + abs(y - 7.5)
                if d < 4:
                    c = CYAN_L if on else hexc("#ffc2f4")
                elif d < 7:
                    c = CYAN if on else MAGENTA
                else:
                    c = CYAN_D if on else MAGENTA_D
                f.putpixel((x, y), c)
        frames.append(f)
    strip = Image.new("RGBA", (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        strip.paste(f, (0, 16 * i))
    strip.save(out("block/hume_field_generator/core.png"))
    with open(out("block/hume_field_generator/core.png.mcmeta"), "w") as m:
        m.write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": true\n  }\n}\n')

    # Cap: like the base top, with a ring of rivets; its side a slim trimmed band.
    cap = panel()
    for i in range(3, 13):
        cap.putpixel((i, 3), PURPLE_D); cap.putpixel((i, 12), PURPLE_D); cap.putpixel((3, i), PURPLE_D); cap.putpixel((12, i), PURPLE_D)
    cap.save(out("block/hume_field_generator/cap_top.png"))
    capside = panel(False)
    for x in range(16):
        capside.putpixel((x, 1), PURPLE)
    capside.save(out("block/hume_field_generator/cap_side.png"))

    # Lens: an eye looking up, the "observer" whose watching decides whether it makes power.
    lens = Image.new("RGBA", (16, 16), DARK)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.2:
                lens.putpixel((x, y), DARK)
            elif d < 4.5:
                lens.putpixel((x, y), CYAN)
            elif d < 6.5:
                lens.putpixel((x, y), CYAN_D)
            elif d < 8:
                lens.putpixel((x, y), STEEL_D)
    lens.putpixel((6, 6), HI); lens.putpixel((5, 5), CYAN_L)
    lens.save(out("block/hume_field_generator/lens.png"))


# ---- Woollen Armour. Everything is greyscale, so the dye tint (the wool's colour) colours it, as leather is coloured.
# Knit: a V stitch, two pixels wide, that zigzags row to row; ribbing (cuffs, hems, the beanie's brim): vertical ribs.
def knit(lx, ly):
    return 238 if (lx % 2 == 0) == (ly % 2 == 0) else 206


def rib(lx, ly):
    return 240 if lx % 2 == 0 else 188


def grey(v, a=255):
    return (v, v, v, a)


WOOLLEN_ICONS = {  # # knit, r ribbing, o pom-pom (only the boots are used now, for the Muffled icon)
    "helmet": [
        "................",
        "................",
        ".......oo.......",
        "......oooo......",
        ".......oo.......",
        ".....######.....",
        "....########....",
        "...##########...",
        "...##########...",
        "..############..",
        "..############..",
        "..rrrrrrrrrrrr..",
        "..rrrrrrrrrrrr..",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "................",
        "...####..####...",
        ".######rr######.",
        "################",
        "################",
        "###.########.###",
        "###.########.###",
        "###.########.###",
        "rrr.########.rrr",
        "rrr.########.rrr",
        "....########....",
        "....rrrrrrrr....",
        "....rrrrrrrr....",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "................",
        "..rrrrrrrrrrrr..",
        "..rrrrrrrrrrrr..",
        "..############..",
        "..#####..#####..",
        "..#####..#####..",
        "..####....####..",
        "..####....####..",
        "..####....####..",
        "..####....####..",
        "..####....####..",
        "..rrrr....rrrr..",
        "..rrrr....rrrr..",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "....rrrr..rrrr..",
        "....rrrr..rrrr..",
        "....####..####..",
        "....####..####..",
        "....####..####..",
        "..######.#####..",
        ".#############..",
        ".#######.#####..",
        "................",
        "................",
        "................",
        "................",
    ],
}



def _fill_faces(img, faces, rows_of_face, pattern):
    """Paints each named face of a box; rows_of_face(face, h) gives the local rows to cover and which are ribbed."""
    for name, (x0, y0, x1, y1) in faces.items():
        h = y1 - y0
        for ly in range(h):
            kind = rows_of_face(name, ly, h)
            if kind is None:
                continue
            for lx in range(x1 - x0):
                v = rib(lx, ly) if kind == "rib" else pattern(lx, ly)
                img.putpixel((x0 + lx, y0 + ly), grey(v))


def generate_woollen_armor():
    # The item icons are in generate_woollen_icons.

    # The humanoid layer (helmet, jumper, boots) and the leggings layer, at the player model's texture offsets.
    head, body = box_faces(0, 0, 8, 8, 8), box_faces(16, 16, 8, 12, 4)
    arm, leg = box_faces(40, 16, 4, 12, 4), box_faces(0, 16, 4, 12, 4)
    sides = ("north", "south", "east", "west")

    layer = Image.new("RGBA", (64, 32), CLEAR)

    def beanie(face, ly, h):  # the top, and the upper 5 rows of the sides with a 2-row brim; the face stays open
        if face == "top":
            return "knit"
        if face in sides and ly < 5:
            return "rib" if ly >= 3 else "knit"
        return None

    def jumper(face, ly, h):  # all round, with a ribbed hem and cuffs
        if face == "bottom":
            return None
        if face in sides and ly >= h - 2:
            return "rib"
        return "knit"

    def booties(face, ly, h):  # the lowest 5 rows and the sole, with a ribbed top
        if face == "bottom":
            return "knit"
        if face in sides and ly >= h - 5:
            return "rib" if ly == h - 5 else "knit"
        return None

    _fill_faces(layer, head, beanie, knit)
    _fill_faces(layer, body, jumper, knit)
    _fill_faces(layer, arm, jumper, knit)
    _fill_faces(layer, leg, booties, knit)
    layer.save(out("entity/equipment/humanoid/wool.png"))

    legs = Image.new("RGBA", (64, 32), CLEAR)

    def waistband(face, ly, h):
        return "rib" if face in sides and ly >= h - 3 else None

    def trouser_legs(face, ly, h):  # down to where the booties start, with a ribbed ankle
        if face in sides and ly < h - 4:
            return "rib" if ly >= h - 6 else "knit"
        return None

    _fill_faces(legs, body, waistband, knit)
    _fill_faces(legs, leg, trouser_legs, knit)
    legs.save(out("entity/equipment/humanoid_leggings/wool.png"))

    # The Muffled effect icon: a cream bootie with its sound fading out behind it.
    icon = Image.new("RGBA", (18, 18), CLEAR)
    boot = WOOLLEN_ICONS["boots"]
    cream, outline = hexc("#efe6d2"), hexc("#4b4036")
    filled = {(x, y) for y in range(16) for x in range(8) if boot[y][x + 1] != "." and x + 1 < 9}
    for (x, y) in filled:
        edge = any((x + dx, y + dy) not in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        rows_kind = boot[y][x + 1]
        c = outline if edge else shade(cream, (knit(x, y) if rows_kind == "#" else rib(x, y)) / 240)
        icon.putpixel((x + 2, y + 1), c)
    for i, (x, y) in enumerate([(12, 8), (13, 9), (13, 10), (12, 11), (14, 7), (15, 8), (15, 9), (15, 10), (15, 11), (14, 12)]):
        icon.putpixel((x, y), hexc("#cfc6b4", 150 if i < 4 else 70))
    icon.save(out("mob_effect/muffled.png"))


# Woollen Armour icons: vanilla's leather sprites (tinted the wool's colour) with a subtle knit laid over them. The
# knit is drawn here, only inside the leather sprite's silhouette (read from the dev jar, not copied), as faint light
# and dark stitches that shade any colour. Also writes the item models, including one per armour trim.
VANILLA_JAR = os.path.join(os.path.dirname(__file__), "..", "..", "build", "moddev", "artifacts", "minecraft-patched-26.3.0.101-merged.jar")
MODELS = os.path.join(ROOT, "..", "models", "item")
ITEMS = os.path.join(ROOT, "..", "items")


def generate_woollen_icons():
    import io, json, zipfile
    if not os.path.exists(VANILLA_JAR):
        print("Skipping Woollen Armour icons: run ./gradlew build first so", VANILLA_JAR, "exists")
        return
    jar = zipfile.ZipFile(VANILLA_JAR)
    def sprite(name):
        return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/item/{name}.png"))).convert("RGBA")
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        base, over = sprite(f"leather_{piece}"), sprite(f"leather_{piece}_overlay")
        cloth = {(x, y) for x in range(16) for y in range(16) if base.getpixel((x, y))[3] and not over.getpixel((x, y))[3]}
        knit_img = Image.new("RGBA", (16, 16), CLEAR)
        for (x, y) in cloth:
            if not all((x + dx, y + dy) in cloth for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                continue  # leave the outline crisp
            # A V stitch: the two pixels of each stitch column swap light and dark row by row.
            knit_img.putpixel((x, y), (255, 255, 255, 46) if (x % 2 == 0) == (y % 2 == 0) else (0, 0, 0, 30))
        knit_img.save(out(f"item/woollen_{piece}_knit.png"))

        layers = {"layer0": f"minecraft:item/leather_{piece}", "layer1": f"minecraft:item/leather_{piece}_overlay",
                  "layer2": f"barelyfunctionalcontraptions:item/woollen_{piece}_knit"}
        os.makedirs(MODELS, exist_ok=True)
        def model(name, extra=None):
            textures = dict(layers)
            if extra:
                textures["layer3"] = extra
            with open(os.path.join(MODELS, name + ".json"), "w") as fh:
                fh.write(json.dumps({"parent": "minecraft:item/generated", "textures": textures}, indent=2) + "\n")
        model(f"woollen_{piece}")
        # The item definition: vanilla leather's, with each model swapped for the knitted one.
        item = json.loads(jar.read(f"assets/minecraft/items/leather_{piece}.json"))
        def swap(o):
            if isinstance(o, dict):
                if o.get("type") == "minecraft:dye":
                    o["default"] = (0xFF000000 | 0xF9FFFE) - (1 << 32)  # white wool
                ref = o.get("model")
                if isinstance(ref, str) and ref.startswith(f"minecraft:item/leather_{piece}"):
                    vanilla = json.loads(jar.read("assets/minecraft/models/item/" + ref.split("/", 1)[1] + ".json"))
                    name = "woollen_" + ref.split("leather_", 1)[1]
                    model(name, vanilla["textures"].get("layer2"))
                    o["model"] = "barelyfunctionalcontraptions:item/" + name
                for v in o.values():
                    swap(v)
            elif isinstance(o, list):
                for v in o:
                    swap(v)
        swap(item)
        with open(os.path.join(ITEMS, f"woollen_{piece}.json"), "w") as fh:
            fh.write(json.dumps(item, indent=2) + "\n")


def generate_sheep_treadmill():
    """Sheep Treadmill (WIP preview): a belt between steel rollers in a blue steel frame, a gantry with wheat hanging on a
    string, and a generator housing in the machines' casing at the back with a copper flywheel. Also writes its block and item models."""
    import json
    rnd = random.Random(31)
    BELT, BELT_L, BELT_D = hexc("#3b3a3f"), hexc("#55545b"), hexc("#26252a")
    STEEL, STEEL_L, STEEL_D = hexc("#8d929c"), hexc("#c3c8d0"), hexc("#5c616b")
    COPPER, COPPER_L, COPPER_D = hexc("#c7724a"), hexc("#e9a27a"), hexc("#8a4a2c")
    WHEAT, WHEAT_L, WHEAT_D, STRING = hexc("#d9b44a"), hexc("#f1d77a"), hexc("#9c7a2a"), hexc("#e8e2d0")
    LAMP, LAMP_D = hexc("#ffd75e"), hexc("#c9952a")

    # Side rails and gantry: the machines' blue-steel casing (CARTOON palette): outlined, a lit top edge and bolts.
    RAIL = ["OOOOOOOOOOOOOOOO",
            "HHHHHHHHHHHHHHHH",
            "MMBbMMMMMMMMBbMM",
            "MMbSMMMMMMMMbSMM",
            "MMMMMMMMMMMMMMMM",
            "SSSSSSSSSSSSSSSS",
            "OOOOOOOOOOOOOOOO"] + ["MMMMMMMMMMMMMMMM"] * 9
    grid(RAIL, CARTOON, Image.new("RGBA", (16, 16), CLEAR)).save(out("block/sheep_treadmill/rail.png"))
    # Strut: the thin parts (rail tops, gantry posts and crossbar) in plain casing metal, a lit edge on one side.
    grid(["HMMMMMMMMMMMMMMS"] * 16, CARTOON, Image.new("RGBA", (16, 16), CLEAR)).save(out("block/sheep_treadmill/strut.png"))

    # Belt: dark slats across it with a lighter tread on each, animated: the slats repeat every 3 pixels, so 3 frames
    # each one pixel further on scroll the belt from front to back (north to south on the model), under the sheep's feet.
    frames = 3
    belt = Image.new("RGBA", (16, 16 * frames), BELT)
    for f in range(frames):
        for y in range(16):
            row = (y - f) % 3
            for x in range(16):
                if row == 2:
                    belt.putpixel((x, f * 16 + y), BELT_D)
                elif row == 0 and 1 < x < 14:
                    belt.putpixel((x, f * 16 + y), BELT_L)
    belt.save(out("block/sheep_treadmill/belt.png"))
    belt.crop((0, 0, 16, 16)).save(out("block/sheep_treadmill/belt_still.png"))  # the first frame, for a stopped belt
    with open(out("block/sheep_treadmill/belt.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 2\n  }\n}\n')

    # Roller: banded steel.
    roller = Image.new("RGBA", (16, 16), STEEL)
    for y in range(16):
        for x in range(16):
            if y in (0, 1):
                roller.putpixel((x, y), STEEL_L)
            elif y in (14, 15) or x % 5 == 0:
                roller.putpixel((x, y), STEEL_D)
    roller.save(out("block/sheep_treadmill/roller.png"))

    # Housing: the machines' casing (as on the Powered Furnace and Separator): frame, corner bolts and a port that
    # glows while it runs. The housing is 12 pixels tall, so these use the top 12 rows (its top face the top 6).
    filler = ["MMMMMMMMMMMMMMMM"] * 4
    HOUSING_SIDE = ["OOOOOOOOOOOOOOOO",
                    "OHHHHHHHHHHHHHMO",
                    "OHBbMMMMMMMMBbSO",
                    "OHbSMMOOOOMMbSSO",
                    "OHMMMOPPPPOMMMSO",
                    "OHMMOPpggpPOMMSO",
                    "OHMMOPpggpPOMMSO",
                    "OHMMMOPPPPOMMMSO",
                    "OHMMMMOOOOMMMMSO",
                    "OHBbMMMMMMMMBbSO",
                    "OHbSSSSSSSSSbSSO",
                    "OOOOOOOOOOOOOOOO"] + filler
    HOUSING_FRONT = ["OOOOOOOOOOOOOOOO",  # the side the belt runs into
                     "OHHHHHHHHHHHHHMO",
                     "OHBbMMMMMMMMBbSO",
                     "OHbSMMMMMMMMbSSO",
                     "OHMMMMMMMMMMMMSO",
                     "OHMOOOOOOOOOOMSO",
                     "OHMOKKKKKKKKOMSO",
                     "OHMOKKKKKKKKOMSO",
                     "OHMOOOOOOOOOOMSO",
                     "OHBbMMMMMMMMBbSO",
                     "OHbSSSSSSSSSbSSO",
                     "OOOOOOOOOOOOOOOO"] + filler
    HOUSING_TOP = ["OOOOOOOOOOOOOOOO",
                   "OHHHHHHHHHHHHHMO",
                   "OHBbMMMMMMMMBbSO",
                   "OHbSMMMMMMMMbSSO",
                   "OHbSSSSSSSSSbSSO",
                   "OOOOOOOOOOOOOOOO"] + ["MMMMMMMMMMMMMMMM"] * 10
    for name, rows in (("housing_side", HOUSING_SIDE), ("housing_front", HOUSING_FRONT), ("housing_top", HOUSING_TOP)):
        grid(rows, CARTOON, Image.new("RGBA", (16, 16), CLEAR)).save(out(f"block/sheep_treadmill/{name}.png"))

    # Flywheel: a copper wheel with four spokes and a steel hub.
    fly = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 6.0 <= d <= 7.6:
                fly.putpixel((x, y), COPPER_L if y < 8 else COPPER)
            elif d < 6.0 and (abs(x - 7.5) < 1 or abs(y - 7.5) < 1):
                fly.putpixel((x, y), COPPER_D)
            if d < 2.2:
                fly.putpixel((x, y), STEEL_L if d < 1.2 else STEEL)
    fly.save(out("block/sheep_treadmill/flywheel.png"))

    # Wheat: a tied bundle, ears up, on a clear background; and a string.
    wheat = Image.new("RGBA", (16, 16), CLEAR)
    for x in range(3, 13):
        for y in range(1, 15):
            if y < 7 and (x + y) % 3 == 0:
                continue
            c = WHEAT_L if y < 6 else (WHEAT if y < 12 else WHEAT_D)
            if x in (3, 12) and y > 6:
                continue
            wheat.putpixel((x, y), c)
    for x in range(4, 12):
        wheat.putpixel((x, 8), STRING)
    wheat.save(out("block/sheep_treadmill/wheat.png"))
    rope = Image.new("RGBA", (16, 16), STRING)
    for y in range(0, 16, 3):
        for x in range(16):
            rope.putpixel((x, y), shade(STRING, 0.82))
    rope.save(out("block/sheep_treadmill/rope.png"))
    lamp = Image.new("RGBA", (16, 16), LAMP)
    for i in range(16):
        lamp.putpixel((i, 15), LAMP_D); lamp.putpixel((15, i), LAMP_D)
    lamp.save(out("block/sheep_treadmill/lamp.png"))

    # The model, facing north: the back block is z 0-16 and the front block z -16-0 (block models may reach a block
    # beyond their own). The blockstate turns it to face the way it was placed.
    T = "barelyfunctionalcontraptions:block/sheep_treadmill/"
    def box(frm, to, tex, faces=None, shade_on=True):
        faces = faces or {}
        spans = {"north": (0, 1), "south": (0, 1), "east": (2, 1), "west": (2, 1), "up": (0, 2), "down": (0, 2)}
        el = {"from": frm, "to": to, "faces": {}}
        for face, (a, b) in spans.items():
            w, h = abs(to[a] - frm[a]), abs(to[b] - frm[b])
            if w == 0 or h == 0:
                continue
            el["faces"][face] = {"uv": [0, 0, min(w, 16), min(h, 16)], "texture": "#" + faces.get(face, tex)}
        return el
    elements = [
        box([0, 0, -16], [2, 7, 10], "rail", {"up": "strut", "down": "strut"}),                        # left rail
        box([14, 0, -16], [16, 7, 10], "rail", {"up": "strut", "down": "strut"}),                      # right rail
        box([2, 1, -15], [14, 5, -11], "roller"),                    # front roller
        box([2, 5, -14], [14, 6, 10], "belt"),                       # belt
        box([2, 1, -11], [14, 2, 10], "belt"),                       # the belt's return run
        box([0, 7, -16], [2, 24, -14], "strut"),                     # gantry uprights
        box([14, 7, -16], [16, 24, -14], "strut"),
        box([0, 22, -16], [16, 24, -14], "strut"),                   # crossbar
        box([7.5, 16, -15.5], [8.5, 22, -14.5], "rope"),             # string
        box([0, 0, 10], [16, 12, 16], "housing_side", {"north": "housing_front", "up": "housing_top", "down": "housing_side"}),
        box([16, 1, 9], [17, 11, 15], "flywheel", {"north": "rail", "south": "rail", "up": "rail", "down": "rail"}),
        box([6, 12, 12], [10, 13, 14], "lamp"),                      # running light
    ]
    # The wheat: two crossed planes, so it looks like a bundle from any side.
    for rot in (45, -45):
        elements.append({"from": [5, 8, -15], "to": [11, 16, -15], "rotation": {"angle": rot, "axis": "y", "origin": [8, 12, -15]},
                         "faces": {"north": {"uv": [3, 1, 13, 15], "texture": "#wheat"}, "south": {"uv": [3, 1, 13, 15], "texture": "#wheat"}}})
    textures = {k: T + k for k in ("rail", "strut", "belt", "roller", "housing_side", "housing_front", "housing_top", "flywheel", "wheat", "rope", "lamp")}
    textures["particle"] = T + "rail"
    models = os.path.join(ROOT, "..", "models")
    with open(os.path.join(models, "block", "sheep_treadmill.json"), "w") as fh:
        fh.write(json.dumps({"parent": "minecraft:block/block", "textures": textures, "elements": elements}, indent=2) + "\n")
    # Stopped: the same model with the still belt.
    still = dict(textures, belt=T + "belt_still")
    with open(os.path.join(models, "block", "sheep_treadmill_still.json"), "w") as fh:
        fh.write(json.dumps({"parent": "barelyfunctionalcontraptions:block/sheep_treadmill", "textures": still}, indent=2) + "\n")
    with open(os.path.join(models, "block", "sheep_treadmill_front.json"), "w") as fh:
        fh.write(json.dumps({"textures": {"particle": T + "rail"}}, indent=2) + "\n")
    # Item: the whole model moved half a block back so it's centred, then turned and shrunk to fit a slot.
    import copy
    centred = copy.deepcopy(elements)
    for el in centred:
        el["from"][2] += 8; el["to"][2] += 8
        if "rotation" in el:
            el["rotation"]["origin"][2] += 8
    with open(os.path.join(models, "item", "sheep_treadmill.json"), "w") as fh:
        fh.write(json.dumps({"parent": "minecraft:block/block", "textures": textures, "elements": centred, "display": {
            "gui": {"rotation": [30, 135, 0], "translation": [0, 0.5, 0], "scale": [0.44, 0.44, 0.44]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}}, indent=2) + "\n")


TREADMILL_WHEAT = [  # a sheaf of wheat tied round the middle, 14x14
    "...Y..Y..Y....",
    "..YWY.WY.YWY..",
    "..YYWYYWYWYY..",
    "...YYWYYWYY...",
    "....YYWWYY....",
    ".....YWWY.....",
    ".....FYYF.....",
    ".....SSSS.....",
    ".....FYYF.....",
    "....FF..FF....",
    "....F.FF.F....",
    "...FF.F.F.F...",
    "...F..F..FF...",
    "..F...F...F...",
]


def generate_sheep_treadmill_gui():
    """Sheep Treadmill window: energy gauge, food box (wheat over the food slot, a stamina bar), info window."""
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # energy gauge, fill 11..40 x 19..82
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 46, 16, 88, 84, GUI["gauge"])          # food box
    grid(TREADMILL_WHEAT, {"S": GUI["tick"], "Y": GUI["tick"], "W": GUI["tick"], "F": GUI["tick"]}, img, 60, 24)
    slot(img, 59, 56)                                 # food slot
    inset(img, 48, 77, 86, 82, GUI["panel"])          # stamina bar, fill 49..84 x 78..80
    inset(img, 92, 16, 168, 78, GUI["panel"])         # info window
    for x in range(96, 164):
        img.putpixel((x, 44), hexc("#2c2e36"))
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    rnd = random.Random(41)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice([hexc("#e3e1ea"), hexc("#b7b3c6"), hexc("#d9b44a")]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    low, high, shine = hexc("#7a5a1a"), hexc("#f1d77a"), hexc("#fff3c4")
    for y in range(64):                                             # energy fill: golden, like the wheat
        for x in range(30):
            c = mix(high, low, y / 63.0)
            if (x + y) % 4 == 0:
                c = mix(c, shine, 0.2)
            img.putpixel((176 + x, y), c)
    for f in range(4):                                              # energy surface shimmer
        for x in range(30):
            img.putpixel((176 + x, 66 + f * 2), shine if (x + f * 2) % 8 == 0 else high)
            img.putpixel((176 + x, 67 + f * 2), mix(high, low, 0.3))
    grid(TREADMILL_WHEAT, {"S": hexc("#e8e2d0"), "Y": hexc("#d9b44a"), "W": hexc("#f1d77a"), "F": hexc("#9c7a2a")}, img, 176, 80)
    for x in range(36):                                             # stamina fill
        for y in range(3):
            img.putpixel((176 + x, 96 + y), hexc("#8fd5ff") if y == 0 else hexc("#3f8fd0"))
    img.save(out("gui/sheep_treadmill.png"))


# ---------------------------------------------------------------------------
# Thermal Reaction Chamber: the machines' blue-steel casing split down the middle (hot on the left as you face it,
# cold on the right). Red-hot vent slats on the hot side, frosted ones on the cold side, a gauge panel at the front
# (two thermometers and the balance needle), water and power ports at the back, and a window on top where flame and
# snow chase each other while it reacts. Each texture also comes "frozen", frosted over, for when it freezes solid.
# ---------------------------------------------------------------------------
TRC_VENTS = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSOOOOOOOOSSSO",
    "OHMOVVVVVVVVOMSO",
    "OHMOkkkkkkkkOMSO",
    "OHMOVVVVVVVVOMSO",
    "OHMOkkkkkkkkOMSO",
    "OHMOVVVVVVVVOMSO",
    "OHMOkkkkkkkkOMSO",
    "OHMOVVVVVVVVOMSO",
    "OHMOkkkkkkkkOMSO",
    "OHbSOOOOOOOOSSSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]
TRC_FRONT = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMOOMOOOOMOOMSO",
    "OHMOrMOWWOMOuMSO",
    "OHMOrMOWWOMOuMSO",
    "OHMOrMOXNOMOuMSO",
    "OHMOrMOXXOMOuMSO",
    "OHMORMOOOOMOUMSO",
    "OHMORMMMMMMOUMSO",
    "OHMOOMOOOOMOOMSO",
    "OHMMMMOLLOMMMMSO",
    "OHBbMMOOOOMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]
TRC_BACK = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbSMMMMMMMMbSSO",
    "OHMMMMMMMMMMMMSO",
    "OHOOOOMMMMOOOOSO",
    "OOuUUuOMMOeEEeOO",
    "OOUKKUOMMOEKKEOO",
    "OOUKKUOMMOEKKEOO",
    "OOuUUuOMMOeEEeOO",
    "OHOOOOMMMMOOOOSO",
    "OHMMMMMMMMMMMMSO",
    "OHMMMMMMMMMMMMSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]
TRC_TOP = [
    "OOOOOOOOOOOOOOOO",
    "OHHHHHHHHHHHHHMO",
    "OHBbMMMMMMMMBbSO",
    "OHbOOOOOOOOOOSSO",
    "OHMOggggggggOMSO",
    "OHMOggggggggOMSO",
    "OHMOggggggggOMSO",
    "OHMOggggggggOMSO",
    "OHMOggggggggOMSO",
    "OHMOggggggggOMSO",
    "OHMOggggggggOMSO",
    "OHMOggggggggOMSO",
    "OHbOOOOOOOOOOSSO",
    "OHBbMMMMMMMMBbSO",
    "OHbSSSSSSSSSbSSO",
    "OOOOOOOOOOOOOOOO",
]
TRC_PAL = dict(CARTOON)
TRC_PAL.update({
    "r": hexc("#7d3a44"), "R": hexc("#e8561a"),   # hot thermometer: tube / bulb
    "u": hexc("#2f4560"), "U": hexc("#3f8fcf"),   # cold thermometer / water port ring
    "W": hexc("#e3e1ea"), "X": hexc("#2a2238"), "N": hexc("#ffffff"),  # dial face / needle
    "L": hexc("#55d66b"),                          # status light
    "e": hexc("#c98a12"), "E": hexc("#ffe14a"),   # power port ring
    "g": hexc("#2a3550"),                          # dark glass
})


def _trc_frost(img, seed):
    """Frosts a texture over: pale ice creeping in from the edges, with glints."""
    rnd = random.Random(seed)
    out_img = img.copy()
    for y in range(16):
        for x in range(16):
            c = img.getpixel((x, y))
            edge = min(x, y, 15 - x, 15 - y)
            amount = 0.7 if edge == 0 else 0.55 if edge <= 2 else 0.35
            amount += rnd.random() * 0.15
            f = mix(c, ICE[4], amount)
            if rnd.random() < 0.06:
                f = ICE[5]
            out_img.putpixel((x, y), f)
    return out_img


def _trc_window_frame(f, frames):
    """One frame of the top window: a flame and a snow swirl chasing each other round the glass."""
    img = Image.new("RGBA", (8, 8), hexc("#2a3550"))
    angle = 2 * math.pi * f / frames
    for y in range(8):
        for x in range(8):
            dx, dy = x - 3.5, y - 3.5
            a = math.atan2(dy, dx)
            r = math.hypot(dx, dy)
            # Two arms of a spiral, 180 degrees apart: fire and frost.
            for arm, pal in ((0.0, FIRE), (math.pi, ICE)):
                d = (a - angle - arm - r * 0.9) % (2 * math.pi)
                if d < 1.1 and r < 4.2:
                    t = 1 - d / 1.1
                    c = pal[3 + min(2, int(t * 3))] if pal is FIRE else pal[2 + min(3, int(t * 4))]
                    img.putpixel((x, y), mix(img.getpixel((x, y)), c, 0.4 + 0.6 * t))
            if r < 1.2:
                img.putpixel((x, y), mix(FIRE[5], ICE[5], 0.5))  # the white-hot, ice-cold core
    return img


def generate_thermal_reaction_chamber():
    base = "block/thermal_reaction_chamber/"
    os.makedirs(out(base), exist_ok=True)
    hot_idle = dict(TRC_PAL, V=hexc("#5a2a22"))
    cold_idle = dict(TRC_PAL, V=hexc("#2f4560"))
    textures = {
        "front": grid(TRC_FRONT, TRC_PAL),
        "back": grid(TRC_BACK, TRC_PAL),
        "top": grid(TRC_TOP, TRC_PAL),
        "bottom": grid(MACHINE_BLOCK, CARTOON),
        "hot": grid(TRC_VENTS, hot_idle),
        "cold": grid(TRC_VENTS, cold_idle),
    }
    # The hot side has a warm cast, the cold side a frosty one.
    for x, y in [(x, y) for x in range(16) for y in range(16)]:
        textures["hot"].putpixel((x, y), mix(textures["hot"].getpixel((x, y)), FIRE[1], 0.12))
        textures["cold"].putpixel((x, y), mix(textures["cold"].getpixel((x, y)), ICE[3], 0.15))
    for name, img in textures.items():
        img.save(out(base + name + ".png"))
        _trc_frost(img, sum(map(ord, name))).save(out(base + name + "_frozen.png"))

    # Active vents: glowing slats that flicker (4 frames, animated).
    for side, pal, rows in (("hot", FIRE, (2, 3, 4, 5)), ("cold", ICE, (2, 3, 4, 5))):
        strip = Image.new("RGBA", (16, 64))
        for f in range(4):
            frame = textures[side].copy()
            for i, y in enumerate((4, 6, 8, 10)):
                for x in range(4, 12):
                    flicker = (x * 3 + i * 5 + f * 7) % 9
                    c = pal[5] if flicker == 0 else pal[4] if flicker < 4 else pal[3]
                    frame.putpixel((x, y), c)
                for x in range(4, 12):  # the glow spills onto the slat below
                    frame.putpixel((x, y + 1), mix(frame.getpixel((x, y + 1)), pal[2], 0.45))
            strip.paste(frame, (0, f * 16))
        strip.save(out(base + side + "_active.png"))
        with open(out(base + side + "_active.png.mcmeta"), "w") as fh:
            fh.write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": true\n  }\n}\n')

    # Active top: the window's swirl, 16 frames.
    frames = 16
    strip = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        frame = textures["top"].copy()
        frame.paste(_trc_window_frame(f, frames), (4, 4))
        strip.paste(frame, (0, f * 16))
    strip.save(out(base + "top_active.png"))
    with open(out(base + "top_active.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 2\n  }\n}\n')

    # Fins on the sides: plain casing metal for the hot side (scorched) and the cold side (frosted).
    fin = Image.new("RGBA", (16, 16), CARTOON["M"])
    for x in range(16):
        fin.putpixel((x, 0), CARTOON["H"]); fin.putpixel((x, 15), CARTOON["O"])
    for name, tint, amount in (("fin_hot", FIRE[1], 0.25), ("fin_cold", ICE[4], 0.35)):
        img = fin.copy()
        for x in range(16):
            for y in range(16):
                img.putpixel((x, y), mix(img.getpixel((x, y)), tint, amount))
        img.save(out(base + name + ".png"))
        _trc_frost(img, len(name)).save(out(base + name + "_frozen.png"))


def generate_thermal_reaction_chamber_gui():
    """Thermal Reaction Chamber window: energy gauge, hot thermometer, centre panel (pairs, output, balance scale,
    danger bar), cold thermometer, water gauge, the two boost slots with burn bars."""
    shears = Image.open(out("gui/automatic_shears.png")).convert("RGBA")
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    for gx in (10, 135):                              # energy and water gauges, fill 30x64 at (11|136, 19)
        inset(img, gx, 18, gx + 32, 84, GUI["gauge"])
        for k in range(1, 8):
            ty = 19 + round(64 * k / 8)
            for x in range(gx + 26, gx + 31):
                img.putpixel((x, ty), GUI["tick"])
    inset(img, 47, 18, 57, 84, GUI["gauge"])          # hot thermometer, fill 8x64 at (48, 19)
    inset(img, 121, 18, 131, 84, GUI["gauge"])        # cold thermometer, fill 8x64 at (122, 19)
    inset(img, 61, 16, 117, 57, GUI["panel"])         # centre panel
    inset(img, 61, 49, 117, 57, GUI["gauge"])         # danger bar housing, fill 62..115 x 51..54
    slot(img, 67, 62)                                 # Blaze Rod
    slot(img, 95, 62)                                 # Blue Ice
    inset(img, 65, 80, 84, 85, GUI["gauge"])          # burn bars, fill 66..82 x 81..83
    inset(img, 93, 80, 112, 85, GUI["gauge"])
    # Faint item hints in the empty boost slots: a rod and a block of ice.
    for i in range(10):
        img.putpixel((70 + i, 74 - i), mix(GUI["gauge"], FIRE[3], 0.35))
    for y in range(66, 74):
        for x in range(99, 107):
            if x in (99, 106) or y in (66, 73):
                img.putpixel((x, y), mix(GUI["gauge"], ICE[3], 0.35))
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    rnd = random.Random(97)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 190), rnd.choice(FIRE[2:5] if x < 88 else ICE[2:5]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    img.paste(shears.crop((176, 64, 208, 80)), (176, 162))          # area button background (below the thermometer fills)
    for u, top, bottom, shine in ((176, FIRE[4], FIRE[1], FIRE[5]), (206, hexc("#2f6fd0"), hexc("#163a8a"), hexc("#7fb8ff"))):
        for y in range(64):                                         # energy (amber) and water (blue) fills
            for x in range(30):
                c = mix(top, bottom, y / 63.0)
                if (x + y) % 4 == 0:
                    c = mix(c, shine, 0.2)
                img.putpixel((u + x, y), c)
        for f in range(4):                                          # surface shimmer
            for x in range(30):
                img.putpixel((u + x, 66 + f * 2), shine if (x + f * 2) % 8 == 0 else top)
                img.putpixel((u + x, 67 + f * 2), mix(top, bottom, 0.3))
    for x in range(50):                                             # balance scale: hot (left) to cold (right)
        t = x / 49.0
        c = mix(FIRE[3], hexc("#8a8e9a"), t * 2) if t < 0.5 else mix(hexc("#8a8e9a"), ICE[3], (t - 0.5) * 2)
        for y in range(6):
            img.putpixel((176 + x, 80 + y), mix(c, CARTOON["O"], 0.35) if y in (0, 5) else c)
        if x in (24, 25):
            for y in range(6):
                img.putpixel((176 + x, 80 + y), hexc("#e3e1ea"))   # the balanced mark
    for u, pal in ((176, FIRE), (186, ICE)):                        # thermometer fills, 8x64
        for y in range(64):
            for x in range(8):
                c = mix(pal[4], pal[1], y / 63.0)
                if x == 1:
                    c = mix(c, pal[5], 0.5)
                img.putpixel((u + x, 90 + y), c)
    img.save(out("gui/thermal_reaction_chamber.png"))


# ---------------------------------------------------------------------------
# Echo Chamber (WIP preview): a 3x3x3 machine in the blue-steel casing, lined with sculk. A big resonator dish on the
# front whose rings pulse outward from a glowing heart, egg-crate sound-dampening foam on the side walls with sculk
# creeping over some of it, a power port between louvred vents at the back, and a sculk-covered roof with a catalyst dome
# in a crown of sculk fronds, the biggest two like the Warden sheep's "ears". The model is built by generate_echo_chamber_model.py; these
# are its textures. Wall textures tile, one per block face.
# ---------------------------------------------------------------------------
SCULK = [hexc("#051a1f"), hexc("#0b2e36"), hexc("#10474f"), hexc("#0f7e8a"), hexc("#29dfeb"), hexc("#c9fbff")]
ECHO_FOAM = [hexc("#161b29"), hexc("#222a3e"), hexc("#303b56"), hexc("#43507a"), hexc("#5a6a98")]


def _echo_animated(base, name, frames, frametime, draw):
    strip = Image.new("RGBA", (draw(0).width, draw(0).height * frames))
    for f in range(frames):
        strip.paste(draw(f), (0, f * draw(0).height))
    strip.save(out(base + name + ".png"))
    with open(out(base + name + ".png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": %d,\n    "interpolate": true\n  }\n}\n' % frametime)


def _echo_foam():
    """Egg-crate foam: bumps lit from the top left. Their period is 4 pixels, so it tiles seamlessly."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            bump = (math.cos(math.pi * x / 2) + math.cos(math.pi * y / 2)) / 2
            slope = math.sin(math.pi * x / 2) + math.sin(math.pi * y / 2)
            v = 2 + bump * 1.2 - slope * 0.5
            img.putpixel((x, y), ECHO_FOAM[max(0, min(4, int(round(v))))])
    return img


def _echo_patch(img, seed, centre, radius):
    """A ragged patch of sculk spreading from {@code centre}, with a few glowing nodes."""
    rnd = random.Random(seed)
    cx, cy = centre
    for y in range(img.height):
        for x in range(img.width):
            d = math.hypot(x - cx, y - cy) + rnd.random() * 1.6
            if d < radius:
                img.putpixel((x, y), SCULK[1] if rnd.random() < 0.6 else SCULK[2])
                if d < radius * 0.7 and rnd.random() < 0.07:
                    img.putpixel((x, y), SCULK[4])
            elif d < radius + 0.8 and rnd.random() < 0.5:
                img.putpixel((x, y), mix(img.getpixel((x, y)), SCULK[1], 0.6))


def _echo_dish(f, frames):
    """The 32x32 resonator dish: a dark sculk well in rings, a wave running outward from the middle, one frame."""
    img = Image.new("RGBA", (32, 32), SCULK[0])
    phase = f / frames
    for y in range(32):
        for x in range(32):
            r = math.hypot(x - 15.5, y - 15.5)
            if r > 15.6:
                img.putpixel((x, y), SCULK[1] if (x + y) % 2 else SCULK[2])  # the corners outside the dish
                continue
            ring = int(r / 2) % 2
            c = SCULK[0] if ring else SCULK[1]
            if int(r) % 4 == 3:
                c = SCULK[2]  # the dish's ridges
            d = (r / 15.6 - phase) % 1.0
            if d < 0.12:
                c = mix(c, SCULK[4], 1 - d / 0.12)
            elif d > 0.9:
                c = mix(c, SCULK[3], (d - 0.9) / 0.1 * 0.6)
            img.putpixel((x, y), c)
    # The glowing heart in the middle, breathing in and out with the wave.
    glow = 0.5 + 0.5 * math.cos(2 * math.pi * phase)
    for y in range(32):
        for x in range(32):
            r = math.hypot(x - 15.5, y - 15.5)
            if r < 3.6:
                img.putpixel((x, y), mix(SCULK[4], SCULK[5], max(0.0, 1 - r / 3.6) * (0.4 + 0.6 * glow)))
            elif r < 4.6:
                img.putpixel((x, y), mix(SCULK[2], SCULK[3], glow))
    for a in range(0, 360, 6):  # the rim
        x, y = 15.5 + 15.2 * math.cos(math.radians(a)), 15.5 + 15.2 * math.sin(math.radians(a))
        img.putpixel((int(round(x)), int(round(y))), CARTOON["S"])
    return img


def generate_echo_chamber():
    base = "block/echo_chamber/"
    import shutil
    shutil.rmtree(out(base), ignore_errors=True)
    os.makedirs(out(base), exist_ok=True)

    foam = _echo_foam()
    foam.save(out(base + "foam.png"))
    for i, (centre, radius) in enumerate((((2, 14), 7.5), ((14, 15), 6.0), ((3, 1), 5.0))):
        patched = foam.copy()
        _echo_patch(patched, 20 + i, centre, radius)
        patched.save(out(base + "foam_sculk_%d.png" % i))

    _echo_animated(base, "dish", 16, 2, lambda f: _echo_dish(f, 16))

    # Casing plate: plain metal panels with a seam round each block and rivets in the corners.
    plate = Image.new("RGBA", (16, 16), CARTOON["M"])
    for i in range(16):
        plate.putpixel((i, 0), CARTOON["H"]); plate.putpixel((0, i), CARTOON["H"])
        plate.putpixel((i, 15), CARTOON["S"]); plate.putpixel((15, i), CARTOON["S"])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        plate.putpixel((x, y), CARTOON["B"]); plate.putpixel((x + 1, y + 1), CARTOON["b"])
    plate.save(out(base + "plate.png"))

    # Pillars: a column of casing with bolts down it, lit on the left.
    pillar = Image.new("RGBA", (16, 16), CARTOON["M"])
    for y in range(16):
        for x in range(16):
            c = CARTOON["H"] if x < 3 else CARTOON["S"] if x > 12 else CARTOON["M"]
            if x in (0, 15):
                c = CARTOON["O"]
            pillar.putpixel((x, y), c)
        if y % 8 == 3:
            for x in (6, 9):
                pillar.putpixel((x, y), CARTOON["B"]); pillar.putpixel((x, y + 1), CARTOON["b"])
    pillar.save(out(base + "pillar.png"))

    # The plinth and the roof band: a dark rail with bright bolts, the bottom four rows of each block face.
    band = plate.copy()
    for y in range(12, 16):
        for x in range(16):
            c = CARTOON["S"] if y in (12, 15) else CARTOON["k"]
            if y in (13, 14) and x % 8 == 3:
                c = CARTOON["B"] if y == 13 else CARTOON["b"]
            band.putpixel((x, y), c)
    band.save(out(base + "band.png"))

    # Power port: a big yellow ring on a casing plate.
    port = plate.copy()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 6.5:
                port.putpixel((x, y), CARTOON["O"] if r >= 5.6 else CARTOON["E"] if r >= 3.8 else CARTOON["e"] if r >= 2.8 else CARTOON["K"])
    port.save(out(base + "port.png"))

    # Vents: the dark inside of a vent housing, glowing faintly cyan towards the bottom from the sculk inside.
    vent = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            vent.putpixel((x, y), mix(CARTOON["K"], SCULK[3], max(0.0, (y - 6) / 10) * 0.55))
    vent.save(out(base + "vent_back.png"))
    # The exhaust grille: dark slots between casing bars, tiling sideways; its four rows sit at the bottom.
    grille = Image.new("RGBA", (16, 16), CARTOON["M"])
    for x in range(16):
        for y in range(12, 16):
            slot = x % 4 != 0 and y in (13, 14)
            grille.putpixel((x, y), CARTOON["K"] if slot else CARTOON["H"] if y == 12 else CARTOON["S"] if y == 15 else CARTOON["M"])
    grille.save(out(base + "grille.png"))

    # Trim: plain casing metal for bezels and edges.
    trim = Image.new("RGBA", (16, 16), CARTOON["M"])
    for x in range(16):
        trim.putpixel((x, 0), CARTOON["H"]); trim.putpixel((x, 15), CARTOON["S"])
    trim.save(out(base + "trim.png"))

    grid(MACHINE_BLOCK, CARTOON).save(out(base + "bottom.png"))

    # The ears: big sculk fronds like the Warden sheep's, narrow at the tip and wide at the root, their glow breathing
    # in and out.
    def ear(f):
        img = Image.new("RGBA", (16, 16), CLEAR)
        glow = 0.5 + 0.5 * math.sin(2 * math.pi * f / 8)
        for y in range(16):
            half = 1.2 + y * 0.42
            for x in range(16):
                cx = abs(x - 7.5)
                if cx > half:
                    continue
                if y <= 2 and cx < 1.6:
                    c = mix(SCULK[4], SCULK[5], glow)
                elif cx < 0.9:
                    c = mix(SCULK[3], SCULK[4], 0.4 + 0.6 * glow)
                elif cx > half - 1:
                    c = hexc("#0A3F47")
                else:
                    c = hexc("#0E5A63") if (x + y) % 3 else hexc("#0A3F47")
                    if (x * 7 + y * 3) % 11 == 0:
                        c = mix(SCULK[3], SCULK[4], glow)
                img.putpixel((x, y), c)
        return img
    _echo_animated(base, "ear", 8, 4, ear)

def generate_echo_chamber_gui():
    """Echo Chamber window: energy gauge, then a panel with the herd and output, the 16-step noise meter (too quiet,
    the sweet spot, too loud), the steadiness bar, and the drowsiness and shriek bars; the status lamp below."""
    trc = Image.open(out("gui/thermal_reaction_chamber.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 108)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # energy gauge, fill 30x64 at (11, 19)
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 48, 16, 166, 84, GUI["panel"])         # the listening panel
    inset(img, 50, 41, 164, 51, GUI["gauge"])         # noise meter: 16 segments of 6 (+1 gap) at (51 + 7k, 42), 8 tall
    for k in range(16):
        zone = hexc("#5a3a14") if k < 5 else hexc("#1f5a2e") if k <= 10 else hexc("#5a1f24")
        for y in range(42, 50):
            for x in range(51 + 7 * k, 57 + 7 * k):
                img.putpixel((x, y), mix(GUI["gauge"], zone, 0.55))
    for x in (51 + 7 * 5, 56 + 7 * 10):              # the sweet spot's ends, marked under the meter
        for y in range(52, 55):
            img.putpixel((x, y), hexc("#55d66b"))
    for x in range(51 + 7 * 5, 57 + 7 * 10):
        img.putpixel((x, 54), hexc("#55d66b"))
    inset(img, 50, 59, 164, 64, GUI["gauge"])         # steadiness, fill 113x3 at (51, 60)
    inset(img, 50, 70, 105, 75, GUI["gauge"])         # drowsiness, fill 54x3 at (51, 71)
    inset(img, 109, 70, 164, 75, GUI["gauge"])        # shriek, fill 54x3 at (110, 71)
    # Tiny marks over the two meters: "z" for drowsy, "!" for shriek.
    for x, y in ((51, 66), (52, 66), (53, 66), (52, 67), (51, 68), (52, 68), (53, 68)):  # a little "z"
        img.putpixel((x, y), SCULK[4])
    for y in (65, 66, 67):
        img.putpixel((110, y), hexc("#e35d5d"))
    img.putpixel((110, 69), hexc("#e35d5d"))
    inset(img, 8, 89, 18, 99, GUI["gauge"])           # status lamp housing, lamp at (9, 90)
    rnd = random.Random(41)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 105), rnd.choice(SCULK[2:5]))
    # ---- sprites ----
    img.paste(trc.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    shears = Image.open(out("gui/automatic_shears.png")).convert("RGBA")
    img.paste(shears.crop((176, 64, 208, 80)), (176, 146))          # area button background
    img.paste(trc.crop((176, 0, 206, 74)), (176, 0))                # energy fill (amber) and its shimmer
    for k in range(16):                                             # lit noise segments, 6x8 at (7k, 200)
        zone = (FIRE[4], FIRE[2]) if k < 5 else (hexc("#8cf08c"), hexc("#2f9a45")) if k <= 10 else (hexc("#ff8a8a"), hexc("#b8262f"))
        for y in range(8):
            for x in range(6):
                c = mix(zone[0], zone[1], y / 7.0)
                if y == 0:
                    c = mix(c, hexc("#ffffff"), 0.4)
                img.putpixel((7 * k + x, 200 + y), c)
    img.save(out("gui/echo_chamber.png"))


# ---------------------------------------------------------------------------
# Static Generator: a blue-steel base with a charge gauge on its front, a rubber belt running up a column into a
# polished copper dome that crackles while it charges. The Copper Comb's post uses vanilla's copper textures (one for
# each oxidation stage); its bristles are drawn here.
# ---------------------------------------------------------------------------
COPPER = [hexc("#5a2a1c"), hexc("#8a4630"), hexc("#b4684a"), hexc("#d88a63"), hexc("#efb08a"), hexc("#fbe0c8")]
SPARK = [hexc("#5fb8ff"), hexc("#bfe6ff"), hexc("#ffffff")]


def generate_static_generator():
    base = "block/static_generator/"
    os.makedirs(out(base), exist_ok=True)

    side = grid(MACHINE_BLOCK, CARTOON)  # the casing with its corner bolts; a lightning bolt in the socket
    for y in range(4, 12):
        for x in range(4, 12):
            side.putpixel((x, y), CARTOON["k"])
    for x, y in ((8, 5), (7, 6), (8, 6), (6, 7), (7, 7), (7, 8), (8, 8), (9, 8), (8, 9), (7, 10), (8, 10), (7, 11)):
        if 4 <= y < 12:
            side.putpixel((x, y), CARTOON["E"] if (x + y) % 3 else CARTOON["Y"])
    side.save(out(base + "side.png"))

    # Front: a charge gauge, a tall glass tube that glows blue as the dome charges (dark here; the window shows the level).
    front = grid(MACHINE_BLOCK, CARTOON)
    for y in range(4, 12):
        for x in range(4, 12):
            front.putpixel((x, y), CARTOON["k"])
    for y in range(5, 11):
        for x in range(6, 10):
            front.putpixel((x, y), hexc("#1b2a3a") if x in (6, 9) else hexc("#24405e"))
    for x in range(5, 11):
        front.putpixel((x, 11), CARTOON["S"])
    front.save(out(base + "front.png"))

    top = grid(MACHINE_BLOCK, CARTOON)
    top.save(out(base + "top.png"))

    # The belt: dark rubber with ribs. Running, the ribs climb (8 frames).
    def belt(f):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                c = hexc("#26222a") if (y + f) % 4 else hexc("#3a3440")
                if x in (0, 15):
                    c = hexc("#141218")
                img.putpixel((x, y), c)
        return img
    belt(0).save(out(base + "belt.png"))
    strip = Image.new("RGBA", (16, 16 * 4))
    for f in range(4):
        strip.paste(belt(f), (0, 16 * f))
    strip.save(out(base + "belt_active.png"))
    with open(out(base + "belt_active.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 2\n  }\n}\n')

    # The dome: polished copper, lit from the top left. Running, sparks crawl over it (8 frames).
    rnd = random.Random(5)
    def dome(spark_frame=None):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                v = 3.3 - (x + y) / 14.0 + rnd.random() * 0.4
                img.putpixel((x, y), COPPER[max(0, min(5, int(round(v))))])
        img.putpixel((4, 3), COPPER[5]); img.putpixel((5, 3), COPPER[5]); img.putpixel((4, 4), COPPER[5])
        if spark_frame is not None:
            r = random.Random(100 + spark_frame)
            for _ in range(3):
                x, y = r.randrange(16), r.randrange(16)
                for k in range(r.randrange(2, 5)):
                    if 0 <= x < 16 and 0 <= y < 16:
                        img.putpixel((x, y), SPARK[2] if k == 0 else SPARK[r.randrange(2)])
                    x += r.choice((-1, 0, 1)); y += r.choice((-1, 0, 1))
        return img
    dome().save(out(base + "dome.png"))
    strip = Image.new("RGBA", (16, 16 * 8))
    for f in range(8):
        strip.paste(dome(f), (0, 16 * f))
    strip.save(out(base + "dome_active.png"))
    with open(out(base + "dome_active.png.mcmeta"), "w") as fh:
        fh.write('{\n  "animation": {\n    "frametime": 2\n  }\n}\n')

    # The Copper Comb's bristles: a row of stiff dark bristles hanging from a copper back, transparent between them.
    comb = Image.new("RGBA", (16, 16), CLEAR)
    for x in range(16):
        for y in range(2, 5):
            comb.putpixel((x, y), COPPER[2] if y == 2 else COPPER[1])
        if x % 2 == 0:
            for y in range(5, 14 - (x % 4 == 0)):
                comb.putpixel((x, y), hexc("#4a3b2e") if y < 12 else hexc("#2e241c"))
    comb.save(out(base + "bristles.png"))


def generate_grass_seeder():
    """Grass Seeder: a squat machine casing on four legs, a window full of seeds on each side, a seed hopper on top and
    a brass sprinkler standing on it."""
    base = "block/grass_seeder/"
    os.makedirs(out(base), exist_ok=True)
    rnd = random.Random(11)
    seeds = [hexc("#c9a64a"), hexc("#a8862e"), hexc("#e2c76c"), hexc("#7d6420")]
    leaf = [hexc("#3f7a2a"), hexc("#5aa13a"), hexc("#7cc24e")]

    side = grid(MACHINE_BLOCK, CARTOON)  # the casing; a window of seeds with a sprout in it
    for y in range(4, 12):
        for x in range(4, 12):
            side.putpixel((x, y), CARTOON["k"] if x in (4, 11) or y in (4, 11) else seeds[rnd.randrange(4)])
    for x, y, c in ((7, 6, 2), (8, 5, 2), (7, 7, 1), (8, 7, 0), (7, 8, 0), (8, 6, 1)):
        side.putpixel((x, y), leaf[c])
    side.save(out(base + "side.png"))

    top = grid(MACHINE_BLOCK, CARTOON)  # a grated seed hopper
    for y in range(3, 13):
        for x in range(3, 13):
            if x in (3, 12) or y in (3, 12):
                top.putpixel((x, y), CARTOON["S"])
            elif (x + y) % 3 == 0:
                top.putpixel((x, y), CARTOON["M"])
            else:
                top.putpixel((x, y), seeds[rnd.randrange(4)])
    top.save(out(base + "top.png"))

    metal = Image.new("RGBA", (16, 16))  # the legs: plain steel, lit on the left
    for y in range(16):
        for x in range(16):
            metal.putpixel((x, y), CARTOON["H"] if x % 4 == 0 else CARTOON["S"] if x % 4 == 3 else CARTOON["M"])
    metal.save(out(base + "metal.png"))

    brass = Image.new("RGBA", (16, 16))  # the sprinkler: brass, with a few water drops beaded on it
    for y in range(16):
        for x in range(16):
            brass.putpixel((x, y), CARTOON["Y"] if (x + y) % 7 == 0 else CARTOON["c"] if y % 4 == 3 else CARTOON["C"])
    for x, y in ((3, 5), (10, 2), (12, 9), (6, 12)):
        brass.putpixel((x, y), hexc("#9fd8ff"))
    brass.save(out(base + "brass.png"))


def generate_grass_seeder_gui():
    """Grass Seeder window: energy and water gauges, a map of its ground, a grass bar under it, an info panel with the
    Bone Meal slot, the status lamp and the player inventory."""
    shears = Image.open(out("gui/automatic_shears.png")).convert("RGBA")
    sep = Image.open(out("gui/mechanical_separator.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 8, 18, 24, 84, GUI["gauge"])           # energy, fill 14x64 at (9, 19)
    inset(img, 26, 18, 42, 84, GUI["gauge"])          # water, fill 14x64 at (27, 19)
    for k in range(1, 4):
        ty = 19 + round(64 * k / 4)
        for x in list(range(19, 23)) + list(range(37, 41)):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 46, 18, 99, 71, GUI["gauge"])          # ground map, 51x51 at (47, 19)
    inset(img, 46, 74, 99, 84, GUI["gauge"])          # grass bar, 51x8 at (47, 75)
    inset(img, 102, 18, 170, 84, GUI["panel"])        # info panel
    slot(img, 128, 22)                                # Bone Meal
    inset(img, 8, 86, 18, 96, GUI["gauge"])           # status lamp housing
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    rnd = random.Random(31)
    for x in range(4, 172):                           # a fringe of grass along the bottom edge
        if rnd.random() < 0.35:
            img.putpixel((x, 190), rnd.choice([hexc("#3f7a2a"), hexc("#5aa13a"), hexc("#7cc24e")]))
    # ---- sprites ----
    img.paste(sep.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    img.paste(shears.crop((176, 64, 208, 80)), (176, 146))          # area button background, normal and hovered
    for y in range(64):
        for x in range(14):
            e = mix(hexc("#ffd84a"), hexc("#d0601c"), y / 63.0)     # energy: amber, deeper at the bottom
            if (x * 2 + y) % 9 == 0:
                e = mix(e, hexc("#fff0a0"), 0.35)
            img.putpixel((176 + x, y), e)
            w = mix(hexc("#2f6fd0"), hexc("#163a8a"), y / 63.0)     # water: deep blue, lighter at the top
            if (x * 3 + y) % 11 == 0:
                w = mix(w, hexc("#7fb8ff"), 0.35)
            img.putpixel((192 + x, y), w)
    img.save(out("gui/grass_seeder.png"))


def generate_herd_counter():
    """Herd Counter: a slim blue-steel panel with a green-on-black screen on its front (the count is drawn over it) and a
    tally wheel on its side."""
    base = "block/herd_counter/"
    os.makedirs(out(base), exist_ok=True)
    front = grid(MACHINE_BLOCK, CARTOON)
    for y in range(3, 11):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (3, 10)
            front.putpixel((x, y), CARTOON["O"] if edge else (hexc("#0c1a10") if (x + y) % 5 else hexc("#10241a")))
    for x in range(4, 12):
        front.putpixel((x, 11), CARTOON["H"])                         # a lip under the screen
    for x, c in ((5, CARTOON["R"]), (7, hexc("#7cff8a")), (9, CARTOON["E"])):   # three little lamps under it
        front.putpixel((x, 13), c)
    front.save(out(base + "front.png"))

    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = CARTOON["M"]
            if x in (0, 15) or y in (0, 15):
                c = CARTOON["O"]
            elif x == 1 or y == 1:
                c = CARTOON["H"]
            elif x == 14 or y == 14:
                c = CARTOON["S"]
            side.putpixel((x, y), c)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        side.putpixel((x, y), CARTOON["b"])
    side.save(out(base + "side.png"))

    wheel = Image.new("RGBA", (16, 16))                               # the tally wheel: brass, notched
    for y in range(16):
        for x in range(16):
            wheel.putpixel((x, y), CARTOON["c"] if y % 3 == 0 else CARTOON["Y"] if x % 5 == 0 else CARTOON["C"])
    wheel.save(out(base + "wheel.png"))


def generate_herd_counter_gui():
    """Herd Counter window: a screen-like readout with a strip of signal pips under it, room for the mode buttons, a
    herd list, the ghost filter slot and the Full target, and the player inventory."""
    shears = Image.open(out("gui/automatic_shears.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 193)
    inset(img, 7, 16, 67, 58, hexc("#0c1a10"))         # the readout screen, 58x40 at (8, 17)
    for y in range(17, 57, 2):                            # scan lines
        for x in range(8, 66):
            img.putpixel((x, y), hexc("#10241a"))
    inset(img, 7, 60, 67, 66, GUI["gauge"])            # signal pips, 58x4 at (8, 61)
    inset(img, 70, 16, 170, 77, GUI["panel"])          # herd list
    slot(img, 77, 80)                                  # filter (a ghost slot)
    for row in range(3):
        for col in range(9):
            slot(img, 8 + col * 18, 111 + row * 18)
    for col in range(9):
        slot(img, 8 + col * 18, 169)
    img.paste(shears.crop((176, 64, 208, 80)), (176, 146))          # area button background, normal and hovered
    img.save(out("gui/herd_counter.png"))


def generate_status_panel():
    """Machine Status Panel: a thin blue-steel frame round dark glass (its rows are drawn over it), bolts in the corners."""
    base = "block/status_panel/"
    os.makedirs(out(base), exist_ok=True)
    front = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = CARTOON["O"]
            elif x == 1 or y == 1:
                c = CARTOON["H"]
            elif x == 14 or y == 14:
                c = CARTOON["S"]
            else:
                c = hexc("#0d1118") if (x + y) % 6 else hexc("#121823")
            front.putpixel((x, y), c)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        front.putpixel((x, y), CARTOON["b"])
    front.save(out(base + "front.png"))
    glass = Image.new("RGBA", (16, 16))                     # the glass alone, for joined screens (the bezel is separate)
    for y in range(16):
        for x in range(16):
            glass.putpixel((x, y), hexc("#0d1118") if (x + y) % 6 else hexc("#121823"))
    glass.save(out(base + "glass.png"))
    frame = Image.new("RGBA", (16, 16), CLEAR)             # the bezel round the outside edge of a screen
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                frame.putpixel((x, y), CARTOON["O"])
            elif x == 1 or y == 1:
                frame.putpixel((x, y), CARTOON["H"])
            elif x == 14 or y == 14:
                frame.putpixel((x, y), CARTOON["S"])
    frame.save(out(base + "frame.png"))
    # The bezel as four strips, each the same all along its length, so joined screens show no seams between panels.
    for side in ("top", "bottom", "left", "right"):
        strip = Image.new("RGBA", (16, 16), CLEAR)
        for a in range(16):
            for b, c in enumerate((CARTOON["O"], CARTOON["H"])):
                if side == "top":
                    strip.putpixel((a, b), c)
                elif side == "bottom":
                    strip.putpixel((a, 15 - b), c if b == 0 else CARTOON["S"])
                elif side == "left":
                    strip.putpixel((b, a), c)
                else:
                    strip.putpixel((15 - b, a), c if b == 0 else CARTOON["S"])
        strip.save(out(base + "frame_" + side + ".png"))
    back = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            back.putpixel((x, y), CARTOON["O"] if x in (0, 15) or y in (0, 15) else CARTOON["M"] if (x // 4 + y // 4) % 2 else CARTOON["S"])
    back.save(out(base + "back.png"))



def generate_crate():
    """Wool-Lined Crate: oak boards in a darker frame, wool lining showing in the gaps (grey, tinted the lining's
    colour in game), a dark panel on the front for its item, wool for its bulging lid, an iron strap, and the strap item."""
    rnd = random.Random(207)
    base = "block/wool_lined_crate/"
    plank = [hexc("#a8804c"), hexc("#9a7444"), hexc("#b58d57")]
    frame_l, frame_d, nail = hexc("#6e4c2a"), hexc("#553a1f"), hexc("#c9c3b8")
    gap = hexc("#2b1d10")

    def boards(img, rows, x0=0, x1=16, y0=0, y1=16):
        for y in range(y0, y1):
            for x in range(x0, x1):
                if y in rows:
                    img.putpixel((x, y), gap)
                    continue
                c = plank[(y // 4 + x // 7) % 3]
                v = rnd.uniform(0.92, 1.06)
                if (x * 3 + y * 5) % 11 == 0:
                    v *= 0.86                      # grain
                img.putpixel((x, y), shade(c, v))

    def border(img, width=2):
        for y in range(16):
            for x in range(16):
                if x < width or y < width or x >= 16 - width or y >= 16 - width:
                    edge = x in (0, 15) or y in (0, 15)
                    img.putpixel((x, y), shade(frame_d if edge else frame_l, rnd.uniform(0.94, 1.04)))
        for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
            img.putpixel((x, y), nail)

    side = Image.new("RGBA", (16, 16), CLEAR)
    boards(side, (6, 10))
    border(side)
    side.save(out(base + "side.png"))

    top = Image.new("RGBA", (16, 16), CLEAR)
    boards(top, (8,))
    border(top)
    top.save(out(base + "top.png"))

    bottom = Image.new("RGBA", (16, 16), CLEAR)
    boards(bottom, ())
    border(bottom)
    bottom.save(out(base + "bottom.png"))

    front = Image.new("RGBA", (16, 16), CLEAR)
    boards(front, ())
    border(front, 3)
    for y in range(3, 13):
        for x in range(3, 13):
            front.putpixel((x, y), hexc("#2a2018") if (x + y) % 5 else hexc("#30251c"))
    front.save(out(base + "front.png"))

    def wool_px(x, y):
        v = 236 - (y % 3) * 12 - (10 if (x + y // 3) % 4 == 3 else 0) + rnd.randint(-6, 6)
        return _grey(v)

    # The lining, showing in the side gaps, round the front panel and in the lid's seam: grey wool, tinted in game.
    lining_side = Image.new("RGBA", (16, 16), CLEAR)
    for y in (6, 10):
        for x in range(2, 14):
            lining_side.putpixel((x, y), wool_px(x, y))
    lining_side.save(out(base + "lining_side.png"))
    lining_top = Image.new("RGBA", (16, 16), CLEAR)
    for x in range(2, 14):
        lining_top.putpixel((x, 8), wool_px(x, 8))
    lining_top.save(out(base + "lining_top.png"))
    lining_front = Image.new("RGBA", (16, 16), CLEAR)
    for i in range(3, 13):
        for x, y in ((i, 3), (i, 12), (3, i), (12, i)):
            lining_front.putpixel((x, y), wool_px(x, y))
    lining_front.save(out(base + "lining_front.png"))
    wool = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            wool.putpixel((x, y), wool_px(x, y))
    wool.save(out(base + "wool.png"))

    # The Crate Strap: an iron band with rivets and a buckle, on the crate and as an item.
    strap = Image.new("RGBA", (16, 16), CLEAR)
    for x in range(16):
        strap.putpixel((x, 0), CARTOON["H"])
        strap.putpixel((x, 1), CARTOON["M"])
        strap.putpixel((x, 2), CARTOON["S"])
        if x % 5 == 2:
            strap.putpixel((x, 1), CARTOON["B"])
    for x in (7, 8):
        for y in range(3):
            strap.putpixel((x, y), CARTOON["C"] if y < 2 else CARTOON["c"])
    strap.save(out(base + "strap.png"))

    lock = Image.new("RGBA", (16, 16), CLEAR)    # a padlock, drawn in its top-left 4x5 corner
    for y, row in enumerate([".SS.", "S..S", "GGGG", "GKGG", "gggg"]):
        for x, ch in enumerate(row):
            if ch != ".":
                lock.putpixel((x, y), {"S": CARTOON["b"], "G": CARTOON["Y"], "K": CARTOON["c"], "g": CARTOON["C"]}[ch])
    lock.save(out(base + "lock.png"))

    tag = Image.new("RGBA", (16, 16), CLEAR)     # the brass tag of a crate in a controller's store, top-left 3x2
    for y, row in enumerate(["CYC", "ccc"]):
        for x, ch in enumerate(row):
            tag.putpixel((x, y), CARTOON[ch])
    tag.save(out(base + "tag.png"))

    # The Crate Controller: a crate whose front is a brass plate with a little dark screen and a row of card drawers.
    cbase = "block/crate_controller/"
    cfront = Image.new("RGBA", (16, 16), CLEAR)
    boards(cfront, ())
    border(cfront, 2)
    brass, brass_d, brass_l = CARTOON["C"], CARTOON["c"], CARTOON["Y"]
    for y in range(2, 14):
        for x in range(2, 14):
            c = brass if (x + y) % 7 else brass_l
            if x == 13 or y == 13:
                c = brass_d
            cfront.putpixel((x, y), c)
    for y in range(3, 9):                       # the screen (the readout is drawn over it)
        for x in range(3, 13):
            cfront.putpixel((x, y), hexc("#0f1a12") if (x + y) % 5 else hexc("#132018"))
    for x in range(3, 13):
        cfront.putpixel((x, 9), brass_d)
    for dx in (3, 8):                           # two card drawers, each with a pull
        for y in range(10, 13):
            for x in range(dx, dx + 5):
                cfront.putpixel((x, y), brass_d if y in (10, 12) or x in (dx, dx + 4) else brass)
        cfront.putpixel((dx + 2, 11), CARTOON["B"])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        cfront.putpixel((x, y), nail)
    cfront.save(out(cbase + "front.png"))

    item = Image.new("RGBA", (16, 16), CLEAR)
    for a in range(360):                      # a coiled band, seen from the side
        t = math.radians(a)
        for r, c in ((6.0, CARTOON["H"]), (5.0, CARTOON["M"]), (4.0, CARTOON["S"])):
            x, y = int(round(7.5 + math.cos(t) * r)), int(round(8.5 + math.sin(t) * r * 0.62))
            if 0 <= x < 16 and 0 <= y < 16:
                item.putpixel((x, y), c)
    for x, y in ((6, 3), (7, 3), (8, 3), (9, 3), (6, 4), (9, 4), (6, 5), (7, 5), (8, 5), (9, 5)):
        item.putpixel((x, y), CARTOON["C"] if y != 5 else CARTOON["c"])
    for x, y in ((2, 8), (13, 8), (4, 12), (11, 12)):
        item.putpixel((x, y), CARTOON["B"])
    item.save(out("item/crate_strap.png"))


def generate_woollen_steel():
    """Woollen Steel in the style of Minecraft's own ingots and metal blocks, in the machines' blue steel: the ingot a
    bar seen corner-on (lit top face, darker front and end) with a band of wool wrapped round it; the block rows of
    plates like a Block of Iron, each lit along its top edge, with wool stitched along the seams between them."""
    rnd = random.Random(311)
    steel_l, steel, steel_d, edge = CARTOON["H"], CARTOON["M"], CARTOON["S"], CARTOON["O"]
    shine = CARTOON["B"]
    wool_l, wool, wool_d = hexc("#ffffff"), hexc("#ece8e0"), hexc("#c9c3b8")

    # The ingot: the usual bar silhouette, shaded here: top face light, front face mid, the end and underside dark.
    silhouette = [
        "................",
        "................",
        "..........##....",
        ".......#######..",
        "....##########..",
        ".##############.",
        "################",
        "################",
        "################",
        "################",
        "###############.",
        ".############...",
        "..########......",
        "...####.........",
        "................",
        "................",
    ]
    mask = {(x, y) for y, row in enumerate(silhouette) for x, ch in enumerate(row) if ch == "#"}
    item = Image.new("RGBA", (16, 16), CLEAR)
    # The bar's lit front edge: down from its left tip, along the front, and up to its right end.
    ridge = {0: 6, 1: 6, 2: 7, 3: 8, 4: 9, 5: 9, 6: 8, 7: 8, 8: 8, 9: 7, 10: 7, 11: 7, 12: 6, 13: 6, 14: 6, 15: 6}
    for (x, y) in mask:
        boundary = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        r = ridge[x]
        if y == r:
            c = shine                          # the lit edge
        elif y < r:
            c = steel_l if not boundary else steel # the top face
        elif x >= 13:
            c = steel_d if not boundary else edge  # the end
        else:
            c = edge if boundary else steel if y <= r + 2 else steel_d  # the front face
        item.putpixel((x, y), c)
    # A band of wool wrapped round the middle of the bar.
    for (x, y) in mask:
        if x in (7, 8):
            r = ridge[x]
            item.putpixel((x, y), wool_l if y <= r else wool if y <= r + 2 else wool_d)
    item.save(out("item/woollen_steel.png"))

    # The block: rows of plates like a Block of Iron, each lit along its top, with a seam under it stitched now and then
    # with wool; a lit top-left and shaded bottom-right edge.
    block = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            if y == 0 or x == 0:
                c = steel_l
            elif y == 15 or x == 15:
                c = steel_d
            else:
                band = (y - 1) % 3
                if band == 0:
                    c = shine if x < 4 + (y * 5) % 7 else steel_l
                elif band == 1:
                    c = steel_l if rnd.random() < 0.7 else mix(steel_l, steel, 0.5)
                else:
                    c = wool_l if (x + y) % 5 == 0 else steel
            block.putpixel((x, y), c)
    block.save(out("block/woollen_steel_block.png"))

def generate_static_generator_gui():
    """Static Generator window: energy gauge, then a panel with the herd and output, the dome's charge bar (the shock
    line marked) and four comb chips; the status lamp below."""
    trc = Image.open(out("gui/thermal_reaction_chamber.png")).convert("RGBA")
    shears = Image.open(out("gui/automatic_shears.png")).convert("RGBA")
    img = Image.new("RGBA", (256, 256), CLEAR)
    panel(img, 0, 0, 176, 108)
    inset(img, 10, 18, 42, 84, GUI["gauge"])          # energy gauge, fill 30x64 at (11, 19)
    for k in range(1, 8):
        ty = 19 + round(64 * k / 8)
        for x in range(36, 41):
            img.putpixel((x, ty), GUI["tick"])
    inset(img, 48, 16, 166, 84, GUI["panel"])         # the panel
    inset(img, 50, 42, 164, 52, GUI["gauge"])         # dome charge, fill 112x8 at (51, 43)
    for i in range(4):                                # comb chips, 22x18 at (52 + 28i, 58), copper square 12x12 at (57 + 28i, 61)
        inset(img, 52 + 28 * i, 58, 74 + 28 * i, 76, GUI["gauge"])
    inset(img, 8, 89, 18, 99, GUI["gauge"])           # status lamp housing, lamp at (9, 90)
    rnd = random.Random(43)
    for x in range(4, 172):
        if rnd.random() < 0.2:
            img.putpixel((x, 105), rnd.choice((SPARK[0], SPARK[1], COPPER[3])))
    # ---- sprites ----
    img.paste(trc.crop((186, 194, 256, 202)), (186, 194))           # status lamps
    img.paste(shears.crop((176, 64, 208, 80)), (176, 146))          # area button background
    img.paste(trc.crop((176, 0, 206, 74)), (176, 0))                # energy fill (amber) and its shimmer
    for y in range(8):                                              # dome charge fill, 112x8 at (0, 200): blue to white
        for x in range(112):
            c = mix(SPARK[0], SPARK[2], x / 111.0 * 0.8)
            if y == 0:
                c = mix(c, SPARK[2], 0.5)
            if (x + y) % 6 == 0:
                c = mix(c, SPARK[2], 0.3)
            img.putpixel((x, 200 + y), c)
    img.save(out("gui/static_generator.png"))


def main():
    generate_wool_items()
    generate_sheep_fur()
    generate_hay_wool()
    generate_snow_wool()
    generate_melon_wool()
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
    generate_energy_meter()
    generate_glow_panel()
    generate_blaze_furnace()
    generate_wool_burner()
    generate_manual()
    generate_reservoir_gui()
    generate_energy_cube()
    generate_cube_gui()
    generate_fluid_tank()
    generate_tank_gui()
    generate_zero_point_gui()
    generate_blaze_furnace_gui()
    generate_wool_burner_gui()
    generate_wool_bale()
    generate_wool_packer()
    generate_wool_packer_gui()
    generate_infusion_altar()
    generate_hume_field_generator()
    generate_sheep_treadmill()
    generate_sheep_treadmill_gui()
    generate_chiller_gui()
    generate_chiller_block()
    generate_powered_furnace()
    generate_thermal_reaction_chamber()
    generate_thermal_reaction_chamber_gui()
    generate_echo_chamber()
    generate_echo_chamber_gui()
    generate_static_generator()
    generate_static_generator_gui()
    generate_grass_seeder()
    generate_grass_seeder_gui()
    generate_herd_counter()
    generate_herd_counter_gui()
    generate_status_panel()
    generate_crate()
    generate_woollen_steel()
    generate_woollen_armor()
    generate_woollen_icons()
    generate_infusion_altar_gui()
    generate_advancement_background()
    generate_jei()
    generate_logo()
    print("Textures written to", os.path.abspath(ROOT))


if __name__ == "__main__":
    main()
