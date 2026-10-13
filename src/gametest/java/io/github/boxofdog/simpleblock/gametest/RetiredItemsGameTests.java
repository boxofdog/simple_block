package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.RemovedRocks;
import io.github.boxofdog.simpleblock.train.TrainContent;

/**
 * M06 work package 4: retired redstone rails, the creative menu without retired items, and no shoving
 * of carts that cannot carry a passenger. Test ids refer to the matrix in {@code docs/MILESTONE_06_REPORT.md}.
 */
public class RetiredItemsGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";

	/** Every item retired through M06 (the audit list in {@code docs/MILESTONE_06_REPORT.md}). */
	static Set<Item> retired() {
		Set<Item> items = new LinkedHashSet<>(List.of(Items.ELYTRA, Items.SHULKER_BOX));
		items.addAll(Items.DYED_SHULKER_BOX.asList());
		items.addAll(List.of(Items.ENDER_CHEST, Items.CHEST_MINECART, Items.FURNACE_MINECART, Items.POWERED_RAIL, Items.DETECTOR_RAIL,
			Items.ACTIVATOR_RAIL));
		items.addAll(RemovedRocks.items());
		return items;
	}

	/**
	 * RET-01: built as the client builds it, no creative tab and not the search shows a retired item,
	 * while their ids are still registered; rails, the loading track, furnaces, minecarts, chests and
	 * the items later milestones may retire (gold and chainmail armor, the blast furnace) are still there.
	 */
	@GameTest
	public void creativeMenuHasNoRetiredItems(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
		List<String> found = new ArrayList<>();
		int shown = 0;
		Set<Item> retired = retired();
		for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
			for (ItemStack stack : tab.getDisplayItems()) {
				shown++;
				if (retired.contains(stack.getItem())) {
					found.add(tab.getDisplayName().getString() + ": " + stack.getItem());
				}
			}
		}
		List<String> searched = new ArrayList<>();
		for (ItemStack stack : CreativeModeTabs.searchTab().getSearchTabDisplayItems()) {
			if (retired.contains(stack.getItem())) {
				searched.add(stack.getItem().toString());
			}
		}
		helper.assertTrue(found.isEmpty(), "retired items in creative tabs: " + found);
		helper.assertTrue(searched.isEmpty(), "retired items in creative search: " + searched);
		for (Item item : retired) {
			helper.assertTrue(BuiltInRegistries.ITEM.getKey(item) != null && BuiltInRegistries.ITEM.containsKey(BuiltInRegistries.ITEM.getKey(item)),
				"still registered: " + item);
		}
		List<Item> kept = List.of(Items.RAIL, TrainContent.LOADING_TRACK_ITEM, Items.FURNACE, Items.MINECART, Items.CHEST, Items.BLAST_FURNACE,
			Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.STONE, Items.REDSTONE, Items.COAL);
		for (Item item : kept) {
			helper.assertTrue(CreativeModeTabs.searchTab().getSearchTabDisplayItems().stream().anyMatch(stack -> stack.is(item)), "still in creative search: " + item);
		}
		SimpleBlockTestLog.info("RET-01: {} tab entries and the search checked: none of the {} retired items ({}) shown, all still registered; kept: {}",
			shown, retired.size(), retired.stream().map(item -> BuiltInRegistries.ITEM.getKey(item).getPath()).toList(),
			kept.stream().map(item -> BuiltInRegistries.ITEM.getKey(item).getPath()).toList());
		helper.succeed();
	}

	/** RET-02: the powered, detector and activator rail recipes are gone; the rail recipe stays. */
	@GameTest
	public void redstoneRailsCannotBeCrafted(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		List<String> log = new ArrayList<>();
		for (String id : List.of("powered_rail", "detector_rail", "activator_rail")) {
			boolean present = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(id))).isPresent();
			helper.assertFalse(present, id + " recipe still loaded");
			boolean makes = server.getRecipeManager().getRecipes().stream()
				.anyMatch(holder -> holder.value().getType() == RecipeType.CRAFTING && holder.id().identifier().getPath().contains(id));
			helper.assertFalse(makes, "a crafting recipe still named after " + id);
			log.add(id + ": no recipe");
		}
		helper.assertTrue(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("rail"))).isPresent(),
			"the rail recipe stays");
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 3));
		Item g = Items.GOLD_INGOT;
		Item s = Items.STICK;
		Item r = Items.REDSTONE;
		ItemStack powered = RecipeGameTests.craft(helper, player, g, Items.AIR, g, g, s, g, g, r, g);
		helper.assertTrue(powered.isEmpty(), "the powered rail pattern makes nothing: " + powered);
		Item i = Items.IRON_INGOT;
		ItemStack rails = RecipeGameTests.craft(helper, player, i, Items.AIR, i, i, s, i, i, Items.AIR, i);
		helper.assertTrue(rails.is(Items.RAIL), "the rail pattern still makes rails: " + rails);
		log.add("powered rail pattern in a crafting table: nothing; rail pattern: " + rails);
		SimpleBlockTestLog.info("RET-02: {}", log);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** RET-03: abandoned mineshaft chests (the only vanilla loot with redstone rails) no longer give them; they still give rails. */
	@GameTest
	public void mineshaftChestsHaveNoRedstoneRails(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.ABANDONED_MINESHAFT);
		int rails = 0;
		int retired = 0;
		int stacks = 0;
		for (int roll = 0; roll < 500; roll++) {
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
			for (ItemStack stack : table.getRandomItems(params)) {
				stacks++;
				if (stack.is(Items.RAIL)) {
					rails++;
				} else if (stack.is(Items.POWERED_RAIL) || stack.is(Items.DETECTOR_RAIL) || stack.is(Items.ACTIVATOR_RAIL)) {
					retired++;
				}
			}
		}
		helper.assertValueEqual(retired, 0, "redstone rail stacks from 500 mineshaft chests");
		helper.assertTrue(rails > 50, "mineshaft chests still give rails: " + rails);
		SimpleBlockTestLog.info("RET-03: 500 abandoned mineshaft chests: {} stacks, {} of rails, {} of redstone rails", stacks, rails, retired);
		helper.succeed();
	}

	/**
	 * PUSH-01: a player walking into a freight cart (empty or loaded) or an engine, or run into by one,
	 * does not move it; a cushioned cart is pushed as in vanilla; a freight cart still rolls downhill.
	 */
	@GameTest(structure = YARD, maxTicks = 200)
	public void playersCannotShoveFreightCarts(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 8));
		TrainGameTests.track(helper, 2, 0, 29);
		TrainGameTests.track(helper, 4, 0, 29);
		TrainGameTests.track(helper, 6, 0, 29);
		Minecart freight = TrainGameTests.cart(helper, 10.5, 2);
		MinecartFurnace engine = TrainGameTests.engine(helper, 10.5, 4, 0);
		Minecart seat = TrainGameTests.cart(helper, 10.5, 6);
		// A slope along z 8: rail falling eastwards from x 20 (3 blocks up) to x 23 (level).
		for (int x = 20; x <= 26; x++) {
			int height = Math.max(0, 23 - x);
			for (int y = 1; y <= height; y++) {
				helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
			}
		}
		for (int x = 17; x <= 26; x++) {
			int height = Math.max(0, 23 - x);
			RailShape shape = x >= 20 && x < 23 ? RailShape.ASCENDING_WEST : RailShape.EAST_WEST;
			CableLoopGameTests.rail(helper, new BlockPos(x, 1 + height, 8), shape);
		}
		Minecart roller = helper.spawn(EntityTypes.MINECART, new Vec3(22.5, 2.0625, 8.5));
		roller.setDeltaMovement(Vec3.ZERO);
		List<AbstractMinecart> shoved = List.of(freight, engine, seat);
		double[] start = new double[3];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				MinecartCargo.cart(seat).simpleBlock$setCushion(DyeColor.BLUE);
				for (int i = 0; i < 3; i++) {
					start[i] = TrainGameTests.x(helper, shoved.get(i));
				}
			}))
			.thenExecuteFor(20, CableGameTests.step(failed, () -> {
				for (AbstractMinecart cart : shoved) {
					// A player standing just west of the cart's middle, walking into it, and being run into by it.
					player.setPos(cart.getX() - 0.4, cart.getY(), cart.getZ());
					cart.push(player);
					player.push(cart);
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				double[] moved = new double[3];
				for (int i = 0; i < 3; i++) {
					moved[i] = TrainGameTests.x(helper, shoved.get(i)) - start[i];
				}
				helper.assertTrue(Math.abs(moved[0]) < 1.0E-6, "freight cart shoved by " + moved[0]);
				helper.assertTrue(Math.abs(moved[1]) < 1.0E-6, "engine shoved by " + moved[1]);
				helper.assertTrue(moved[2] > 0.1, "the cushioned cart is pushed: " + moved[2]);
				MinecartCargo.cart(freight).simpleBlock$setCargo(Items.RAW_IRON, 64);
				log.add(String.format("20 ticks of shoving: freight cart %.3f, engine %.3f, cushioned cart %.3f blocks", moved[0], moved[1], moved[2]));
			}))
			.thenExecuteFor(20, CableGameTests.step(failed, () -> {
				player.setPos(freight.getX() - 0.4, freight.getY(), freight.getZ());
				freight.push(player);
				player.push(freight);
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				double moved = TrainGameTests.x(helper, freight) - start[0];
				helper.assertTrue(Math.abs(moved) < 1.0E-6, "loaded freight cart shoved by " + moved);
				log.add("loaded freight cart: not moved");
			}))
			.thenWaitUntil(() -> helper.assertTrue(TrainGameTests.x(helper, roller) > 24.0, "the freight cart rolls down the slope: x " + TrainGameTests.x(helper, roller)))
			.thenExecute(CableGameTests.step(failed, () -> {
				log.add(String.format("freight cart left on a slope at x 22.5 rolled down it to x %.1f", TrainGameTests.x(helper, roller)));
				SimpleBlockTestLog.info("PUSH-01: {}", log);
				List.of(freight, engine, seat, roller).forEach(AbstractMinecart::discard);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}
}
