package io.github.boxofdog.simpleblock.feature;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * GEO-01: granite, diorite, andesite and tuff no longer occur in new worlds.
 *
 * <ul>
 *   <li>The seven rock-blob placed features are removed from Overworld biomes (Fabric biome API).</li>
 *   <li>Large ore veins drop their tuff/granite filler ({@code mixin/geology/OreVeinRuleMixin}), so
 *       the vein's surroundings are the ordinary stone or deepslate of that depth.</li>
 *   <li>Anything else that places one of these blocks during world generation (structures such as
 *       trial chambers, villages and ocean ruins, sulfur springs, ...) gets a stone- or
 *       deepslate-family substitute with the same shape ({@code mixin/geology/WorldGenRegionMixin},
 *       rules in {@link #substitute}).</li>
 *   <li>Recipes that make them and their recipe advancements are disabled by data overrides; the
 *       mason's level-3 trade tag no longer lists them; chest/archaeology/fishing loot drops them.</li>
 * </ul>
 *
 * The blocks and items stay registered (structures, commands and the creative menu still know them).
 */
public final class RemovedRocks {
	/** Placed features that scatter blobs of the removed rocks through Overworld stone. */
	public static final List<ResourceKey<PlacedFeature>> BLOB_FEATURES = List.of(
		placed("ore_granite_upper"), placed("ore_granite_lower"),
		placed("ore_diorite_upper"), placed("ore_diorite_lower"),
		placed("ore_andesite_upper"), placed("ore_andesite_lower"),
		placed("ore_tuff"));

	/** Removed block -> {stone-depth substitute, deepslate-depth substitute}. */
	private static final Map<Block, Block[]> SUBSTITUTES = new IdentityHashMap<>();
	private static final Set<Item> ITEMS;
	private static final Set<Block> RAW = Set.of(Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.TUFF);
	private static final Set<Block> TUFF_FAMILY = Set.of(Blocks.TUFF, Blocks.TUFF_SLAB, Blocks.TUFF_STAIRS, Blocks.TUFF_WALL,
		Blocks.POLISHED_TUFF, Blocks.POLISHED_TUFF_SLAB, Blocks.POLISHED_TUFF_STAIRS, Blocks.POLISHED_TUFF_WALL, Blocks.TUFF_BRICKS,
		Blocks.TUFF_BRICK_SLAB, Blocks.TUFF_BRICK_STAIRS, Blocks.TUFF_BRICK_WALL, Blocks.CHISELED_TUFF, Blocks.CHISELED_TUFF_BRICKS);

	static {
		for (Block[] family : List.of(
			new Block[] {Blocks.GRANITE, Blocks.POLISHED_GRANITE, Blocks.GRANITE_SLAB, Blocks.GRANITE_STAIRS, Blocks.GRANITE_WALL,
				Blocks.POLISHED_GRANITE_SLAB, Blocks.POLISHED_GRANITE_STAIRS},
			new Block[] {Blocks.DIORITE, Blocks.POLISHED_DIORITE, Blocks.DIORITE_SLAB, Blocks.DIORITE_STAIRS, Blocks.DIORITE_WALL,
				Blocks.POLISHED_DIORITE_SLAB, Blocks.POLISHED_DIORITE_STAIRS},
			new Block[] {Blocks.ANDESITE, Blocks.POLISHED_ANDESITE, Blocks.ANDESITE_SLAB, Blocks.ANDESITE_STAIRS, Blocks.ANDESITE_WALL,
				Blocks.POLISHED_ANDESITE_SLAB, Blocks.POLISHED_ANDESITE_STAIRS},
			new Block[] {Blocks.TUFF, Blocks.POLISHED_TUFF, Blocks.TUFF_SLAB, Blocks.TUFF_STAIRS, Blocks.TUFF_WALL,
				Blocks.POLISHED_TUFF_SLAB, Blocks.POLISHED_TUFF_STAIRS})) {
			substitute(family[0], Blocks.STONE, Blocks.DEEPSLATE);
			substitute(family[1], Blocks.SMOOTH_STONE, Blocks.POLISHED_DEEPSLATE);
			substitute(family[2], Blocks.STONE_SLAB, Blocks.COBBLED_DEEPSLATE_SLAB);
			substitute(family[3], Blocks.STONE_STAIRS, Blocks.COBBLED_DEEPSLATE_STAIRS);
			substitute(family[4], Blocks.COBBLESTONE_WALL, Blocks.COBBLED_DEEPSLATE_WALL);
			substitute(family[5], Blocks.SMOOTH_STONE_SLAB, Blocks.POLISHED_DEEPSLATE_SLAB);
			substitute(family[6], Blocks.STONE_STAIRS, Blocks.POLISHED_DEEPSLATE_STAIRS);
		}
		substitute(Blocks.POLISHED_TUFF_WALL, Blocks.STONE_BRICK_WALL, Blocks.POLISHED_DEEPSLATE_WALL);
		substitute(Blocks.TUFF_BRICKS, Blocks.STONE_BRICKS, Blocks.DEEPSLATE_BRICKS);
		substitute(Blocks.TUFF_BRICK_SLAB, Blocks.STONE_BRICK_SLAB, Blocks.DEEPSLATE_BRICK_SLAB);
		substitute(Blocks.TUFF_BRICK_STAIRS, Blocks.STONE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_STAIRS);
		substitute(Blocks.TUFF_BRICK_WALL, Blocks.STONE_BRICK_WALL, Blocks.DEEPSLATE_BRICK_WALL);
		substitute(Blocks.CHISELED_TUFF, Blocks.CHISELED_STONE_BRICKS, Blocks.CHISELED_DEEPSLATE);
		substitute(Blocks.CHISELED_TUFF_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.CHISELED_DEEPSLATE);
		ITEMS = SUBSTITUTES.keySet().stream().map(Block::asItem).collect(Collectors.toUnmodifiableSet());
	}

	private RemovedRocks() {
	}

	private static void substitute(Block removed, Block stone, Block deepslate) {
		SUBSTITUTES.put(removed, new Block[] {stone, deepslate});
	}

	private static ResourceKey<PlacedFeature> placed(String path) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace(path));
	}

	public static Set<Block> blocks() {
		return SUBSTITUTES.keySet();
	}

	public static Set<Item> items() {
		return ITEMS;
	}

	public static boolean isRemoved(BlockState state) {
		return state != null && SUBSTITUTES.containsKey(state.getBlock());
	}

	/**
	 * The block world generation places instead of a removed rock.
	 *
	 * <p>Raw rock (granite, diorite, andesite, tuff) becomes the rock of its depth. Inside existing
	 * rock it follows that rock (stone or deepslate), so it matches the terrain's own
	 * stone-to-deepslate transition exactly. Elsewhere it follows the vanilla transition: deepslate
	 * below y 0, stone from y 8, and a per-position blend between, the same gradient the terrain
	 * itself uses.
	 *
	 * <p>Crafted variants (polished, bricks, chiseled, slabs, stairs, walls) only occur in
	 * structures, so they keep one palette per structure instead of changing family halfway up a
	 * building that crosses y 0-8: tuff's become deepslate-family blocks (trial chambers), granite's,
	 * diorite's and andesite's become stone-family blocks (villages, ocean ruins, igloos, mansions).
	 */
	public static BlockState substitute(BlockState removed, BlockState existing, BlockPos pos) {
		Block block = removed.getBlock();
		Block[] choices = SUBSTITUTES.get(block);
		boolean deepslate;
		if (RAW.contains(block)) {
			deepslate = isDeepslateAt(existing, pos);
		} else {
			deepslate = TUFF_FAMILY.contains(block);
		}
		return copyProperties(removed, choices[deepslate ? 1 : 0]);
	}

	static boolean isDeepslateAt(BlockState existing, BlockPos pos) {
		if (existing.is(Blocks.DEEPSLATE)) {
			return true;
		}
		if (existing.is(Blocks.STONE)) {
			return false;
		}
		int y = pos.getY();
		if (y < 0) {
			return true;
		}
		if (y >= 8) {
			return false;
		}
		double deepslateChance = (8 - y) / 8.0;
		long hash = Mth.getSeed(pos.getX(), y, pos.getZ());
		return ((hash >>> 16) & 0xFFFF) / 65536.0 < deepslateChance;
	}

	private static BlockState copyProperties(BlockState from, Block to) {
		BlockState result = to.defaultBlockState();
		for (Property<?> property : from.getProperties()) {
			Property<?> target = to.getStateDefinition().getProperty(property.getName());
			if (target != null) {
				result = copy(from, result, property, target);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property, Property<?> target) {
		T value = from.getValue(property);
		return target.getPossibleValues().contains(value) ? to.setValue((Property<T>) target, value) : to;
	}

	public static void register() {
		BiomeModifications.create(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "remove_rock_blobs"))
			.add(ModificationPhase.REMOVALS, BiomeSelectors.tag(BiomeTags.IS_OVERWORLD), context -> {
				for (ResourceKey<PlacedFeature> feature : BLOB_FEATURES) {
					context.getGenerationSettings().removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES, feature);
				}
			});

		// Chest, archaeology, fishing and other non-block loot. Block loot tables are left alone: a
		// removed rock only exists in survival if it was placed from the creative menu or a command.
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			boolean blockLoot = table.unwrapKey().map(key -> key.identifier().getPath().startsWith("blocks/")).orElse(false);
			if (!blockLoot) {
				drops.removeIf(stack -> ITEMS.contains(stack.getItem()));
			}
		});
	}
}
