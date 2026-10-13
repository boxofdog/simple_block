package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.train.CobblestoneEngine;
import io.github.boxofdog.simpleblock.train.Couplings;

/**
 * M05 work package C: the Cobblestone Engine (a furnace used on an empty minecart) and iron-chain
 * couplings. Every use goes through {@code Player.interactOn}, the method the server runs for a
 * player's use-entity packet. Test ids refer to the matrix in {@code docs/MILESTONE_05_REPORT.md}.
 *
 * <p>Most tests use a straight east-west track along z 4 of {@code cable_yard}, rails at y 1.
 */
public class TrainGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";
	private static final double RAIL_Y = 1.0625;

	static void track(GameTestHelper helper, int z, int fromX, int toX) {
		for (int x = fromX; x <= toX; x++) {
			CableLoopGameTests.rail(helper, new BlockPos(x, 1, z), RailShape.EAST_WEST);
		}
	}

	static Minecart cart(GameTestHelper helper, double x, int z) {
		Minecart cart = helper.spawn(EntityTypes.MINECART, new Vec3(x, RAIL_Y, z + 0.5));
		cart.setDeltaMovement(Vec3.ZERO);
		return cart;
	}

	static MinecartFurnace engine(GameTestHelper helper, double x, int z, int coal) {
		MinecartFurnace engine = helper.spawn(EntityTypes.FURNACE_MINECART, new Vec3(x, RAIL_Y, z + 0.5));
		engine.setDeltaMovement(Vec3.ZERO);
		CobblestoneEngine.engine(engine).simpleBlock$setFuel(coal * CobblestoneEngine.FUEL_PER_ITEM);
		return engine;
	}

	static void use(ServerPlayer player, Entity target, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		player.interactOn(target, InteractionHand.MAIN_HAND, Vec3.ZERO);
	}

	/** Couples two carts with a chain, as a player would: the chain used on one, then the other. */
	static void couple(ServerPlayer player, AbstractMinecart a, AbstractMinecart b) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_CHAIN, 4));
		player.interactOn(a, InteractionHand.MAIN_HAND, Vec3.ZERO);
		player.interactOn(b, InteractionHand.MAIN_HAND, Vec3.ZERO);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	static boolean coupled(AbstractMinecart a, AbstractMinecart b) {
		Couplings.Coupled state = Couplings.coupled(a);
		return b.getUUID().equals(state.simpleBlock$coupling(0)) || b.getUUID().equals(state.simpleBlock$coupling(1));
	}

	static double x(GameTestHelper helper, Entity entity) {
		return entity.getX() - helper.absoluteVec(Vec3.ZERO).x;
	}

	static List<ItemEntity> itemsAround(GameTestHelper helper, Vec3 at) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(3.0));
	}

	static int total(List<ItemEntity> items, Item item) {
		return MinecartCargoGameTests.total(items, item);
	}

	static List<MinecartFurnace> engines(GameTestHelper helper) {
		// This test's structure only: tests in the same batch stand side by side, and others have engines too.
		return helper.getLevel().getEntitiesOfClass(MinecartFurnace.class, helper.getBounds());
	}

	// ---- Making and breaking an engine ----

	/**
	 * ENG-01: a furnace used on an empty minecart turns it into an engine where it stands, keeping its
	 * coupling; a loaded or cushioned cart is refused. Broken, the engine drops one minecart, one
	 * furnace and (for its coupling) one chain, and never a furnace minecart.
	 */
	@GameTest(structure = YARD)
	public void furnaceTurnsAnEmptyMinecartIntoAnEngine(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		track(helper, 4, 2, 20);
		Minecart cart = cart(helper, 8.5, 4);
		Minecart partner = cart(helper, 10.1, 4);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				MinecartCargo.cart(cart).simpleBlock$setCargo(Items.COBBLESTONE, 10);
				use(player, cart, new ItemStack(Items.FURNACE));
				helper.assertFalse(cart.isRemoved(), "a loaded cart should not become an engine");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "furnace kept after the refusal");
				MinecartCargo.cart(cart).simpleBlock$setCargo(Items.AIR, 0);
				MinecartCargo.cart(cart).simpleBlock$setCushion(DyeColor.RED);
				use(player, cart, new ItemStack(Items.FURNACE));
				helper.assertFalse(cart.isRemoved(), "a cushioned cart should not become an engine");
				MinecartCargo.cart(cart).simpleBlock$setCushion(null);
				log.add("loaded cart and cushioned cart: refused, furnace kept");

				couple(player, cart, partner);
				Vec3 at = cart.position();
				use(player, cart, new ItemStack(Items.FURNACE));
				helper.assertTrue(cart.isRemoved(), "the empty minecart should be replaced");
				helper.assertTrue(player.getMainHandItem().isEmpty(), "the furnace should be used up");
				List<MinecartFurnace> engines = engines(helper);
				helper.assertValueEqual(engines.size(), 1, "engines");
				MinecartFurnace engine = engines.getFirst();
				helper.assertTrue(engine.position().distanceTo(at) < 0.01, "the engine should stand where the cart was");
				helper.assertTrue(coupled(engine, partner) && coupled(partner, engine), "the coupling should pass to the engine");
				log.add("empty cart: became an engine in place, coupling kept");
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				MinecartFurnace engine = engines(helper).getFirst();
				Vec3 at = engine.position();
				helper.assertTrue(itemsAround(helper, at).isEmpty(), "nothing should have dropped yet (the old cart's coupling moved, no chain)");
				engine.hurtServer(level, level.damageSources().playerAttack(player), 100.0F);
				helper.assertTrue(engine.isRemoved(), "the engine should be broken");
				List<ItemEntity> drops = itemsAround(helper, at);
				List<String> dropped = drops.stream().map(item -> item.getItem().toString()).toList();
				helper.assertValueEqual(total(drops, Items.MINECART), 1, "minecarts dropped " + dropped);
				helper.assertValueEqual(total(drops, Items.FURNACE), 1, "furnaces dropped " + dropped);
				helper.assertValueEqual(total(drops, Items.IRON_CHAIN), 1, "chains dropped " + dropped);
				helper.assertValueEqual(total(drops, Items.FURNACE_MINECART), 0, "furnace minecarts dropped " + dropped);
				helper.assertFalse(Couplings.hasCouplings(partner), "the partner should be uncoupled");
				log.add("broken: " + dropped);
				drops.forEach(Entity::discard);
				partner.discard();

				// Caught in an explosion (which breaks no blocks here), it still drops the two parts once.
				MinecartFurnace blasted = engine(helper, 16.5, 4, 1);
				Vec3 blast = blasted.position();
				level.explode(null, blast.x, blast.y, blast.z, 2.0F, Level.ExplosionInteraction.NONE);
				helper.assertTrue(blasted.isRemoved(), "the engine should be destroyed by the explosion");
				List<ItemEntity> blastDrops = itemsAround(helper, blast);
				List<String> blastDropped = blastDrops.stream().map(item -> item.getItem().toString()).toList();
				helper.assertTrue(total(blastDrops, Items.MINECART) <= 1 && total(blastDrops, Items.FURNACE) <= 1
					&& total(blastDrops, Items.FURNACE_MINECART) == 0, "explosion drops " + blastDropped);
				log.add("exploded: " + blastDropped);
				SimpleBlockTestLog.info("ENG-01: {}", log);
				blastDrops.forEach(Entity::discard);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	private static ItemStack craft(GameTestHelper helper, Item... grid) {
		List<ItemStack> items = new ArrayList<>();
		for (Item item : grid) {
			items.add(item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
		}
		CraftingInput input = CraftingInput.of(3, 3, items);
		ServerLevel level = helper.getLevel();
		return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
			.map(holder -> holder.value().assemble(input))
			.orElse(ItemStack.EMPTY);
	}

	/** ENG-02: the furnace minecart recipe and its advancement are gone; a furnace over a minecart crafts nothing. */
	@GameTest
	public void furnaceMinecartIsNotCraftable(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		RecipeManager recipes = server.getRecipeManager();
		helper.assertTrue(recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("furnace_minecart"))).isEmpty(),
			"recipe furnace_minecart still exists");
		helper.assertTrue(server.getAdvancements().get(Identifier.withDefaultNamespace("recipes/transportation/furnace_minecart")) == null,
			"advancement recipes/transportation/furnace_minecart still exists");
		Item air = Items.AIR;
		ItemStack made = craft(helper, Items.FURNACE, air, air, Items.MINECART, air, air, air, air, air);
		helper.assertTrue(made.isEmpty(), "a furnace over a minecart crafts " + made);
		SimpleBlockTestLog.info("ENG-02: furnace_minecart recipe and advancement absent; furnace over minecart crafts nothing");
		helper.succeed();
	}

	// ---- Driving ----

	/**
	 * ENG-03: coal and charcoal refuel it (nothing else does); with an empty hand it starts away from
	 * the player, or stops if running; it needs fuel to start; fuel burns only while it drives.
	 */
	@GameTest(structure = YARD, maxTicks = 400)
	public void engineRunsOnCoalFromTheGround(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		track(helper, 4, 1, 28);
		MinecartFurnace engine = engine(helper, 10.5, 4, 0);
		CobblestoneEngine.Engine state = CobblestoneEngine.engine(engine);
		double[] marks = new double[4];
		int[] fuel = new int[2];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				use(player, engine, ItemStack.EMPTY);
				helper.assertFalse(state.simpleBlock$running(), "an engine without fuel should not start");
				use(player, engine, new ItemStack(Items.OAK_LOG));
				helper.assertValueEqual(state.simpleBlock$fuel(), 0, "fuel after an oak log (not engine fuel)");
				use(player, engine, new ItemStack(Items.COAL));
				helper.assertValueEqual(state.simpleBlock$fuel(), CobblestoneEngine.FUEL_PER_ITEM, "fuel after a coal");
				helper.assertTrue(player.getMainHandItem().isEmpty(), "the coal should be used up");
				use(player, engine, new ItemStack(Items.CHARCOAL));
				helper.assertValueEqual(state.simpleBlock$fuel(), 2 * CobblestoneEngine.FUEL_PER_ITEM, "fuel after a charcoal");
				marks[0] = x(helper, engine);
				use(player, engine, ItemStack.EMPTY);
				helper.assertTrue(state.simpleBlock$running(), "an empty-hand use should start it");
				fuel[0] = state.simpleBlock$fuel();
				log.add("no fuel: won't start; oak log: refused; coal and charcoal: " + fuel[0] + " ticks of fuel");
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[1] = x(helper, engine);
				helper.assertTrue(marks[1] - marks[0] > 3.0, "started from the west, it should run east: x " + marks[0] + " -> " + marks[1]);
				helper.assertTrue(fuel[0] - state.simpleBlock$fuel() >= 35, "fuel should burn while it drives: " + fuel[0] + " -> " + state.simpleBlock$fuel());
				use(player, engine, ItemStack.EMPTY);
				helper.assertFalse(state.simpleBlock$running(), "a second use should stop it");
			}))
			.thenIdle(20)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(state.simpleBlock$speed() == 0.0, "stopped: speed " + state.simpleBlock$speed());
				marks[2] = x(helper, engine);
				fuel[1] = state.simpleBlock$fuel();
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(x(helper, engine) - marks[2]) < 0.01, "a stopped engine should stay put, moved " + (x(helper, engine) - marks[2]));
				helper.assertValueEqual(state.simpleBlock$fuel(), fuel[1], "fuel while stopped");
				log.add(String.format("ran east x %.2f -> %.2f in 40 ticks; stopped at x %.2f; no fuel burnt while stopped", marks[0], marks[1], marks[2]));
				CableGameTests.teleport(helper, player, new BlockPos((int) Math.floor(marks[2]) + 3, 1, 6));
				use(player, engine, ItemStack.EMPTY);
				helper.assertTrue(state.simpleBlock$running(), "started again");
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[3] = x(helper, engine);
				helper.assertTrue(marks[2] - marks[3] > 3.0, "started from the east, it should run west: x " + marks[2] + " -> " + marks[3]);
				log.add(String.format("started from the east: ran west x %.2f -> %.2f", marks[2], marks[3]));
				SimpleBlockTestLog.info("ENG-03: {}", log);
				engine.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * ENG-04 (owner's choice): a player in a cushioned cart coupled to the engine drives it: forward
	 * powers it towards where they look along the track, so it brakes and reverses when they turn
	 * round; back brakes it to a stop, where it holds.
	 */
	@GameTest(structure = YARD, maxTicks = 400)
	public void riderDrivesTheTrainFromACushionedCart(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		track(helper, 4, 1, 28);
		MinecartFurnace engine = engine(helper, 10.5, 4, 2);
		Minecart seat = cart(helper, 12.1, 4);
		CobblestoneEngine.Engine state = CobblestoneEngine.engine(engine);
		double[] marks = new double[4];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				couple(player, engine, seat);
				MinecartCargo.cart(seat).simpleBlock$setCushion(DyeColor.BLUE);
				helper.assertTrue(player.startRiding(seat), "the player should get into the cushioned cart");
				marks[0] = x(helper, engine);
				player.setYRot(-90.0F);
				player.setXRot(0.0F);
				player.setLastClientInput(new Input(true, false, false, false, false, false, false));
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[1] = x(helper, engine);
				helper.assertTrue(marks[1] - marks[0] > 2.0, "holding forward looking east should drive east: x " + marks[0] + " -> " + marks[1]);
				helper.assertFalse(state.simpleBlock$running(), "the ground setting should stay off");
				player.setYRot(90.0F);
			}))
			.thenIdle(60)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[2] = x(helper, engine);
				helper.assertTrue(marks[2] < marks[1] - 1.0, "turned to look west, it should brake and run west: x " + marks[1] + " -> " + marks[2]);
				player.setLastClientInput(new Input(false, true, false, false, false, false, false));
			}))
			.thenIdle(20)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(state.simpleBlock$speed() == 0.0, "holding back should brake it to a stop: speed " + state.simpleBlock$speed());
				marks[3] = x(helper, engine);
				player.setLastClientInput(Input.EMPTY);
			}))
			.thenIdle(30)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(x(helper, engine) - marks[3]) < 0.01, "let go, it should hold where it stopped");
				helper.assertTrue(coupled(engine, seat), "still coupled");
				log.add(String.format("forward looking east: x %.2f -> %.2f; looking west: -> %.2f; back: stopped at %.2f and held", marks[0], marks[1],
					marks[2], marks[3]));
				SimpleBlockTestLog.info("ENG-04: {}", log);
				player.stopRiding();
				engine.discard();
				seat.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- Couplings ----

	/**
	 * COUP-01: a chain used on two carts couples them (one chain); too far apart or with both ends
	 * taken is refused; shears take a coupling off and drop its chain; a broken cart drops its chains
	 * and frees its partners.
	 */
	@GameTest(structure = YARD, maxTicks = 100)
	public void chainsCoupleAndShearsUncouple(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		track(helper, 4, 1, 28);
		Minecart a = cart(helper, 10.5, 4);
		Minecart b = cart(helper, 12.1, 4);
		Minecart c = cart(helper, 8.9, 4);
		Minecart far = cart(helper, 20.5, 4);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_CHAIN, 4));
				player.interactOn(a, InteractionHand.MAIN_HAND, Vec3.ZERO);
				player.interactOn(far, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertFalse(Couplings.hasCouplings(a) || Couplings.hasCouplings(far), "carts 10 blocks apart should not couple");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "chains after the refusal");
				player.interactOn(a, InteractionHand.MAIN_HAND, Vec3.ZERO);
				player.interactOn(b, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertTrue(coupled(a, b) && coupled(b, a), "a and b should be coupled both ways");
				helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "chains after coupling (one used)");
				log.add("10 blocks apart: refused; 1.6 apart: coupled with one chain");

				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
				player.interactOn(b, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertFalse(Couplings.hasCouplings(a) || Couplings.hasCouplings(b), "shears should take the coupling off both carts");
				helper.assertValueEqual(total(itemsAround(helper, a.position()), Items.IRON_CHAIN), 1, "chain dropped by the shears");
				itemsAround(helper, a.position()).forEach(Entity::discard);
				log.add("shears: uncoupled, chain dropped");

				couple(player, a, b);
				couple(player, c, a);
				helper.assertTrue(coupled(a, b) && coupled(a, c), "a coupled to b and c");
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_CHAIN, 4));
				player.interactOn(a, InteractionHand.MAIN_HAND, Vec3.ZERO);
				helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "a cart with both ends coupled takes no third chain");
				helper.assertFalse(coupled(a, far), "no third coupling");
				log.add("both ends taken: refused");
			}))
			.thenIdle(20)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(b.distanceTo(a) - Couplings.REST) < 0.3, "a free train should settle about " + Couplings.REST
					+ " apart, a-b " + b.distanceTo(a));
				Vec3 at = a.position();
				a.hurtServer(level, level.damageSources().playerAttack(player), 100.0F);
				helper.assertTrue(a.isRemoved(), "a should be broken");
				List<ItemEntity> drops = itemsAround(helper, at);
				helper.assertValueEqual(total(drops, Items.IRON_CHAIN), 2, "chains dropped by the broken middle cart");
				helper.assertFalse(Couplings.hasCouplings(b) || Couplings.hasCouplings(c), "its partners should be uncoupled");
				log.add("middle cart broken: 2 chains dropped, partners free");
				SimpleBlockTestLog.info("COUP-01: {}", log);
				drops.forEach(Entity::discard);
				b.discard();
				c.discard();
				far.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- Hauling trains ----

	/**
	 * ENG-05: the engine pushes and pulls a mixed train both ways, a loaded freight cart behind it and
	 * a cushioned cart and a rod cart ahead, and the cars keep their spacing.
	 */
	@GameTest(structure = YARD, maxTicks = 400)
	public void engineHaulsAMixedTrainBothWays(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(5, 1, 6));
		track(helper, 4, 0, 29);
		Minecart freight = cart(helper, 8.0, 4);
		MinecartFurnace engine = engine(helper, 9.6, 4, 2);
		Minecart seat = cart(helper, 11.2, 4);
		Minecart rod = cart(helper, 12.8, 4);
		List<AbstractMinecart> train = List.of(freight, engine, seat, rod);
		double[][] marks = new double[3][4];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				MinecartCargo.cart(freight).simpleBlock$setCargo(Items.RAW_IRON, 256);
				MinecartCargo.cart(seat).simpleBlock$setCushion(DyeColor.GREEN);
				ConnectingRods.fitted(rod).simpleBlock$setRod(true);
				couple(player, freight, engine);
				couple(player, engine, seat);
				couple(player, seat, rod);
				for (int i = 0; i < 4; i++) {
					marks[0][i] = x(helper, train.get(i));
				}
				use(player, engine, ItemStack.EMPTY);
				helper.assertTrue(CobblestoneEngine.engine(engine).simpleBlock$running(), "started");
			}))
			.thenIdle(80)
			.thenExecute(CableGameTests.step(failed, () -> {
				for (int i = 0; i < 4; i++) {
					marks[1][i] = x(helper, train.get(i));
					helper.assertTrue(marks[1][i] - marks[0][i] > 3.0, "car " + i + " should have gone east: x " + marks[0][i] + " -> " + marks[1][i]);
				}
				assertSpacing(helper, train, "running east");
				use(player, engine, ItemStack.EMPTY);
			}))
			.thenIdle(20)
			.thenExecute(CableGameTests.step(failed, () -> {
				CableGameTests.teleport(helper, player, new BlockPos((int) Math.floor(x(helper, rod)) + 3, 1, 6));
				use(player, engine, ItemStack.EMPTY);
				for (int i = 0; i < 4; i++) {
					marks[1][i] = x(helper, train.get(i));
				}
			}))
			.thenIdle(80)
			.thenExecute(CableGameTests.step(failed, () -> {
				for (int i = 0; i < 4; i++) {
					marks[2][i] = x(helper, train.get(i));
					helper.assertTrue(marks[1][i] - marks[2][i] > 3.0, "car " + i + " should have gone west: x " + marks[1][i] + " -> " + marks[2][i]);
				}
				assertSpacing(helper, train, "running west");
				log.add(String.format("east 80 ticks: freight %.2f -> %.2f, rod cart %.2f -> %.2f; west 80 ticks: freight -> %.2f, rod cart -> %.2f",
					marks[0][0], marks[1][0], marks[0][3], marks[1][3], marks[2][0], marks[2][3]));
				SimpleBlockTestLog.info("ENG-05: {}", log);
				train.forEach(Entity::discard);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	static void assertSpacingPublic(GameTestHelper helper, List<AbstractMinecart> train, String when) {
		assertSpacing(helper, train, when);
	}

	private static void assertSpacing(GameTestHelper helper, List<AbstractMinecart> train, String when) {
		for (int i = 1; i < train.size(); i++) {
			double gap = train.get(i).distanceTo(train.get(i - 1));
			helper.assertTrue(gap > 1.2 && gap < 2.2, "cars " + (i - 1) + "-" + i + " " + when + " are " + gap + " apart");
		}
	}

	/**
	 * ENG-06: mass and grade. Side by side on the level, an engine alone gets away much faster than one
	 * with three loaded carts; on a sustained climb (8 blocks of rail rising 1 in 1) the engine alone
	 * climbs to the top while the loaded train stalls part-way up and holds there without rolling back.
	 */
	@GameTest(structure = YARD, maxTicks = 600)
	public void sustainedClimbStallsALoadedTrain(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(2, 1, 4));
		int[] lanes = {2, 6};
		for (int z : lanes) {
			track(helper, z, 1, 11);
			for (int i = 0; i < 8; i++) {
				for (int y = 0; y < 1 + i; y++) {
					helper.setBlock(new BlockPos(12 + i, y, z), Blocks.STONE);
				}
				CableLoopGameTests.rail(helper, new BlockPos(12 + i, 1 + i, z), RailShape.ASCENDING_EAST);
			}
			for (int x = 20; x < 30; x++) {
				for (int y = 0; y < 8; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
				}
				CableLoopGameTests.rail(helper, new BlockPos(x, 9, z), RailShape.EAST_WEST);
			}
		}
		MinecartFurnace alone = engine(helper, 8.5, 2, 2);
		MinecartFurnace loaded = engine(helper, 8.5, 6, 2);
		Minecart[] wagons = {cart(helper, 6.9, 6), cart(helper, 5.3, 6), cart(helper, 3.7, 6)};
		double[] marks = new double[6];
		int[] ticks = {0};
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				couple(player, loaded, wagons[0]);
				couple(player, wagons[0], wagons[1]);
				couple(player, wagons[1], wagons[2]);
				for (Minecart wagon : wagons) {
					MinecartCargo.cart(wagon).simpleBlock$setCargo(Items.COBBLESTONE, 256);
				}
				use(player, alone, ItemStack.EMPTY);
				use(player, loaded, ItemStack.EMPTY);
				helper.assertTrue(CobblestoneEngine.engine(alone).simpleBlock$running() && CobblestoneEngine.engine(loaded).simpleBlock$running(),
					"both started");
				TestPlayers.remove(player);
			}))
			.thenIdle(20)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[0] = x(helper, alone) - 8.5;
				marks[1] = x(helper, loaded) - 8.5;
				helper.assertTrue(marks[0] > 2.0 * marks[1], "on the level the engine alone should get away much faster: " + marks[0] + " vs " + marks[1]
					+ " blocks in 20 ticks");
			}))
			.thenExecuteFor(400, CableGameTests.step(failed, () -> {
				ticks[0]++;
				marks[2] = Math.max(marks[2], x(helper, loaded));
				marks[3] = Math.max(marks[3], x(helper, alone));
				if (marks[3] > 22.0) {
					// Over the top: stop it before the end of the line.
					CobblestoneEngine.engine(alone).simpleBlock$setRunning(false);
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(marks[3] > 21.0, "the engine alone should climb to the top (x 20): reached x " + marks[3]);
				helper.assertTrue(marks[2] > 12.0 && marks[2] < 19.5, "the loaded train should stall on the climb (x 12-20): reached x " + marks[2]);
				helper.assertTrue(CobblestoneEngine.engine(loaded).simpleBlock$speed() == 0.0, "stalled: speed " + CobblestoneEngine.engine(loaded).simpleBlock$speed());
				marks[4] = x(helper, loaded);
				marks[5] = x(helper, wagons[2]);
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(x(helper, loaded) - marks[4]) < 0.05 && Math.abs(x(helper, wagons[2]) - marks[5]) < 0.05,
					"a stalled train should hold, not roll back: engine moved " + (x(helper, loaded) - marks[4]) + ", last wagon " + (x(helper, wagons[2]) - marks[5]));
				log.add(String.format("20 ticks on the level: alone %.2f blocks, with 3 loaded carts %.2f; climb: alone reached x %.2f (top 20), loaded "
					+ "stalled at x %.2f and held", marks[0], marks[1], marks[3], marks[2]));
				SimpleBlockTestLog.info("ENG-06: {}", log);
				alone.discard();
				loaded.discard();
				for (Minecart wagon : wagons) {
					wagon.discard();
				}
			}))
			.thenSucceed();
	}

	// ---- Saving ----

	/** ENG-07: an engine's fuel, setting, heading and speed, and a cart's couplings, are saved and loaded back. */
	@GameTest(structure = YARD)
	public void enginesAndCouplingsAreSaved(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		track(helper, 4, 1, 28);
		MinecartFurnace engine = engine(helper, 10.5, 4, 0);
		Minecart cart = cart(helper, 12.1, 4);
		couple(player, engine, cart);
		CobblestoneEngine.Engine state = CobblestoneEngine.engine(engine);
		state.simpleBlock$setFuel(5000);
		state.simpleBlock$setRunning(true);
		state.simpleBlock$setHeading(new Vec3(-1.0, 0.0, 0.0));
		state.simpleBlock$setSpeed(0.125);

		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		engine.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		MinecartFurnace copy = EntityTypes.FURNACE_MINECART.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		CobblestoneEngine.Engine loaded = CobblestoneEngine.engine(copy);
		helper.assertValueEqual(loaded.simpleBlock$fuel(), 5000, "fuel after loading");
		helper.assertTrue(loaded.simpleBlock$running(), "running after loading");
		helper.assertTrue(loaded.simpleBlock$heading().distanceTo(new Vec3(-1.0, 0.0, 0.0)) < 1.0E-6, "heading after loading " + loaded.simpleBlock$heading());
		helper.assertTrue(Math.abs(loaded.simpleBlock$speed() - 0.125) < 1.0E-9, "speed after loading");
		helper.assertTrue(cart.getUUID().equals(Couplings.coupled(copy).simpleBlock$coupling(0)), "the engine's coupling after loading");

		TagValueOutput cartOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		cart.saveWithoutId(cartOutput);
		Minecart cartCopy = EntityTypes.MINECART.create(level, EntitySpawnReason.LOAD);
		cartCopy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), cartOutput.buildResult()));
		helper.assertTrue(engine.getUUID().equals(Couplings.coupled(cartCopy).simpleBlock$coupling(0)), "the cart's coupling after loading");
		SimpleBlockTestLog.info("ENG-07: engine saved and loaded: fuel {}, running {}, heading {}, speed {}; couplings kept on both carts",
			loaded.simpleBlock$fuel(), loaded.simpleBlock$running(), loaded.simpleBlock$heading(), loaded.simpleBlock$speed());
		engine.discard();
		cart.discard();
		TestPlayers.remove(player);
		helper.succeed();
	}
}
