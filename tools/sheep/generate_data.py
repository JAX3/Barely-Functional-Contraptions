#!/usr/bin/env python3
"""
Writes the JSON assets and data for Resourceful Sheep (models, blockstates, item model
definitions, language file, built-in resource definitions, recipes, loot, tags).

Run from the project root:  python3 tools/generate_data.py
The generated files are committed; this script exists so they stay consistent and are easy to extend.
"""
import json
import os

MOD = "barelyfunctionalcontraptions"
RES = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", MOD)
DATA = os.path.join(RES, "data")


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# name -> definition. Keep in sync with tools/sheep/generate_textures.py BUILTIN colours.
# The red_mushroom and brown_mushroom resources (the Mycelial Power link) are hand-written JSON and use the tinted fallback texture.
RESOURCES = {
    "iron": dict(display="Iron", feed="minecraft:iron_ingot", out="minecraft:raw_iron", cat="ore", uses=9,
                 wool="#C8C8C8", accent="#F4F4F4", energy=20000, ticks=100,
                 bonus={"item": {"id": "minecraft:iron_nugget", "count": 3}, "chance": 0.1}),
    "gold": dict(display="Gold", feed="minecraft:gold_ingot", out="minecraft:raw_gold", cat="ore", uses=9,
                 wool="#F2C230", accent="#FFF6A8", energy=20000, ticks=100,
                 bonus={"item": {"id": "minecraft:gold_nugget", "count": 3}, "chance": 0.1}),
    "copper": dict(display="Copper", feed="minecraft:copper_ingot", out="minecraft:raw_copper", cat="ore", uses=9,
                   wool="#D27A4E", accent="#5FD3B5", energy=16000, ticks=100),
    "diamond": dict(display="Diamond", feed="minecraft:diamond", out="minecraft:diamond", cat="gem", uses=1,
                    wool="#7FE6E0", accent="#E8FFFF", energy=40000, ticks=160),
    "emerald": dict(display="Emerald", feed="minecraft:emerald", out="minecraft:emerald", cat="gem", uses=1,
                    wool="#3FC46A", accent="#B9FFC9", energy=40000, ticks=160),
    "redstone": dict(display="Redstone", feed="minecraft:redstone", out="minecraft:redstone", cat="dust", uses=9,
                     wool="#9E1D1D", accent="#FF3B2B", energy=12000, ticks=80),
    "lapis": dict(display="Lapis", feed="minecraft:lapis_lazuli", out="minecraft:lapis_lazuli", cat="gem", uses=9,
                  wool="#2F55B5", accent="#9DB8FF", energy=12000, ticks=80),
    "coal": dict(display="Coal", feed="minecraft:coal", out="minecraft:coal", cat="material", uses=9,
                 wool="#3A3A3E", accent="#8C8C96", energy=8000, ticks=80),
    "netherite": dict(display="Netherite", growth="minecraft:ancient_debris", feed="minecraft:netherite_scrap", out="minecraft:netherite_scrap", cat="material", uses=1,
                      wool="#4A3E44", accent="#9B6FB0", energy=80000, ticks=200),
    "stone": dict(display="Stone", feed="minecraft:cobblestone", out="minecraft:cobblestone", cat="material", uses=1,
                  wool="#8E8E8E", accent="#B5B5B5", energy=4000, ticks=40),
    "wood": dict(display="Wood", feed="minecraft:oak_log", out="minecraft:oak_log", cat="organic", uses=1,
                 wool="#A8834F", accent="#6B4F2A", energy=4000, ticks=40),
    "quartz": dict(display="Quartz", feed="minecraft:quartz", out="minecraft:quartz", cat="gem", uses=1,
                   wool="#EDE6DC", accent="#FFFFFF", energy=16000, ticks=100),
}


def resource_definitions():
    for name, r in RESOURCES.items():
        sep = {
            "primary_output": r["out"],
            "primary_count": {"min": 2, "max": 4},
            "secondary_output": "minecraft:string",
            "secondary_count": 2,
            "energy_cost": r["energy"],
            "processing_ticks": r["ticks"],
        }
        if "bonus" in r:
            sep["bonus_output"] = r["bonus"]
        extra = {"growth_item": r["growth"]} if "growth" in r else {}
        write(os.path.join(DATA, MOD, MOD, "sheep_resources", f"{name}.json"), {
            "feeding_item": r["feed"],
            "feeding_amount": 1,
            "wool_color": r["wool"],
            "accent_color": r["accent"],
            "category": r["cat"],
            "wool_drop_count": 1,
            "infusion_uses": r["uses"],
            "separator": sep,
            **extra,
        })


def item_models():
    cases = []
    for name in RESOURCES:
        write(os.path.join(ASSETS, "models", "item", f"{name}_wool.json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}_wool"}})
        cases.append({"when": f"{MOD}:{name}", "model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}_wool"}})
    # Fallback for data-pack resources: greyscale wool tinted with wool_color + accent flecks tinted with accent_color.
    write(os.path.join(ASSETS, "models", "item", "resource_wool.json"), {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{MOD}:item/resource_wool_base", "layer1": f"{MOD}:item/resource_wool_overlay"},
    })
    write(os.path.join(ASSETS, "items", "resource_wool.json"), {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:component",
            "component": f"{MOD}:sheep_resource",
            "cases": cases,
            "fallback": {
                "type": "minecraft:model",
                "model": f"{MOD}:item/resource_wool",
                "tints": [
                    {"type": f"{MOD}:resource_wool", "channel": "wool"},
                    {"type": f"{MOD}:resource_wool", "channel": "accent"},
                ],
            },
        }
    })
    write(os.path.join(ASSETS, "items", "mechanical_separator.json"),
          {"model": {"type": "minecraft:model", "model": f"{MOD}:block/mechanical_separator"}})
    write(os.path.join(ASSETS, "items", "automatic_shears.json"),
          {"model": {"type": "minecraft:model", "model": f"{MOD}:block/automatic_shears"}})
    write(os.path.join(ASSETS, "items", "hume_energy_cube.json"),
          {"model": {"type": "minecraft:model", "model": f"{MOD}:block/hume_energy_cube/item"}})
    # The item shows the reservoir full of water.
    write(os.path.join(ASSETS, "items", "fluid_reservoir.json"),
          {"model": {"type": "minecraft:model", "model": f"{MOD}:block/fluid_reservoir_4"}})


