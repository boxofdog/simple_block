package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.RemovedRocks;

/**
 * GEO-01: granite, diorite, andesite and tuff cannot be crafted, traded for or looted, and world
 * generation swaps them for stone- or deepslate-family blocks of the same shape. Natural terrain
 * and structures are measured on real normal-terrain worlds by the ore sampler (see
 * docs/MILESTONE_02_REPORT.md); these tests check the rules themselves.
 */
public class GeologyGameTests {
	private static Set<Item> removedItems() {
		return RemovedRocks.blocks().stream().map(Block::asItem).collect(Collectors.toSet());
	}

	/** GEO-04: no recipe of any type makes a removed rock, and the 83 vanilla ones are gone. */
	@GameTest
	public void noRecipeMakesRemovedRocks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		RecipeManager recipes = server.getRecipeManager();
		Set<Item> removed = removedItems();
		helper.assertValueEqual(removed.size(), 35, "removed rock items");

		// A few of the vanilla recipe ids, including stonecutting and the diorite/andesite/granite mixes.
		for (String id : List.of("diorite", "andesite", "granite", "polished_tuff", "tuff_bricks", "chiseled_tuff_bricks",
			"tuff_brick_wall_from_tuff_stonecutting", "polished_granite_stairs_from_granite_stonecutting", "andesite_slab")) {
			helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(id))).isEmpty(),
				"recipe minecraft:" + id + " still exists");
		}
		helper.assertTrue(server.getAdvancements().get(Identifier.withDefaultNamespace("recipes/building_blocks/polished_andesite")) == null,
			"recipe advancement for polished andesite still exists");

		ContextMap context = SlotDisplayContext.fromLevel(level);
		int scanned = 0;
		List<String> problems = new ArrayList<>();
		for (RecipeHolder<?> holder : recipes.getRecipes()) {
			for (RecipeDisplay display : holder.value().display()) {
				for (ItemStack result : display.result().resolveForStacks(context)) {
					if (removed.contains(result.getItem())) {
						problems.add(holder.id().identifier() + " -> " + result.getItem());
					}
				}
			}
			scanned++;
		}
		SimpleBlockTestLog.info("GEO-04 recipes: {} recipes scanned, {} make a removed rock {}", scanned, problems.size(), problems);
		helper.assertTrue(scanned > 1000, "recipe scan should cover the vanilla recipes (" + scanned + ")");
		helper.assertTrue(problems.isEmpty(), "recipes still make removed rocks: " + problems);
		helper.succeed();
	}

	/**
	 * GEO-04: chest loot that lists a removed rock (trial chamber corridor and supply chests list
	 * tuff and its bricks) never actually gives one. The table data still lists them, which is
	 * what vanilla would hand out.
	 */
	@GameTest
	public void chestLootNeverGivesRemovedRocks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HolderLookup.Provider lookup = level.getServer().reloadableRegistries().lookup();
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, lookup);
		Set<Item> removed = removedItems();
		Set<String> removedIds = removed.stream().map(item -> "\"" + BuiltInRegistries.ITEM.getKey(item) + "\"").collect(Collectors.toSet());

		int tablesListingRemoved = 0;
		int rolls = 0;
		int itemsDropped = 0;
		List<String> problems = new ArrayList<>();
		for (var table : lookup.lookupOrThrow(Registries.LOOT_TABLE).listElements().toList()) {
			String id = table.key().identifier().getPath();
			if (id.startsWith("blocks/")) {
				continue;
			}
			String json = LootTable.DIRECT_CODEC.encodeStart(ops, table.value()).getOrThrow().toString();
			if (removedIds.stream().noneMatch(json::contains)) {
				continue;
			}
			tablesListingRemoved++;
			helper.assertTrue(id.startsWith("chests/"), "unexpected non-chest table lists a removed rock: " + id);
			for (int i = 0; i < 400; i++) {
				LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
				for (ItemStack stack : table.value().getRandomItems(params)) {
					itemsDropped++;
					if (removed.contains(stack.getItem())) {
						problems.add(id + " -> " + stack);
					}
				}
				rolls++;
			}
		}
		SimpleBlockTestLog.info("GEO-04 loot: {} chest tables list a removed rock; {} rolls gave {} stacks, {} of them removed rocks",
			tablesListingRemoved, rolls, itemsDropped, problems.size());
		helper.assertTrue(tablesListingRemoved >= 2, "expected the trial chamber tables to list tuff (" + tablesListingRemoved + ")");
		helper.assertTrue(itemsDropped > 1000, "loot rolls should have produced items (" + itemsDropped + ")");
		helper.assertTrue(problems.isEmpty(), "loot gave removed rocks: " + problems.stream().limit(5).toList());
		helper.succeed();
	}

	/** GEO-04: masons no longer buy or sell granite, diorite or andesite; no trade gives a removed rock. */
	@GameTest
	public void noVillagerTradeInvolvesRemovedRocks(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		var trades = server.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
		Set<String> removedIds = removedItems().stream().map(item -> "\"" + BuiltInRegistries.ITEM.getKey(item) + "\"").collect(Collectors.toSet());

		TagKey<VillagerTrade> masonLevel3 = TagKey.create(Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace("mason/level_3"));
		List<String> masonTrades = trades.getOrThrow(masonLevel3).stream()
			.map(holder -> holder.unwrapKey().map(key -> key.identifier().toString()).orElse("?")).sorted().toList();
		helper.assertValueEqual(masonTrades, List.of("minecraft:mason/3/emerald_dripstone_block"), "mason level 3 trades");

		// Every trade any profession can actually be offered (through its trade sets).
		int offered = 0;
		List<String> problems = new ArrayList<>();
		for (var tradeSet : server.registryAccess().lookupOrThrow(Registries.TRADE_SET).listElements().toList()) {
			TradeSet set = tradeSet.value();
			for (Holder<VillagerTrade> trade : set.trades()) {
				offered++;
				String json = VillagerTrade.CODEC.encodeStart(ops, trade.value()).getOrThrow().toString();
				if (removedIds.stream().anyMatch(json::contains)) {
					problems.add(tradeSet.key().identifier() + ": " + trade.unwrapKey().map(Object::toString).orElse(json));
				}
			}
		}
		SimpleBlockTestLog.info("GEO-04 trades: mason level 3 = {}; {} offerable trades scanned, {} involve removed rocks",
			masonTrades, offered, problems.size());
		helper.assertTrue(offered > 100, "trade scan should cover the vanilla trade sets (" + offered + ")");
		helper.assertTrue(problems.isEmpty(), "trades involving removed rocks: " + problems);
		helper.succeed();
	}

	/**
	 * GEO-01/03: every state of every removed block maps to a kept block of the same shape. Raw rock
	 * becomes stone at stone depths and deepslate at deepslate depths, following the rock it
	 * replaces when there is rock; crafted variants keep one family per structure (tuff's deepslate,
	 * the others' stone). Facing, half, slab type, stair shape, wall sides and waterlogging carry over.
	 */
	@GameTest
	public void worldgenSubstitutesKeepShapeAndFollowDepth(GameTestHelper helper) {
		int states = 0;
		List<String> problems = new ArrayList<>();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (Block removed : RemovedRocks.blocks()) {
			for (BlockState state : removed.getStateDefinition().getPossibleStates()) {
				states++;
				BlockState high = RemovedRocks.substitute(state, air, new BlockPos(0, 64, 0));
				BlockState deep = RemovedRocks.substitute(state, air, new BlockPos(0, -30, 0));
				BlockState inDeepslate = RemovedRocks.substitute(state, Blocks.DEEPSLATE.defaultBlockState(), new BlockPos(0, 40, 0));
				BlockState inStone = RemovedRocks.substitute(state, Blocks.STONE.defaultBlockState(), new BlockPos(0, -20, 0));
				boolean raw = removed == Blocks.GRANITE || removed == Blocks.DIORITE || removed == Blocks.ANDESITE || removed == Blocks.TUFF;
				boolean tuffFamily = BuiltInRegistries.BLOCK.getKey(removed).getPath().contains("tuff");
				check(problems, state, high, raw ? false : tuffFamily);
				check(problems, state, deep, raw ? true : tuffFamily);
				check(problems, state, inDeepslate, raw ? true : tuffFamily);
				check(problems, state, inStone, raw ? false : tuffFamily);
			}
		}
		// The y 0-8 transition: a per-position blend, mostly deepslate at the bottom, mostly stone at the top.
		int deepslateLow = 0;
		int deepslateHigh = 0;
		for (int x = 0; x < 64; x++) {
			for (int z = 0; z < 64; z++) {
				deepslateLow += RemovedRocks.substitute(Blocks.TUFF.defaultBlockState(), air, new BlockPos(x, 1, z)).is(Blocks.DEEPSLATE) ? 1 : 0;
				deepslateHigh += RemovedRocks.substitute(Blocks.TUFF.defaultBlockState(), air, new BlockPos(x, 7, z)).is(Blocks.DEEPSLATE) ? 1 : 0;
			}
		}
		SimpleBlockTestLog.info("GEO-01 substitution: {} block states checked, problems {}; transition y1 {}% deepslate, y7 {}% deepslate",
			states, problems.size(), deepslateLow * 100 / 4096, deepslateHigh * 100 / 4096);
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems.stream().limit(5).toList()));
		helper.assertTrue(deepslateLow > 4096 * 0.75 && deepslateLow < 4096 && deepslateHigh > 0 && deepslateHigh < 4096 * 0.25,
			"transition blend: y1 " + deepslateLow + "/4096, y7 " + deepslateHigh + "/4096 deepslate");
		helper.succeed();
	}

	private static void check(List<String> problems, BlockState from, BlockState to, boolean expectDeepslate) {
		String name = BuiltInRegistries.BLOCK.getKey(to.getBlock()).getPath();
		if (RemovedRocks.isRemoved(to)) {
			problems.add(from + " -> removed " + to);
		}
		if (name.contains("deepslate") != expectDeepslate) {
			problems.add(from + " -> " + to + ", expected the " + (expectDeepslate ? "deepslate" : "stone") + " family");
		}
		for (Property<?> property : from.getProperties()) {
			Property<?> target = to.getBlock().getStateDefinition().getProperty(property.getName());
			if (target != null && target.getPossibleValues().contains(from.getValue(property)) && !to.getValue(target).equals(from.getValue(property))) {
				problems.add(from + " -> " + to + " lost " + property.getName());
			}
		}
		if (from.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
			&& !to.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
			problems.add(from + " -> " + to + " cannot be waterlogged");
		}
		for (Class<?> shape : List.of(SlabBlock.class, StairBlock.class, WallBlock.class)) {
			if (shape.isInstance(from.getBlock()) != shape.isInstance(to.getBlock())) {
				problems.add(from + " -> " + to + " changes shape");
			}
		}
	}
}
