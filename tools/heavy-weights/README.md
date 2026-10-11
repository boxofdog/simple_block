# Heavy-load weight table

`src/main/resources/simple_block/heavy_weights.json` lists how many ingot-equivalents of iron,
gold and copper each item carries (INV-04). It was generated with `heavy_weights.py` and then
reviewed by hand (the `notes` in the JSON).

```
python3 heavy_weights.py <extracted vanilla data>/data/minecraft/recipe computed.json
```

The script gives each item the metal of its cheapest recipe (base items fixed: ingots, raw metal
and ores 1, nuggets 1/9), adds copper oxidation states (same as the unoxidised item), filled
buckets, damaged anvils, maps and loot-only gear, and checks that no recipe makes more metal than
it consumes. Items it cannot classify sensibly are listed under `notes` with "decision needed".
The game test `HeavyLoadGameTests.noRecipeCreatesCarryingCapacity` repeats that check against the
recipes the running game actually loaded.