def machine_model(prefix, chamber):
    """Same shape as the Mycelial Generator: a casing cube plus a raised glass chamber over the front window."""
    full = [0, 0, 16, 16]
    casing = {}
    for face, tex, cull in (("north", "#front", "north"), ("east", "#side", "east"), ("south", "#back", "south"),
                            ("west", "#side", "west"), ("up", "#top", "up"), ("down", "#bottom", "down")):
        casing[face] = {"texture": tex, "uv": full, "cullface": cull}
    window = {face: {"texture": "#chamber", "uv": [3, 3, 13, 13]}
              for face in ("north", "east", "south", "west", "up", "down")}
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": f"{MOD}:block/{prefix}_side",
            "front": f"{MOD}:block/{prefix}_front",
            "back": f"{MOD}:block/{prefix}_back",
            "side": f"{MOD}:block/{prefix}_side",
            "top": f"{MOD}:block/{prefix}_top",
            "bottom": f"{MOD}:block/{prefix}_bottom",
            "chamber": f"{MOD}:block/{chamber}",
        },
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": casing},
            {"from": [3, 3, -0.5], "to": [13, 13, 0], "faces": window},
        ],
    }


def block_models():
    for prefix in ("mechanical_separator", "automatic_shears"):
        write(os.path.join(ASSETS, "models", "block", f"{prefix}.json"), machine_model(prefix, f"{prefix}_chamber_off"))
        write(os.path.join(ASSETS, "models", "block", f"{prefix}_active.json"), machine_model(prefix, f"{prefix}_chamber_on"))
        variants = {}
        for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            for active in ("false", "true"):
                v = {"model": f"{MOD}:block/{prefix}" + ("_active" if active == "true" else "")}
                if rot:
                    v["y"] = rot
                variants[f"active={active},facing={facing}"] = v
        write(os.path.join(ASSETS, "blockstates", f"{prefix}.json"), {"variants": variants})
    # Fluid Reservoir: one model per fill level (0..4), the side windows showing the water level.
    for fill in range(5):
        write(os.path.join(ASSETS, "models", "block", f"fluid_reservoir_{fill}.json"), {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": f"{MOD}:block/fluid_reservoir_side_{fill}",
                "north": f"{MOD}:block/fluid_reservoir_side_{fill}",
                "south": f"{MOD}:block/fluid_reservoir_side_{fill}",
                "east": f"{MOD}:block/fluid_reservoir_side_{fill}",
                "west": f"{MOD}:block/fluid_reservoir_side_{fill}",
                "up": f"{MOD}:block/fluid_reservoir_top",
                "down": f"{MOD}:block/fluid_reservoir_bottom",
            },
        })
    # Hume Energy Cube: connected textures. Each face is its own small model, picked by a multipart blockstate from
    # the four neighbours in that face's plane (no frame on edges where another cube touches) and, for the sides,
    # the cube's charge (its share of the group's energy level).
    models_dir = os.path.join(ASSETS, "models", "block")
    cube_dir = os.path.join(models_dir, "hume_energy_cube")
    for name in os.listdir(models_dir):
        if name.startswith("hume_energy_cube"):
            path = os.path.join(models_dir, name)
            if os.path.isfile(path):
                os.remove(path)
    os.makedirs(cube_dir, exist_ok=True)
    for name in os.listdir(cube_dir):
        os.remove(os.path.join(cube_dir, name))
    # face -> which block sides are the texture's (up, down, left, right), seen from outside the face
    planes = {
        "north": ("up", "down", "east", "west"),
        "south": ("up", "down", "west", "east"),
        "east": ("up", "down", "south", "north"),
        "west": ("up", "down", "north", "south"),
        "up": ("north", "south", "west", "east"),
        "down": ("south", "north", "west", "east"),
    }

    def face_model(face, texture):
        return {"parent": "minecraft:block/block",
                "textures": {"particle": texture, "face": texture},
                "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                              "faces": {face: {"texture": "#face", "cullface": face}}}]}

    parts = []
    for face, sides in planes.items():
        for m in range(16):
            bits = [bool(m & 8), bool(m & 4), bool(m & 2), bool(m & 1)]
            mask = "".join("1" if b else "0" for b in bits)
            when = {side: "true" if b else "false" for side, b in zip(sides, bits)}
            if face in ("up", "down"):
                model = f"{face}_{mask}"
                write(os.path.join(cube_dir, model + ".json"), face_model(face, f"{MOD}:block/hume_energy_cube/end_{mask}"))
                parts.append({"when": when, "apply": {"model": f"{MOD}:block/hume_energy_cube/{model}"}})
            else:
                for charge in range(9):
                    model = f"{face}_{mask}_{charge}"
                    write(os.path.join(cube_dir, model + ".json"),
                          face_model(face, f"{MOD}:block/hume_energy_cube/side_{mask}_{charge}"))
                    parts.append({"when": dict(when, charge=str(charge)),
                                  "apply": {"model": f"{MOD}:block/hume_energy_cube/{model}"}})
    # Inside-corner overlays, drawn a hair in front of the face where both edges continue but the diagonal cube's face
    # doesn't (the inside of an L).
    offset = {"north": ([0, 0, -0.01], [16, 16, 16]), "south": ([0, 0, 0], [16, 16, 16.01]),
              "west": ([-0.01, 0, 0], [16, 16, 16]), "east": ([0, 0, 0], [16.01, 16, 16]),
              "down": ([0, -0.01, 0], [16, 16, 16]), "up": ([0, 0, 0], [16, 16.01, 16])}
    for face in planes:
        for corner in ("tl", "tr", "bl", "br"):
            texture = f"{MOD}:block/hume_energy_cube/inner_{corner}"
            frm, to = offset[face]
            write(os.path.join(cube_dir, f"inner_{face}_{corner}.json"), {
                "parent": "minecraft:block/block",
                "textures": {"particle": texture, "face": texture},
                "elements": [{"from": frm, "to": to, "faces": {face: {"texture": "#face", "cullface": face}}}],
            })
    # Which face model each face uses (and the inside corners) is decided from the world by EnergyCubeModel, since it
    # needs blocks two steps away (whether a neighbour's face is hidden). The face models above are its building blocks.
    write(os.path.join(ASSETS, "blockstates", "hume_energy_cube.json"),
          {"multipart": [{"apply": {"type": f"{MOD}:energy_cube"}}]})
    # The item: a single, unconnected cube two thirds full.
    write(os.path.join(cube_dir, "item.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"side": f"{MOD}:block/hume_energy_cube/side_0000_6", "top": f"{MOD}:block/hume_energy_cube/end_0000",
                     "bottom": f"{MOD}:block/hume_energy_cube/end_0000"},
    })
    write(os.path.join(ASSETS, "blockstates", "fluid_reservoir.json"),
          {"variants": {f"fill={fill}": {"model": f"{MOD}:block/fluid_reservoir_{fill}"} for fill in range(5)}})
    write(os.path.join(ASSETS, "models", "block", "functional_machine_block.json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/functional_machine_block"}})
    write(os.path.join(ASSETS, "blockstates", "functional_machine_block.json"),
          {"variants": {"": {"model": f"{MOD}:block/functional_machine_block"}}})
    write(os.path.join(ASSETS, "items", "functional_machine_block.json"),
          {"model": {"type": "minecraft:model", "model": f"{MOD}:block/functional_machine_block"}})
    # Zero Point Energy Module: base plate plus emitter, pointing up; the ceiling variant is the same model flipped.
    zpe = f"{MOD}:block/zero_point_energy_module/"
    for state in ("off", "on"):
        side = {"uv": [0, 12, 16, 16], "texture": "#plate_side"}
        emitter_side = {"uv": [4, 0, 12, 4], "texture": "#emitter"}
        emitter = {"from": [4, 4, 4], "to": [12, 8, 12], "faces": {
            "up": {"uv": [4, 4, 12, 12], "texture": "#emitter"},
            "north": emitter_side, "south": emitter_side, "east": emitter_side, "west": emitter_side}}
        if state == "on":
            emitter["light_emission"] = 15
        write(os.path.join(ASSETS, "models", "block", "zero_point_energy_module", state + ".json"), {
            "parent": "minecraft:block/block",
            "textures": {"particle": zpe + f"plate_{state}", "plate": zpe + f"plate_{state}",
                         "plate_side": zpe + f"plate_side_{state}", "emitter": zpe + f"emitter_{state}",
                         "bottom": f"{MOD}:block/functional_machine_block"},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 4, 16], "faces": {
                    "down": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down"},
                    "up": {"uv": [0, 0, 16, 16], "texture": "#plate"},
                    "north": dict(side, cullface="north"), "south": dict(side, cullface="south"),
                    "east": dict(side, cullface="east"), "west": dict(side, cullface="west")}},
                emitter,
            ],
        })
    variants = {}
    for facing, x in (("up", None), ("down", 180)):
        for linked in ("false", "true"):
            v = {"model": zpe + ("on" if linked == "true" else "off")}
            if x:
                v["x"] = x
            variants[f"linked={linked},vertical_direction={facing}"] = v
    write(os.path.join(ASSETS, "blockstates", "zero_point_energy_module.json"), {"variants": variants})
    write(os.path.join(ASSETS, "items", "zero_point_energy_module.json"),
          {"model": {"type": "minecraft:model", "model": zpe + "on"}})
    write(os.path.join(ASSETS, "models", "block", "creative_generator.json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/creative_generator"}})
    write(os.path.join(ASSETS, "blockstates", "creative_generator.json"),
          {"variants": {"": {"model": f"{MOD}:block/creative_generator"}}})
    write(os.path.join(ASSETS, "items", "creative_generator.json"),
          {"model": {"type": "minecraft:model", "model": f"{MOD}:block/creative_generator"}})


