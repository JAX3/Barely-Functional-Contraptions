"""Builds the Echo Chamber's block and item models (work in progress, development builds only).

The Echo Chamber is a 3x3x3 machine. A block model can't reach more than one block outside its own, so two of its 27
blocks draw it: the middle block of the bottom layer draws the bottom two layers, and the middle block of the top layer
draws the top layer, roof and ears. The other blocks draw nothing.

The machine is laid out here in pixels across the whole 48x48x48 space, front (north, the way it faces) at z = 0, and
cut on the block grid so each wall texture tiles once per block face. Run it after generate_textures.py:

    python3 tools/sheep/generate_echo_chamber_model.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "barelyfunctionalcontraptions")
NS = "barelyfunctionalcontraptions"
TEX = {name: f"{NS}:block/echo_chamber/{name}" for name in (
    "band", "bottom", "dish", "ear", "foam", "foam_sculk_0", "foam_sculk_1", "foam_sculk_2", "grate", "heart", "pillar",
    "plate", "port", "trim")}
TEX["catalyst_side"] = "minecraft:block/sculk_catalyst_side_bloom"
TEX["catalyst_top"] = "minecraft:block/sculk_catalyst_top_bloom"
TEX["particle"] = TEX["plate"]

SIDES = ("north", "south", "east", "west")
ALL = SIDES + ("up", "down")
AXIS = {"x": 0, "y": 1, "z": 2}

boxes = []    # (from, to, {face: texture or fn(cell) -> texture}, {axis: (anchor, size)})
rotated = []  # elements that are rotated, given whole, in world pixels


def box(fr, to, faces, span=None):
    boxes.append((fr, to, faces, span or {}))


def uv(face, lo, hi):
    """Minecraft's own UVs for a face of a box from lo to hi, in a texture's 0-16 space."""
    x1, y1, z1 = lo
    x2, y2, z2 = hi
    return {
        "down": [x1, 16 - z2, x2, 16 - z1],
        "up": [x1, z1, x2, z2],
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
        "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1],
        "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    }[face]


def cuts(a, b):
    points = [a] + [c for c in range(16, 64, 16) if a < c < b] + [b]
    return list(zip(points, points[1:]))


def r(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


def pieces():
    """Every box cut on the block grid: (cell, from, to, faces) with the faces' UVs worked out."""
    for fr, to, faces, span in boxes:
        for x1, x2 in cuts(fr[0], to[0]):
            for y1, y2 in cuts(fr[1], to[1]):
                for z1, z2 in cuts(fr[2], to[2]):
                    lo, hi = [x1, y1, z1], [x2, y2, z2]
                    cell = tuple(int(min(lo[i], hi[i] - 0.001) // 16) for i in range(3))
                    # Texture space: tiled per block, or stretched over a span of the machine.
                    tlo, thi = [], []
                    for axis, i in AXIS.items():
                        if axis in span:
                            anchor, size = span[axis]
                            tlo.append((lo[i] - anchor) * 16 / size); thi.append((hi[i] - anchor) * 16 / size)
                        else:
                            tlo.append(lo[i] - cell[i] * 16); thi.append(hi[i] - cell[i] * 16)
                    out = {}
                    for face, tex in faces.items():
                        # Only the outside of the original box, not the cuts.
                        edge = {"north": z1 == fr[2], "south": z2 == to[2], "west": x1 == fr[0], "east": x2 == to[0],
                                "down": y1 == fr[1], "up": y2 == to[1]}[face]
                        if not edge:
                            continue
                        name = tex(cell) if callable(tex) else tex
                        out[face] = {"uv": [r(v) for v in uv(face, tlo, thi)], "texture": "#" + name}
                    if out:
                        yield cell, lo, hi, out


def faces(tex, which=ALL, **overrides):
    f = {face: tex for face in which}
    f.update(overrides)
    return f


# ---- The machine ----
# Plinth and roof band: the dark bolted rail (the bottom rows of "band").
box([0, 0, 0], [48, 4, 48], faces("band", SIDES, up="plate", down="bottom"))
box([0, 40, 0], [48, 44, 48], faces("band", SIDES, up="grate", down="plate"), {"y": (28, 16)})
# Corner pillars.
for x0 in (0, 43):
    for z0 in (0, 43):
        box([x0, 4, z0], [x0 + 5, 40, z0 + 5], faces("pillar", SIDES))


# The walls: plate at the front and back, foam on the sides with sculk creeping over a few panels.
def foam(side):
    patches = {("west", 0, 2): "foam_sculk_0", ("east", 1, 0): "foam_sculk_1", ("west", 2, 1): "foam_sculk_2",
               ("east", 0, 2): "foam_sculk_1"}
    return lambda cell: patches.get((side, cell[1], cell[2]), "foam")


box([2, 4, 2], [46, 40, 46], {"north": "plate", "south": "plate", "west": foam("west"), "east": foam("east")})
# Baffle ribs down the side walls, over the seams between the foam panels.
for x0, x1 in ((0, 2), (46, 48)):
    for z in (15, 31):
        box([x0, 4, z], [x1, 40, z + 2], faces("trim", ("north", "south", "east", "west")))

# Front: the resonator dish, sunk in a bezel, with its glowing heart in the middle.
for fr, to in (([6, 38, 0], [42, 40, 2]), ([6, 4, 0], [42, 6, 2]), ([6, 6, 0], [8, 38, 2]), ([40, 6, 0], [42, 38, 2])):
    box(fr, to, faces("trim", ("north", "east", "west", "up", "down")))
box([8, 6, 1], [40, 38, 2], {"north": "dish"}, {"x": (8, 32), "y": (6, 32)})
box([20, 18, 0], [28, 26, 1], faces("trim", ("east", "west", "up", "down"), north="heart"), {"x": (20, 8), "y": (18, 8)})

# Back: a power port between two bays of sculk catalysts plugged into the wall, and a conduit across above them.
box([20, 12, 46], [28, 20, 47], faces("trim", ("east", "west", "up", "down"), south="port"), {"x": (20, 8), "y": (12, 8)})
for x0 in (6, 30):
    box([x0 - 1, 9, 46], [x0 + 13, 23, 46.5], faces("trim", ("south", "east", "west", "up", "down")))
    box([x0, 10, 46], [x0 + 12, 22, 48], faces("catalyst_side", ("south", "east", "west"), up="catalyst_top", down="catalyst_top"),
        {"x": (x0, 12), "y": (10, 12), "z": (36, 12)})
box([5, 28, 46], [43, 31, 47.5], faces("trim", ("south", "east", "west", "up", "down")))
for x in (12, 24, 36):  # its brackets
    box([x, 27, 45.5], [x + 2, 32, 48], faces("pillar", ("south", "east", "west", "up", "down")))

# Roof: a big sculk catalyst dome, and two sculk fronds leaning outwards like the Warden sheep's ears.
box([17, 44, 17], [31, 50, 31], faces("catalyst_side", SIDES, up="catalyst_top"), {"x": (17, 14), "y": (44, 14), "z": (17, 14)})
for x, lean in ((9, 22.5), (39, -22.5)):
    box([x - 3, 44, 22], [x + 3, 46, 26], faces("trim", SIDES + ("up",)))
    rotated.append({"name": "ear", "from": [x - 5, 46, 24], "to": [x + 5, 64, 24], "shade": False,
                    "rotation": {"angle": lean, "axis": "z", "origin": [x, 46, 24]},
                    "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#ear"}, "south": {"uv": [16, 0, 0, 16], "texture": "#ear"}}})


