package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableLines;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.skip.OreSkip;
import io.github.boxofdog.simpleblock.skip.SkipCargo;
import io.github.boxofdog.simpleblock.skip.SkipContent;
import io.github.boxofdog.simpleblock.skip.SkipMenu;
import io.github.boxofdog.simpleblock.skip.SkipStations;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.train.TrainContent;

/**
 * M07: the upright ore skip. Test ids refer to the matrix in {@code docs/MILESTONE_07_REPORT.md}.
 *
 * <p>The shaft, in {@code skip_shaft} (24 x 16 x 12, empty): a stone floor at y 0; the shaft column at
 * x 12, z 5, the skip facing north (its front, the chute's mouth and the stations, towards z 4); a
 * drum at x 2, z 5 whose cable runs through an anchor at (7, 6, 5) to the head anchor at (12, 11, 5)
 * over the shaft. Stations: a Loading Rail at (12, 1, 4) beside the chute of a skip standing on the
 * floor, and one at (12, 9, 4) beside the bridle of a skip at height 6.
 */
public class SkipGameTests {
	private static final String SHAFT = "simple_block-gametest:skip_shaft";
	private static final BlockPos DRUM = new BlockPos(2, 1, 5);
	private static final BlockPos MID_ANCHOR = new BlockPos(7, 6, 5);
	private static final BlockPos HEAD_ANCHOR = new BlockPos(12, 11, 5);
	private static final BlockPos COLUMN = new BlockPos(12, 1, 5);
	private static final BlockPos BOTTOM_RAIL = new BlockPos(12, 1, 4);
	private static final BlockPos TOP_RAIL = new BlockPos(12, 9, 4);
	/** The skip's bottom when the top station's cart is level with its bridle. */
	private static final double TOP_STATION = 6.0;

	// ---- Helpers ----

	private static double y(GameTestHelper helper, OreSkip skip) {
		return skip.getY() - helper.absoluteVec(Vec3.ZERO).y;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 24; x++) {
			for (int z = 0; z < 12; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	/** A skip standing at {@code column} (relative), facing north, spawned as a placed one would be. */
	static OreSkip spawnSkip(GameTestHelper helper, BlockPos column) {
		ServerLevel level = helper.getLevel();
		OreSkip skip = SkipContent.ORE_SKIP.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(column));
		skip.setFacing(Direction.NORTH);
		skip.setPos(at.x, at.y, at.z);
		level.addFreshEntity(skip);
		return skip;
	}

	private static void loadingRail(GameTestHelper helper, BlockPos rail) {
		helper.setBlock(rail.below(), Blocks.STONE);
		helper.setBlock(rail, TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, Direction.SOUTH)
			.setValue(LoadingTrackBlock.SHAPE, RailShape.NORTH_SOUTH));
	}

	/** A freight cart standing on {@code rail} (relative) with {@code count} of {@code item}; it docks on its next tick. */
	private static Minecart cartOn(GameTestHelper helper, BlockPos rail, Item item, int count) {
		Minecart cart = helper.spawn(EntityTypes.MINECART, Vec3.atBottomCenterOf(rail).add(0.0, 0.0625, 0.0));
		cart.setDeltaMovement(Vec3.ZERO);
		MinecartCargo.cart(cart).simpleBlock$setCargo(item, count);
		return cart;
	}

	private static int cartCount(Minecart cart) {
		return MinecartCargo.cart(cart).simpleBlock$count();
	}

	/** The shaft's drum, anchors and route (the cable end put down under the head anchor). Run at least a tick after {@link #layShaft}. */
	private static void route(GameTestHelper helper, ServerPlayer player) {
		// Next to the drum, where its loose end lies (the end only follows the hand between ticks).
		CableGameTests.teleport(helper, player, new BlockPos(3, 1, 6));
		CableLoopGameTests.route(helper, player, DRUM, MID_ANCHOR, HEAD_ANCHOR);
	}

	private static CableDrumBlockEntity layShaft(GameTestHelper helper, ServerPlayer player) {
		floor(helper);
		CableDrumBlockEntity drum = CableGameTests.windDrum(helper, player, DRUM, 1);
		CableGameTests.hangAnchor(helper, player, MID_ANCHOR.above());
		CableGameTests.hangAnchor(helper, player, HEAD_ANCHOR.above());
		return drum;
	}

	/** Picks up the cable end from under the head anchor and uses it on the skip. */
	private static void hang(GameTestHelper helper, ServerPlayer player, OreSkip skip) {
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		var hook = CableGameTests.hook(helper, DRUM);
		player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
		// The end follows the hand on its next tick, as it does between a player's clicks.
		hook.tick();
		player.interactOn(skip, InteractionHand.MAIN_HAND, skip.position());
	}

	private static CableLines.Line line(GameTestHelper helper) {
		return CableLines.line(helper.getLevel(), helper.absolutePos(DRUM));
	}