def lang():
    en = {
        "itemGroup.barelyfunctionalcontraptions": "Barely Functional Contraptions",
        "itemGroup.barelyfunctionalcontraptions.resource_wool": "Resource Wool",
        "block.barelyfunctionalcontraptions.mechanical_separator": "Mechanical Separator",
        "block.barelyfunctionalcontraptions.creative_generator": "Creative Generator",
        "block.barelyfunctionalcontraptions.automatic_shears": "Automatic Shears",
        "tooltip.barelyfunctionalcontraptions.shift_place": "Hold Shift while placing to face it away from you",
        "jei.barelyfunctionalcontraptions.energy_short": "%s FE",
        "jei.barelyfunctionalcontraptions.time_short": "%ss",
        "jei.barelyfunctionalcontraptions.energy_time_short": "%s FE · %ss",
        "tooltip.barelyfunctionalcontraptions.automatic_shears": "Uses FE to shear sheep in the 3x3 area in front of it and keeps the wool. Take it out by hand or with a hopper below.",
        "gui.barelyfunctionalcontraptions.shears.energy_stored": "%s FE",
        "gui.barelyfunctionalcontraptions.shears.energy_capacity": "of %s",
        "gui.barelyfunctionalcontraptions.shears.energy_per_shear": "Uses %s FE per sheep sheared",
        "gui.barelyfunctionalcontraptions.shears.energy_sides": "Accepts FE on every side except the front",
        "gui.barelyfunctionalcontraptions.shears.area": "Shears every sheep in the 3x3 area in front",
        "gui.barelyfunctionalcontraptions.shears.next_sweep": "Next sweep in %ss",
        "gui.barelyfunctionalcontraptions.shears.interval": "Sweeps every %ss",
        "gui.barelyfunctionalcontraptions.shears.status.idle": "Waiting for Sheep",
        "gui.barelyfunctionalcontraptions.shears.status.idle.desc": "No sheep with wool in the 3x3 area in front right now",
        "gui.barelyfunctionalcontraptions.shears.status.processing": "Shearing",
        "gui.barelyfunctionalcontraptions.shears.status.processing.desc": "Just sheared the sheep in front of it",
        "gui.barelyfunctionalcontraptions.shears.status.no_energy": "No Energy",
        "gui.barelyfunctionalcontraptions.shears.status.no_energy.desc": "Not enough FE stored to shear the next sheep",
        "gui.barelyfunctionalcontraptions.shears.status.output_full": "Storage Full",
        "gui.barelyfunctionalcontraptions.shears.status.output_full.desc": "Take the wool out, or put a hopper below, to keep shearing",
        "gui.barelyfunctionalcontraptions.shears.status.invalid_input": "Idle",
        "gui.barelyfunctionalcontraptions.shears.status.invalid_input.desc": "",
        "gui.barelyfunctionalcontraptions.shears.status.redstone_disabled": "Disabled by Redstone",
        "gui.barelyfunctionalcontraptions.shears.status.redstone_disabled.desc": "Change the redstone signal or mode to run",
        "barelyfunctionalcontraptions.configuration.energyPerShear": "Energy per shear",
        "barelyfunctionalcontraptions.configuration.automatic_shears": "Automatic Shears",
        "barelyfunctionalcontraptions.configuration.fluid_reservoir": "Fluid Reservoir",
        "barelyfunctionalcontraptions.configuration.waterPerSecond": "Water per Second (mB)",
        "barelyfunctionalcontraptions.configuration.capacity": "Tank Capacity (mB)",
        "barelyfunctionalcontraptions.configuration.requireInfiniteSource": "Require Infinite Source",
        "barelyfunctionalcontraptions.configuration.outputPerTick": "Push to Neighbours (mB/t)",
        "block.barelyfunctionalcontraptions.fluid_reservoir": "Fluid Reservoir",
        "block.barelyfunctionalcontraptions.hume_energy_cube": "Hume Energy Cube",
        "block.barelyfunctionalcontraptions.functional_machine_block": "Functional Machine Block",
        "block.barelyfunctionalcontraptions.zero_point_energy_module": "Zero Point Energy Module",
        "tooltip.barelyfunctionalcontraptions.zero_point_energy_module": "Generates Forge Energy in pairs: place one on the floor and one on the ceiling directly above it, with open space between. The bigger the gap, the more power, and all of it can be taken from either module. Right-click to see the beam connection.",
        "gui.barelyfunctionalcontraptions.zpe.generating": "+%s FE/t",
        "gui.barelyfunctionalcontraptions.zpe.shared": "Shared pool",
        "gui.barelyfunctionalcontraptions.zpe.not_linked": "Not linked",
        "gui.barelyfunctionalcontraptions.zpe.status.linked": "Linked: %s block gap",
        "gui.barelyfunctionalcontraptions.zpe.status.full": "Linked: buffer full",
        "gui.barelyfunctionalcontraptions.zpe.status.full.desc": "Nothing is taking the energy. Put a machine, cable or Hume Energy Cube against the module.",
        "gui.barelyfunctionalcontraptions.zpe.status.blocked.above": "Beam blocked %s above",
        "gui.barelyfunctionalcontraptions.zpe.status.blocked.below": "Beam blocked %s below",
        "gui.barelyfunctionalcontraptions.zpe.status.wrong_facing": "Partner faces wrong way",
        "gui.barelyfunctionalcontraptions.zpe.status.too_close": "Partner too close",
        "gui.barelyfunctionalcontraptions.zpe.status.no_module.above": "No module above",
        "gui.barelyfunctionalcontraptions.zpe.status.no_module.below": "No module below",
        "gui.barelyfunctionalcontraptions.zpe.beam.tooltip": "Beam connection",
        "gui.barelyfunctionalcontraptions.zpe.beam.this.up": "This module (marked) is on the floor, pointing up.",
        "gui.barelyfunctionalcontraptions.zpe.beam.this.down": "This module (marked) is on the ceiling, pointing down.",
        "gui.barelyfunctionalcontraptions.zpe.desc.linked.above": "Linked to the module %s blocks above (%s blocks of open space).",
        "gui.barelyfunctionalcontraptions.zpe.desc.linked.below": "Linked to the module %s blocks below (%s blocks of open space).",
        "gui.barelyfunctionalcontraptions.zpe.desc.blocked.above": "A solid block %s blocks above stops the beam. Clear it, or swap it for glass.",
        "gui.barelyfunctionalcontraptions.zpe.desc.blocked.below": "A solid block %s blocks below stops the beam. Clear it, or swap it for glass.",
        "gui.barelyfunctionalcontraptions.zpe.desc.wrong_facing.above": "The module %s blocks above points up too. Place it on the underside of a block so it points down.",
        "gui.barelyfunctionalcontraptions.zpe.desc.wrong_facing.below": "The module %s blocks below points down too. Place it on top of a block so it points up.",
        "gui.barelyfunctionalcontraptions.zpe.desc.too_close": "The modules need more open space between them. Move one further away.",
        "gui.barelyfunctionalcontraptions.zpe.desc.no_module.above": "No module within %s blocks above. Place one on a ceiling directly above, pointing down.",
        "gui.barelyfunctionalcontraptions.zpe.desc.no_module.below": "No module within %s blocks below. Place one on the floor directly below, pointing up.",
        "gui.barelyfunctionalcontraptions.zpe.generation.tooltip": "Generation",
        "gui.barelyfunctionalcontraptions.zpe.generation.desc": "A linked pair makes more power the further apart the modules are. The two modules share one pool of energy, so all of it can be taken from either one. The gauge shows the whole pool. Out: energy leaving the pool through either module, averaged over a second.",
        "jade.barelyfunctionalcontraptions.zpe.unlinked": "No partner: needs a module facing it in the same column",
        "jade.barelyfunctionalcontraptions.zpe.linked_above": "Linked to the module above (%s block gap)",
        "jade.barelyfunctionalcontraptions.zpe.linked_below": "Linked to the module below (%s block gap)",
        "jade.barelyfunctionalcontraptions.zpe.output": "+%s FE/t, from either module",
        "config.jade.plugin_barelyfunctionalcontraptions.zero_point_module": "Zero Point Energy Module",
        "barelyfunctionalcontraptions.configuration.zero_point_energy_module": "Zero Point Energy Module",
        "tooltip.barelyfunctionalcontraptions.functional_machine_block": "The casing every machine is built around.",
        "tooltip.barelyfunctionalcontraptions.hume_energy_cube": "Stores Forge Energy, in and out on every side. Touching cubes join into one battery: every cube adds to the buffer and the transfer rate.",
        "tooltip.barelyfunctionalcontraptions.hume_energy_cube.stored": "Holds %s FE",
        "gui.barelyfunctionalcontraptions.cube.stored": "%s FE",
        "gui.barelyfunctionalcontraptions.cube.capacity": "of %s FE",
        "gui.barelyfunctionalcontraptions.cube.group": "x%s \u00b7 %s FE/t",
        "gui.barelyfunctionalcontraptions.cube.in": "In: %s FE/t",
        "gui.barelyfunctionalcontraptions.cube.out": "Out: %s FE/t",
        "gui.barelyfunctionalcontraptions.cube.percent": "%s%% full",
        "gui.barelyfunctionalcontraptions.cube.group.tooltip": "%s connected cube(s)",
        "gui.barelyfunctionalcontraptions.cube.group.desc": "Every touching cube adds to the buffer and the transfer rate",
        "gui.barelyfunctionalcontraptions.cube.flow.tooltip": "Energy flow (average over the last second)",
        "gui.barelyfunctionalcontraptions.cube.flow.desc": "Up to %s FE/t each way, on every side",
        "gui.barelyfunctionalcontraptions.cube.status.charging": "Charging",
        "gui.barelyfunctionalcontraptions.cube.status.charging.desc": "More energy is coming in than going out",
        "gui.barelyfunctionalcontraptions.cube.status.discharging": "Discharging",
        "gui.barelyfunctionalcontraptions.cube.status.discharging.desc": "More energy is going out than coming in",
        "gui.barelyfunctionalcontraptions.cube.status.full": "Full",
        "gui.barelyfunctionalcontraptions.cube.status.full.desc": "The buffer is full; add cubes to store more",
        "gui.barelyfunctionalcontraptions.cube.status.empty": "Empty",
        "gui.barelyfunctionalcontraptions.cube.status.empty.desc": "No energy stored",
        "gui.barelyfunctionalcontraptions.cube.status.idle": "Idle",
        "gui.barelyfunctionalcontraptions.cube.status.idle.desc": "No energy is moving in or out",
        "barelyfunctionalcontraptions.configuration.energy_cube": "Hume Energy Cube",
        "barelyfunctionalcontraptions.configuration.capacityPerCube": "Buffer per Cube (FE)",
        "barelyfunctionalcontraptions.configuration.transferPerCube": "Transfer Rate per Cube (FE/t)",
        "barelyfunctionalcontraptions.configuration.maxGroupSize": "Max Cubes per Group",
        "tooltip.barelyfunctionalcontraptions.fluid_reservoir": "Place it on top of an infinite water source to fill its water tank (10 buckets by default). Fills buckets and pushes water into tanks beside it.",
        "gui.barelyfunctionalcontraptions.reservoir.stored": "%s mB",
        "gui.barelyfunctionalcontraptions.reservoir.capacity": "of %s mB",
        "gui.barelyfunctionalcontraptions.reservoir.buckets": "%s buckets",
        "gui.barelyfunctionalcontraptions.reservoir.rate": "+%s mB/s",
        "gui.barelyfunctionalcontraptions.reservoir.tank": "Water: %s / %s mB",
        "gui.barelyfunctionalcontraptions.reservoir.tank.desc": "Take it out with a bucket, pipes, or a tank placed beside or on top",
        "gui.barelyfunctionalcontraptions.reservoir.buckets_slot": "Empty buckets in the top slot are filled into the bottom slot",
        "gui.barelyfunctionalcontraptions.reservoir.status.filling": "Filling",
        "gui.barelyfunctionalcontraptions.reservoir.status.filling.desc": "Drawing water from the infinite source below",
        "gui.barelyfunctionalcontraptions.reservoir.status.full": "Tank Full",
        "gui.barelyfunctionalcontraptions.reservoir.status.full.desc": "Take water out to keep filling",
        "gui.barelyfunctionalcontraptions.reservoir.status.not_infinite": "Source Not Infinite",
        "gui.barelyfunctionalcontraptions.reservoir.status.not_infinite.desc": "The water below needs at least two water source neighbours (e.g. a 2x2 pool)",
        "gui.barelyfunctionalcontraptions.reservoir.status.no_source": "No Water Source",
        "gui.barelyfunctionalcontraptions.reservoir.status.no_source.desc": "Place it directly on top of a water source block",
        "jade.barelyfunctionalcontraptions.reservoir.water": "Water: %s / %s mB",
        "barelyfunctionalcontraptions.configuration.shearInterval": "Shear interval",
        "tooltip.barelyfunctionalcontraptions.creative_generator": "Infinite Forge Energy for testing. Pushes FE into adjacent machines; cables can also pull from any side.",
        "jei.barelyfunctionalcontraptions.info.creative_generator": "Creative-only. Produces unlimited Forge Energy (FE): every tick it pushes power into all adjacent energy receivers (rate set by creativeGeneratorOutput in the server config), and cables or machines can pull unlimited FE from any side. Place it next to a Mechanical Separator to test without a power setup.",
        "item.barelyfunctionalcontraptions.resource_wool": "Resource Wool",
        "item.barelyfunctionalcontraptions.resource_wool.named": "%s Wool",
        "item.barelyfunctionalcontraptions.resource_wool.unknown": "Unknown Resource Wool",
        "tooltip.barelyfunctionalcontraptions.wool.contains": "Infused with: %s",
        "tooltip.barelyfunctionalcontraptions.wool.no_resource": "No resource - craft or shear it from an infused sheep",
        "tooltip.barelyfunctionalcontraptions.wool.unknown_resource": "Unknown resource '%s' (removed by a data pack?)",
        "tooltip.barelyfunctionalcontraptions.wool.separates_into": "Mechanical Separator output:",
        "tooltip.barelyfunctionalcontraptions.output_line": "%sx %s",
        "tooltip.barelyfunctionalcontraptions.chance_line": "%sx %s (%s%% chance)",
        "tooltip.barelyfunctionalcontraptions.energy_time": "%s FE over %s s",
        "tooltip.barelyfunctionalcontraptions.hold_shift": "Hold Shift for separator details",
        "category.barelyfunctionalcontraptions.ore": "Ore",
        "category.barelyfunctionalcontraptions.gem": "Gem",
        "category.barelyfunctionalcontraptions.dust": "Dust",
        "category.barelyfunctionalcontraptions.material": "Material",
        "category.barelyfunctionalcontraptions.organic": "Organic",
        "message.barelyfunctionalcontraptions.infused": "The sheep's wool shimmers with %s!",
        "message.barelyfunctionalcontraptions.too_young": "This lamb is too young to be infused",
        "message.barelyfunctionalcontraptions.needs_wool": "This sheep needs to regrow its wool first",
        "message.barelyfunctionalcontraptions.cooldown": "This sheep needs to rest for %s more seconds",
        "message.barelyfunctionalcontraptions.already_infused": "This sheep is already fully infused with %s",
        "message.barelyfunctionalcontraptions.already_infused_other": "This sheep is already infused with %s",
        "message.barelyfunctionalcontraptions.needs_more": "Infusing needs %s x %s",
        "message.barelyfunctionalcontraptions.inspect.none": "This sheep is not infused",
        "message.barelyfunctionalcontraptions.inspect.cooldown": "This sheep is resting (%s s before it can be infused)",
        "message.barelyfunctionalcontraptions.inspect.infused": "Infused with %s - %s shearing(s) left",
        "message.barelyfunctionalcontraptions.inspect.permanent": "Permanently infused with %s",
        "gui.barelyfunctionalcontraptions.energy": "Energy: %s / %s FE",
        "gui.barelyfunctionalcontraptions.energy_short": "%s / %s FE",
        "gui.barelyfunctionalcontraptions.energy_input": "Max input: %s FE/t",
        "gui.barelyfunctionalcontraptions.energy_usage": "Usage: %s FE/t",
        "gui.barelyfunctionalcontraptions.progress": "Progress: %s%%",
        "gui.barelyfunctionalcontraptions.processing_percent": "Processing: %s%%",
        "gui.barelyfunctionalcontraptions.time_left": "%s s left of %s s",
        "gui.barelyfunctionalcontraptions.operation_energy": "Operation cost: %s FE",
        "gui.barelyfunctionalcontraptions.status.idle": "Idle",
        "gui.barelyfunctionalcontraptions.status.idle.desc": "Insert resource wool to begin",
        "gui.barelyfunctionalcontraptions.status.processing": "Processing",
        "gui.barelyfunctionalcontraptions.status.processing.desc": "Separating wool into its resource",
        "gui.barelyfunctionalcontraptions.status.no_energy": "No Energy",
        "gui.barelyfunctionalcontraptions.status.no_energy.desc": "Not enough FE for the next tick - progress is paused",
        "gui.barelyfunctionalcontraptions.status.output_full": "Output Full",
        "gui.barelyfunctionalcontraptions.status.output_full.desc": "Empty the output slots to continue",
        "gui.barelyfunctionalcontraptions.status.invalid_input": "Invalid Input",
        "gui.barelyfunctionalcontraptions.status.invalid_input.desc": "This item cannot be separated",
        "gui.barelyfunctionalcontraptions.status.redstone_disabled": "Disabled by Redstone",
        "gui.barelyfunctionalcontraptions.status.redstone_disabled.desc": "Change the redstone signal or mode to run",
        "gui.barelyfunctionalcontraptions.redstone": "Redstone: %s",
        "gui.barelyfunctionalcontraptions.redstone.ignored": "Ignored",
        "gui.barelyfunctionalcontraptions.redstone.ignored.desc": "Always runs",
        "gui.barelyfunctionalcontraptions.redstone.high": "Active with Signal",
        "gui.barelyfunctionalcontraptions.redstone.high.desc": "Runs only while powered",
        "gui.barelyfunctionalcontraptions.redstone.low": "Active without Signal",
        "gui.barelyfunctionalcontraptions.redstone.low.desc": "Runs only while unpowered",
        "gui.barelyfunctionalcontraptions.redstone.disabled": "Disabled",
        "gui.barelyfunctionalcontraptions.redstone.disabled.desc": "Never runs",
        "gui.barelyfunctionalcontraptions.redstone.powered": "Currently powered",
        "gui.barelyfunctionalcontraptions.redstone.unpowered": "Currently unpowered",
        "gui.barelyfunctionalcontraptions.redstone.click": "Click to change",
        "gui.barelyfunctionalcontraptions.redstone.locked": "Redstone control is disabled in the server config",
        "jei.barelyfunctionalcontraptions.resource_infusion": "Resource Infusion",
        "jei.barelyfunctionalcontraptions.mechanical_separation": "Mechanical Separation",
        "jei.barelyfunctionalcontraptions.consumed": "Consumed: %s per feeding",
        "jei.barelyfunctionalcontraptions.per_shearing": "Dropped per shearing: %s",
        "jei.barelyfunctionalcontraptions.uses": "Lasts %s shearing(s)",
        "jei.barelyfunctionalcontraptions.uses_permanent": "Permanent infusion",
        "jei.barelyfunctionalcontraptions.category": "Category: %s",
        "jei.barelyfunctionalcontraptions.energy": "Energy: %s FE",
        "jei.barelyfunctionalcontraptions.energy_per_tick": "%s FE/t",
        "jei.barelyfunctionalcontraptions.time": "Time: %s seconds",
        "jei.barelyfunctionalcontraptions.chance": "%s%% chance",
        "jei.barelyfunctionalcontraptions.average": "Average: %s",
        "jei.barelyfunctionalcontraptions.range": "Produces %s per wool (average %s)",
        "jei.barelyfunctionalcontraptions.info.resource_wool": "Right-click an adult sheep with a resource (see Resource Infusion) to infuse it. Shearing an infused sheep drops this wool, which a powered Mechanical Separator turns back into the resource plus string.",
        "jei.barelyfunctionalcontraptions.info.separator": "Separates resource wool into its resource and string using Forge Energy (FE). Hoppers and pipes insert from the top and sides and extract from the bottom by default; outputs are pushed into an inventory below. Configure sides and costs in the server config.",
        "command.barelyfunctionalcontraptions.preview.none": "Every material in c:storage_blocks tags already has a sheep resource.",
        "command.barelyfunctionalcontraptions.preview.header": "%s new sheep resource(s) would be generated:",
        "command.barelyfunctionalcontraptions.preview.footer": "Run /barelyfunctionalcontraptions generate to create them.",
        "command.barelyfunctionalcontraptions.generate.done": "Generated %s sheep resource(s) into the world data pack '%s'. Reloading...",
        "command.barelyfunctionalcontraptions.generate.failed": "Could not write the generated data pack: %s",
        "command.barelyfunctionalcontraptions.list.header": "%s sheep resource(s) loaded:",
        "command.barelyfunctionalcontraptions.material.unknown": "No c:storage_blocks/%s tag with items exists. Use tab-completion to pick a material, or /barelyfunctionalcontraptions generate item <item>.",
        "command.barelyfunctionalcontraptions.item.empty": "Hold an item in your main hand (or use /barelyfunctionalcontraptions generate item <item>).",
        "command.barelyfunctionalcontraptions.item.wool": "Resource wool can't be used as a feeding item.",
        "command.barelyfunctionalcontraptions.item.covered": "That item already makes %s wool (%s). Nothing was generated.",
        "command.barelyfunctionalcontraptions.single.done": "Added %s wool (barelyfunctionalcontraptions_generated:%s). Reloading...",
        "command.barelyfunctionalcontraptions.remove.unknown": "No generated wool type named '%s'. Only resources made by /barelyfunctionalcontraptions generate can be removed here.",
        "command.barelyfunctionalcontraptions.remove.done": "Removed %s wool from the generated data pack. Reloading...",
        "command.barelyfunctionalcontraptions.dedupe.none": "No duplicate wool types found.",
        "command.barelyfunctionalcontraptions.dedupe.done": "Removed %s duplicate wool type(s) from the generated data pack. Reloading...",
        "jade.barelyfunctionalcontraptions.infused": "Infused: %s",
        "jade.barelyfunctionalcontraptions.uses_left": "%s shearing(s) left",
        "jade.barelyfunctionalcontraptions.permanent": "Permanent infusion",
        "jade.barelyfunctionalcontraptions.cooldown": "Resting: %s s",
        "jade.barelyfunctionalcontraptions.status": "Status: %s",
        "config.jade.plugin_barelyfunctionalcontraptions.sheep_infusion": "Sheep infusion",
        "config.jade.plugin_barelyfunctionalcontraptions.separator_status": "Separator status",
        "config.jade.plugin_barelyfunctionalcontraptions.separator_progress": "Separator progress",
        "barelyfunctionalcontraptions.configuration.title": "Barely Functional Contraptions Configuration",
        "barelyfunctionalcontraptions.configuration.section.barelyfunctionalcontraptions.sheep.synced.toml": "Resourceful Sheep: gameplay (per world)",
        "barelyfunctionalcontraptions.configuration.section.barelyfunctionalcontraptions.sheep.synced.toml.title": "Resourceful Sheep: gameplay (per world)",
        "barelyfunctionalcontraptions.configuration.section.barelyfunctionalcontraptions.sheep.client.toml": "Resourceful Sheep: client",
        "barelyfunctionalcontraptions.configuration.section.barelyfunctionalcontraptions.sheep.client.toml.title": "Resourceful Sheep: client",
        "barelyfunctionalcontraptions.configuration.sheep": "Sheep",
        "barelyfunctionalcontraptions.configuration.separator": "Mechanical Separator",
        "barelyfunctionalcontraptions.configuration.creative_generator": "Creative Generator",
    }
    for name, r in RESOURCES.items():
        en[f"resource.{MOD}.{name}"] = r["display"]
    # The lang file is shared with the Mycelial Power module, so merge into it instead of replacing it.
    path = os.path.join(ASSETS, "lang", "en_us.json")
    merged = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            merged = json.load(f)
    merged.update(en)
    write(path, merged)


