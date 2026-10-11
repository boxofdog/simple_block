package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.feature.InventorySlots;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;

/**
 * M03 FRT-01..09: the minecart carries 256 of one mined resource and nobody rides it. Every
 * transfer goes through {@code Player.interactOn}, the method the server runs for a player's
 * use-entity packet. Test ids refer to {@code docs/M03_TEST_PLAN.md}.
 */
public class MinecartCargoGameTests {
	static Minecart cartOnRail(GameTestHelper helper, BlockPos rail) {
		helper.setBlock(rail.below(), Blocks.STONE);
		helper.setBlock(rail, Blocks.RAIL);
		Minecart cart = helper.spawn(EntityTypes.MINECART, Vec3.atBottomCenterOf(rail).add(0, 0.0625, 0));
		cart.setDeltaMovement(Vec3.ZERO);
		return cart;
	}

	static MinecartCargo.Cart cargo(Minecart cart) {
		return MinecartCargo.cart(cart);
	}

	static void use(ServerPlayer player, Minecart cart, InteractionHand hand) {
		player.interactOn(cart, hand, cart.position());
	}

	/** Loads {@code count} of {@code item} from the player's main hand, returning what is left in hand. */
	static int load(ServerPlayer player, Minecart cart, Item item, int count) {
		player.setShiftKeyDown(false);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, count));
		use(player, cart, InteractionHand.MAIN_HAND);
		return player.getMainHandItem().is(item) ? player.getMainHandItem().getCount() : 0;
	}

	private static void unload(ServerPlayer player, Minecart cart) {
		player.setShiftKeyDown(true);
		use(player, cart, InteractionHand.MAIN_HAND);
		player.setShiftKeyDown(false);
	}

	private static void assertCargo(GameTestHelper helper, Minecart cart, Item item, int count, String when) {
		helper.assertValueEqual(cargo(cart).simpleBlock$count(), count, "cargo count " + when);
		helper.assertValueEqual(cargo(cart).simpleBlock$cargo(), count > 0 ? item : Items.AIR, "cargo item " + when);
	}

	/** How many of {@code item} the player carries (inventory, off hand). */
	static int carried(ServerPlayer player, Item item) {
		int total = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			total += stack.is(item) ? stack.getCount() : 0;
		}
		return total + (player.getOffhandItem().is(item) ? player.getOffhandItem().getCount() : 0);
	}

	static List<ItemEntity> itemsAround(ServerLevel level, Vec3 pos) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos).inflate(3.0));
	}

	static int total(List<ItemEntity> items, Item item) {
		return items.stream().filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	/** T02, T15, T23: a minecart is placed on rails as before, but can't be ridden or used as a container. */
	@GameTest(maxTicks = 120)
	public void minecartPlacesOnRailsButNobodyRidesIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 8; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 2, 1), Blocks.RAIL);
		}
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 2, 3));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MINECART));
		BlockPos rail = helper.absolutePos(new BlockPos(1, 2, 1));
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(rail), Direction.UP, rail, false));
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the minecart item should be used up");
		List<Minecart> carts = level.getEntitiesOfClass(Minecart.class, new AABB(rail).inflate(1.0));
		helper.assertValueEqual(carts.size(), 1, "minecarts placed on the rail");
		Minecart cart = carts.getFirst();
		helper.assertFalse(cart instanceof Container, "a minecart is not a container");

		use(player, cart, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getVehicle() == null && !cart.isVehicle(), "using a minecart should not seat the player");
		helper.assertFalse(player.startRiding(cart), "a player can't get in");

		// A rolling vanilla minecart picks up any mob it bumps into; this one must not.
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(5, 2, 1));
		zombie.setNoAi(true);
		cart.setDeltaMovement(0.4, 0.0, 0.0);
		double startX = cart.getX();
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(zombie.getVehicle() == null && !cart.isVehicle(), "the rolling minecart picked up the zombie");
			helper.assertTrue(cart.getX() > startX + 1.0, "the minecart should have rolled towards the zombie (moved " + (cart.getX() - startX) + ")");
			SimpleBlockTestLog.info("FRT-06: placed on a rail by item; player and a zombie in its path stayed out (cart rolled {} blocks)",
				String.format("%.1f", cart.getX() - startX));
			zombie.discard();
			cart.discard();
			TestPlayers.remove(player);
			helper.succeed();
		});
	}

	/** T04-T08: exact loading, the 256 cap, one kind at a time, reuse after emptying, refused items. */
	@GameTest
	public void loadingIsExactAndSingleKind(GameTestHelper helper) {
		Minecart cart = cartOnRail(helper, new BlockPos(2, 2, 2));
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(4, 2, 2));
		player.getInventory().clearContent();

		// T04: 1, then 63, then whole stacks.
		helper.assertValueEqual(load(player, cart, Items.RAW_IRON, 1), 0, "left in hand after loading 1");
		assertCargo(helper, cart, Items.RAW_IRON, 1, "after 1");
		helper.assertValueEqual(load(player, cart, Items.RAW_IRON, 63), 0, "left in hand after loading 63");
		assertCargo(helper, cart, Items.RAW_IRON, 64, "after 63 more");
		load(player, cart, Items.RAW_IRON, 64);
		load(player, cart, Items.RAW_IRON, 64);
		helper.assertValueEqual(load(player, cart, Items.RAW_IRON, 63), 0, "left in hand after loading 63 more");
		assertCargo(helper, cart, Items.RAW_IRON, 255, "after 255");

		// T05: 255 + 64 stops at 256; the other 63 stay in hand, and stay there when it is full.
		helper.assertValueEqual(load(player, cart, Items.RAW_IRON, 64), 63, "left in hand when topping up to 256");
		assertCargo(helper, cart, Items.RAW_IRON, 256, "when full");
		helper.assertValueEqual(load(player, cart, Items.RAW_IRON, 63), 63, "left in hand when the cart is full");
		assertCargo(helper, cart, Items.RAW_IRON, 256, "after loading into a full cart");

		// T06: another kind is refused while raw iron is in.
		helper.assertValueEqual(load(player, cart, Items.RAW_COPPER, 10), 10, "raw copper left in hand");
		assertCargo(helper, cart, Items.RAW_IRON, 256, "after offering raw copper");

		// T07: empty it (unloading a stack at a time, putting each one away), then load something else.
		int unloaded = 0;
		for (int round = 0; round < 8 && cargo(cart).simpleBlock$count() > 0; round++) {
			player.getInventory().clearContent();
			unload(player, cart);
			unloaded += carried(player, Items.RAW_IRON);
		}
		helper.assertValueEqual(unloaded, 256, "raw iron unloaded in total");
		assertCargo(helper, cart, Items.RAW_IRON, 0, "after unloading everything");
		player.getInventory().clearContent();
		helper.assertValueEqual(load(player, cart, Items.COBBLESTONE, 40), 0, "cobblestone left in hand");
		assertCargo(helper, cart, Items.COBBLESTONE, 40, "after reuse for cobblestone");
		player.getInventory().clearContent();
		for (int round = 0; round < 2; round++) {
			unload(player, cart);
		}
		player.getInventory().clearContent();

		// T08: compressed, processed, equipment, containers, renamed or bundled cargo: all refused by an empty cart.
		ItemStack named = new ItemStack(Items.RAW_IRON, 5);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Lucky iron"));
		ItemStack bundle = new ItemStack(Items.BUNDLE);
		bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.RAW_IRON, 16)))));
		List<ItemStack> refused = List.of(new ItemStack(Items.IRON_BLOCK, 3), new ItemStack(Items.RAW_IRON_BLOCK, 3), new ItemStack(Items.IRON_INGOT, 8),
			new ItemStack(Items.IRON_NUGGET, 8), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.CHEST, 2), new ItemStack(Items.SHULKER_BOX),
			new ItemStack(Items.BUCKET), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.MINECART), new ItemStack(Items.DIRT, 30),
			new ItemStack(Items.GRAVEL, 30), named, bundle);
		List<String> names = new ArrayList<>();
		for (ItemStack offer : refused) {
			ItemStack held = offer.copy();
			player.setItemInHand(InteractionHand.MAIN_HAND, held);
			use(player, cart, InteractionHand.MAIN_HAND);
			helper.assertTrue(ItemStack.matches(player.getMainHandItem(), offer), offer + " should stay in hand untouched");
			assertCargo(helper, cart, Items.AIR, 0, "after offering " + offer);
			names.add(offer.getItem().toString() + (offer.has(DataComponents.CUSTOM_NAME) ? " (renamed)" : ""));
		}
		SimpleBlockTestLog.info("FRT-02/03: loaded 1+63+64+64+63=255, +64 stopped at 256 with 63 left in hand; raw copper refused while raw iron "
			+ "was in; unloaded 256, then reused for cobblestone; refused: {}", String.join(", ", names));
		cart.discard();
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** T09, T10: unloading takes only what fits in the inventory and under the heavy-load cap. */
	@GameTest
	public void unloadingRespectsSpaceAndHeavyLoad(GameTestHelper helper) {
		Minecart cart = cartOnRail(helper, new BlockPos(2, 2, 2));
		Minecart stone = cartOnRail(helper, new BlockPos(2, 2, 5));
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(4, 2, 2));
		player.getInventory().clearContent();
		cargo(cart).simpleBlock$setCargo(Items.RAW_IRON, 200);
		cargo(stone).simpleBlock$setCargo(Items.COBBLESTONE, 100);

		// T09 (space): every usable slot full of dirt except one holding 60 cobblestone (weightless): only 4 fit.
		List<ItemStack> slots = player.getInventory().getNonEquipmentItems();
		for (int i = 0; i < slots.size(); i++) {
			if (!InventorySlots.isDisabled(i)) {
				player.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
			}
		}
		player.getInventory().setItem(5, new ItemStack(Items.COBBLESTONE, 60));
		unload(player, stone);
		helper.assertValueEqual(carried(player, Items.COBBLESTONE), 64, "cobblestone carried after unloading into one part-full slot");
		assertCargo(helper, stone, Items.COBBLESTONE, 96, "after a 4-item unload");
		unload(player, stone);
		assertCargo(helper, stone, Items.COBBLESTONE, 96, "after unloading into a full inventory");

		// T10 (weight): plenty of room, but 60 ingots already carried: 4 raw iron fit under the cap, then none.
		player.getInventory().clearContent();
		player.getInventory().setItem(20, new ItemStack(Items.IRON_INGOT, 60));
		unload(player, cart);
		helper.assertValueEqual(carried(player, Items.RAW_IRON), 4, "raw iron taken with 60 ingots carried");
		assertCargo(helper, cart, Items.RAW_IRON, 196, "after the capped unload");
		unload(player, cart);
		helper.assertValueEqual(carried(player, Items.RAW_IRON), 4, "raw iron taken at the cap");
		assertCargo(helper, cart, Items.RAW_IRON, 196, "after unloading at the cap");

		// At the heavy cap, weightless cargo is limited by space only.
		unload(player, stone);
		helper.assertValueEqual(carried(player, Items.COBBLESTONE), 64, "cobblestone taken while at the heavy cap");
		assertCargo(helper, stone, Items.COBBLESTONE, 32, "cobblestone left");
		SimpleBlockTestLog.info("FRT-04: full inventory + one slot of 60 cobblestone took 4 (cart 100 -> 96), then 0; with 60 ingots carried took "
			+ "4 raw iron (cart 200 -> 196), then 0 at the cap; at the cap a full 64 cobblestone (weightless) still came out");
		cart.discard();
		stone.discard();
		TestPlayers.remove(player);
		helper.succeed();
	}

	/** T11: cargo is saved with the cart and loaded back exactly; an empty cart saves nothing extra. */
	@GameTest
	public void cargoSurvivesSaveAndLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Minecart cart = cartOnRail(helper, new BlockPos(2, 2, 2));
		cargo(cart).simpleBlock$setCargo(Items.DEEPSLATE_IRON_ORE, 180);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		cart.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		helper.assertValueEqual(tag.getIntOr("simple_block:cargo_count", -1), 180, "saved count");

		Minecart copy = EntityTypes.MINECART.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		assertCargo(helper, copy, Items.DEEPSLATE_IRON_ORE, 180, "after loading the saved cart");

		Minecart empty = cartOnRail(helper, new BlockPos(2, 2, 4));
		TagValueOutput emptyOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		empty.saveWithoutId(emptyOutput);
		CompoundTag emptyTag = emptyOutput.buildResult();
		helper.assertFalse(emptyTag.contains("simple_block:cargo") || emptyTag.contains("simple_block:cargo_count"), "an empty cart saves no cargo");
		Minecart emptyCopy = EntityTypes.MINECART.create(level, EntitySpawnReason.LOAD);
		emptyCopy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), emptyTag));
		assertCargo(helper, emptyCopy, Items.AIR, 0, "after loading an empty cart");
		SimpleBlockTestLog.info("FRT-05: saved {} -> loaded {} x{}", tag.get("simple_block:cargo"), cargo(copy).simpleBlock$cargo(),
			cargo(copy).simpleBlock$count());
		cart.discard();
		empty.discard();
		helper.succeed();
	}

	/** T12, T13: however a loaded cart is destroyed, its cargo comes out once, as ordinary stacks. */
	@GameTest(maxTicks = 60)
	public void destroyedCartsSpillCargoOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer survivor = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(0, 2, 0));
		ServerPlayer creative = TestPlayers.create(helper, GameType.CREATIVE, new BlockPos(0, 2, 7));
		List<String> log = new ArrayList<>();
		helper.startSequence()
			.thenExecute(() -> {
				// Broken by a survival player: 256 raw iron in 4 stacks, plus an empty minecart item.
				Minecart broken = cartOnRail(helper, new BlockPos(1, 2, 1));
				cargo(broken).simpleBlock$setCargo(Items.RAW_IRON, 256);
				Vec3 at = broken.position();
				broken.hurtServer(level, level.damageSources().playerAttack(survivor), 10.0F);
				List<ItemEntity> items = itemsAround(level, at);
				helper.assertTrue(broken.isRemoved(), "the broken cart should be gone");
				helper.assertValueEqual(total(items, Items.RAW_IRON), 256, "raw iron dropped by the broken cart");
				helper.assertTrue(items.stream().filter(e -> e.getItem().is(Items.RAW_IRON)).allMatch(e -> e.getItem().getCount() <= 64), "stacks of at most 64");
				List<ItemEntity> carts = items.stream().filter(e -> e.getItem().is(Items.MINECART)).toList();
				helper.assertValueEqual(carts.size(), 1, "minecart items dropped");
				helper.assertTrue(carts.getFirst().getItem().getComponentsPatch().isEmpty(), "the dropped minecart carries no data");
				helper.assertValueEqual(cargo(broken).simpleBlock$count(), 0, "cargo left in the broken cart");
				MinecartCargo.dropCargo(broken, level);
				helper.assertValueEqual(total(itemsAround(level, at), Items.RAW_IRON), 256, "raw iron after spilling the broken cart again");
				log.add("survival break: 256 raw iron in " + items.stream().filter(e -> e.getItem().is(Items.RAW_IRON)).count() + " stacks + 1 empty minecart");
				items.forEach(Entity::discard);

				// Blown up (no block damage, so the rail survives).
				Minecart blown = cartOnRail(helper, new BlockPos(6, 2, 1));
				cargo(blown).simpleBlock$setCargo(Items.COBBLESTONE, 100);
				Vec3 blast = blown.position();
				level.explode(null, blast.x, blast.y, blast.z, 3.0F, Level.ExplosionInteraction.NONE);
				helper.assertTrue(blown.isRemoved(), "the blown-up cart should be gone");
				List<ItemEntity> blownItems = itemsAround(level, blast);
				helper.assertValueEqual(total(blownItems, Items.COBBLESTONE), 100, "cobblestone dropped by the blown-up cart");
				log.add("explosion: 100 cobblestone + " + total(blownItems, Items.MINECART) + " minecart");
				blownItems.forEach(Entity::discard);

				// Killed (as by /kill): cargo, but no minecart item, as in vanilla.
				Minecart killed = cartOnRail(helper, new BlockPos(1, 2, 5));
				cargo(killed).simpleBlock$setCargo(Items.COAL, 50);
				Vec3 killedAt = killed.position();
				killed.kill(level);
				List<ItemEntity> killedItems = itemsAround(level, killedAt);
				helper.assertValueEqual(total(killedItems, Items.COAL), 50, "coal dropped by the killed cart");
				log.add("kill: 50 coal + " + total(killedItems, Items.MINECART) + " minecart");
				killedItems.forEach(Entity::discard);

				// A creative player's hit discards the cart: the cargo still spills.
				Minecart discarded = cartOnRail(helper, new BlockPos(6, 2, 5));
				cargo(discarded).simpleBlock$setCargo(Items.RAW_GOLD, 30);
				Vec3 discardedAt = discarded.position();
				discarded.hurtServer(level, level.damageSources().playerAttack(creative), 1.0F);
				helper.assertTrue(discarded.isRemoved(), "the creative hit should remove the cart");
				List<ItemEntity> creativeItems = itemsAround(level, discardedAt);
				helper.assertValueEqual(total(creativeItems, Items.RAW_GOLD), 30, "raw gold dropped by the creative-broken cart");
				log.add("creative hit: 30 raw gold + " + total(creativeItems, Items.MINECART) + " minecart");
				creativeItems.forEach(Entity::discard);

				SimpleBlockTestLog.info("FRT-07: {}", String.join("; ", log));
				TestPlayers.remove(survivor);
				TestPlayers.remove(creative);
			})
			.thenSucceed();
	}

	/** T14: two players clicking in the same tick never create or lose items. */
	@GameTest
	public void quickRepeatedUseByTwoPlayersConservesItems(GameTestHelper helper) {
		Minecart cart = cartOnRail(helper, new BlockPos(3, 2, 3));
		ServerPlayer a = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 2, 3));
		ServerPlayer b = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(5, 2, 3));
		a.getInventory().clearContent();
		b.getInventory().clearContent();
		cargo(cart).simpleBlock$setCargo(Items.RAW_IRON, 200);
		a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_IRON, 64));
		b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_IRON, 64));
		int start = 200 + 64 + 64;
		for (int i = 0; i < 5; i++) {
			use(a, cart, InteractionHand.MAIN_HAND);
			use(b, cart, InteractionHand.MAIN_HAND);
		}
		int afterLoading = cargo(cart).simpleBlock$count() + carried(a, Items.RAW_IRON) + carried(b, Items.RAW_IRON);
		helper.assertValueEqual(cargo(cart).simpleBlock$count(), 256, "cart after both loaded");
		helper.assertValueEqual(afterLoading, start, "raw iron in total after loading");
		for (int i = 0; i < 10; i++) {
			unload(a, cart);
			unload(b, cart);
		}
		int afterUnloading = cargo(cart).simpleBlock$count() + carried(a, Items.RAW_IRON) + carried(b, Items.RAW_IRON);
		helper.assertValueEqual(afterUnloading, start, "raw iron in total after 20 unloads in one tick");
		helper.assertValueEqual(carried(a, Items.RAW_IRON), 64, "A carries the 64 the heavy cap allows");
		helper.assertValueEqual(carried(b, Items.RAW_IRON), 64, "B carries the 64 the heavy cap allows");
		helper.assertValueEqual(cargo(cart).simpleBlock$count(), 200, "left in the cart");
		SimpleBlockTestLog.info("FRT-04 (T14): two players, 10 loads then 20 unloads in one tick: {} raw iron before, {} after loading, {} after "
			+ "unloading (cart {}, each player 64 = heavy cap)", start, afterLoading, afterUnloading, cargo(cart).simpleBlock$count());
		cart.discard();
		TestPlayers.remove(a);
		TestPlayers.remove(b);
		helper.succeed();
	}

	/** T15: a hopper under a loaded cart cannot take its cargo; the cargo is not a container's. */
	@GameTest(maxTicks = 60)
	public void hopperCannotUnloadACart(GameTestHelper helper) {
		BlockPos rail = new BlockPos(2, 2, 2);
		helper.setBlock(rail.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(rail, Blocks.RAIL);
		Minecart cart = helper.spawn(EntityTypes.MINECART, Vec3.atBottomCenterOf(rail).add(0, 0.0625, 0));
		cargo(cart).simpleBlock$setCargo(Items.RAW_COPPER, 128);
		HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(rail.below()));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(hopper.isEmpty(), "the hopper took something from the cart");
			assertCargo(helper, cart, Items.RAW_COPPER, 128, "after 40 ticks over a hopper");
			cart.discard();
			helper.succeed();
		});
	}

	/** T03: loaded and empty carts run a loop of straight, curved, sloped and powered rail. */
	@GameTest(maxTicks = 700)
	public void cartsRunALoopOfEveryRailKind(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// An 6x6 loop at y=2 on a stone floor; the east side climbs to y=3 and back; the west side is powered.
		List<BlockPos> track = new ArrayList<>();
		List<net.minecraft.world.level.block.state.BlockState> shapes = new ArrayList<>();
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		Block rail = Blocks.RAIL;
		for (int x = 2; x <= 5; x++) {
			track.add(new BlockPos(x, 2, 1));
			shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
			track.add(new BlockPos(x, 2, 6));
			shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
		}
		track.add(new BlockPos(1, 2, 1));
		shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.SOUTH_EAST));
		track.add(new BlockPos(6, 2, 1));
		shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.SOUTH_WEST));
		track.add(new BlockPos(6, 2, 6));
		shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_WEST));
		track.add(new BlockPos(1, 2, 6));
		shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_EAST));
		// East side: up at z=2, level at z=3..4 (one higher), down at z=5.
		helper.setBlock(new BlockPos(6, 2, 3), Blocks.STONE);
		helper.setBlock(new BlockPos(6, 2, 4), Blocks.STONE);
		track.add(new BlockPos(6, 2, 2));
		shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.ASCENDING_SOUTH));
		track.add(new BlockPos(6, 3, 3));
		shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
		track.add(new BlockPos(6, 3, 4));
		shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
		track.add(new BlockPos(6, 2, 5));
		shapes.add(rail.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.ASCENDING_NORTH));
		// West side: powered rail on redstone blocks, pushing north-to-south traffic along.
		for (int z = 2; z <= 5; z++) {
			helper.setBlock(new BlockPos(1, 1, z), Blocks.REDSTONE_BLOCK);
			track.add(new BlockPos(1, 2, z));
			shapes.add(Blocks.POWERED_RAIL.defaultBlockState().setValue(PoweredRailBlock.SHAPE, RailShape.NORTH_SOUTH).setValue(PoweredRailBlock.POWERED, true));
		}
		// Place, then set every shape again without updates: placing a rail re-shapes it to its neighbours.
		for (int i = 0; i < track.size(); i++) {
			helper.setBlock(track.get(i), shapes.get(i));
		}
		for (int i = 0; i < track.size(); i++) {
			level.setBlock(helper.absolutePos(track.get(i)), shapes.get(i), Block.UPDATE_CLIENTS);
		}

		// One listener for the whole test (scheduling from inside scheduled tasks upsets the test runner).
		BlockPos[] checkpoints = {new BlockPos(1, 2, 6), new BlockPos(6, 2, 6), new BlockPos(6, 3, 4), new BlockPos(6, 2, 1), new BlockPos(1, 2, 1)};
		Minecart[] current = {null};
		boolean[] seen = new boolean[checkpoints.length];
		int[] laps = {0};
		helper.onEachTick(() -> {
			Minecart cart = current[0];
			if (cart == null || cart.isRemoved()) {
				return;
			}
			BlockPos at = cart.blockPosition();
			for (int i = 0; i < checkpoints.length; i++) {
				if (at.equals(helper.absolutePos(checkpoints[i]))) {
					seen[i] = true;
					if (i == checkpoints.length - 1 && seen[0] && seen[1] && seen[2] && seen[3]) {
						laps[0]++;
						java.util.Arrays.fill(seen, false);
					}
				}
			}
		});
		List<String> results = new ArrayList<>();
		helper.startSequence()
			.thenExecute(() -> current[0] = startLoop(helper, true))
			.thenIdle(300)
			.thenExecute(() -> {
				checkLoop(helper, current[0], true, laps[0], seen, results);
				laps[0] = 0;
				java.util.Arrays.fill(seen, false);
				current[0] = startLoop(helper, false);
			})
			.thenIdle(300)
			.thenExecute(() -> {
				checkLoop(helper, current[0], false, laps[0], seen, results);
				SimpleBlockTestLog.info("FRT-01 (T03): {}", String.join("; ", results));
			})
			.thenSucceed();
	}

	/** A cart on the powered west side of the loop, rolling south. */
	private static Minecart startLoop(GameTestHelper helper, boolean loaded) {
		Minecart cart = helper.spawn(EntityTypes.MINECART, Vec3.atBottomCenterOf(new BlockPos(1, 2, 3)).add(0, 0.0625, 0));
		if (loaded) {
			cargo(cart).simpleBlock$setCargo(Items.RAW_IRON, 256);
		}
		cart.setDeltaMovement(0.0, 0.0, 0.4);
		return cart;
	}

	private static void checkLoop(GameTestHelper helper, Minecart cart, boolean loaded, int laps, boolean[] seen, List<String> results) {
		String what = loaded ? "loaded cart (256 raw iron)" : "empty cart";
		helper.assertTrue(laps >= 1, what + " never went all the way round (checkpoints seen: " + java.util.Arrays.toString(seen) + ")");
		helper.assertTrue(!loaded || cargo(cart).simpleBlock$count() == 256, "the loaded cart lost cargo on the way: " + cargo(cart).simpleBlock$count()
			+ " " + cargo(cart).simpleBlock$cargo() + ", removed: " + cart.getRemovalReason() + ", at " + cart.position());
		results.add(what + ": " + laps + " laps in 300 ticks, cargo after " + cargo(cart).simpleBlock$count());
		cart.discard();
	}
}
