// Example KubeJS server script for Barely Functional Contraptions.
// Copy it into kubejs/server_scripts/ and edit to taste. Every recipe here is plain JSON passed to event.custom, so it
// works with any KubeJS version that has event.custom; the JSON formats are the mod's own, documented in the README.

ServerEvents.recipes(event => {
  // A separator recipe for one kind of resource wool, using the mod's resource_wool ingredient.
  // An explicit recipe always wins over the wool's resource definition.
  event.custom({
    type: 'barelyfunctionalcontraptions:separating',
    ingredient: { 'neoforge:ingredient_type': 'barelyfunctionalcontraptions:resource_wool', resource: 'barelyfunctionalcontraptions:iron' },
    primary_output: { id: 'minecraft:raw_iron', count: 4 },
    secondary_output: { id: 'minecraft:iron_nugget', count: 3 },
    bonus_output: { item: 'minecraft:raw_gold', chance: 0.05 },
    energy_cost: 30000,
    processing_ticks: 120
  })

  // Any gem wool can be crafted into 2 string (the category filter matches every loaded gem resource).
  event.custom({
    type: 'minecraft:crafting_shapeless',
    ingredients: [{ 'neoforge:ingredient_type': 'barelyfunctionalcontraptions:resource_wool', category: 'gem' }],
    result: { id: 'minecraft:string', count: 2 }
  })

  // Process something that isn't wool at all in the Mechanical Separator.
  event.custom({
    type: 'barelyfunctionalcontraptions:separating',
    ingredient: 'minecraft:gravel',
    primary_output: 'minecraft:flint',
    bonus_output: { item: 'minecraft:iron_nugget', chance: 0.1 },
    energy_cost: 4000,
    processing_ticks: 40
  })
})

ServerEvents.highPriorityData(event => {
  // Extra Mycelial Generator fuels, through the barelyfunctionalcontraptions:mycelial_fuel data map.
  event.addJson('kubejs:data_maps/item/mycelial_fuel.json', {
    values: {
      'minecraft:mushroom_stem': { burn_time: 200, energy: 0.8 },
      'minecraft:shroomlight': { burn_time: 1200, energy: 1.5, mycelium: 2.0 }
    }
  })
})