def recipes_loot_tags():
    # The casing every machine recipe uses.
    write(os.path.join(DATA, MOD, "recipe", "functional_machine_block.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "key": {
            "I": "minecraft:iron_ingot",
            "C": "minecraft:copper_ingot",
            "R": "minecraft:redstone",
        },
        "pattern": ["ICI", "CRC", "ICI"],
        "result": {"id": f"{MOD}:functional_machine_block"},
    })
    write(os.path.join(DATA, MOD, "recipe", "mechanical_separator.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "key": {
            "I": "minecraft:iron_ingot",
            "C": "minecraft:copper_ingot",
            "H": "minecraft:hopper",
            "P": "minecraft:piston",
            "M": f"{MOD}:functional_machine_block",
            "S": "minecraft:shears",
        },
        "pattern": ["IHI", "PMP", "CSC"],
        "result": {"id": f"{MOD}:mechanical_separator"},
    })
    write(os.path.join(DATA, MOD, "recipe", "automatic_shears.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "key": {
            "I": "minecraft:iron_ingot",
            "S": "minecraft:shears",
            "M": f"{MOD}:functional_machine_block",
            "C": "minecraft:chest",
            "R": "minecraft:redstone",
            "T": "minecraft:copper_ingot",
        },
        "pattern": ["TRT", "SMS", "ICI"],
        "result": {"id": f"{MOD}:automatic_shears"},
    })
    write(os.path.join(DATA, MOD, "recipe", "hume_energy_cube.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "key": {
            "I": "minecraft:iron_ingot",
            "R": "minecraft:redstone_block",
            "A": "minecraft:amethyst_shard",
            "M": f"{MOD}:functional_machine_block",
        },
        "pattern": ["IAI", "ARA", "IMI"],
        "result": {"id": f"{MOD}:hume_energy_cube"},
    })
    write(os.path.join(DATA, MOD, "recipe", "fluid_reservoir.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "key": {
            "I": "minecraft:iron_ingot",
            "G": "#c:glass_blocks",
            "B": "minecraft:bucket",
            "M": f"{MOD}:functional_machine_block",
            "C": "minecraft:hopper",
        },
        "pattern": ["IBI", "GMG", "ICI"],
        "result": {"id": f"{MOD}:fluid_reservoir"},
    })
    write(os.path.join(DATA, MOD, "recipe", "zero_point_energy_module.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "key": {
            "I": "minecraft:iron_ingot",
            "C": "minecraft:copper_ingot",
            "S": "minecraft:nether_star",
            "A": "minecraft:amethyst_shard",
            "M": f"{MOD}:functional_machine_block",
            "R": "minecraft:redstone_block",
        },
        "pattern": ["ISI", "AMA", "CRC"],
        "result": {"id": f"{MOD}:zero_point_energy_module", "count": 2},
    })
    write(os.path.join(DATA, MOD, "loot_table", "blocks", "zero_point_energy_module.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                   "entries": [{"type": "minecraft:item", "name": f"{MOD}:zero_point_energy_module"}]}],
        "random_sequence": f"{MOD}:blocks/zero_point_energy_module",
    })
    # Example explicit separating recipe: plain wool back into string.
    write(os.path.join(DATA, MOD, "recipe", "separating", "wool_to_string.json"), {
        "type": f"{MOD}:separating",
        "ingredient": "#minecraft:wool",
        "primary_output": {"id": "minecraft:string", "count": 3},
        "energy_cost": 2000,
        "processing_ticks": 40,
    })
    write(os.path.join(DATA, MOD, "loot_table", "blocks", "mechanical_separator.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{
                "type": "minecraft:item",
                "name": f"{MOD}:mechanical_separator",
                "modifier": {
                    "type": "minecraft:copy_components",
                    "source": "block_entity",
                    "include": ["minecraft:custom_name", f"{MOD}:machine_contents"],
                },
            }],
        }],
        "random_sequence": f"{MOD}:blocks/mechanical_separator",
    })
    write(os.path.join(DATA, MOD, "loot_table", "blocks", "automatic_shears.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{
                "type": "minecraft:item",
                "name": f"{MOD}:automatic_shears",
                "modifier": {"type": "minecraft:copy_components", "source": "block_entity",
                             "include": ["minecraft:custom_name", f"{MOD}:machine_contents"]},
            }],
        }],
        "random_sequence": f"{MOD}:blocks/automatic_shears",
    })
    write(os.path.join(DATA, MOD, "loot_table", "blocks", "hume_energy_cube.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{
                "type": "minecraft:item",
                "name": f"{MOD}:hume_energy_cube",
                "modifier": {"type": "minecraft:copy_components", "source": "block_entity",
                             "include": ["minecraft:custom_name", f"{MOD}:machine_contents"]},
            }],
        }],
        "random_sequence": f"{MOD}:blocks/hume_energy_cube",
    })
    write(os.path.join(DATA, MOD, "loot_table", "blocks", "functional_machine_block.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                   "entries": [{"type": "minecraft:item", "name": f"{MOD}:functional_machine_block"}]}],
        "random_sequence": f"{MOD}:blocks/functional_machine_block",
    })
    write(os.path.join(DATA, MOD, "loot_table", "blocks", "fluid_reservoir.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "condition": {"type": "minecraft:survives_explosion"},
            "entries": [{
                "type": "minecraft:item",
                "name": f"{MOD}:fluid_reservoir",
                "modifier": {"type": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:custom_name"]},
            }],
        }],
        "random_sequence": f"{MOD}:blocks/fluid_reservoir",
    })
    write(os.path.join(DATA, MOD, "loot_modifiers", "infused_wool.json"), {
        "type": f"{MOD}:infused_wool",
        "table_prefix": "shearing/",
    })
    write(os.path.join(DATA, MOD, "loot_modifiers", "infused_sheep_kill.json"), {
        "type": f"{MOD}:infused_sheep_kill",
    })
    write(os.path.join(DATA, MOD, "tags", "entity_type", "infusable.json"), {"values": ["minecraft:sheep"]})
    write(os.path.join(DATA, MOD, "tags", "entity_type", "auto_shearable.json"), {"values": [f"#{MOD}:infusable"]})
    write(os.path.join(DATA, MOD, "tags", "item", "replaceable_wool.json"), {"values": ["#minecraft:wool"]})
    # Machines a Mycelial Generator can power wirelessly through a Barely Functional Wrench link.
    write(os.path.join(DATA, MOD, "tags", "block", "wireless_power_receivers.json"),
          {"values": [f"{MOD}:mechanical_separator", f"{MOD}:automatic_shears", f"{MOD}:hume_energy_cube"]})
    write(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json"), {"values": [f"{MOD}:mycelial_generator", f"{MOD}:mechanical_separator", f"{MOD}:automatic_shears", f"{MOD}:fluid_reservoir", f"{MOD}:hume_energy_cube", f"{MOD}:functional_machine_block", f"{MOD}:zero_point_energy_module"]})
    write(os.path.join(DATA, "minecraft", "tags", "block", "needs_stone_tool.json"), {"values": [f"{MOD}:mycelial_generator", f"{MOD}:mechanical_separator", f"{MOD}:automatic_shears", f"{MOD}:fluid_reservoir", f"{MOD}:hume_energy_cube", f"{MOD}:functional_machine_block", f"{MOD}:zero_point_energy_module"]})


if __name__ == "__main__":
    resource_definitions()
    item_models()
    block_models()
    lang()
    recipes_loot_tags()
    print("Data written to", os.path.abspath(RES))


# ---------------------------------------------------------------------------
# Game test structure: 7x5x7 with a stone floor (NBT, gzip).
# ---------------------------------------------------------------------------
def write_test_structure(data_version=5023):
    import gzip
    import struct

    def tag_string(s):
        b = s.encode("utf-8")
        return struct.pack(">H", len(b)) + b

    def named(tag_id, name, payload):
        return bytes([tag_id]) + tag_string(name) + payload

    def int_list(values):
        return bytes([3]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)

    def compound(entries):
        return b"".join(entries) + b"\x00"

    sx, sy, sz = 7, 5, 7
    palette = [compound([named(8, "Name", tag_string("minecraft:air"))]),
               compound([named(8, "Name", tag_string("minecraft:stone"))])]
    blocks = []
    for y in range(sy):
        for z in range(sz):
            for x in range(sx):
                blocks.append(compound([
                    named(9, "pos", int_list([x, y, z])),
                    named(3, "state", struct.pack(">i", 1 if y == 0 else 0)),
                ]))
    root = compound([
        named(3, "DataVersion", struct.pack(">i", data_version)),
        named(9, "size", int_list([sx, sy, sz])),
        named(9, "palette", bytes([10]) + struct.pack(">i", len(palette)) + b"".join(palette)),
        named(9, "blocks", bytes([10]) + struct.pack(">i", len(blocks)) + b"".join(blocks)),
        named(9, "entities", bytes([0]) + struct.pack(">i", 0)),
    ])
    data = bytes([10]) + tag_string("") + root
    path = os.path.join(DATA, MOD, "structure", "test_platform.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.open(path, "wb") as f:
        f.write(data)


write_test_structure()
