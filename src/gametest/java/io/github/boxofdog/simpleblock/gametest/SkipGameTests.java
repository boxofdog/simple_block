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

import io.github.boxofdog.simpleblock.cable.CableAnchorBlock;
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
	static final String SHAFT = "simple_block-gametest:skip_shaft";
	private static final BlockPos DRUM = new BlockPos(2, 1, 5);
	private static final BlockPos MID_ANCHOR = new BlockPos(7, 6, 5);
	private static final BlockPos HEAD_ANCHOR = new BlockPos(12, 11, 5);
	static final BlockPos COLUMN = new BlockPos(12, 1, 5);
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
	static Minecart cartOn(GameTestHelper helper, BlockPos rail, Item item, int count) {
		Minecart cart = helper.spawn(EntityTypes.MINECART, Vec3.atBottomCenterOf(rail).add(0.0, 0.0625, 0.0));
		cart.setDeltaMovement(Vec3.ZERO);
		MinecartCargo.cart(cart).simpleBlock$setCargo(item, count);
		return cart;
	}

	private static int cartCount(Minecart cart) {
		return MinecartCargo.cart(cart).simpleBlock$count();
	}

	/** The shaft's drum, anchors and route (the cable end put down under the head anchor). Run at least a tick after {@link #layShaft}. */
	static void route(GameTestHelper helper, ServerPlayer player) {
		// Next to the drum, where its loose end lies (the end only follows the hand between ticks).
		CableGameTests.teleport(helper, player, new BlockPos(3, 1, 6));
		CableLoopGameTests.route(helper, player, DRUM, MID_ANCHOR, HEAD_ANCHOR);
	}

	static CableDrumBlockEntity layShaft(GameTestHelper helper, ServerPlayer player) {
		floor(helper);
		CableDrumBlockEntity drum = CableGameTests.windDrum(helper, player, DRUM, 1);
		CableGameTests.hangAnchor(helper, player, MID_ANCHOR.above());
		CableGameTests.hangAnchor(helper, player, HEAD_ANCHOR.above());
		return drum;
	}

	/** Picks up the cable end from under the head anchor and uses it on the skip. */
	static void hang(GameTestHelper helper, ServerPlayer player, OreSkip skip) {
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
	/**
	 * The lever's next setting in the old M06 cycle, NEUTRAL, PULL, FEED, NEUTRAL, made with the owner's
	 * clicks (2026-10-13): from NEUTRAL a right-click (PULL); from PULL a left-click (FEED); from FEED a
	 * left-click again (NEUTRAL).
	 */
	private static void lever(GameTestHelper helper, ServerPlayer player) {
		if (line(helper).lever() == CableLines.Lever.NEUTRAL) {
			pullClick(helper, player);
		} else {
			feedClick(helper, player);
		}
	}

	/** A right-click on the coal winch's lever: PULL, or NEUTRAL if it was at PULL. */
	private static void pullClick(GameTestHelper helper, ServerPlayer player) {
		BlockPos pos = helper.absolutePos(DRUM);
		BlockState state = helper.getLevel().getBlockState(pos);
		Direction side = CableDrumBlock.crankSide(state);
		Vec3 hit = Vec3.atLowerCornerOf(pos).add(0.5 + side.getStepX() * 0.55, 9.5 / 16.0, 0.5 + side.getStepZ() * 0.55);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(hit, side, pos, false));
	}

	/** A left-click on the coal winch's lever (as the client sends it): FEED, or NEUTRAL if it was at FEED. */
	private static void feedClick(GameTestHelper helper, ServerPlayer player) {
		BlockPos pos = helper.absolutePos(DRUM);
		BlockState state = helper.getLevel().getBlockState(pos);
		Direction side = CableDrumBlock.crankSide(state);
		Vec3 hit = Vec3.atLowerCornerOf(pos).add(0.5 + side.getStepX() * 0.55, 9.5 / 16.0, 0.5 + side.getStepZ() * 0.55);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		CoalWinchGameTests.leftClickLever(helper.getLevel(), pos, player);
	}

	/** Fits a furnace and {@code coal} coal. */
	private static void coalWinch(GameTestHelper helper, ServerPlayer player, int coal) {
		body(helper, player, new ItemStack(Items.FURNACE));
		for (int i = 0; i < coal; i++) {
			body(helper, player, new ItemStack(Items.COAL));
		}
	}

	/** Sets the lever to {@code lever} with the owner's clicks: right for PULL, left for FEED, the same click again for NEUTRAL. */
	private static void setLever(GameTestHelper helper, ServerPlayer player, CableLines.Lever lever) {
		CableLines.Lever now = line(helper).lever();
		if (now != lever) {
			if (lever == CableLines.Lever.PULL || lever == CableLines.Lever.NEUTRAL && now == CableLines.Lever.PULL) {
				pullClick(helper, player);
			} else {
				feedClick(helper, player);
			}
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
	 * player's way back. Owner's playtest patch §3: placed, it is only its body, a hitbox one block wide
	 * and deep and three high, with no bridle (a block in the fourth cell does not stop it), solid to
	 * other entities. It is refused (and the item kept) where a block is in its body's cells or in a side
	 * cell beside them; a block in front or behind is fine.
	 */
	@GameTest(structure = SHAFT)
	public void skipNeedsItsColumnAndSideCells(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(12, 1, 2));
		player.setYRot(0.0F);
		List<String> log = new ArrayList<>();
		BlockPos[] obstacles = {new BlockPos(13, 2, 5), new BlockPos(11, 3, 5), new BlockPos(12, 3, 5)};
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
		// The fourth cell (where a bridle would be) and beside it: no bridle yet, so no matter.
		helper.setBlock(COLUMN.above(3), Blocks.STONE);
		helper.setBlock(COLUMN.above(3).east(), Blocks.STONE);
		CableGameTests.useBlock(helper, player, COLUMN.below(), Direction.UP);
		List<OreSkip> skips = helper.getLevel().getEntitiesOfClass(OreSkip.class, new AABB(helper.absolutePos(COLUMN)).inflate(3.0));
		helper.assertValueEqual(skips.size(), 1, "skips placed with blocks in front, behind and over the body");
		OreSkip skip = skips.getFirst();
		AABB box = skip.getBoundingBox();
		helper.assertValueEqual(skip.facing(), Direction.NORTH, "facing (front towards the player, who faced south)");
		helper.assertTrue(Math.abs(box.getXsize() - 1.0) < 1.0E-6 && Math.abs(box.getZsize() - 1.0) < 1.0E-6 && Math.abs(box.getYsize() - 3.0) < 1.0E-6,
			"hitbox 1 x 3 x 1 (no bridle): " + box);
		helper.assertFalse(skip.bridled(), "placed without a bridle");
		helper.assertTrue(Math.abs(box.minY - helper.absolutePos(COLUMN).getY()) < 1.0E-6, "standing on the block it was placed on: " + box);
		helper.assertTrue(skip.canBeCollidedWith(player), "solid to other entities");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "the item used up");
		helper.assertValueEqual(skip.lateral().getAxis(), Direction.Axis.X, "side cells along x (across its front)");
		SimpleBlockTestLog.info("SKIP-02: {}; placed with blocks in front, behind and over the body: box {} (no bridle), facing {}, side cells along {}", log,
			box, skip.facing(), skip.lateral().getAxis());
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
			MinecartCargoGameTests.holdUse(player, cart, InteractionHand.MAIN_HAND);
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
				helper.assertTrue(Math.abs(top + OreSkip.HEIGHT - (roller - io.github.boxofdog.simpleblock.cable.CableHoists.APEX_GAP)) < 0.07,
					"apex just under the head anchor's pulley wheel: skip at " + top);
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
				feedClick(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "after the stop at the top, a left-click");
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
				// The block taken away: PULL it up a little, then FEED again.
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
				setLever(helper, player, CableLines.Lever.NEUTRAL);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "stopped");
			}))
			.thenExecuteAfter(2, CableGameTests.step(failed, () -> {
				mark[0] = y(helper, skip);
				pullClick(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "PULL again");
			}))
			.thenExecuteAfter(3, CableGameTests.step(failed, () -> {
				feedClick(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "PULL, left-clicked: FEED");
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

	/**
	 * M07 playtest (owner): a skip with no cable drops like a heavy anchor and clangs on landing, never
	 * before. A loaded skip (512 raw iron) let fall from 8 blocks up gains speed every tick, lands on the
	 * floor much sooner than the old steady 0.25 a tick, and clangs once, on landing; dropped again onto a
	 * ledge part way down it lands on the ledge and clangs; the ledge taken away, it falls the rest; a
	 * half-block drop still clangs, while a skip standing on the floor stays put and silent. Saved and
	 * loaded mid-fall it still comes down and lands. The cargo stays in it throughout.
	 */
	@GameTest(structure = SHAFT, maxTicks = 1200)
	public void droppedSkipFallsLikeAnAnchor(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		layShaft(helper, player);
		OreSkip[] skip = {spawnSkip(helper, COLUMN)};
		BlockPos ledge = COLUMN.above(3);
		int[] ticks = {0};
		int[] fall = new int[2];
		int[] early = {0};
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		Runnable lift = () -> {
			Vec3 at = helper.absoluteVec(new Vec3(COLUMN.getX() + 0.5, 9.0, COLUMN.getZ() + 0.5));
			skip[0].setPos(at.x, at.y, at.z);
			ticks[0] = 0;
		};
		helper.startSequence()
			.thenExecute(CableGameTests.step(failed, () -> {
				skip[0].setCargo(Items.RAW_IRON, SkipCargo.CAPACITY);
				lift.run();
			}))
			// The long fall: no clang until it lands.
			.thenWaitUntil(() -> {
				ticks[0]++;
				if (Math.abs(y(helper, skip[0]) - 1.0) > 1.0E-3) {
					early[0] += skip[0].landings();
					helper.assertTrue(false, "falling: " + y(helper, skip[0]));
				}
			})
			.thenExecute(CableGameTests.step(failed, () -> {
				fall[0] = ticks[0];
				helper.assertValueEqual(early[0], 0, "clangs while still falling");
				helper.assertValueEqual(skip[0].landings(), 1, "clangs on landing");
				log.add("8 blocks onto the floor in " + fall[0] + " ticks (at the old steady 0.25 a tick: 32), one clang, on landing");
				helper.setBlock(ledge, Blocks.STONE);
				lift.run();
			}))
			.thenWaitUntil(() -> helper.assertTrue(skip[0].landings() == 2, "waiting to land on the ledge"))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(y(helper, skip[0]) - (ledge.getY() + 1.0)) < 1.0E-3, "resting on the ledge: " + y(helper, skip[0]));
				helper.assertValueEqual(skip[0].landings(), 2, "one clang on the ledge");
				log.add("dropped onto a ledge 3 up: lands on it, one clang");
				helper.setBlock(ledge, Blocks.AIR);
			}))
			.thenWaitUntil(() -> helper.assertTrue(skip[0].landings() == 3 && Math.abs(y(helper, skip[0]) - 1.0) < 1.0E-3, "waiting to land on the floor"))
			.thenExecute(CableGameTests.step(failed, () -> {
				Vec3 at = helper.absoluteVec(new Vec3(COLUMN.getX() + 0.5, 1.5, COLUMN.getZ() + 0.5));
				skip[0].setPos(at.x, at.y, at.z);
			}))
			.thenIdle(10)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(Math.abs(y(helper, skip[0]) - 1.0) < 1.0E-3, "down from half a block");
				helper.assertValueEqual(skip[0].landings(), 4, "a half-block drop clangs too");
				log.add("ledge taken away: falls the rest, clang; a half-block drop: clang");
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(skip[0].landings(), 4, "standing on the floor: no more clangs");
				helper.assertTrue(Math.abs(y(helper, skip[0]) - 1.0) < 1.0E-3, "standing on the floor: stays put");
				log.add("standing on the floor 40 ticks: put, silent");
				// Saved and loaded in mid-fall.
				lift.run();
			}))
			.thenIdle(4)
			.thenExecute(CableGameTests.step(failed, () -> {
				ServerLevel level = helper.getLevel();
				double before = y(helper, skip[0]);
				TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
				skip[0].saveWithoutId(output);
				OreSkip copy = SkipContent.ORE_SKIP.create(level, EntitySpawnReason.LOAD);
				copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
				skip[0].discard();
				level.addFreshEntity(copy);
				skip[0] = copy;
				helper.assertTrue(before > 1.5, "saved in mid-fall: " + before);
				log.add(String.format("saved and loaded at %.2f in mid-fall", before));
			}))
			.thenWaitUntil(() -> helper.assertTrue(skip[0].landings() == 1 && Math.abs(y(helper, skip[0]) - 1.0) < 1.0E-3, "waiting for the loaded skip to land"))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(skip[0].cargo() == Items.RAW_IRON && skip[0].count() == SkipCargo.CAPACITY, "cargo kept: " + skip[0]);
				helper.assertTrue(fall[0] <= 18, "the 8-block fall should take far fewer than the old 32 ticks: " + fall[0]);
				log.add("loaded copy lands with a clang; 512 raw iron in it throughout");
				SimpleBlockTestLog.info("M07 heavy skip drop: {}", log);
				skip[0].discard();
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
	 * not by who has cargo). The doors close, the cart is let go and the stop folds (M07 playtest), nothing restarts. Lowered towards the
	 * floor, where an empty cart is docked one level below its chute (owner's playtest patch §1), it stops
	 * with the chute's lip on the cart's rim ({@link SkipStations#UNLOAD_OFFSET} above the rail) and fills
	 * that cart (256 of its 512; the cart's front stays shut, the skip's discharge gate opens). Totals are
	 * kept throughout.
	 */
	@GameTest(structure = SHAFT, maxTicks = 2200)
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
			.thenExecuteFor(360, () -> {
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
				// M07 playtest: the exchange finished, so the cart is let go and the stop folds.
				helper.assertTrue(Couplings.coupled(carts[0]).simpleBlock$dock() == null, "the cart should be let go after its finished exchange");
				helper.assertFalse(helper.getBlockState(TOP_RAIL).getValue(LoadingTrackBlock.RAISED), "the stop should fold");
				log.add("PULL: stopped at y " + TOP_STATION + " (" + line(helper).lastStop() + "), cart 200 -> 88 into the skip 400 -> 512 via the ramp");
				// Now the bottom cart, empty; and down.
				carts[1] = cartOn(helper, BOTTOM_RAIL, Items.AIR, 0);
				seen[0] = seen[1] = seen[2] = false;
				lever(helper, player);
				if (line(helper).lever() != CableLines.Lever.FEED) {
					setLever(helper, player, CableLines.Lever.FEED);
				}
			}))
			.thenExecuteFor(420, () -> {
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
				// Patch §1: it stops with its chute's lip on the cart's rim, the cart one level lower, kitty-corner.
				helper.assertTrue(Math.abs(y(helper, skip) - (1.0 + SkipStations.UNLOAD_OFFSET)) < 1.0E-6, "at the unloading station: " + y(helper, skip));
				helper.assertValueEqual(cartCount(carts[1]), MinecartCargo.CAPACITY, "bottom cart filled");
				helper.assertValueEqual(skip.count(), SkipCargo.CAPACITY - MinecartCargo.CAPACITY, "skip after unloading");
				helper.assertTrue(!seen[0] && !seen[1] && seen[2], "cart front shut " + !seen[0] + ", lid shut " + !seen[1] + ", discharge gate opened " + seen[2]);
				helper.assertValueEqual(cartCount(carts[0]) + skip.count() + cartCount(carts[1]), 600, "total raw iron");
				log.add("FEED: down to the unloading station (bottom 1 + 11/16, lip on the rim), skip 512 -> 256 into the empty bottom cart (front shut, discharge gate open); total 600 kept");
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
	 * M07 playtest (owner): a skip wound up past a Loading Rail at y 5 first stops where the empty cart
	 * there is level with its bridle; nothing to move, so the cart stays docked, its stop up. Wound on
	 * up, it stops with its chute over the cart and fills it (256 of its 300); the exchange finished,
	 * the cart is let go and the stop folds. Fed back down past the cart's bridle height, it no longer
	 * stops there and takes nothing back: before this change the still-docked cart would have tipped its
	 * 256 straight back into the skip.
	 */
	@GameTest(structure = SHAFT, maxTicks = 1600)
	public void finishedExchangeLetsTheCartGo(GameTestHelper helper) {
		BlockPos rail = new BlockPos(12, 5, 4);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		layShaft(helper, player);
		loadingRail(helper, rail);
		OreSkip skip = spawnSkip(helper, COLUMN);
		skip.setCargo(Items.RAW_IRON, 300);
		Minecart[] cart = new Minecart[1];
		double load = rail.getY() - SkipStations.LOAD_OFFSET;
		double unload = rail.getY() + SkipStations.UNLOAD_OFFSET;
		List<String> log = new ArrayList<>();
		double[] lowest = {Double.MAX_VALUE};
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				cart[0] = cartOn(helper, rail, Items.AIR, 0);
				hang(helper, player, skip);
				coalWinch(helper, player, 2);
			}))
			.thenExecuteAfter(2, CableGameTests.step(failed, () -> setLever(helper, player, CableLines.Lever.PULL)))
			.thenWaitUntil(() -> helper.assertTrue(line(helper).lever() == CableLines.Lever.NEUTRAL && Math.abs(y(helper, skip) - load) < 1.0E-6,
				"waiting for the stop at the cart's bridle height"))
			.thenIdle(30)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(helper.absolutePos(rail).equals(Couplings.coupled(cart[0]).simpleBlock$dock()), "nothing to move: the cart stays docked");
				helper.assertTrue(helper.getBlockState(rail).getValue(LoadingTrackBlock.RAISED), "nothing to move: the stop stays up");
				log.add("up: stopped at the empty cart's bridle height " + load + ", nothing moved, cart still docked, stop up");
				setLever(helper, player, CableLines.Lever.PULL);
			}))
			.thenWaitUntil(() -> helper.assertTrue(line(helper).lever() == CableLines.Lever.NEUTRAL && Math.abs(y(helper, skip) - unload) < 1.0E-6,
				"waiting for the stop with the chute over the cart"))
			.thenWaitUntil(() -> helper.assertTrue(Couplings.coupled(cart[0]).simpleBlock$dock() == null, "waiting for the finished exchange to let the cart go"))
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(cartCount(cart[0]), MinecartCargo.CAPACITY, "cart filled");
				helper.assertValueEqual(skip.count(), 300 - MinecartCargo.CAPACITY, "skip after unloading");
				helper.assertFalse(helper.getBlockState(rail).getValue(LoadingTrackBlock.RAISED), "the stop should fold");
				helper.assertTrue(helper.absolutePos(rail).equals(LoadingTracks.freight(cart[0]).simpleBlock$released()), "the cart is marked released from this rail");
				log.add("up again: stopped with the chute over the cart (" + unload + "), 256 into it, then the cart let go and the stop folded");
				setLever(helper, player, CableLines.Lever.FEED);
			}))
			.thenExecuteFor(300, () -> lowest[0] = Math.min(lowest[0], y(helper, skip)))
			.thenExecute(CableGameTests.step(failed, () -> {
				log.add(String.format("fed down: lowest bottom %.2f (the cart's bridle height %s passed), cart %d, skip %d, last stop %s", lowest[0], load,
					cartCount(cart[0]), skip.count(), line(helper).lastStop()));
				SimpleBlockTestLog.info("M07 stop folds after a finished exchange: {}", log);
				helper.assertTrue(lowest[0] < load - 0.5, "the skip should pass the released cart's bridle height: lowest " + lowest[0]);
				helper.assertValueEqual(cartCount(cart[0]), MinecartCargo.CAPACITY, "the cart keeps its load");
				helper.assertValueEqual(skip.count(), 300 - MinecartCargo.CAPACITY, "the skip took nothing back");
				cart[0].discard();
				skip.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/** A hung skip in the shaft (route, cable end used on it), held still at {@code bottom} (relative), with the given cargo. */
	static void holdAt(GameTestHelper helper, OreSkip skip, double bottom, Item item, int count) {
		Vec3 at = helper.absoluteVec(new Vec3(COLUMN.getX() + 0.5, bottom, COLUMN.getZ() + 0.5));
		skip.setPos(at.x, at.y, at.z);
		skip.setCargo(item, count);
	}

	private static void clearRail(GameTestHelper helper, BlockPos rail) {
		helper.setBlock(rail, Blocks.AIR);
		if (rail.getY() > 1) {
			helper.setBlock(rail.below(), Blocks.AIR);
		}
	}

	/** One negative case of SKIP-07: what is set up, and the rail it used. */
	private record Case(String name, double bottom, BlockPos rail, Direction railFacing, Item skipItem, int skipCount, Item cartItem, int cartCount,
		BlockPos solid) {
	}

	/**
	 * SKIP-07, direction and geometry negative controls, with a hung skip (its bridle on) held still at
	 * each height. Nothing moves, whatever either holds:
	 * <ol>
	 *   <li>the M07 build's old unloading place, a cart beside the skip's bottom block at the same level
	 *       (owner's playtest patch §1: the chute cannot reach it), with a full skip and an empty cart;</li>
	 *   <li>an empty skip at the unloading place with a loaded cart below its chute: it never takes;</li>
	 *   <li>a full skip with an empty cart level with its bridle: it never gives;</li>
	 *   <li>a loaded cart beside the skip's body;</li>
	 *   <li>a rail level with the bridle beside the skip instead of in front of it (any rail in front
	 *       works, end-on or side-on, since every cart has a gate on each side: owner's cart redesign);</li>
	 *   <li>a coal cart beside the bridle of a skip holding raw iron;</li>
	 *   <li>a solid block over the cart in the chute's way at the unloading place.</li>
	 * </ol>
	 * And an unhung skip (no bridle) standing exactly at the unloading place has no station at all
	 * (patch §1: only a hung skip unloads), while the same skip hung does.
	 */
	@GameTest(structure = SHAFT, maxTicks = 1200)
	public void skipNeverExchangesTheWrongWay(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		layShaft(helper, player);
		OreSkip skip = spawnSkip(helper, COLUMN);
		double unload2 = 2.0 + SkipStations.UNLOAD_OFFSET;
		double unload1 = 1.0 + SkipStations.UNLOAD_OFFSET;
		List<Case> cases = List.of(
			new Case("same level as the chute", 1.0, new BlockPos(12, 1, 4), Direction.SOUTH, Items.RAW_IRON, 300, Items.AIR, 0, null),
			new Case("empty skip, loaded cart below the chute", unload2, new BlockPos(12, 2, 4), Direction.SOUTH, Items.AIR, 0, Items.RAW_IRON, 100, null),
			new Case("full skip, empty cart at the bridle", 1.0, new BlockPos(12, 4, 4), Direction.SOUTH, Items.RAW_IRON, 512, Items.AIR, 0, null),
			new Case("loaded cart beside the body", 1.0, new BlockPos(12, 2, 4), Direction.SOUTH, Items.AIR, 0, Items.RAW_IRON, 100, null),
			new Case("rail at the bridle beside the skip, not in front", 1.0, new BlockPos(13, 4, 5), Direction.SOUTH, Items.AIR, 0, Items.RAW_IRON, 100, null),
			new Case("coal cart at an iron skip's bridle", 1.0, new BlockPos(12, 4, 4), Direction.SOUTH, Items.RAW_IRON, 10, Items.COAL, 100, null),
			new Case("a block over the cart in the chute's way", unload1, new BlockPos(12, 1, 4), Direction.SOUTH, Items.RAW_IRON, 300, Items.AIR, 0,
				new BlockPos(12, 2, 4)));
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		Minecart[] cart = new Minecart[1];
		var sequence = helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				hang(helper, player, skip);
				helper.assertTrue(skip.bridled(), "hung");
				// Unhung versus hung at the unloading place, by the station rule itself.
				loadingRail(helper, BOTTOM_RAIL);
				cart[0] = cartOn(helper, BOTTOM_RAIL, Items.AIR, 0);
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				OreSkip loose = spawnSkip(helper, new BlockPos(18, 1, 5));
				Vec3 at = helper.absoluteVec(new Vec3(12.5, 1.0 + SkipStations.UNLOAD_OFFSET, 5.5));
				loose.setPos(at.x, at.y, at.z);
				boolean looseStation = SkipStations.station(helper.getLevel(), loose, loose.getY()) != null;
				loose.discard();
				holdAt(helper, skip, 1.0 + SkipStations.UNLOAD_OFFSET, Items.AIR, 0);
				boolean hungStation = SkipStations.station(helper.getLevel(), skip, skip.getY()) != null;
				helper.assertTrue(!looseStation && hungStation, "unloading place: unhung skip station " + looseStation + ", hung " + hungStation);
				log.add("unloading place: no station for an unhung skip, a station for the hung one");
				cart[0].discard();
				clearRail(helper, BOTTOM_RAIL);
				helper.setBlock(BOTTOM_RAIL.below(), Blocks.STONE);
			}));
		for (Case c : cases) {
			sequence = sequence
				.thenExecute(CableGameTests.step(failed, () -> {
					helper.setBlock(c.rail().below(), Blocks.STONE);
					helper.setBlock(c.rail(), TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, c.railFacing())
						.setValue(LoadingTrackBlock.SHAPE, RailShape.NORTH_SOUTH));
					if (c.solid() != null) {
						helper.setBlock(c.solid(), Blocks.STONE);
					}
					cart[0] = cartOn(helper, c.rail(), c.cartItem(), c.cartCount());
					holdAt(helper, skip, c.bottom(), c.skipItem(), c.skipCount());
				}))
				.thenIdle(40)
				.thenExecute(CableGameTests.step(failed, () -> {
					helper.assertTrue(Couplings.coupled(cart[0]).simpleBlock$dock() != null, c.name() + ": the cart docked");
					helper.assertTrue(skip.count() == c.skipCount() && cartCount(cart[0]) == c.cartCount(), c.name() + ": skip " + skip.count() + ", cart "
						+ cartCount(cart[0]));
					helper.assertFalse(SkipStations.busy(skip) || skip.lidOpen() || skip.gateOpen() || LoadingTracks.freight(cart[0]).simpleBlock$gateOpen(),
						c.name() + ": no door opened");
					log.add(c.name() + ": skip " + skip.count() + ", cart " + cartCount(cart[0]));
					cart[0].discard();
					if (c.solid() != null) {
						helper.setBlock(c.solid(), Blocks.AIR);
					}
					clearRail(helper, c.rail());
					helper.setBlock(new BlockPos(12, 0, 4), Blocks.STONE);
				}));
		}
		sequence.thenExecute(CableGameTests.step(failed, () -> {
			SimpleBlockTestLog.info("SKIP-07: {}", log);
			skip.discard();
			TestPlayers.remove(player);
		})).thenSucceed();
	}

	/**
	 * SKIP-08: an exchange cut short is lossless. The cart at the bridle is released half way through:
	 * the exchange stops, the doors close, and cart + skip still hold what they held together. At the
	 * unloading place a cart with room for only part of the skip's load takes what fits (partial), and the
	 * rest stays in the skip.
	 */
	@GameTest(structure = SHAFT, maxTicks = 800)
	public void skipExchangeCutShortLosesNothing(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		layShaft(helper, player);
		loadingRail(helper, TOP_RAIL);
		OreSkip skip = spawnSkip(helper, COLUMN);
		Minecart[] carts = new Minecart[2];
		int[] moved = new int[1];
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				hang(helper, player, skip);
				holdAt(helper, skip, TOP_STATION, Items.AIR, 0);
				carts[0] = cartOn(helper, TOP_RAIL, Items.RAW_GOLD, 256);
			}))
			.thenWaitUntil(() -> helper.assertTrue(skip.count() > 20, "loading under way: " + skip.count()))
			.thenExecute(() -> {
				LoadingTracks.releaseTrain(carts[0], helper.absolutePos(TOP_RAIL));
				moved[0] = skip.count();
			})
			.thenExecuteAfter(30, CableGameTests.step(failed, () -> {
				helper.assertValueEqual(skip.count() + cartCount(carts[0]), 256, "gold in the skip and the released cart");
				helper.assertTrue(skip.count() <= moved[0] + 4, "nothing more moved after the release: " + moved[0] + " -> " + skip.count());
				helper.assertFalse(SkipStations.busy(skip) || skip.lidOpen() || LoadingTracks.freight(carts[0]).simpleBlock$gateOpen(), "doors closed");
				moved[0] = skip.count();
				carts[0].discard();
				// Partial: at the unloading place, a cart with 200 takes 56 of the skip's 300.
				loadingRail(helper, BOTTOM_RAIL);
				carts[1] = cartOn(helper, BOTTOM_RAIL, Items.RAW_GOLD, 200);
				holdAt(helper, skip, 1.0 + SkipStations.UNLOAD_OFFSET, Items.RAW_GOLD, 300);
			}))
			.thenExecuteAfter(120, CableGameTests.step(failed, () -> {
				helper.assertValueEqual(cartCount(carts[1]), MinecartCargo.CAPACITY, "the partial cart filled to 256");
				helper.assertValueEqual(skip.count(), 300 - 56, "the rest stays in the skip");
				SimpleBlockTestLog.info("SKIP-08: released mid-way: skip {} + cart {} = 256, nothing more moved, doors closed; partial at the unloading place: "
					+ "cart 200 -> 256, skip 300 -> {}", moved[0], 256 - moved[0], skip.count());
				carts[1].discard();
				skip.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- SKIP-09..11 owner's playtest patch ----

	private static BlockState headAnchor(GameTestHelper helper) {
		return helper.getLevel().getBlockState(helper.absolutePos(HEAD_ANCHOR));
	}

	private static CableAnchorBlock.Return headReturn(GameTestHelper helper) {
		return headAnchor(helper).getValue(CableAnchorBlock.RETURN);
	}

	/** The free hook's eye (where the cable meets it), relative y. */
	private static double hookEye(GameTestHelper helper) {
		var hook = CableGameTests.hook(helper, DRUM);
		return hook.getY() + hook.getBbHeight() - helper.absoluteVec(Vec3.ZERO).y;
	}

	/**
	 * SKIP-09 (patch §3, §4): the bridle and the pulley come with the cable. A placed skip has no bridle
	 * (1 x 3). Hooking the cable on with a block in the fourth cell is refused: no bridle, the end stays in
	 * the hand, nothing used, the head anchor stays an ordinary anchor. Without the block, hooking on adds
	 * the bridle (1 x 4, collidable) and turns the head anchor into a pulley facing the way the cable comes
	 * in; saved and loaded, the skip still has its bridle. Taken off on the floor: no bridle, the anchor
	 * an anchor again, and the end hangs where the bridle's apex was (not back at the top). Disconnecting
	 * the route at the anchor before it does the same, and lets the skip go.
	 */
	@GameTest(structure = SHAFT, maxTicks = 400)
	public void skipBridleAndPulleyComeWithTheCable(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		layShaft(helper, player);
		OreSkip skip = spawnSkip(helper, COLUMN);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				helper.assertTrue(!skip.bridled() && Math.abs(skip.getBoundingBox().getYsize() - 3.0) < 1.0E-6, "placed: no bridle, 1 x 3");
				helper.assertValueEqual(headReturn(helper), CableAnchorBlock.Return.NONE, "head anchor before hooking on");
				helper.setBlock(COLUMN.above(3), Blocks.STONE);
				hang(helper, player, skip);
				var hook = CableGameTests.hook(helper, DRUM);
				helper.assertFalse(skip.bridled(), "no bridle with a block in the fourth cell");
				helper.assertTrue(hook.isHeld() && line(helper).hoist() == null, "the end still in the hand, nothing hung");
				helper.assertValueEqual(headReturn(helper), CableAnchorBlock.Return.NONE, "head anchor after the refused hook-on");
				log.add("block in the fourth cell: refused, end still held, anchor ordinary");
				helper.setBlock(COLUMN.above(3), Blocks.AIR);
				hang(helper, player, skip);
				helper.assertTrue(skip.bridled() && Math.abs(skip.getBoundingBox().getYsize() - 4.0) < 1.0E-6, "hooked on: bridle, 1 x 4");
				helper.assertValueEqual(headReturn(helper), CableAnchorBlock.Return.PULLEY, "head anchor once a skip hangs from it");
				helper.assertValueEqual(headAnchor(helper).getValue(CableAnchorBlock.FACING), Direction.EAST, "pulley facing the way the cable comes in");
				helper.assertValueEqual(CableGameTests.hooks(helper, DRUM).size(), 0, "no loose end while it hangs");
				ServerLevel level = helper.getLevel();
				TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
				skip.saveWithoutId(output);
				OreSkip copy = SkipContent.ORE_SKIP.create(level, EntitySpawnReason.LOAD);
				copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
				helper.assertTrue(copy.bridled() && Math.abs(copy.getBoundingBox().getYsize() - 4.0) < 1.0E-6, "loaded copy keeps its bridle");
				log.add("hooked on: bridle (1 x 4), head anchor a pulley facing east; a saved copy loads with its bridle");
				// Taken off on the floor.
				player.setShiftKeyDown(true);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.interactOn(skip, InteractionHand.MAIN_HAND, skip.position());
				player.setShiftKeyDown(false);
				helper.assertTrue(!skip.bridled() && Math.abs(skip.getBoundingBox().getYsize() - 3.0) < 1.0E-6, "taken off: no bridle");
				helper.assertValueEqual(headReturn(helper), CableAnchorBlock.Return.NONE, "head anchor after taking the skip off");
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				double eye = hookEye(helper);
				helper.assertTrue(Math.abs(eye - (1.0 + OreSkip.HEIGHT)) < 0.01, "the end hangs where the bridle's apex was (y 5): " + eye);
				log.add("taken off: no bridle, anchor ordinary, end hanging at y " + String.format("%.2f", eye) + " (the apex), not at the top");
				// Hooked on again, then the route disconnected at the mid anchor: the head anchor is freed as an ordinary anchor.
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				hang(helper, player, skip);
				helper.assertTrue(skip.bridled(), "hooked on again");
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				CableGameTests.useBlock(helper, player, MID_ANCHOR, true);
				helper.assertValueEqual(headReturn(helper), CableAnchorBlock.Return.NONE, "head anchor after the route was cut before it");
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertFalse(skip.bridled(), "let go: no bridle");
				log.add("route cut before the head anchor: skip let go without its bridle, anchor ordinary");
				SimpleBlockTestLog.info("SKIP-09: {}", log);
				skip.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * SKIP-10 (patch §5A B): before any skip, the free hook hanging under the head anchor is paid out and
	 * wound in by the drum. The crank's left-click (feed) lowers it, right-click (pull) raises it. The coal
	 * winch's FEED takes it down to the floor (stops, `blocked`, lever NEUTRAL), PULL back to its place
	 * under the anchor (`end`); coal burns only while it moves. The head anchor stays an ordinary anchor
	 * throughout, and no item is made or used.
	 */
	@GameTest(structure = SHAFT, maxTicks = 1200)
	public void freeHookIsWoundUpAndDown(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		layShaft(helper, player);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		double[] mark = new double[2];
		int[] burn = new int[1];
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				player.getInventory().clearContent();
			}))
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				mark[0] = hookEye(helper);
				helper.assertTrue(Math.abs(mark[0] - (HEAD_ANCHOR.getY() + 0.75 - 0.5)) < 0.01, "the free hook in its place under the anchor: " + mark[0]);
				CableGameTests.teleport(helper, player, new BlockPos(3, 1, 6));
			}))
			.thenExecuteFor(40, CableGameTests.step(failed, () -> {
				if (helper.getTick() % 4 == 0) {
					CableLoopGameTests.crank(helper, player, DRUM, false);
				}
			}))
			.thenExecuteAfter(10, CableGameTests.step(failed, () -> {
				mark[1] = hookEye(helper);
				helper.assertTrue(mark[1] < mark[0] - 1.0, "the crank's feed lowers the free hook: " + mark[0] + " -> " + mark[1]);
				helper.assertValueEqual(headReturn(helper), CableAnchorBlock.Return.NONE, "anchor while the free hook hangs lower");
				log.add("crank feed 40 ticks: hook " + String.format("%.2f -> %.2f", mark[0], mark[1]) + ", anchor ordinary");
			}))
			.thenExecuteFor(20, CableGameTests.step(failed, () -> {
				if (helper.getTick() % 4 == 0) {
					CableLoopGameTests.crank(helper, player, DRUM, true);
				}
			}))
			.thenExecuteAfter(10, CableGameTests.step(failed, () -> {
				double eye = hookEye(helper);
				helper.assertTrue(eye > mark[1] + 0.3, "the crank's pull raises it: " + mark[1] + " -> " + eye);
				log.add("crank pull 20 ticks: up to " + String.format("%.2f", eye));
				coalWinch(helper, player, 1);
				setLever(helper, player, CableLines.Lever.FEED);
			}))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever after lowering the hook"))
			.thenExecuteAfter(5, CableGameTests.step(failed, () -> {
				double eye = hookEye(helper);
				var hook = CableGameTests.hook(helper, DRUM);
				double bottom = hook.getY() - helper.absoluteVec(Vec3.ZERO).y;
				helper.assertValueEqual(line(helper).lastStop(), "blocked", "why the winch stopped");
				helper.assertTrue(Math.abs(bottom - 1.0) < 0.01, "the hook down on the floor: its bottom at " + bottom);
				helper.assertValueEqual(headReturn(helper), CableAnchorBlock.Return.NONE, "anchor with the hook on the floor");
				burn[0] = line(helper).burn() + line(helper).coal() * 1600;
				log.add("coal winch FEED: hook down on the floor (eye " + String.format("%.2f", eye) + ", " + line(helper).lastStop() + ")");
			}))
			.thenExecuteAfter(20, CableGameTests.step(failed, () -> {
				helper.assertValueEqual(line(helper).burn() + line(helper).coal() * 1600, burn[0], "coal burnt while stopped");
				lever(helper, player);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "after a stop going down, the next click");
			}))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "lever after raising the hook"))
			.thenExecute(CableGameTests.step(failed, () -> {
				double eye = hookEye(helper);
				helper.assertValueEqual(line(helper).lastStop(), "end", "why the winch stopped at the top");
				helper.assertTrue(Math.abs(eye - mark[0]) < 0.01, "back in its place under the anchor: " + eye);
				helper.assertTrue(line(helper).burn() + line(helper).coal() * 1600 < burn[0], "coal burnt while it moved");
				helper.assertValueEqual(headReturn(helper), CableAnchorBlock.Return.NONE, "anchor at the end");
				helper.assertTrue(player.getInventory().countItem(CableContent.CABLE) == 0 && player.getInventory().countItem(CableContent.CABLE_COIL) == 0
					&& CableGameTests.drum(helper, DRUM).hasCoil(), "no cable made; the one coil on the drum");
				log.add("coal winch PULL: back under the anchor (" + line(helper).lastStop() + "); coal only while moving; anchor ordinary throughout");
				SimpleBlockTestLog.info("SKIP-10: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * SKIP-11 (patch §5A A): carried by hand, the end is measured across the ground from its support. 14
	 * blocks out it goes taut (the hook stays 12 blocks out, towards the hand, still held); 17 blocks out
	 * it is put down and hangs at its support again. Straight under its anchor, far below, it stays in
	 * the hand (height does not count). A span of more than 12 blocks to a new anchor is still refused.
	 */
	@GameTest(structure = SHAFT, maxTicks = 300)
	public void cableEndLeashIsMeasuredAcrossTheGround(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 6));
		layShaft(helper, player);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				var hook = CableGameTests.hook(helper, DRUM);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				Vec3 support = helper.absoluteVec(new Vec3(12.5, 1.0, 5.5));
				// Straight under the anchor, 10 blocks below it: held.
				player.teleportTo(support.x, support.y, support.z);
				hook.tick();
				helper.assertTrue(hook.isHeld(), "held straight under the anchor");
				// 14 across: taut, the hook 12 out.
				player.teleportTo(support.x - 14.0, support.y, support.z);
				hook.tick();
				double across = Math.abs(hook.getX() - support.x);
				helper.assertTrue(hook.isHeld() && Math.abs(across - 12.0) < 0.05, "14 out: taut, held, hook " + across + " out");
				log.add("14 blocks across: taut, the hook " + String.format("%.2f", across) + " out, still held");
				// 17 across: put down, back under its support.
				player.teleportTo(support.x - 17.0, support.y, support.z);
				hook.tick();
				helper.assertFalse(hook.isHeld(), "17 out: put down");
				hook.tick();
				helper.assertTrue(Math.abs(hook.getX() - support.x) < 0.01 && Math.abs(hook.getZ() - support.z) < 0.01, "put down: back under its support "
					+ hook.position());
				log.add("17 blocks across: put down, hanging under its support again");
				// Picked up again; a new anchor 13 blocks on is refused (spans stay at most 12).
				player.teleportTo(support.x - 1.0, support.y, support.z);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				hook.tick();
				BlockPos far = CableGameTests.hangAnchor(helper, player, new BlockPos(12, 12, 18).above()).immutable();
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				CableGameTests.useBlock(helper, player, far, false);
				helper.assertValueEqual(CableGameTests.drum(helper, DRUM).route().size(), 2, "route after offering a 13-block span");
				log.add("picked up again; a 13-block span to a new anchor refused");
				SimpleBlockTestLog.info("SKIP-11: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * SKIP-12: both exchanges work the same whichever way the skip faces. A hung skip turned to each of the
	 * four directions, with the Loading Rails on that side: lifted to the cart at its bridle it is loaded
	 * (ramp and lid), then held with its chute's lip on a lower cart's rim it fills it (discharge gate).
	 */
	@GameTest(structure = SHAFT, maxTicks = 2400)
	public void skipExchangesFacingEveryWay(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		layShaft(helper, player);
		OreSkip skip = spawnSkip(helper, COLUMN);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		Minecart[] carts = new Minecart[2];
		BlockPos[] rails = new BlockPos[2];
		var sequence = helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				hang(helper, player, skip);
				CableGameTests.teleport(helper, player, new BlockPos(3, 1, 9));
			}));
		for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
			sequence = sequence
				.thenExecute(CableGameTests.step(failed, () -> {
					skip.setFacing(facing);
					rails[0] = COLUMN.above(4).relative(facing);
					rails[1] = COLUMN.relative(facing);
					for (BlockPos rail : rails) {
						helper.setBlock(rail.below(), Blocks.STONE);
						helper.setBlock(rail, TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, facing.getOpposite())
							.setValue(LoadingTrackBlock.SHAPE, facing.getAxis() == Direction.Axis.Z ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST));
					}
					carts[0] = cartOn(helper, rails[0], Items.RAW_COPPER, 100);
					holdAt(helper, skip, 2.0, Items.AIR, 0);
				}))
				.thenExecuteAfter(160, CableGameTests.step(failed, () -> {
					helper.assertTrue(skip.count() == 100 && cartCount(carts[0]) == 0, facing + ": loaded at the bridle: skip " + skip.count() + ", cart "
						+ cartCount(carts[0]));
					carts[1] = cartOn(helper, rails[1], Items.AIR, 0);
					holdAt(helper, skip, 1.0 + SkipStations.UNLOAD_OFFSET, Items.RAW_COPPER, 100);
				}))
				.thenExecuteAfter(160, CableGameTests.step(failed, () -> {
					helper.assertTrue(skip.count() == 0 && cartCount(carts[1]) == 100, facing + ": unloaded below the chute: skip " + skip.count() + ", cart "
						+ cartCount(carts[1]));
					log.add(facing + ": 100 in at the bridle, 100 out through the chute");
					carts[0].discard();
					carts[1].discard();
					for (BlockPos rail : rails) {
						helper.setBlock(rail, Blocks.AIR);
						helper.setBlock(rail.below(), rail.getY() == 1 ? Blocks.STONE : Blocks.AIR);
					}
				}));
		}
		sequence.thenExecute(CableGameTests.step(failed, () -> {
			SimpleBlockTestLog.info("SKIP-12: {}", log);
			skip.discard();
			TestPlayers.remove(player);
		})).thenSucceed();
	}

	/**
	 * SKIP-13 (owner's cart redesign): side-on to the skip. A Loading Rail running across the skip's front,
	 * the docked car of a coupled two-car train standing on it: at the bridle it tips its load sideways
	 * into the skip through its long side gate (the one facing the skip opens, never an end gate), and below
	 * the chute it is filled through its rim; the other car is not touched and the train stays coupled.
	 */
	@GameTest(structure = SHAFT, maxTicks = 900)
	public void skipExchangesSideOnWithACoupledTrain(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(8, 1, 6));
		layShaft(helper, player);
		OreSkip skip = spawnSkip(helper, COLUMN);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		Minecart[] train = new Minecart[2];
		java.util.function.IntConsumer lay = y -> {
			for (int x = 10; x <= 14; x++) {
				helper.setBlock(new BlockPos(x, y - 1, 4), Blocks.STONE);
				helper.setBlock(new BlockPos(x, y, 4), x == 12
					? TrainContent.LOADING_TRACK.defaultBlockState().setValue(LoadingTrackBlock.FACING, Direction.EAST).setValue(LoadingTrackBlock.SHAPE, RailShape.EAST_WEST)
					: Blocks.RAIL.defaultBlockState().setValue(net.minecraft.world.level.block.RailBlock.SHAPE, RailShape.EAST_WEST));
			}
		};
		Direction[] seen = new Direction[2];
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				route(helper, player);
				CableGameTests.teleport(helper, player, new BlockPos(12, 1, 8));
				hang(helper, player, skip);
				lay.accept(5);
				train[0] = cartOn(helper, new BlockPos(12, 5, 4), Items.RAW_COPPER, 100);
				train[1] = cartOn(helper, new BlockPos(14, 5, 4), Items.RAW_COPPER, 50);
				TrainGameTests.couple(player, train[0], train[1]);
				holdAt(helper, skip, 2.0, Items.AIR, 0);
			}))
			.thenExecuteAfter(20, CableGameTests.step(failed, () -> seen[0] = LoadingTracks.freight(train[0]).simpleBlock$gateSide()))
			.thenExecuteAfter(160, CableGameTests.step(failed, () -> {
				helper.assertTrue(skip.count() == 100 && cartCount(train[0]) == 0 && cartCount(train[1]) == 50, "side-on at the bridle: skip " + skip.count()
					+ ", docked car " + cartCount(train[0]) + ", other car " + cartCount(train[1]));
				helper.assertValueEqual(seen[0], Direction.SOUTH, "the gate that opened (the side towards the skip)");
				helper.assertTrue(TrainGameTests.coupled(train[0], train[1]) && !LoadingTracks.freight(train[0]).simpleBlock$gateOpen(), "still coupled, gate shut");
				log.add("bridle, side-on: docked car 100 -> 0 through its south side gate (" + seen[0] + "), other car 50 untouched, still coupled");
				train[0].discard();
				train[1].discard();
				for (int x = 10; x <= 14; x++) {
					helper.setBlock(new BlockPos(x, 5, 4), Blocks.AIR);
					helper.setBlock(new BlockPos(x, 4, 4), Blocks.AIR);
				}
				lay.accept(1);
				train[0] = cartOn(helper, new BlockPos(12, 1, 4), Items.AIR, 0);
				train[1] = cartOn(helper, new BlockPos(14, 1, 4), Items.AIR, 0);
				TrainGameTests.couple(player, train[0], train[1]);
				holdAt(helper, skip, 1.0 + SkipStations.UNLOAD_OFFSET, Items.RAW_COPPER, 100);
			}))
			.thenExecuteAfter(20, CableGameTests.step(failed, () -> seen[1] = LoadingTracks.freight(train[0]).simpleBlock$gateSide()))
			.thenExecuteAfter(160, CableGameTests.step(failed, () -> {
				helper.assertTrue(skip.count() == 0 && cartCount(train[0]) == 100 && cartCount(train[1]) == 0, "side-on below the chute: skip " + skip.count()
					+ ", docked car " + cartCount(train[0]) + ", other car " + cartCount(train[1]));
				helper.assertTrue(seen[1] == null && TrainGameTests.coupled(train[0], train[1]), "filled through the rim, gates shut, still coupled: " + seen[1]);
				log.add("below the chute, side-on: docked car 0 -> 100 through its rim, gates shut, other car untouched, still coupled");
				SimpleBlockTestLog.info("SKIP-13: {}", log);
				train[0].discard();
				train[1].discard();
				skip.discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

}
