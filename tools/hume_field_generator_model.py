"""Generates the Hume Field Generator's textures and models, in the style of the Echo Chamber.

Run from the repository root: python3 tools/hume_field_generator_model.py src/main/resources/assets/barelyfunctionalcontraptions
"""
import json, os, shutil, sys
from PIL import Image

ASSETS = sys.argv[1]
TEX = os.path.join(ASSETS, 'textures/block/hume_field_generator')
ECHO = os.path.join(ASSETS, 'textures/block/echo_chamber')
NS = 'barelyfunctionalcontraptions:block/hume_field_generator/'

O, HI, ST, SH, RV, RV2 = '#231c35', '#8d97bc', '#5d6687', '#3f4766', '#dfe4f5', '#9aa3c4'
DK, PU, PD, PL = '#2a2238', '#9a52f0', '#5c2aa6', '#c99bff'

def rgb(h): return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)

def paint(rows, key):
    im = Image.new('RGBA', (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            im.putpixel((x, y), rgb(key[ch]))
    return im

# ---------------------------------------------------------------- textures
for name in ['plate', 'trim', 'bottom']:
    shutil.copy(os.path.join(ECHO, name + '.png'), os.path.join(TEX, name + '.png'))

# The plinth's and lid's sides: the Echo Chamber's band, its dark strip lit with Hume purple.
band = Image.open(os.path.join(ECHO, 'band.png')).convert('RGBA')
for x in range(16):
    if 5 <= x <= 9 or x <= 1 or x >= 13:
        band.putpixel((x, 13), rgb(PU))
        band.putpixel((x, 14), rgb(PD))
band.save(os.path.join(TEX, 'band.png'))

# Corner pillars are 5 pixels wide, drawn from the texture's first five columns: an outline, a highlight, a purple
# glow strip with a rivet every 8 pixels, steel and shadow.
pillar = []
for y in range(16):
    glow = 'r' if y % 8 == 3 else ('p' if y % 8 in (0, 7) else 'g')
    pillar.append('oh' + glow + 'sd' + 's' * 11)
paint(pillar, {'o': O, 'h': HI, 'g': PU, 'p': PD, 'r': RV, 's': ST, 'd': SH}).save(os.path.join(TEX, 'pillar.png'))

# The emitter's collar seen from above: a steel ring round a purple well.
collar = [
    'oooooooooooooooo',
    'ohhhhhhhhhhhhhho',
    'ohrssssssssssrdo',
    'ohssddddddddssdo',
    'ohsdppppppppdsdo',
    'ohsdpgggggggpsdo',
    'ohsdpgllllggpsdo',
    'ohsdpglkklggpsdo',
    'ohsdpglkklggpsdo',
    'ohsdpgllllggpsdo',
    'ohsdpgggggggpsdo',
    'ohsdppppppppdsdo',
    'ohssddddddddssdo',
    'ohrssssssssssrdo',
    'odddddddddddddddo'[:16],
    'oooooooooooooooo']
paint(collar, {'o': O, 'h': HI, 's': ST, 'd': SH, 'r': RV, 'p': PD, 'g': PU, 'l': PL, 'k': '#f4e8ff'}).save(os.path.join(TEX, 'collar.png'))

# The windows: glass tinted with the field's purple, dark enough that the core glows through it, with a glint.
glass = Image.new('RGBA', (16, 16), (92, 42, 166, 150))
for x in range(16):
    for y in (0, 15):
        glass.putpixel((x, y), (154, 82, 240, 190))
        glass.putpixel((y, x), (154, 82, 240, 190))
for i in range(4):
    glass.putpixel((3 + i, 6 - i), (230, 214, 255, 170))
    glass.putpixel((10 + i, 13 - i), (230, 214, 255, 140))
glass.save(os.path.join(TEX, 'glass.png'))

for old in ['base_side', 'base_top', 'cap_side', 'cap_top']:
    p = os.path.join(TEX, old + '.png')
    if os.path.exists(p): os.remove(p)

# ---------------------------------------------------------------- geometry
# Boxes in pixels across the whole 2x2 machine (x, z 0-32), y 0-48. faces: {face: texture}; 'u': a fixed u range for
# the side faces (pillars); 'uv': fixed uvs, for a box kept whole.
boxes = []
def box(f, t, faces, **kw):
    boxes.append(dict(f=f, t=t, faces=faces, **kw))

SIDES = ['north', 'south', 'east', 'west']
def all6(side, top=None, bottom=None):
    d = {s: side for s in SIDES}
    d['up'] = top or side
    d['down'] = bottom or top or side
    return d

# Plinth and lid, the full footprint, like the Echo Chamber's.
box([0, 0, 0], [32, 4, 32], all6('#band', '#plate', '#bottom'))
box([0, 34, 0], [32, 38, 32], all6('#band', '#plate', '#plate'))
# Chunky corner pillars, full height between them.
for x in (0, 27):
    for z in (0, 27):
        box([x, 4, z], [x + 5, 34, z + 5], {s: '#pillar' for s in SIDES}, u=(0, 5))
# The four walls, each a steel frame round a big window onto the field core.
def wall(axis, plane, outward):
    """One wall in the plane {axis} = plane (2 or 30): steel round a window, a trim frame standing out from it."""
    def b(a1, a2, y1, y2, d1, d2, faces):
        if axis == 'z':
            box([a1, y1, d1], [a2, y2, d2], faces)
        else:
            box([d1, y1, a1], [d2, y2, a2], faces)
    out, inn = ('north', 'south') if axis == 'z' else ('west', 'east')
    if outward > 0: out, inn = inn, out
    thin = (plane, plane + 1) if outward < 0 else (plane - 1, plane)
    across = ['east', 'west'] if axis == 'z' else ['north', 'south']
    steel = {out: '#plate', inn: '#plate'}
    b(5, 27, 4, 9, *thin, dict(steel, up='#plate'))
    b(5, 27, 29, 34, *thin, dict(steel, down='#plate'))
    b(5, 9, 9, 29, *thin, dict(steel, **{across[1]: '#plate'}) if axis == 'z' else dict(steel, **{across[0]: '#plate'}))
    b(23, 27, 9, 29, *thin, dict(steel, **{across[0]: '#plate'}) if axis == 'z' else dict(steel, **{across[1]: '#plate'}))
    # The window's trim frame, two pixels wide and one proud of the wall, like the Echo Chamber's round its dish.
    proud = (plane - 1, plane) if outward < 0 else (plane, plane + 1)
    trim = dict({out: '#trim', inn: '#trim', 'up': '#trim', 'down': '#trim', across[0]: '#trim', across[1]: '#trim'})
    b(7, 25, 7, 9, *proud, trim)
    b(7, 25, 29, 31, *proud, trim)
    b(7, 9, 9, 29, *proud, trim)
    b(23, 25, 9, 29, *proud, trim)
    # The glass itself, tinted with the field, both ways round.
    mid = plane + 0.5 * (1 if outward < 0 else -1)
    b(9, 23, 9, 29, mid, mid, {out: '#glass', inn: '#glass'})
wall('z', 2, -1)
wall('z', 30, +1)
wall('x', 2, -1)
wall('x', 30, +1)
# Inside: the core, flickering, with three field coils round it.
box([12, 6, 12], [20, 32, 20], all6('#core'))
for y in (10, 18, 26):
    box([9, y, 9], [23, y + 2, 10], all6('#coil'))
    box([9, y, 22], [23, y + 2, 23], all6('#coil'))
    box([9, y, 10], [10, y + 2, 22], {'east': '#coil', 'west': '#coil', 'up': '#coil', 'down': '#coil'})
    box([22, y, 10], [23, y + 2, 22], {'east': '#coil', 'west': '#coil', 'up': '#coil', 'down': '#coil'})
# On the lid: the field emitter (a collar and a lens housing), and a bolt at each corner like the Echo Chamber's.
box([10, 38, 10], [22, 40, 22], dict({s: '#trim' for s in SIDES}, up='#collar'), uv={'up': [0, 0, 16, 16]}, whole=True)
box([11, 40, 11], [21, 44, 21], dict({s: '#pillar' for s in SIDES}, up='#lens'),
    uv=dict({s: [0, 0, 5, 4] for s in SIDES}, up=[0, 0, 16, 16]), whole=True)
for x in (3, 26):
    for z in (3, 26):
        box([x, 38, z], [x + 3, 39.5, z + 3], dict({s: '#trim' for s in SIDES}, up='#trim'))

# ---------------------------------------------------------------- split into 16-pixel cells, with 1:1 uvs
def cuts(a, b):
    pts = [a] + [c for c in (16, 32) if a < c < b] + [b]
    return list(zip(pts, pts[1:]))

def uv_for(face, lo, hi):
    lx1, ly1, lz1 = lo
    lx2, ly2, lz2 = hi
    return {
        'north': [16 - lx2, 16 - ly2, 16 - lx1, 16 - ly1],
        'south': [lx1, 16 - ly2, lx2, 16 - ly1],
        'west': [lz1, 16 - ly2, lz2, 16 - ly1],
        'east': [16 - lz2, 16 - ly2, 16 - lz1, 16 - ly1],
        'up': [lx1, lz1, lx2, lz2],
        'down': [lx1, 16 - lz2, lx2, 16 - lz1],
    }[face]

def r(v):
    return int(v) if float(v).is_integer() else round(v, 4)

pieces = []  # (from, to, faces, layer-of-y)
for bx in boxes:
    f, t = bx['f'], bx['t']
    if bx.get('whole'):
        faces = {k: {'uv': bx['uv'].get(k, uv_for(k, [c % 16 for c in f], [c % 16 or 16 for c in t])), 'texture': v} for k, v in bx['faces'].items()}
        pieces.append((f, t, faces))
        continue
    for x1, x2 in cuts(f[0], t[0]) if f[0] != t[0] else [(f[0], t[0])]:
        for y1, y2 in cuts(f[1], t[1]):
            for z1, z2 in cuts(f[2], t[2]) if f[2] != t[2] else [(f[2], t[2])]:
                faces = {}
                cx, cy, cz = (min(x1, x2 - 1e-6) // 16) * 16 if x2 > x1 else (x1 // 16) * 16, (y1 // 16) * 16, (min(z1, z2 - 1e-6) // 16) * 16 if z2 > z1 else (z1 // 16) * 16
                if x1 == x2 and x1 % 16 == 0: cx = x1 - 16 if x1 > 0 else 0
                if z1 == z2 and z1 % 16 == 0: cz = z1 - 16 if z1 > 0 else 0
                lo, hi = [x1 - cx, y1 - cy, z1 - cz], [x2 - cx, y2 - cy, z2 - cz]
                for face, tex in bx['faces'].items():
                    # Only the box's own outside faces, not the planes it was cut along.
                    edge = {'north': z1 == f[2], 'south': z2 == t[2], 'west': x1 == f[0], 'east': x2 == t[0], 'down': y1 == f[1], 'up': y2 == t[1]}[face]
                    if not edge: continue
                    uv = uv_for(face, lo, hi)
                    if 'u' in bx and face in SIDES:
                        uv[0], uv[2] = bx['u']
                    faces[face] = {'uv': [r(v) for v in uv], 'texture': tex}
                if faces:
                    pieces.append(([x1, y1, z1], [x2, y2, z2], faces))

def element(f, t, faces, dy=0, scale=1.0):
    return {'from': [r(f[0] * scale), r((f[1] + dy) * scale), r(f[2] * scale)],
            'to': [r(t[0] * scale), r((t[1] + dy) * scale), r(t[2] * scale)], 'faces': faces}

textures = {n: NS + n for n in ['band', 'plate', 'bottom', 'pillar', 'trim', 'glass', 'core', 'coil', 'collar', 'lens']}
textures['particle'] = NS + 'plate'

def model(elements, extra=None):
    m = {'parent': 'minecraft:block/block', 'textures': textures, 'elements': elements}
    if extra: m.update(extra)
    return m

lower = [element(f, t, fc) for f, t, fc in pieces if t[1] <= 32]
upper = [element(f, t, fc, dy=-32) for f, t, fc in pieces if f[1] >= 32]
assert len(lower) + len(upper) == len(pieces), 'a piece crosses y=32'
item = [element(f, t, fc, scale=0.5) for f, t, fc in pieces]

MODELS = os.path.join(ASSETS, 'models')
def write(path, data):
    with open(path, 'w') as out:
        json.dump(data, out, indent=2)
        out.write('\n')
write(os.path.join(MODELS, 'block/hume_field_generator.json'), model(lower))
write(os.path.join(MODELS, 'block/hume_field_generator_top.json'), model(upper))
write(os.path.join(MODELS, 'block/hume_field_generator_part.json'), {'textures': {'particle': NS + 'plate'}})
write(os.path.join(MODELS, 'item/hume_field_generator.json'), model(item, {'display': {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, -2.5, 0], 'scale': [0.52, 0.52, 0.52]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, -2, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}}))
print(len(lower), 'lower elements,', len(upper), 'upper,', len(item), 'item')
