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
    "netherite": dict(display="Netherite", feed="minecraft:netherite_scrap", out="minecraft:netherite_scrap", cat="material", uses=1,
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


def block_models():
    def model(front):
        return {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": f"{MOD}:block/mechanical_separator_side",
                "north": f"{MOD}:block/{front}",
                "south": f"{MOD}:block/mechanical_separator_back",
                "east": f"{MOD}:block/mechanical_separator_side",
                "west": f"{MOD}:block/mechanical_separator_side",
                "up": f"{MOD}:block/mechanical_separator_top",
                "down": f"{MOD}:block/mechanical_separator_bottom",
            },
        }
    write(os.path.join(ASSETS, "models", "block", "mechanical_separator.json"), model("mechanical_separator_front"))
    write(os.path.join(ASSETS, "models", "block", "mechanical_separator_active.json"), model("mechanical_separator_front_active"))
    variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for active in ("false", "true"):
            v = {"model": f"{MOD}:block/mechanical_separator" + ("_active" if active == "true" else "")}
            if rot:
                v["y"] = rot
            variants[f"active={active},facing={facing}"] = v
    write(os.path.join(ASSETS, "blockstates", "mechanical_separator.json"), {"variants": variants})
    write(os.path.join(ASSETS, "models", "block", "automatic_shears.json"), {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": f"{MOD}:block/automatic_shears_side",
            "north": f"{MOD}:block/automatic_shears_front",
            "south": f"{MOD}:block/automatic_shears_back",
            "east": f"{MOD}:block/automatic_shears_side",
            "west": f"{MOD}:block/automatic_shears_side",
            "up": f"{MOD}:block/automatic_shears_top",
            "down": f"{MOD}:block/automatic_shears_bottom",
        },
    })
    shear_variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        v = {"model": f"{MOD}:block/automatic_shears"}
        if rot:
            v["y"] = rot
        shear_variants[f"facing={facing}"] = v
    write(os.path.join(ASSETS, "blockstates", "automatic_shears.json"), {"variants": shear_variants})
    write(os.path.join(ASSETS, "models", "block", "creative_generator.json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/creative_generator"}})
    write(os.path.join(ASSETS, "blockstates", "creative_generator.json"),
          {"variants": {"": {"model": f"{MOD}:block/creative_generator"}}})
    write(os.path.join(ASSETS, "items", "creative_generator.json"),
          {"model": {"type": "minecraft:model", "model": f"{MOD}:block/creative_generator"}})


def lang():
    en = {
        "itemGroup.barelyfunctionalcontraptions": "Barely Functional Contraptions",
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
    write(os.path.join(DATA, MOD, "recipe", "mechanical_separator.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "key": {
            "I": "minecraft:iron_ingot",
            "C": "minecraft:copper_ingot",
            "H": "minecraft:hopper",
            "P": "minecraft:piston",
            "R": "minecraft:redstone",
            "S": "minecraft:shears",
        },
        "pattern": ["IHI", "PSP", "CRC"],
        "result": {"id": f"{MOD}:mechanical_separator"},
    })
    write(os.path.join(DATA, MOD, "recipe", "automatic_shears.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "key": {
            "I": "minecraft:iron_ingot",
            "S": "minecraft:shears",
            "R": "minecraft:redstone",
            "C": "minecraft:chest",
        },
        "pattern": ["ISI", "SRS", "ICI"],
        "result": {"id": f"{MOD}:automatic_shears"},
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
    write(os.path.join(DATA, MOD, "loot_modifiers", "infused_wool.json"), {
        "type": f"{MOD}:infused_wool",
        "table_prefix": "shearing/",
    })
    write(os.path.join(DATA, MOD, "tags", "entity_type", "infusable.json"), {"values": ["minecraft:sheep"]})
    write(os.path.join(DATA, MOD, "tags", "entity_type", "auto_shearable.json"), {"values": [f"#{MOD}:infusable"]})
    write(os.path.join(DATA, MOD, "tags", "item", "replaceable_wool.json"), {"values": ["#minecraft:wool"]})
    write(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json"), {"values": [f"{MOD}:mycelial_generator", f"{MOD}:mechanical_separator", f"{MOD}:automatic_shears"]})
    write(os.path.join(DATA, "minecraft", "tags", "block", "needs_stone_tool.json"), {"values": [f"{MOD}:mycelial_generator", f"{MOD}:mechanical_separator", f"{MOD}:automatic_shears"]})


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
