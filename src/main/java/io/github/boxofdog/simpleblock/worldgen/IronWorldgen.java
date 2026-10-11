package io.github.boxofdog.simpleblock.worldgen;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * ORE-01 and ORE-02 worldgen wiring.
 *
 * <p>Vanilla's three scattered iron features ({@code ore_iron_upper}, {@code ore_iron_middle},
 * {@code ore_iron_small}) are replaced by:
 * <ul>
 *   <li>{@code simple_block:ore_iron_stone} and {@code ore_iron_deepslate}: even background iron in
 *       every Overworld biome, sparser in stone than in deepslate;</li>
 *   <li>{@code ore_iron_deep_ocean} and {@code ore_iron_deep_ocean_body}: extra background iron and
 *       large clustered bodies in deepslate under {@code #simple_block:deep_ocean_iron};</li>
 *   <li>{@code iron_banded}: banded iron formations under {@code #simple_block:banded_iron_deposits};</li>
 *   <li>{@code iron_placer}: alluvial deposits in {@code #simple_block:placer_iron_deposits};</li>
 *   <li>{@code iron_geothermal}: pockets beside natural lava, every Overworld biome.</li>
 * </ul>
 * The large iron ore veins of the deep (formerly in tuff) are kept. Diamonds use the vanilla
 * placed features, overridden in {@code data/minecraft/worldgen/placed_feature} to keep
 * {@code ore.diamond.keep_chance} of the placements. All amounts come from the tuning table.
 */
public final class IronWorldgen {
	public static final TagKey<Biome> DEEP_OCEAN_IRON = biomeTag("deep_ocean_iron");
	public static final TagKey<Biome> BANDED_IRON_DEPOSITS = biomeTag("banded_iron_deposits");
	public static final TagKey<Biome> PLACER_IRON_DEPOSITS = biomeTag("placer_iron_deposits");

	public static final List<ResourceKey<PlacedFeature>> VANILLA_IRON = List.of(
		ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("ore_iron_upper")),
		ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("ore_iron_middle")),
		ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace("ore_iron_small")));

	public static final ResourceKey<PlacedFeature> ORE_IRON_STONE = placed("ore_iron_stone");
	public static final ResourceKey<PlacedFeature> ORE_IRON_DEEPSLATE = placed("ore_iron_deepslate");
	public static final ResourceKey<PlacedFeature> ORE_IRON_DEEP_OCEAN = placed("ore_iron_deep_ocean");
	public static final ResourceKey<PlacedFeature> ORE_IRON_DEEP_OCEAN_BODY = placed("ore_iron_deep_ocean_body");
	public static final ResourceKey<PlacedFeature> IRON_BANDED = placed("iron_banded");
	public static final ResourceKey<PlacedFeature> IRON_PLACER = placed("iron_placer");
	public static final ResourceKey<PlacedFeature> IRON_GEOTHERMAL = placed("iron_geothermal");

	private IronWorldgen() {
	}

	public static void register() {
		placement("tuned_count", TunedPlacements.Count.CODEC);
		placement("tuned_chance", TunedPlacements.Chance.CODEC);
		placement("tuned_height", TunedPlacements.Height.CODEC);
		placement("tuned_volume", TunedPlacements.Volume.CODEC);
		placement("tuned_cluster", TunedPlacements.Cluster.CODEC);
		placement("surface_biome", TunedPlacements.SurfaceBiome.CODEC);

		feature("tuned_ore", TunedOreFeature.CODEC);
		feature("banded_iron", BandedIronFeature.CODEC);
		feature("placer_iron", PlacerIronFeature.CODEC);
		feature("geothermal_iron", GeothermalIronFeature.CODEC);

		BiomeModifications.create(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "iron"))
			.add(ModificationPhase.REMOVALS, BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), context -> {
				for (ResourceKey<PlacedFeature> feature : VANILLA_IRON) {
					context.getGenerationSettings().removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature);
				}
			})
			// One modification, fixed order: every biome sees these features in the same relative order.
			.add(ModificationPhase.ADDITIONS, BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), (selection, context) -> {
				context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_IRON_STONE);
				context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_IRON_DEEPSLATE);
				if (selection.hasTag(DEEP_OCEAN_IRON)) {
					context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_IRON_DEEP_OCEAN);
					context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_IRON_DEEP_OCEAN_BODY);
				}
				if (selection.hasTag(BANDED_IRON_DEPOSITS)) {
					context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, IRON_BANDED);
				}
				if (selection.hasTag(PLACER_IRON_DEPOSITS)) {
					context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, IRON_PLACER);
				}
				context.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, IRON_GEOTHERMAL);
			});
	}

	private static void placement(String name, MapCodec<? extends PlacementModifier> codec) {
		Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, name), codec);
	}

	private static void feature(String name, MapCodec<? extends Feature> codec) {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, name), codec);
	}

	private static ResourceKey<PlacedFeature> placed(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, name));
	}

	private static TagKey<Biome> biomeTag(String name) {
		return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, name));
	}
}
