# Heavy-load weight table

`src/main/resources/simple_block/heavy_weights.json` lists how many ingot-equivalents of iron,
gold, copper, diamond and netherite each item carries (INV-04), one section per material (the
client's load bar shows them in separate colours). It was generated with `heavy_weights.py` and
then reviewed by hand (the `notes` in the JSON).

```
python3 heavy_weights.py <extracted vanilla data>/data/minecraft/recipe computed.json \
    ../../src/main/resources/data/simple_block/recipe
```

The optional extra directories add recipes for the mod's own items (Milestone 04: cable, coil,
drum, anchor, connecting rod); their ingredients' tags still come from the vanilla data.

The script gives each item the content of its cheapest recipe (base items fixed: ingots, raw metal
and ores 1, nuggets 1/9, a diamond and diamond ore 1, a netherite ingot 1; netherite smithing
upgrades weigh what the diamond item did), adds copper oxidation states (same as the unoxidised item), filled
buckets, damaged anvils, maps and loot-only gear, and checks that no recipe makes more metal than
it consumes. Items it cannot classify sensibly are listed under `notes` with "decision needed".
The game test `HeavyLoadGameTests.noRecipeCreatesCarryingCapacity` repeats that check against the
recipes the running game actually loaded.