	/** A right-click on the drum's body with {@code held} (a furnace, coal). */
	private static void body(GameTestHelper helper, ServerPlayer player, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		CableGameTests.useBlock(helper, player, DRUM, Direction.UP);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	/** A right-click on the coal winch's lever. */
	private static void lever(GameTestHelper helper, ServerPlayer player) {
		BlockPos pos = helper.absolutePos(DRUM);
		BlockState state = helper.getLevel().getBlockState(pos);
		Direction side = CableDrumBlock.crankSide(state);
		Vec3 hit = Vec3.atLowerCornerOf(pos).add(0.5 + side.getStepX() * 0.55, 9.5 / 16.0, 0.5 + side.getStepZ() * 0.55);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(hit, side, pos, false));
	}

	/** Fits a furnace and {@code coal} coal. */
	private static void coalWinch(GameTestHelper helper, ServerPlayer player, int coal) {
		body(helper, player, new ItemStack(Items.FURNACE));
		for (int i = 0; i < coal; i++) {
			body(helper, player, new ItemStack(Items.COAL));
		}
	}

	/** Sets the lever to {@code lever} by clicking it (at most three clicks). */
	private static void setLever(GameTestHelper helper, ServerPlayer player, CableLines.Lever lever) {
		for (int i = 0; i < 3 && line(helper).lever() != lever; i++) {
			lever(helper, player);
		}
		helper.assertValueEqual(line(helper).lever(), lever, "lever");
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

	private static int total(List<ItemEntity> items, Item item) {
		return items.stream().filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	// ---- SKIP-01 recipe ----

	/**
	 * SKIP-01: the owner's shaped recipe, exactly: cable top middle, iron trapdoor in the middle, iron
	 * ingot / minecart / iron ingot along the bottom, every other cell empty; one skip. Misplaced, missing
	 * or substituted ingredients make nothing.
	 */
	@GameTest
	public void skipRecipeIsTheOwnersShape(GameTestHelper helper) {
		Item air = Items.AIR;
		Item cable = CableContent.CABLE;
		Item trapdoor = Items.IRON_TRAPDOOR;
		Item ingot = Items.IRON_INGOT;
		Item cart = Items.MINECART;
		ItemStack made = craft(helper, air, cable, air, air, trapdoor, air, ingot, cart, ingot);
		helper.assertTrue(made.is(SkipContent.ORE_SKIP_ITEM) && made.getCount() == 1, "the owner's recipe should make one skip, made " + made);
		List<String> refused = new ArrayList<>();
		Item[][] wrong = {
			{air, trapdoor, air, air, cable, air, ingot, cart, ingot},       // cable and trapdoor swapped
			{air, cable, air, air, trapdoor, air, cart, ingot, ingot},       // minecart in a corner
			{air, air, air, air, trapdoor, air, ingot, cart, ingot},         // no cable
			{air, cable, air, air, trapdoor, air, ingot, cart, air},         // one ingot
			{air, cable, air, air, Items.OAK_TRAPDOOR, air, ingot, cart, ingot}, // wooden trapdoor
			{air, cable, air, air, trapdoor, air, Items.IRON_BLOCK, cart, Items.IRON_BLOCK}, // iron blocks
			{air, cable, air, air, trapdoor, air, ingot, Items.FURNACE_MINECART, ingot}, // not a plain minecart
			{cable, air, air, air, trapdoor, air, ingot, cart, ingot},       // cable in the corner
			{air, cable, cable, air, trapdoor, air, ingot, cart, ingot},     // an extra cable
		};
		for (Item[] grid : wrong) {
			ItemStack result = craft(helper, grid);
			helper.assertTrue(result.isEmpty(), "a wrong layout should make nothing, made " + result + " from " + List.of(grid));
			refused.add(List.of(grid).toString());
		}
		SimpleBlockTestLog.info("SKIP-01: owner's layout makes {}; {} wrong layouts make nothing", made, refused.size());
		helper.succeed();
	}

	// ---- SKIP-02 geometry ----

	/**
	 * SKIP-02: placed from its item on the top of a block, the skip stands in that column facing the
	 * player's way back, its hitbox exactly one block wide and deep and four high (body and bridle), and
	 * solid to other entities. It is refused (and the item kept) where a block is in its column's four
	 * cells (including the bridle's) or in a side cell; a block in front or behind is fine.
	 */
	@GameTest(structure = SHAFT)
	public void skipNeedsItsColumnAndSideCells(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(12, 1, 2));
		player.setYRot(0.0F);
		List<String> log = new ArrayList<>();
		// Placed where something is in the way: in a side cell, at the bridle, then fine with blocks in front and behind.
		BlockPos[] obstacles = {new BlockPos(13, 2, 5), new BlockPos(11, 4, 5), new BlockPos(12, 4, 5)};
		for (BlockPos obstacle : obstacles) {
			helper.setBlock(obstacle, Blocks.STONE);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SkipContent.ORE_SKIP_ITEM));
			CableGameTests.useBlock(helper, player, COLUMN.below(), Direction.UP);
			int skips = helper.getLevel().getEntitiesOfClass(OreSkip.class, new AABB(helper.absolutePos(COLUMN)).inflate(3.0)).size();
			helper.assertValueEqual(skips, 0, "skips placed with a block at " + obstacle);
			helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "the item kept with a block at " + obstacle);
			helper.setBlock(obstacle, Blocks.AIR);
			log.add("refused with a block at " + obstacle);
		}
		helper.setBlock(COLUMN.north(), Blocks.STONE);
		helper.setBlock(COLUMN.south().above(2), Blocks.STONE);
		CableGameTests.useBlock(helper, player, COLUMN.below(), Direction.UP);
		List<OreSkip> skips = helper.getLevel().getEntitiesOfClass(OreSkip.class, new AABB(helper.absolutePos(COLUMN)).inflate(3.0));
		helper.assertValueEqual(skips.size(), 1, "skips placed with only front and back blocks");
		OreSkip skip = skips.getFirst();
		AABB box = skip.getBoundingBox();
		helper.assertValueEqual(skip.facing(), Direction.NORTH, "facing (front towards the player, who faced south)");
		helper.assertTrue(Math.abs(box.getXsize() - 1.0) < 1.0E-6 && Math.abs(box.getZsize() - 1.0) < 1.0E-6 && Math.abs(box.getYsize() - 4.0) < 1.0E-6,
			"hitbox 1 x 4 x 1: " + box);
		helper.assertTrue(Math.abs(box.minY - helper.absolutePos(COLUMN).getY()) < 1.0E-6, "standing on the block it was placed on: " + box);
		helper.assertTrue(skip.canBeCollidedWith(player), "solid to other entities");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the item used up");
		helper.assertValueEqual(skip.lateral().getAxis(), Direction.Axis.X, "side cells along x (across its front)");
		SimpleBlockTestLog.info("SKIP-02: {}; placed with blocks in front and behind: box {}, facing {}, side cells along {}", log, box, skip.facing(),
			skip.lateral().getAxis());
		skip.discard();
		TestPlayers.remove(player);
		helper.succeed();
	}

	// ---- SKIP-03 cargo ----

	/**
	 * SKIP-03: one slot of up to 512 of one commodity. Cargo in hand loads (64 at a time, the most a
	 * stack holds), up to 512 and no more; another commodity and non-cargo are refused without changing
	 * the slot. Through the screen's menu, the cursor and the player's inventory only ever get ordinary
	 * stacks (at most 64), and every click keeps skip + cursor + inventory at the same total. A freight
	 * cart still holds 256. Saved and loaded, the cargo is the same; broken, the skip drops itself and its
	 * cargo as stacks of at most 64.
	 */
	@GameTest(structure = SHAFT)
	public void skipSlotHolds512OfOneCommodity(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(12, 1, 3));
		OreSkip skip = spawnSkip(helper, COLUMN);
		List<String> log = new ArrayList<>();
		// By hand: 9 stacks of 64 raw iron offered, 512 taken.
		int taken = 0;
		for (int i = 0; i < 9; i++) {
			ItemStack stack = new ItemStack(Items.RAW_IRON, 64);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			player.interactOn(skip, InteractionHand.MAIN_HAND, skip.position());
			taken += 64 - player.getMainHandItem().getCount();
		}
		helper.assertValueEqual(skip.count(), SkipCargo.CAPACITY, "count after offering 576");
		helper.assertValueEqual(taken, SkipCargo.CAPACITY, "taken from the hand");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 64, "the 513th and on stay in the hand");
		log.add("hand: 576 offered, 512 taken, 64 left in hand");
		// Another commodity and non-cargo are refused; the slot is unchanged.
		skip.setCargo(Items.RAW_IRON, 100);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 10));
		player.interactOn(skip, InteractionHand.MAIN_HAND, skip.position());
		helper.assertValueEqual(player.getMainHandItem().getCount(), 10, "coal refused");
		helper.assertTrue(skip.cargo() == Items.RAW_IRON && skip.count() == 100, "slot after coal: " + skip);
		helper.assertValueEqual(SkipCargo.room(skip, new ItemStack(Items.STICK)), 0, "room for sticks");
		helper.assertValueEqual(SkipCargo.room(skip, new ItemStack(Items.RAW_IRON)), 412, "room for raw iron");
		log.add("coal and sticks refused, raw iron 100 kept");

		// The screen: an empty hand opens it.
		skip.setCargo(Items.RAW_IRON, SkipCargo.CAPACITY);
		player.getInventory().clearContent();
		player.interactOn(skip, InteractionHand.MAIN_HAND, skip.position());
		helper.assertTrue(player.containerMenu instanceof SkipMenu, "an empty hand opens the skip's screen: " + player.containerMenu);
		SkipMenu menu = (SkipMenu) player.containerMenu;
		helper.assertTrue(menu.cargo() == Items.RAW_IRON && menu.count() == SkipCargo.CAPACITY, "the menu shows the slot: " + menu.count());
		List<String> clicks = new ArrayList<>();
		int[][] expected = {{SkipMenu.BUTTON_LEFT}, {SkipMenu.BUTTON_LEFT}, {SkipMenu.BUTTON_RIGHT}, {SkipMenu.BUTTON_RIGHT}, {SkipMenu.BUTTON_SHIFT}};
		for (int[] click : expected) {
			menu.clickMenuButton(player, click[0]);
			int carried = menu.getCarried().getCount();
			int inventory = player.getInventory().countItem(Items.RAW_IRON);
			helper.assertTrue(carried <= 64, "the cursor holds at most a stack: " + carried);
			helper.assertValueEqual(skip.count() + carried + inventory, SkipCargo.CAPACITY, "total after button " + click[0]);
			clicks.add("button " + click[0] + ": skip " + skip.count() + ", cursor " + carried + ", inventory " + inventory);
		}
		// Left took 64; left again put them back; right took 32; right again put 1 back; shift moved up to a stack into the inventory.
		helper.assertTrue(clicks.get(0).contains("skip 448, cursor 64"), clicks.get(0));
		helper.assertTrue(clicks.get(1).contains("skip 512, cursor 0"), clicks.get(1));
		helper.assertTrue(clicks.get(2).contains("skip 480, cursor 32"), clicks.get(2));
		helper.assertTrue(clicks.get(3).contains("skip 481, cursor 31"), clicks.get(3));
		// Shift-click on the inventory's stack puts it all back.
		menu.setCarried(ItemStack.EMPTY);
		player.getInventory().add(new ItemStack(Items.RAW_IRON, 31));
		for (int i = 0; i < menu.slots.size(); i++) {
			if (menu.slots.get(i).getItem().is(Items.RAW_IRON)) {
				menu.quickMoveStack(player, i);
			}
		}
		helper.assertValueEqual(skip.count(), SkipCargo.CAPACITY, "after shift-clicking the inventory's raw iron in");
		helper.assertValueEqual(player.getInventory().countItem(Items.RAW_IRON), 0, "raw iron left in the inventory");
		player.closeContainer();
		log.add("screen: " + clicks);

		// A freight cart still holds 256.
		Minecart cart = helper.spawn(EntityTypes.MINECART, new Vec3(4.5, 1.0625, 8.5));
		for (int i = 0; i < 5; i++) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_IRON, 64));
			player.interactOn(cart, InteractionHand.MAIN_HAND, cart.position());
		}
		helper.assertValueEqual(cartCount(cart), MinecartCargo.CAPACITY, "freight cart capacity");
		cart.discard();

		// Saved and loaded.
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		skip.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		OreSkip copy = SkipContent.ORE_SKIP.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		helper.assertTrue(copy.cargo() == Items.RAW_IRON && copy.count() == SkipCargo.CAPACITY && copy.facing() == skip.facing(), "loaded copy: " + copy);

		// Broken by a player: the skip and 512 raw iron in ordinary stacks.
		Vec3 where = skip.position();
		for (int i = 0; i < 10 && skip.isAlive(); i++) {
			skip.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		}
		helper.assertFalse(skip.isAlive(), "broken");
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(where, where).inflate(3.0));
		helper.assertValueEqual(total(items, Items.RAW_IRON), SkipCargo.CAPACITY, "raw iron dropped");
		helper.assertValueEqual(total(items, SkipContent.ORE_SKIP_ITEM), 1, "skips dropped");
		helper.assertTrue(items.stream().allMatch(item -> item.getItem().getCount() <= item.getItem().getMaxStackSize()), "every drop a normal stack");
		SimpleBlockTestLog.info("SKIP-03: {}; saved {}; broken: {}", log, tag, items.stream().map(item -> item.getItem().toString()).toList());
		items.forEach(ItemEntity::discard);
		TestPlayers.remove(player);
		helper.succeed();
	}

	// ---- SKIP-04 hoisting ----

	/**
	 * SKIP-04: the cable end (picked up under the head anchor) used on the skip hangs it: one main cable
	 * (the route's points end at the head anchor; no hook left), the coil the only cable. The hand crank
	 * lifts it (right-click, PULL) and lowers it (left-click, FEED) straight up and down. A coal winch
	 * lifts it to the head of the shaft, where it stops with its apex under the anchor and the lever goes
	 * to NEUTRAL ("end"); coal burns only while it moves; the next click skips the blocked PULL and FEED
	 * lowers it back onto the floor, where it stops again ("blocked"). A block in a side cell stops it on
	 * the way down. Sneak-use takes it off the cable only once it rests on something.
	 */
	@GameTest(structure = SHAFT, maxTicks = 1600)
	public void skipIsHoistedByCrankAndCoalWinch(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		CableDrumBlockEntity drum = layShaft(helper, player);
		OreSkip skip = spawnSkip(helper, COLUMN);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		double[] mark = new double[2];
		int[] burn = new int[2];
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				hang(helper, player, skip);
				helper.assertTrue(helper.absolutePos(DRUM).equals(skip.drum()), "hung on the drum's cable: " + skip.drum());
				helper.assertTrue(skip.getUUID().equals(line(helper).hoist()), "the line carries the skip");
				helper.assertValueEqual(CableGameTests.hooks(helper, DRUM).size(), 0, "loose ends left (the end is made fast to the bridle)");
				helper.assertValueEqual(line(helper).points().size(), 3, "points of the one main cable: drum, mid anchor, head anchor");
				helper.assertTrue(drum.hasCoil() && player.getInventory().countItem(CableContent.CABLE) == 0
					&& player.getInventory().countItem(CableContent.CABLE_COIL) == 0, "only the drum's one coil");
				mark[0] = y(helper, skip);
				log.add("hung at y " + mark[0] + " under the head anchor; one main cable through " + line(helper).points().size() + " points");
			}))
			// The hand crank: 40 ticks of right-click (pull) lifts it.
			.thenExecuteFor(40, CableGameTests.step(failed, () -> {
				if (helper.getTick() % 4 == 0) {
					CableLoopGameTests.crank(helper, player, DRUM, true);
				}
			}))
			.thenExecuteAfter(10, CableGameTests.step(failed, () -> {
				mark[1] = y(helper, skip);
				helper.assertTrue(mark[1] > mark[0] + 0.5, "the crank's pull lifts it: " + mark[0] + " -> " + mark[1]);
				Vec3 column = helper.absoluteVec(Vec3.atBottomCenterOf(COLUMN));
				helper.assertTrue(Math.abs(skip.getX() - column.x) < 1.0E-6 && Math.abs(skip.getZ() - column.z) < 1.0E-6, "straight up: " + skip.position());
				log.add("crank pull 40 ticks: y " + mark[0] + " -> " + String.format("%.2f", mark[1]));
			}))
			.thenExecute(() -> CableGameTests.teleport(helper, player, new BlockPos(3, 1, 6)))
			.thenExecuteFor(20, CableGameTests.step(failed, () -> {
				// A held left-click on the crank (the client's crank packet: only within reach of the drum).
				if (helper.getTick() % 4 == 0) {
					CableLoopGameTests.crank(helper, player, DRUM, false);
				}
			}))
			.thenExecuteAfter(10, CableGameTests.step(failed, () -> {
				double y = y(helper, skip);
				helper.assertTrue(y < mark[1] - 0.2, "the crank's feed lowers it: " + mark[1] + " -> " + y);
				log.add("crank feed 20 ticks: down to " + String.format("%.2f", y));
				// The coal winch, PULL to the head of the shaft.
				coalWinch(helper, player, 2);
				setLever(helper, player, CableLines.Lever.PULL);
			}))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever after the climb"))
			.thenExecuteAfter(5, CableGameTests.step(failed, () -> {
				double top = y(helper, skip);
				double roller = HEAD_ANCHOR.getY() + 0.75;
				helper.assertValueEqual(line(helper).lastStop(), "end", "why the winch stopped at the top");
				helper.assertTrue(Math.abs(top + OreSkip.HEIGHT - (roller - 0.3)) < 0.07, "apex just under the anchor's roller: skip at " + top);
				burn[0] = line(helper).burn() + line(helper).coal() * 1600;
				log.add("coal winch PULL: stopped at the head (y " + String.format("%.2f", top) + ", apex " + String.format("%.2f", top + 4)
					+ " under the roller at " + roller + "), lever NEUTRAL (" + line(helper).lastStop() + ")");
			}))
			.thenExecuteAfter(20, CableGameTests.step(failed, () -> {
				int now = line(helper).burn() + line(helper).coal() * 1600;
				helper.assertValueEqual(now, burn[0], "coal burnt while stopped");
				// Sneak-use while it hangs in the air: refused.
				player.setShiftKeyDown(true);
				player.interactOn(skip, InteractionHand.MAIN_HAND, skip.position());
				player.setShiftKeyDown(false);
				helper.assertTrue(skip.drum() != null, "not taken off the cable while it hangs");
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "after the stop at the top, the next click skips PULL");
				// A block in a side cell half way down.
				helper.setBlock(new BlockPos(13, 3, 5), Blocks.STONE);
			}))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever after lowering onto the side block"))
			.thenExecute(CableGameTests.step(failed, () -> {
				double y = y(helper, skip);
				helper.assertValueEqual(line(helper).lastStop(), "blocked", "why it stopped going down");
				helper.assertTrue(Math.abs(y - 4.0) < 1.0E-3, "stopped with its bottom level with the top of the side block: " + y);
				int now = line(helper).burn() + line(helper).coal() * 1600;
				helper.assertTrue(now < burn[0], "coal burnt while it moved: " + burn[0] + " -> " + now);
				log.add("FEED: stopped by a block in a side cell, bottom at " + String.format("%.3f", y) + " (" + line(helper).lastStop() + "); coal burnt "
					+ (burn[0] - now) + " ticks");
				// The block taken away: FEED stays skipped (BUG-002) until the load moves; PULL it up a little first.
				helper.setBlock(new BlockPos(13, 3, 5), Blocks.AIR);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "after the stop going down, the next click");
			}))
			.thenExecuteAfter(10, CableGameTests.step(failed, () -> setLever(helper, player, CableLines.Lever.FEED)))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever after lowering to the floor"))
			.thenExecute(CableGameTests.step(failed, () -> {
				double y = y(helper, skip);
				helper.assertTrue(Math.abs(y - 1.0) < 1.0E-3, "down on the floor: " + y);
				helper.assertValueEqual(line(helper).lastStop(), "blocked", "why it stopped at the floor");
				player.setShiftKeyDown(true);
				player.interactOn(skip, InteractionHand.MAIN_HAND, skip.position());
				player.setShiftKeyDown(false);
				helper.assertTrue(skip.drum() == null && line(helper).hoist() == null, "taken off the cable on the floor");
				log.add("FEED to the floor (" + line(helper).lastStop() + "), then taken off the cable");
			}))
			.thenExecuteAfter(2, CableGameTests.step(failed, () -> {
				helper.assertValueEqual(CableGameTests.hooks(helper, DRUM).size(), 1, "the loose end back under the anchor");
				helper.assertTrue(drum.hasCoil(), "the coil kept");
				SimpleBlockTestLog.info("SKIP-04: {}", log);
				skip.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * SKIP-05: the coal winch's lever clicked straight from PULL to FEED while the skip rises: the drive
	 * runs down through zero and the skip goes down (a speed left a hair off zero on the way through
	 * must not count as a stop). Then an anchor disconnected while the skip hangs lets it go: it comes
	 * down onto the floor with its cargo, and no cable or coil is made or lost.
	 */
	@GameTest(structure = SHAFT, maxTicks = 600)
	public void skipLetGoWhenTheRouteChanges(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		CableDrumBlockEntity drum = layShaft(helper, player);
		OreSkip skip = spawnSkip(helper, COLUMN);
		skip.setCargo(Items.COAL, 300);
		boolean[] failed = {false};
		double[] mark = new double[1];
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				hang(helper, player, skip);
				coalWinch(helper, player, 1);
				setLever(helper, player, CableLines.Lever.PULL);
			}))
			.thenExecuteAfter(30, CableGameTests.step(failed, () -> {
				helper.assertTrue(y(helper, skip) > 2.0, "lifted: " + y(helper, skip));
				// Stopped (PULL, FEED, NEUTRAL); then a short PULL of 3 ticks clicked straight to FEED: the drive runs down
				// through zero (where a float speed can be left a few billionths off zero) and the skip keeps going.
				lever(helper, player);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "stopped");
			}))
			.thenExecuteAfter(2, CableGameTests.step(failed, () -> {
				mark[0] = y(helper, skip);
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "PULL again");
			}))
			.thenExecuteAfter(3, CableGameTests.step(failed, () -> {
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "PULL, clicked: FEED");
			}))
			.thenExecuteAfter(20, CableGameTests.step(failed, () -> {
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "the lever after reversing through zero (" + line(helper).lastStop()
					+ ", skip " + skip.lastHalt() + ")");
				helper.assertTrue(y(helper, skip) < mark[0] - 0.1, "reversed: " + mark[0] + " -> " + y(helper, skip));
				// Sneak-use the head anchor: the cable winds back from it.
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				CableGameTests.useBlock(helper, player, HEAD_ANCHOR, true);
				helper.assertTrue(line(helper).hoist() == null, "the line let go of the skip");
			}))
			.thenWaitUntil(() -> helper.assertTrue(Math.abs(y(helper, skip) - 1.0) < 1.0E-3, "down on the floor: " + y(helper, skip)))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(skip.drum() == null, "the skip knows it is let go");
				helper.assertTrue(skip.cargo() == Items.COAL && skip.count() == 300, "cargo kept: " + skip);
				helper.assertTrue(drum.hasCoil(), "the coil kept");
				helper.assertValueEqual(drum.route().size(), 1, "route wound back to the mid anchor");
				SimpleBlockTestLog.info("SKIP-05: lifted, head anchor disconnected: let go, down on the floor with {}; route {}", skip, drum.route());
				skip.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- SKIP-06..08 stations ----

	/**
	 * SKIP-06: lifted by the coal winch past nothing to a height where a docked freight cart is level
	 * with its bridle, the skip stops there exactly (lever to NEUTRAL, "docked") and the cart tips its
	 * load into it through its ramp (the gate folded down, the skip's lid open), as much as the skip has
	 * room for: a skip already holding 400 raw iron takes 112 of the cart's 200 (direction by height,
	 * not by who has cargo). The doors close, the cart stays held, nothing restarts. Lowered to the floor,
	 * where an empty cart is docked level with its chute, it fills that cart (256 of its 512; the cart's
	 * front stays shut, the skip's discharge gate opens). Totals are kept throughout.
	 */
	@GameTest(structure = SHAFT, maxTicks = 1600)
	public void skipExchangesByHeightAtTheLoadingRail(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		layShaft(helper, player);
		loadingRail(helper, TOP_RAIL);
		loadingRail(helper, BOTTOM_RAIL);
		OreSkip skip = spawnSkip(helper, COLUMN);
		skip.setCargo(Items.RAW_IRON, 400);
		Minecart[] carts = new Minecart[2];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		boolean[] seen = new boolean[3];
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				// The bottom cart arrives later: first only the top station is occupied.
				carts[0] = cartOn(helper, TOP_RAIL, Items.RAW_IRON, 200);
				hang(helper, player, skip);
				coalWinch(helper, player, 2);
			}))
			.thenExecuteAfter(2, CableGameTests.step(failed, () -> {
				helper.assertTrue(helper.absolutePos(TOP_RAIL).equals(Couplings.coupled(carts[0]).simpleBlock$dock()), "top cart docked");
				setLever(helper, player, CableLines.Lever.PULL);
			}))
			.thenExecuteFor(260, () -> {
				if (carts[0] != null && LoadingTracks.freight(carts[0]).simpleBlock$gateOpen()) {
					seen[0] = true;
				}
				if (skip.lidOpen()) {
					seen[1] = true;
				}
				if (skip.gateOpen()) {
					seen[2] = true;
				}
			})
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(y(helper, skip) - TOP_STATION) < 1.0E-6, "stopped exactly at the top station: " + y(helper, skip));
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever");
				helper.assertValueEqual(line(helper).lastStop(), "docked", "why the winch stopped");
				helper.assertValueEqual(skip.count(), SkipCargo.CAPACITY, "skip after loading (400 + 112)");
				helper.assertValueEqual(cartCount(carts[0]), 88, "cart after loading (200 - 112)");
				helper.assertTrue(seen[0] && seen[1] && !seen[2], "ramp down " + seen[0] + ", lid open " + seen[1] + ", discharge gate never open " + !seen[2]);
				helper.assertFalse(LoadingTracks.freight(carts[0]).simpleBlock$gateOpen() || skip.lidOpen(), "doors closed afterwards");
				helper.assertTrue(Couplings.coupled(carts[0]).simpleBlock$dock() != null, "the cart is still held");
				log.add("PULL: stopped at y " + TOP_STATION + " (" + line(helper).lastStop() + "), cart 200 -> 88 into the skip 400 -> 512 via the ramp");
				// Now the bottom cart, empty; and down.
				carts[1] = cartOn(helper, BOTTOM_RAIL, Items.AIR, 0);
				seen[0] = seen[1] = seen[2] = false;
				lever(helper, player);
				if (line(helper).lever() != CableLines.Lever.FEED) {
					setLever(helper, player, CableLines.Lever.FEED);
				}
			}))
			.thenExecuteFor(240, () -> {
				if (carts[1] != null && LoadingTracks.freight(carts[1]).simpleBlock$gateOpen()) {
					seen[0] = true;
				}
				if (skip.lidOpen()) {
					seen[1] = true;
				}
				if (skip.gateOpen()) {
					seen[2] = true;
				}
			})
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(y(helper, skip) - 1.0) < 1.0E-6, "at the bottom: " + y(helper, skip));
				helper.assertValueEqual(cartCount(carts[1]), MinecartCargo.CAPACITY, "bottom cart filled");
				helper.assertValueEqual(skip.count(), SkipCargo.CAPACITY - MinecartCargo.CAPACITY, "skip after unloading");
				helper.assertTrue(!seen[0] && !seen[1] && seen[2], "cart front shut " + !seen[0] + ", lid shut " + !seen[1] + ", discharge gate opened " + seen[2]);
				helper.assertValueEqual(cartCount(carts[0]) + skip.count() + cartCount(carts[1]), 600, "total raw iron");
				log.add("FEED: down to the floor, skip 512 -> 256 into the empty bottom cart (front shut, discharge gate open); total 600 kept");
				SimpleBlockTestLog.info("SKIP-06: {}", log);
				for (Minecart cart : carts) {
					cart.discard();
				}
				skip.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * SKIP-07, direction negative controls: a skip standing at a station height never exchanges the
	 * wrong way. An empty skip with a loaded cart beside its chute takes nothing from it; a full skip
	 * with an empty cart beside its bridle gives it nothing. A loaded cart beside the skip's body (not
	 * its bridle or chute), a rail facing the wrong way and a cart of
	 * another commodity at the bridle all leave everything where it was.
	 */
	@GameTest(structure = SHAFT, maxTicks = 300)
	public void skipNeverExchangesTheWrongWay(GameTestHelper helper) {
		floor(helper);
		loadingRail(helper, BOTTOM_RAIL);
		OreSkip low = spawnSkip(helper, COLUMN);
		Minecart lowCart = cartOn(helper, BOTTOM_RAIL, Items.RAW_COPPER, 100);
		// A second shaft along x 18: a full skip standing at y 1 with an empty cart beside its bridle (y 4).
		BlockPos column2 = new BlockPos(18, 1, 5);
		loadingRail(helper, column2.north().above(3));
		OreSkip full = spawnSkip(helper, column2);
		full.setCargo(Items.RAW_IRON, SkipCargo.CAPACITY);
		Minecart bridleCart = cartOn(helper, column2.north().above(3), Items.AIR, 0);
		// A third at x 6: a cart beside the body (y 2), not the bridle or the chute.
		BlockPos column3 = new BlockPos(6, 1, 5);
		loadingRail(helper, column3.north().above(1));
		OreSkip body = spawnSkip(helper, column3);
		Minecart bodyCart = cartOn(helper, column3.north().above(1), Items.RAW_IRON, 100);
		// A fourth at x 2: a rail at the bridle facing away from the skip, and a fifth at x 22 with coal at the bridle of an iron skip.
		BlockPos column4 = new BlockPos(2, 1, 9);
		BlockPos rail4 = column4.north().above(3);
		helper.setBlock(rail4.below(), Blocks.STONE);
		helper.setBlock(rail4, TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, Direction.NORTH)
			.setValue(LoadingTrackBlock.SHAPE, RailShape.NORTH_SOUTH));
		OreSkip away = spawnSkip(helper, column4);
		Minecart awayCart = cartOn(helper, rail4, Items.RAW_IRON, 100);
		BlockPos column5 = new BlockPos(22, 1, 9);
		loadingRail(helper, column5.north().above(3));
		OreSkip mixed = spawnSkip(helper, column5);
		mixed.setCargo(Items.RAW_IRON, 10);
		Minecart coalCart = cartOn(helper, column5.north().above(3), Items.COAL, 100);
		helper.startSequence()
			.thenIdle(80)
			.thenExecute(() -> {
				helper.assertTrue(Couplings.coupled(lowCart).simpleBlock$dock() != null && Couplings.coupled(bridleCart).simpleBlock$dock() != null,
					"carts docked");
				helper.assertValueEqual(low.count(), 0, "empty skip beside a loaded cart at its chute");
				helper.assertValueEqual(cartCount(lowCart), 100, "the loaded cart at the chute");
				helper.assertValueEqual(full.count(), SkipCargo.CAPACITY, "full skip beside an empty cart at its bridle");
				helper.assertValueEqual(cartCount(bridleCart), 0, "the empty cart at the bridle");
				helper.assertTrue(body.count() == 0 && cartCount(bodyCart) == 100, "cart beside the body: skip " + body.count() + ", cart " + cartCount(bodyCart));
				helper.assertTrue(away.count() == 0 && cartCount(awayCart) == 100, "rail facing away: skip " + away.count() + ", cart " + cartCount(awayCart));
				helper.assertTrue(mixed.count() == 10 && cartCount(coalCart) == 100, "coal at an iron skip: skip " + mixed + ", cart " + cartCount(coalCart));
				helper.assertFalse(SkipStations.busy(low) || SkipStations.busy(full) || SkipStations.busy(body) || SkipStations.busy(away)
					|| SkipStations.busy(mixed), "no exchange running");
				SimpleBlockTestLog.info("SKIP-07: 80 ticks: empty skip/loaded cart at the chute 0/100; full skip/empty cart at the bridle 512/0; "
					+ "cart at the body 0/100; rail facing away 0/100; coal cart at an iron skip 10/100");
				for (var entity : List.of(lowCart, bridleCart, bodyCart, awayCart, coalCart, low, full, body, away, mixed)) {
					entity.discard();
				}
			})
			.thenSucceed();
	}

	/**
	 * SKIP-08: an exchange cut short is lossless. The cart at the bridle is released half way through:
	 * the exchange stops, the doors close, and cart + skip still hold what they held together. A cart
	 * with no room for the skip's commodity takes only what fits (partial), and the rest stays in the
	 * skip.
	 */
	@GameTest(structure = SHAFT, maxTicks = 400)
	public void skipExchangeCutShortLosesNothing(GameTestHelper helper) {
		floor(helper);
		BlockPos rail = COLUMN.north().above(3);
		loadingRail(helper, rail);
		OreSkip skip = spawnSkip(helper, COLUMN);
		Minecart cart = cartOn(helper, rail, Items.RAW_GOLD, 256);
		BlockPos column2 = new BlockPos(18, 1, 5);
		loadingRail(helper, column2.north());
		OreSkip giver = spawnSkip(helper, column2);
		giver.setCargo(Items.RAW_GOLD, 300);
		Minecart partial = cartOn(helper, column2.north(), Items.RAW_GOLD, 200);
		int[] moved = new int[1];
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(skip.count() > 20, "loading under way: " + skip.count()))
			.thenExecute(() -> {
				LoadingTracks.releaseTrain(cart, helper.absolutePos(rail));
				moved[0] = skip.count();
			})
			.thenExecuteAfter(30, () -> {
				helper.assertValueEqual(skip.count() + cartCount(cart), 256, "gold in the skip and the released cart");
				helper.assertTrue(skip.count() <= moved[0] + 4, "nothing more moved after the release: " + moved[0] + " -> " + skip.count());
				helper.assertFalse(SkipStations.busy(skip) || skip.lidOpen() || LoadingTracks.freight(cart).simpleBlock$gateOpen(), "doors closed");
				helper.assertValueEqual(cartCount(partial), MinecartCargo.CAPACITY, "the partial cart filled to 256");
				helper.assertValueEqual(giver.count(), 300 - 56, "the rest stays in the skip");
				SimpleBlockTestLog.info("SKIP-08: released after {} of 256: skip {} + cart {}; partial: cart 200 -> 256, skip 300 -> {}", moved[0],
					skip.count(), cartCount(cart), giver.count());
				for (var entity : List.of(cart, partial, skip, giver)) {
					entity.discard();
				}
			})
			.thenSucceed();
	}
}
