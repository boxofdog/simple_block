package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.RemovedRocks;
import io.github.boxofdog.simpleblock.worldgen.IronWorldgen;
import io.github.boxofdog.simpleblock.worldgen.TunedPlacements;

/**
 * GEO-01 / ORE-01 / ORE-02 wiring in the running server's registries. The resulting terrain is
 * measured on real normal-terrain worlds by the ore sampler (docs/MILESTONE_02_REPORT.md), since
 * the game-test world is superflat and says nothing about ore generation.
 */
public class WorldgenGameTests {
	private static Set<ResourceKey<PlacedFeature>> features(Biome biome, GenerationStep.Decoration step) {
		HolderSet<PlacedFeature> set = biome.getGenerationSettings().features().size() > step.ordinal()
			? biome.getGenerationSettings().features().get(step.ordinal()) : HolderSet.empty();
		Set<ResourceKey<PlacedFeature>> keys = new java.util.HashSet<>();
		for (Holder<PlacedFeature> holder : set) {
			holder.unwrapKey().ifPresent(keys::add);
		}
		return keys;
	}

	/**
	 * Every Overworld biome: no rock blobs, no vanilla scattered iron, the new background iron and
	 * geothermal pockets everywhere; deep-ocean, banded and placer deposits exactly in their biome tags.
	 */
	@GameTest
	public void overworldBiomesUseSimpleBlockIron(GameTestHelper helper) {
		Registry<Biome> biomes = helper.getLevel().getServer().registryAccess().lookupOrThrow(Registries.BIOME);
		List<String> problems = new ArrayList<>();
		int overworld = 0;
		int deepOcean = 0;
		int banded = 0;
		int placer = 0;
		for (Holder.Reference<Biome> biome : biomes.listElements().toList()) {
			if (!biome.is(BiomeTags.IS_OVERWORLD)) {
				continue;
			}
			overworld++;
			String id = biome.key().identifier().toString();
			Set<ResourceKey<PlacedFeature>> ores = features(biome.value(), GenerationStep.Decoration.UNDERGROUND_ORES);
			Set<ResourceKey<PlacedFeature>> decoration = features(biome.value(), GenerationStep.Decoration.UNDERGROUND_DECORATION);
			for (ResourceKey<PlacedFeature> removed : RemovedRocks.BLOB_FEATURES) {
				if (ores.contains(removed)) {
					problems.add(id + " still has " + removed.identifier());
				}
			}
			for (ResourceKey<PlacedFeature> removed : IronWorldgen.VANILLA_IRON) {
				if (ores.contains(removed)) {
					problems.add(id + " still has " + removed.identifier());
				}
			}
			if (!ores.contains(IronWorldgen.ORE_IRON_STONE) || !ores.contains(IronWorldgen.ORE_IRON_DEEPSLATE)
				|| !decoration.contains(IronWorldgen.IRON_GEOTHERMAL)) {
				problems.add(id + " lacks background or geothermal iron");
			}
			boolean isDeepOcean = biome.is(IronWorldgen.DEEP_OCEAN_IRON);
			boolean isBanded = biome.is(IronWorldgen.BANDED_IRON_DEPOSITS);
			boolean isPlacer = biome.is(IronWorldgen.PLACER_IRON_DEPOSITS);
			deepOcean += isDeepOcean ? 1 : 0;
			banded += isBanded ? 1 : 0;
			placer += isPlacer ? 1 : 0;
			if (ores.contains(IronWorldgen.ORE_IRON_DEEP_OCEAN) != isDeepOcean || ores.contains(IronWorldgen.ORE_IRON_DEEP_OCEAN_BODY) != isDeepOcean) {
				problems.add(id + " deep-ocean iron mismatch");
			}
			if (ores.contains(IronWorldgen.IRON_BANDED) != isBanded) {
				problems.add(id + " banded iron mismatch");
			}
			if (decoration.contains(IronWorldgen.IRON_PLACER) != isPlacer) {
				problems.add(id + " placer iron mismatch");
			}
			// Diamonds stay where vanilla has them (keep_chance is applied inside the placed feature).
			if (!ores.contains(ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("ore_diamond")))) {
				problems.add(id + " lost ore_diamond");
			}
		}
		SimpleBlockTestLog.info("ORE-01 wiring: {} Overworld biomes; deep-ocean iron in {}, banded iron in {}, placer iron in {}; problems {}",
			overworld, deepOcean, banded, placer, problems);
		helper.assertTrue(overworld > 50 && deepOcean == 4 && banded == 10 && placer == 6,
			"biome counts: overworld " + overworld + ", deep ocean " + deepOcean + ", banded " + banded + ", placer " + placer);
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** ORE-02: each vanilla diamond placement keeps only {@code ore.diamond.keep_chance} of its positions. */
	@GameTest
	public void diamondPlacementsAreThinned(GameTestHelper helper) {
		Registry<PlacedFeature> placed = helper.getLevel().getServer().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		for (String name : List.of("ore_diamond", "ore_diamond_buried", "ore_diamond_large", "ore_diamond_medium")) {
			PlacedFeature feature = placed.getValue(Identifier.withDefaultNamespace(name));
			helper.assertTrue(feature != null, name + " missing");
			boolean thinned = feature.placement().stream()
				.anyMatch(modifier -> modifier instanceof TunedPlacements.Chance chance && chance.key().equals("ore.diamond.keep_chance"));
			helper.assertTrue(thinned, name + " has no keep-chance filter: " + feature.placement());
		}
		SimpleBlockTestLog.info("ORE-02 wiring: all 4 vanilla diamond placements carry the ore.diamond.keep_chance filter");
		helper.succeed();
	}
}