# ---- Writing the models ----
def shifted(element, dx, dy, dz, scale=1.0):
    e = json.loads(json.dumps(element))
    e["from"] = [r((e["from"][0] - dx) * scale), r((e["from"][1] - dy) * scale), r((e["from"][2] - dz) * scale)]
    e["to"] = [r((e["to"][0] - dx) * scale), r((e["to"][1] - dy) * scale), r((e["to"][2] - dz) * scale)]
    if "rotation" in e:
        o = e["rotation"]["origin"]
        e["rotation"]["origin"] = [r((o[0] - dx) * scale), r((o[1] - dy) * scale), r((o[2] - dz) * scale)]
    return e


def used(elements):
    names = {f["texture"][1:] for e in elements for f in e["faces"].values()}
    return {k: v for k, v in TEX.items() if k in names or k == "particle"}


def model(elements):
    return {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": used(elements), "elements": elements}


def write(path, data):
    with open(os.path.join(ROOT, path), "w") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


bottom, top, whole = [], [], []
for cell, lo, hi, fcs in pieces():
    element = {"from": lo, "to": hi, "faces": fcs}
    whole.append(element)
    if cell[1] < 2:
        bottom.append(shifted(element, 16, 0, 16))
    else:
        top.append(shifted(element, 16, 32, 16))
for element in rotated:
    whole.append(element)
    top.append(shifted(element, 16, 32, 16))

for e in bottom + top:
    for v in e["from"] + e["to"]:
        assert -16 <= v <= 32, e

write("models/block/echo_chamber.json", model(bottom))
write("models/block/echo_chamber_top.json", model(top))
write("models/block/echo_chamber_part.json", {"textures": {"particle": TEX["plate"]}})
# The item: the whole machine at a third of its size.
write("models/item/echo_chamber.json", model([shifted(e, 0, 0, 0, 1 / 3) for e in whole]))
write("items/echo_chamber.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/echo_chamber"}})

# Blockstates: 4 facings x 9 columns x 3 layers. Column 4 (the middle) draws the machine on layers 0 and 2.
variants = {}
for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for layer in range(3):
        for part in range(9):
            name = "echo_chamber" if (part, layer) == (4, 0) else "echo_chamber_top" if (part, layer) == (4, 2) else "echo_chamber_part"
            v = {"model": f"{NS}:block/{name}"}
            if y and name != "echo_chamber_part":
                v["y"] = y
            variants[f"facing={facing},layer={layer},part={part}"] = v
write("blockstates/echo_chamber.json", {"variants": variants})
print(f"Echo Chamber: {len(bottom)} + {len(top)} elements, item {len(whole)}")
