package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.cable.CableAnchorBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableHook;
import io.github.boxofdog.simpleblock.cable.CableIndex;
import io.github.boxofdog.simpleblock.cable.CableRoutes;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;

/**
 * M04 cable infrastructure: recipes, anchors, the drum, laying out and taking down routes, and the
 * connecting rod. Every use goes through the server's own handlers for the player's packets
 * ({@code ServerPlayerGameMode.useItemOn}, {@code Player.interactOn}). Test ids refer to the matrix in
 * {@code docs/MILESTONE_04_REPORT.md}. The route tests run in {@code cable_yard}, a 30 x 10 x 10 empty
 * structure (the default one is 8 blocks).
 */
public class CableGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";

	// ---- Helpers ----

	/** Uses a block the way a right-click packet does, on {@code face} of {@code relativePos}. */
	static void useBlock(GameTestHelper helper, ServerPlayer player, BlockPos relativePos, Direction face) {
		BlockPos pos = helper.absolutePos(relativePos);
		Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
	}

	static void useBlock(GameTestHelper helper, ServerPlayer player, BlockPos relativePos, boolean sneaking) {
		player.setShiftKeyDown(sneaking);
		useBlock(helper, player, relativePos, Direction.UP);
		player.setShiftKeyDown(false);
	}

	static CableDrumBlockEntity drum(GameTestHelper helper, BlockPos relativePos) {
		if (helper.getLevel().getBlockEntity(helper.absolutePos(relativePos)) instanceof CableDrumBlockEntity drum) {
			return drum;
		}
		throw helper.assertionException(Component.literal("no cable drum at " + relativePos));
	}

	static CableAnchorBlockEntity anchor(GameTestHelper helper, BlockPos relativePos) {
		if (helper.getLevel().getBlockEntity(helper.absolutePos(relativePos)) instanceof CableAnchorBlockEntity anchor) {
			return anchor;
		}
		throw helper.assertionException(Component.literal("no cable anchor at " + relativePos));
	}

	static List<CableHook> hooks(GameTestHelper helper, BlockPos drumRelative) {
		BlockPos drum = helper.absolutePos(drumRelative);
		return helper.getLevel().getEntitiesOfClass(CableHook.class, new AABB(drum).inflate(40.0), hook -> hook.drumPos().equals(drum));
	}

	static CableHook hook(GameTestHelper helper, BlockPos drumRelative) {
		List<CableHook> hooks = hooks(helper, drumRelative);
		helper.assertValueEqual(hooks.size(), 1, "cable ends of the drum at " + drumRelative);
		return hooks.getFirst();
	}

	/** Hangs an anchor under a plank block at {@code beam} (the anchor goes just below it), placed by a player. */
	static BlockPos hangAnchor(GameTestHelper helper, ServerPlayer player, BlockPos beam) {
		helper.setBlock(beam, Blocks.OAK_PLANKS);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_ANCHOR_ITEM));
		useBlock(helper, player, beam, Direction.DOWN);
		helper.assertBlockPresent(CableContent.CABLE_ANCHOR, beam.below());
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return beam.below();
	}

	/** Places a drum on the block below {@code pos} and uses {@code coils} coils on it (M06: the first fits it; more are refused). */
	static CableDrumBlockEntity windDrum(GameTestHelper helper, ServerPlayer player, BlockPos pos, int coils) {
		helper.setBlock(pos.below(), Blocks.STONE);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_DRUM_ITEM));
		useBlock(helper, player, pos.below(), Direction.UP);
		helper.assertBlockPresent(CableContent.CABLE_DRUM, pos);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_COIL, coils));
		for (int i = 0; i < coils; i++) {
			useBlock(helper, player, pos, Direction.UP);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return drum(helper, pos);
	}

	static void teleport(GameTestHelper helper, ServerPlayer player, BlockPos relativePos) {
		Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(relativePos));
		player.teleportTo(pos.x, pos.y, pos.z);
	}

	/** M06: the drum keeps its one coil whatever happens to the route. */
	static void assertCableKept(GameTestHelper helper, CableDrumBlockEntity drum, String when) {
		helper.assertTrue(drum.hasCoil(), "the drum should still have its coil " + when);
	}

	static List<BlockPos> absolute(GameTestHelper helper, BlockPos... relative) {
		List<BlockPos> positions = new ArrayList<>();
		for (BlockPos pos : relative) {
			positions.add(helper.absolutePos(pos));
		}
		return positions;
	}

	static int count(List<ItemEntity> items, Item item) {
		return MinecartCargoGameTests.total(items, item);
	}

	static List<ItemEntity> itemsNear(GameTestHelper helper, BlockPos relativePos) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(relativePos)).inflate(2.0));
	}

	/**
	 * Wraps a sequence step so that, once one step has failed, the later ones are skipped: the runner
	 * keeps only the last failure, and a later step failing because an earlier one did would hide the
	 * real cause.
	 */
	static Runnable step(boolean[] failed, Runnable body) {
		return () -> {
			if (failed[0]) {
				return;
			}
			try {
				body.run();
			} catch (RuntimeException e) {
				failed[0] = true;
				throw e;
			}
		};
	}

	// ---- Recipes ----

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

	private static void assertCrafts(GameTestHelper helper, ItemStack result, Item item, int count, String recipe) {
		helper.assertTrue(result.is(item) && result.getCount() == count, recipe + " should make " + count + " " + item + ", made " + result);
	}

	/** CAB-01: each component is craftable; the drum's top row is empty (stick, iron, stick over plank, iron, plank). */
	@GameTest
	public void cableComponentsAreCraftable(GameTestHelper helper) {
		Item air = Items.AIR;
		Item nugget = Items.IRON_NUGGET;
		Item copperNugget = Items.COPPER_NUGGET;
		Item cable = CableContent.CABLE;
		// M06: down the middle column (the column itself is checked in a real crafting grid by RecipeGameTests).
		assertCrafts(helper, craft(helper, air, nugget, air, air, nugget, air, air, nugget, air), cable, 2, "3 iron nuggets down the middle");
		assertCrafts(helper, craft(helper, air, cable, air, air, cable, air, air, cable, air), CableContent.CABLE_COIL, 1, "3 cables down the middle");
		assertCrafts(helper, craft(helper, CableContent.CABLE_COIL, air, air, air, air, air, air, air, air), cable, 3, "a coil taken apart");
		assertCrafts(helper, craft(helper, air, air, air, Items.STICK, Items.IRON_INGOT, Items.STICK, Items.OAK_PLANKS, Items.IRON_INGOT, Items.SPRUCE_PLANKS),
			CableContent.CABLE_DRUM_ITEM, 1, "drum (empty top row; stick iron stick; plank iron plank)");
		assertCrafts(helper, craft(helper, copperNugget, air, copperNugget, air, copperNugget, air, air, air, air), CableContent.CABLE_ANCHOR_ITEM, 2,
			"3 copper nuggets in a V");
		assertCrafts(helper, craft(helper, air, copperNugget, air, air, Items.COPPER_INGOT, air, air, Items.COPPER_INGOT, air), CableContent.CONNECTING_ROD, 1,
			"copper nugget over 2 copper ingots");
		// The drum is not made from a different layout (sticks below the planks).
		helper.assertTrue(craft(helper, air, air, air, Items.OAK_PLANKS, Items.IRON_INGOT, Items.OAK_PLANKS, Items.STICK, Items.IRON_INGOT, Items.STICK).isEmpty(),
			"an upside-down drum layout should not craft");
		SimpleBlockTestLog.info("CAB-01: cable x2, coil, coil -> 3 cable (M06 recipes), drum, anchor x2 and connecting rod all craft from their recipes");
		helper.succeed();
	}

	// ---- Anchors ----

	/** CAB-02: an anchor hangs only under a wooden block that offers its underside, and drops when the support goes. */
	@GameTest
	public void anchorHangsOnlyUnderWood(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(4, 1, 4));
		List<String> results = new ArrayList<>();
		BlockState[] wooden = {
			Blocks.OAK_PLANKS.defaultBlockState(),
			Blocks.SPRUCE_LOG.defaultBlockState(),
			Blocks.STRIPPED_BIRCH_LOG.defaultBlockState(),
			Blocks.CRIMSON_STEM.defaultBlockState(),
			Blocks.OAK_SLAB.defaultBlockState(),
			Blocks.DARK_OAK_STAIRS.defaultBlockState(),
			Blocks.JUNGLE_FENCE.defaultBlockState(),
			Blocks.BAMBOO_BLOCK.defaultBlockState(),
			Blocks.BAMBOO_MOSAIC.defaultBlockState(),
		};
		BlockState[] refused = {
			Blocks.STONE.defaultBlockState(),
			Blocks.IRON_BLOCK.defaultBlockState(),
			Blocks.BRICKS.defaultBlockState(),
			Blocks.GLASS.defaultBlockState(),
			Blocks.OAK_LEAVES.defaultBlockState(),
			// Wooden, but no underside where the anchor would hang: a top slab and upside-down stairs.
			Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP),
			Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.TOP),
		};
		for (int i = 0; i < wooden.length; i++) {
			BlockState state = wooden[i];
			BlockPos support = new BlockPos(i % 8, 5, i / 8 * 2);
			helper.setBlock(support, state);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_ANCHOR_ITEM));
			useBlock(helper, player, support, Direction.DOWN);
			helper.assertBlockPresent(CableContent.CABLE_ANCHOR, support.below());
			helper.assertTrue(player.getMainHandItem().isEmpty(), "the anchor item should be used up under " + state);
			results.add("under " + state.getBlock().getName().getString() + ": hung");
		}
		for (int i = 0; i < refused.length; i++) {
			BlockState state = refused[i];
			BlockPos support = new BlockPos(i, 5, 6);
			helper.setBlock(support, state);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_ANCHOR_ITEM));
			useBlock(helper, player, support, Direction.DOWN);
			helper.assertBlockNotPresent(CableContent.CABLE_ANCHOR, support.below());
			helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "anchors left in hand under " + state);
			results.add("under " + state + ": refused");
		}
		// On the floor or on a wall it has nothing to hang from.
		helper.setBlock(new BlockPos(7, 1, 6), Blocks.OAK_PLANKS);
		useBlock(helper, player, new BlockPos(7, 1, 6), Direction.UP);
		helper.assertBlockNotPresent(CableContent.CABLE_ANCHOR, new BlockPos(7, 2, 6));
		useBlock(helper, player, new BlockPos(7, 1, 6), Direction.WEST);
		helper.assertBlockNotPresent(CableContent.CABLE_ANCHOR, new BlockPos(6, 1, 6));

		// Take the support away: the anchor drops as an item.
		helper.setBlock(new BlockPos(0, 5, 0), Blocks.AIR);
		helper.assertBlockNotPresent(CableContent.CABLE_ANCHOR, new BlockPos(0, 4, 0));
		helper.assertValueEqual(count(itemsNear(helper, new BlockPos(0, 4, 0)), CableContent.CABLE_ANCHOR_ITEM), 1, "anchors dropped when the plank went");
		results.add("plank removed: anchor dropped");
		SimpleBlockTestLog.info("CAB-02: {}", results);
		itemsNear(helper, new BlockPos(0, 4, 0)).forEach(ItemEntity::discard);
		TestPlayers.remove(player);
		helper.succeed();
	}

	// ---- The drum ----

	/** CAB-03 (M06): one coil makes the drum a winch with a loose end and hook; a second is refused (one is all it needs). */
	@GameTest
	public void coilMakesTheDrumAWinch(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(5, 1, 4));
		BlockPos at = new BlockPos(2, 1, 4);
		CableDrumBlockEntity drum = windDrum(helper, player, at, 0);
		helper.assertFalse(helper.getBlockState(at).getValue(CableDrumBlock.WOUND), "an empty drum should show a bare barrel");
		helper.assertFalse(drum.hasCable(), "a new drum should have no cable");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_COIL, 2));
		useBlock(helper, player, at, Direction.UP);
		helper.assertTrue(drum.hasCoil(), "the drum should have its coil");
		helper.assertTrue(helper.getBlockState(at).getValue(CableDrumBlock.WOUND), "a drum with cable should show a wound barrel");
		useBlock(helper, player, at, Direction.UP);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "coils left in hand: a drum takes one");

		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableHook hook = hook(helper, at);
				helper.assertFalse(hook.isHeld(), "the new loose end should hang, not be held");
				helper.assertTrue(hook.position().distanceTo(helper.absoluteVec(Vec3.atCenterOf(at))) < 1.0, "the loose end should hang at the drum");
				SimpleBlockTestLog.info("CAB-03: one coil fitted (a second refused, kept in hand); loose end hangs at {}", hook.position());
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- Routes ----

	/**
	 * CAB-04..07: pick up the end, run it through anchors (a straight run, a turn and a climb). Spans
	 * longer than cable.max_span or through a solid block are refused. M06: the route's length costs
	 * nothing, so with one coil the route carries on as far as the anchors go.
	 */
	@GameTest(structure = YARD)
	public void cableRoutesThroughAnchorsAndUsesItsLength(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 4));
		BlockPos at = new BlockPos(2, 1, 4);
		CableDrumBlockEntity drum = windDrum(helper, player, at, 1);
		BlockPos a = hangAnchor(helper, player, new BlockPos(8, 6, 4));
		BlockPos b = hangAnchor(helper, player, new BlockPos(18, 6, 4));
		BlockPos c = hangAnchor(helper, player, new BlockPos(24, 8, 8));
		BlockPos d = hangAnchor(helper, player, new BlockPos(28, 8, 8));
		BlockPos e = hangAnchor(helper, player, new BlockPos(28, 8, 2));
		helper.setBlock(new BlockPos(5, 3, 4), Blocks.STONE);
		List<String> log = new ArrayList<>();

		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				// Without holding the end, an anchor only reports.
				useBlock(helper, player, a, false);
				helper.assertTrue(drum.route().isEmpty(), "using an anchor without the cable end should not route it");

				CableHook hook = hook(helper, at);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				helper.assertValueEqual(hook.holderId(), player.getId(), "holder of the picked-up end");

				useBlock(helper, player, b, false);
				helper.assertTrue(drum.route().isEmpty(), "a 16.5-block span should be refused (longest " + CableRoutes.maxSpan() + ")");
				log.add("drum->B " + String.format("%.1f", CableRoutes.point(helper.absolutePos(at), true).distanceTo(
					CableRoutes.point(helper.absolutePos(b), false))) + " blocks: refused, too far");

				useBlock(helper, player, a, false);
				helper.assertTrue(drum.route().isEmpty(), "a span through the stone at (5, 3, 4) should be refused");
				log.add("drum->A through stone: refused");
				helper.setBlock(new BlockPos(5, 3, 4), Blocks.AIR);

				int spanA = CableRoutes.spanLength(helper.absolutePos(at), true, helper.absolutePos(a));
				useBlock(helper, player, a, false);
				helper.assertValueEqual(drum.route(), absolute(helper, a), "route after A");
				helper.assertValueEqual(anchor(helper, a).drum(), helper.absolutePos(at), "drum recorded on A");
				helper.assertTrue(hook.isHeld(), "the end should stay in hand for the next anchor");
				log.add("drum->A: " + spanA + " blocks");

				int spanB = CableRoutes.spanLength(helper.absolutePos(a), false, helper.absolutePos(b));
				useBlock(helper, player, b, false);
				int spanC = CableRoutes.spanLength(helper.absolutePos(b), false, helper.absolutePos(c));
				useBlock(helper, player, c, false);
				helper.assertValueEqual(drum.route(), absolute(helper, a, b, c), "route after the straight run, the turn and the climb");
				assertCableKept(helper, drum, "after C");
				log.add("A->B straight: " + spanB + ", B->C turn and climb 2: " + spanC);

				// Using an anchor already on the route does nothing.
				useBlock(helper, player, a, false);
				helper.assertValueEqual(drum.route().size(), 3, "anchors on the route after using A again");

				int spanD = CableRoutes.spanLength(helper.absolutePos(c), false, helper.absolutePos(d));
				useBlock(helper, player, d, false);
				int spanE = CableRoutes.spanLength(helper.absolutePos(d), false, helper.absolutePos(e));
				useBlock(helper, player, e, false);
				helper.assertValueEqual(drum.route().size(), 5, "route length after E: one coil covers any length (" + drum.laidOut()
					+ " blocks, more than the old 32-block coil)");
				log.add("C->D: " + spanD + ", D->E: " + spanE + " (" + drum.laidOut() + " blocks on one coil: no length budget)");
				assertCableKept(helper, drum, "after E");
				List<CableIndex.Span> indexed = CableIndex.spans(helper.getLevel());
				List<Vec3> points = List.of(CableRoutes.point(helper.absolutePos(at), true), CableRoutes.point(helper.absolutePos(a), false),
					CableRoutes.point(helper.absolutePos(b), false), CableRoutes.point(helper.absolutePos(c), false), CableRoutes.point(helper.absolutePos(d), false));
				for (int i = 0; i < 4; i++) {
					helper.assertTrue(indexed.contains(new CableIndex.Span(points.get(i), points.get(i + 1))), "span " + i + " of the route should be indexed");
				}

				// The last anchor puts the end down; it hangs under E.
				useBlock(helper, player, e, false);
				helper.assertFalse(hook.isHeld(), "using the last anchor should put the end down");
			}))
			.thenIdle(1)
			.thenExecute(step(failed, () -> {
				CableHook hook = hook(helper, at);
				Vec3 under = CableRoutes.point(helper.absolutePos(e), false);
				helper.assertTrue(hook.position().distanceTo(under) < 1.5 && hook.position().y < under.y, "the put-down end should hang under E, at "
					+ hook.position());
				SimpleBlockTestLog.info("CAB-04..07: {}; {} blocks laid out on one coil", log, drum.laidOut());
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/** CAB-08: an anchor carries one cable: a second drum's end can't run through another drum's anchor. */
	@GameTest(structure = YARD)
	public void anchorCarriesOneCable(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(6, 1, 4));
		BlockPos first = new BlockPos(2, 1, 4);
		BlockPos second = new BlockPos(2, 1, 7);
		CableDrumBlockEntity drumOne = windDrum(helper, player, first, 1);
		CableDrumBlockEntity drumTwo = windDrum(helper, player, second, 1);
		BlockPos a = hangAnchor(helper, player, new BlockPos(8, 5, 5));
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableHook one = hook(helper, first);
				player.interactOn(one, InteractionHand.MAIN_HAND, one.position());
				useBlock(helper, player, a, false);
				helper.assertValueEqual(drumOne.route().size(), 1, "first drum's route");
				useBlock(helper, player, first, false);
				helper.assertFalse(one.isHeld(), "using its drum should put the end down");

				CableHook two = hook(helper, second);
				player.interactOn(two, InteractionHand.MAIN_HAND, two.position());
				helper.assertTrue(two.isHeld(), "second end picked up");
				useBlock(helper, player, a, false);
				helper.assertTrue(drumTwo.route().isEmpty(), "the second cable should not run through an anchor that carries the first");
				helper.assertValueEqual(anchor(helper, a).drum(), helper.absolutePos(first), "owner of the shared anchor");

				// One end at a time.
				player.interactOn(one, InteractionHand.MAIN_HAND, one.position());
				helper.assertFalse(one.isHeld(), "a player holding one end should not pick up another");
				SimpleBlockTestLog.info("CAB-08: anchor kept the first drum's cable; second drum refused; second end not picked up while holding one");
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * CAB-09..11: extend (route further, on the same coil), disconnect from an anchor on (sneak-use),
	 * break an anchor (same), dismantle (sneak-use the drum: rewind, then the one coil out) and break a
	 * laid-out drum (one coil drops).
	 */
	@GameTest(structure = YARD)
	public void routesExtendDisconnectAndDismantle(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 4));
		BlockPos at = new BlockPos(2, 1, 4);
		CableDrumBlockEntity drum = windDrum(helper, player, at, 1);
		BlockPos a = hangAnchor(helper, player, new BlockPos(8, 5, 4));
		BlockPos b = hangAnchor(helper, player, new BlockPos(17, 5, 4));
		BlockPos c = hangAnchor(helper, player, new BlockPos(26, 5, 4));
		BlockPos d = hangAnchor(helper, player, new BlockPos(26, 5, 9));
		BlockPos e = hangAnchor(helper, player, new BlockPos(17, 5, 9));
		List<String> log = new ArrayList<>();

		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableHook hook = hook(helper, at);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				for (BlockPos anchor : List.of(a, b, c, d, e)) {
					useBlock(helper, player, anchor, false);
				}
				helper.assertValueEqual(drum.route(), absolute(helper, a, b, c, d, e), "route through A-E on one coil");
				assertCableKept(helper, drum, "after routing A-E");
				log.add("one coil: A-E laid (" + drum.laidOut() + " blocks)");

				// Disconnect from C on.
				int beforeOut = drum.laidOut();
				useBlock(helper, player, c, true);
				helper.assertValueEqual(drum.route(), absolute(helper, a, b), "route after disconnecting at C");
				helper.assertTrue(anchor(helper, c).drum() == null && anchor(helper, d).drum() == null && anchor(helper, e).drum() == null,
					"C, D and E should be free");
				helper.assertFalse(hook.isHeld(), "disconnecting should put the end down");
				assertCableKept(helper, drum, "after disconnecting at C");
				log.add("sneak-use C: " + (beforeOut - drum.laidOut()) + " blocks wound back");

				// Break B: same as disconnecting there.
				player.gameMode.destroyBlock(helper.absolutePos(b));
				helper.assertValueEqual(drum.route(), absolute(helper, a), "route after breaking B");
				assertCableKept(helper, drum, "after breaking B");
				log.add("B broken: route back to A");

				// Dismantle: the first sneak-use winds A back in, the next takes the cable off.
				useBlock(helper, player, at, true);
				helper.assertTrue(drum.route().isEmpty(), "route after the first sneak-use of the drum");
				helper.assertTrue(anchor(helper, a).drum() == null, "A should be free");
				assertCableKept(helper, drum, "after rewinding");
				player.getInventory().clearContent();
				useBlock(helper, player, at, true);
				helper.assertFalse(drum.hasCable(), "the drum should be empty after taking the cable off");
				helper.assertValueEqual(MinecartCargoGameTests.carried(player, CableContent.CABLE_COIL), 1, "coils given back");
				helper.assertValueEqual(MinecartCargoGameTests.carried(player, CableContent.CABLE), 0, "loose cables given back");
				helper.assertFalse(helper.getBlockState(at).getValue(CableDrumBlock.WOUND), "an emptied drum should show a bare barrel");
				log.add("sneak-use drum: rewound, then the one coil out");
			}))
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				helper.assertValueEqual(hooks(helper, at).size(), 0, "loose ends of an empty drum");

				// Lay it out again, then break the drum: the cable drops as coils and the anchors are freed.
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_COIL));
				useBlock(helper, player, at, Direction.UP);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			}))
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableHook hook = hook(helper, at);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				useBlock(helper, player, a, false);
				helper.assertValueEqual(drum.route().size(), 1, "route before breaking the drum");
				player.gameMode.destroyBlock(helper.absolutePos(at));
				helper.assertBlockNotPresent(CableContent.CABLE_DRUM, at);
				List<ItemEntity> items = itemsNear(helper, at);
				helper.assertValueEqual(count(items, CableContent.CABLE_COIL), 1, "coils dropped by the broken drum");
				helper.assertValueEqual(count(items, CableContent.CABLE), 0, "loose cables dropped by the broken drum");
				helper.assertValueEqual(count(items, CableContent.CABLE_DRUM_ITEM), 1, "drums dropped");
				helper.assertTrue(anchor(helper, a).drum() == null, "A should be free once its drum is gone");
				log.add("drum broken with A laid: dropped " + count(items, CableContent.CABLE_COIL) + " coils and the drum, A freed");
				items.forEach(ItemEntity::discard);
			}))
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				helper.assertValueEqual(hooks(helper, at).size(), 0, "loose ends left after the drum broke");
				SimpleBlockTestLog.info("CAB-09..11: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/** CAB-12: the held end follows the hand like a lead and slips out of it beyond the cable's reach. */
	@GameTest(structure = YARD)
	public void heldEndFollowsTheHandAndSlipsBeyondReach(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(4, 1, 4));
		BlockPos at = new BlockPos(2, 1, 4);
		helper.setBlock(new BlockPos(29, 0, 4), Blocks.STONE);
		for (int x = 0; x < 30; x++) {
			helper.setBlock(new BlockPos(x, 0, 4), Blocks.STONE);
		}
		windDrum(helper, player, at, 1);
		double reach = CableRoutes.maxSpan();
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableHook hook = hook(helper, at);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				teleport(helper, player, new BlockPos(9, 1, 4));
			}))
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableHook hook = hook(helper, at);
				helper.assertTrue(hook.isHeld(), "the end should still be in hand 7 blocks from the drum");
				double fromHand = hook.position().distanceTo(player.getRopeHoldPosition(1.0F));
				helper.assertTrue(fromHand < 0.5, "the held end should be at the hand, " + fromHand + " away");
				log.add("7 blocks out: held, " + String.format("%.2f", fromHand) + " from the hand");
				teleport(helper, player, new BlockPos(2 + (int) reach + 6, 1, 4));
			}))
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableHook hook = hook(helper, at);
				helper.assertFalse(hook.isHeld(), "the end should slip out of the hand " + ((int) reach + 6) + " blocks from the drum");
				helper.assertTrue(hook.position().distanceTo(helper.absoluteVec(Vec3.atCenterOf(at))) < 1.0, "the slipped end should hang at the drum again");
				log.add(((int) reach + 6) + " blocks out: slipped back to the drum");
				// Leaving the game puts it down too.
				teleport(helper, player, new BlockPos(5, 1, 4));
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				helper.assertTrue(hook.isHeld(), "picked up again");
				TestPlayers.remove(player);
			}))
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				helper.assertFalse(hook(helper, at).isHeld(), "the end should be put down when its holder leaves");
				log.add("holder left: put down");
				SimpleBlockTestLog.info("CAB-12: {}", log);
			}))
			.thenSucceed();
	}

	/**
	 * CAB-13: the route is saved with the drum; on loading, the cable winds back from the first anchor
	 * that went missing while the drum was away (here removed without telling it).
	 */
	@GameTest(structure = YARD)
	public void routeIsSavedAndCheckedOnLoading(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(3, 1, 4));
		BlockPos at = new BlockPos(2, 1, 4);
		CableDrumBlockEntity drum = windDrum(helper, player, at, 1);
		BlockPos a = hangAnchor(helper, player, new BlockPos(8, 5, 4));
		BlockPos b = hangAnchor(helper, player, new BlockPos(14, 5, 4));
		BlockPos c = hangAnchor(helper, player, new BlockPos(20, 5, 4));
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableHook hook = hook(helper, at);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				useBlock(helper, player, a, false);
				useBlock(helper, player, b, false);
				useBlock(helper, player, c, false);
				helper.assertValueEqual(drum.route().size(), 3, "route before saving");
				CompoundTag saved = drum.saveWithFullMetadata(level.registryAccess());
				log.add("saved " + saved.get("route") + " with coil " + saved.get("coil"));

				// B goes without its drum hearing about it (as if broken while the drum was unloaded).
				level.setBlock(helper.absolutePos(b), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
				// Unload and reload the drum: a fresh block entity from the saved data.
				level.setBlock(helper.absolutePos(at), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
				level.setBlock(helper.absolutePos(at), CableContent.CABLE_DRUM.defaultBlockState().setValue(CableDrumBlock.WOUND, true), Block.UPDATE_ALL);
				drum(helper, at).loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
				helper.assertValueEqual(drum(helper, at).route().size(), 3, "route loaded");
			}))
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableDrumBlockEntity loaded = drum(helper, at);
				helper.assertValueEqual(loaded.route(), absolute(helper, a), "route after the check on loading");
				helper.assertTrue(anchor(helper, c).drum() == null, "C, beyond the missing anchor, should be freed");
				assertCableKept(helper, loaded, "after the check on loading");
				log.add("B missing on load: wound back to A (" + loaded.laidOut() + " out), C freed, coil kept");
				SimpleBlockTestLog.info("CAB-13: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	// ---- The connecting rod ----

	/** CAB-14: a connecting rod is fitted to a minecart, saved with it, comes off last and drops with the cart. */
	@GameTest
	public void connectingRodFitsAMinecart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Minecart cart = MinecartCargoGameTests.cartOnRail(helper, new BlockPos(2, 2, 2));
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(4, 2, 2));
		player.getInventory().clearContent();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CONNECTING_ROD, 2));
		MinecartCargoGameTests.use(player, cart, InteractionHand.MAIN_HAND);
		helper.assertTrue(ConnectingRods.hasRod(cart), "the rod should be fitted");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "rods left in hand");
		MinecartCargoGameTests.use(player, cart, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "rods left after trying to fit a second");
		player.getInventory().clearContent();

		// Cargo comes out before the rod does.
		helper.assertValueEqual(MinecartCargoGameTests.load(player, cart, Items.RAW_COPPER, 20), 0, "raw copper left in hand: a cart with a rod takes cargo");
		player.getInventory().clearContent();
		player.setShiftKeyDown(true);
		// M11: each sneak-use unloads a batch; held until the cart is empty.
		for (int i = 0; i < 20 && MinecartCargoGameTests.cargo(cart).simpleBlock$count() > 0; i++) {
			MinecartCargo.forgetPace(player);
			MinecartCargoGameTests.use(player, cart, InteractionHand.MAIN_HAND);
			helper.assertTrue(ConnectingRods.hasRod(cart), "sneak-uses should unload, not take the rod, while there is cargo");
		}
		MinecartCargo.forgetPace(player);
		helper.assertValueEqual(MinecartCargoGameTests.cargo(cart).simpleBlock$count(), 0, "cargo after unloading");
		player.getInventory().clearContent();
		MinecartCargoGameTests.use(player, cart, InteractionHand.MAIN_HAND);
		player.setShiftKeyDown(false);
		helper.assertFalse(ConnectingRods.hasRod(cart), "the next sneak-use should take the rod off");
		helper.assertValueEqual(MinecartCargoGameTests.carried(player, CableContent.CONNECTING_ROD), 1, "rods given back");

		// Saved, and dropped when the cart is broken.
		ConnectingRods.fitted(cart).simpleBlock$setRod(true);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		cart.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		Minecart copy = EntityTypes.MINECART.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		helper.assertTrue(ConnectingRods.hasRod(copy), "rod after saving and loading");
		Vec3 where = cart.position();
		cart.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		List<ItemEntity> items = MinecartCargoGameTests.itemsAround(level, where);
		helper.assertValueEqual(count(items, CableContent.CONNECTING_ROD), 1, "rods dropped by the broken cart");
		SimpleBlockTestLog.info("CAB-14: rod fitted (second refused), cargo unloaded before the rod came off, saved {}, dropped {}",
			tag.get("simple_block:connecting_rod"), items.stream().map(item -> item.getItem().toString()).toList());
		items.forEach(ItemEntity::discard);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * CAB-15/16: a cart with a rod rolling under a route grips the cable from the first anchor to the
	 * last, without letting go at the anchors in between, and lets go past the last; it rolls exactly
	 * as a cart with a rod on a track with no cable (nothing snags).
	 */
	/** Speed the test keeps rolling carts at (a powered rail's top speed), now that powered rails are retired. */
	private static final double BOOST = 0.4;

	@GameTest(structure = YARD, maxTicks = 300)
	public void rodGripsTheCableAndPassesAnchors(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.CREATIVE, new BlockPos(1, 1, 7));
		BlockPos at = new BlockPos(1, 1, 4);
		helper.setBlock(new BlockPos(1, 0, 4), Blocks.STONE);
		// M06: powered rails are retired; the carts are kept rolling by the test instead (below).
		for (int x = 2; x < 30; x++) {
			for (int z : new int[] {4, 8}) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 1, z), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
			}
		}
		CableDrumBlockEntity drum = windDrum(helper, player, at, 1);
		BlockPos[] anchors = {new BlockPos(7, 3, 4), new BlockPos(15, 3, 4), new BlockPos(23, 3, 4)};
		for (BlockPos anchor : anchors) {
			hangAnchor(helper, player, anchor.above());
		}
		Minecart[] carts = new Minecart[2];
		List<Float> reachesBetween = new ArrayList<>();
		List<String> log = new ArrayList<>();
		boolean[] lostGrip = {false};
		boolean[] gripPast = {false};
		int[] ticks = {0};
		/** Tick on which the cart passed x 26 (the track ends at 29 and the carts bounce back off the yard's wall). */
		int[] arrived = {-1};

		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				CableHook hook = hook(helper, at);
				player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
				for (BlockPos anchor : anchors) {
					useBlock(helper, player, anchor, false);
				}
				helper.assertValueEqual(drum.route().size(), 3, "anchors on the route over the track");
				useBlock(helper, player, anchors[2], false);
				for (int i = 0; i < 2; i++) {
					carts[i] = helper.spawn(EntityTypes.MINECART, Vec3.atBottomCenterOf(new BlockPos(3, 1, i == 0 ? 4 : 8)).add(0, 0.0625, 0));
					ConnectingRods.fitted(carts[i]).simpleBlock$setRod(true);
					carts[i].setDeltaMovement(0.1, 0.0, 0.0);
				}
			}))
			.thenExecuteFor(120, step(failed, () -> {
				ticks[0]++;
				if (arrived[0] >= 0) {
					return;
				}
				Minecart cart = carts[0];
				Minecart control = carts[1];
				// Test-only propulsion standing in for the retired powered rail: the same push to both.
				cart.setDeltaMovement(BOOST, 0.0, 0.0);
				control.setDeltaMovement(BOOST, 0.0, 0.0);
				double x = cart.getX() - helper.absoluteVec(Vec3.ZERO).x;
				float reach = ConnectingRods.fitted(cart).simpleBlock$reach();
				helper.assertTrue(Math.abs(cart.getX() - control.getX()) < 1.0E-6, "the cart under the cable should roll exactly like the control cart");
				helper.assertFalse(ConnectingRods.isGripping(control), "the control cart has no cable to grip");
				if (x >= 7.5 && x <= 23.5) {
					reachesBetween.add(reach);
					lostGrip[0] |= reach <= 0.0F;
				} else if (x >= 24.5) {
					gripPast[0] |= reach > 0.0F;
				}
				if (x > 26.0) {
					arrived[0] = ticks[0];
				}
			}))
			.thenExecute(step(failed, () -> {
				helper.assertTrue(arrived[0] > 0, "the carts should have run past the last anchor");
				helper.assertFalse(reachesBetween.isEmpty(), "ticks under the route");
				helper.assertFalse(lostGrip[0], "the rod let go of the cable between the first and the last anchor");
				helper.assertFalse(gripPast[0], "the rod should let go past the last anchor");
				double min = reachesBetween.stream().mapToDouble(Float::doubleValue).min().orElse(0);
				double max = reachesBetween.stream().mapToDouble(Float::doubleValue).max().orElse(0);
				log.add(reachesBetween.size() + " ticks under the cable from A to C, gripping on every one (reach " + String.format("%.2f-%.2f", min, max)
					+ "), let go past C; past x 26 on tick " + arrived[0] + ", in step with the control cart on every tick");
				SimpleBlockTestLog.info("CAB-15/16: {}", log);
				for (Minecart cart : carts) {
					cart.discard();
				}
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * BUG-003 (M07 playtest): a cart's rod grips cable hung from anchors 3, 4 and 5 blocks over the rail
	 * (the cable runs on the rollers 0.75 lower), and, as a control, not from anchors 6 blocks over it.
	 */
	private static void rodReach(GameTestHelper helper, int... heights) {
		ServerPlayer player = TestPlayers.create(helper, GameType.CREATIVE, new BlockPos(1, 1, 9));
		Minecart[] carts = new Minecart[heights.length];
		BlockPos[][] anchors = new BlockPos[heights.length][];
		CableDrumBlockEntity[] drums = new CableDrumBlockEntity[heights.length];
		boolean[] failed = {false};
		helper.startSequence()
			.thenExecute(step(failed, () -> {
				for (int lane = 0; lane < heights.length; lane++) {
					int z = 1 + 3 * lane;
					for (int x = 2; x < 20; x++) {
						helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
						helper.setBlock(new BlockPos(x, 1, z), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
					}
					drums[lane] = windDrum(helper, player, new BlockPos(1, 1, z), 1);
					anchors[lane] = new BlockPos[] {new BlockPos(7, 1 + heights[lane], z), new BlockPos(13, 1 + heights[lane], z)};
					for (BlockPos anchor : anchors[lane]) {
						hangAnchor(helper, player, anchor.above());
					}
				}
			}))
			.thenIdle(2)
			.thenExecute(step(failed, () -> {
				for (int lane = 0; lane < heights.length; lane++) {
					BlockPos at = new BlockPos(1, 1, 1 + 3 * lane);
					teleport(helper, player, new BlockPos(2, 1, 2 + 3 * lane));
					CableHook hook = hook(helper, at);
					player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
					for (BlockPos anchor : anchors[lane]) {
						useBlock(helper, player, anchor, false);
					}
					useBlock(helper, player, anchors[lane][1], false);
					helper.assertValueEqual(drums[lane].route().size(), 2, "anchors routed in lane " + lane);
					carts[lane] = helper.spawn(EntityTypes.MINECART, Vec3.atBottomCenterOf(new BlockPos(10, 1, 1 + 3 * lane)).add(0, 0.0625, 0));
					carts[lane].setDeltaMovement(Vec3.ZERO);
					ConnectingRods.fitted(carts[lane]).simpleBlock$setRod(true);
				}
			}))
			.thenIdle(3)
			.thenExecute(step(failed, () -> {
				List<String> log = new ArrayList<>();
				for (int lane = 0; lane < heights.length; lane++) {
					float reach = ConnectingRods.fitted(carts[lane]).simpleBlock$reach();
					log.add("anchors " + heights[lane] + " over the rail: " + (reach > 0.0F ? String.format("grips at %.2f", reach) : "no grip"));
					boolean expected = heights[lane] <= 5;
					helper.assertTrue(ConnectingRods.isGripping(carts[lane]) == expected, "anchors " + heights[lane] + " blocks over the rail: gripping "
						+ ConnectingRods.isGripping(carts[lane]) + ", expected " + expected);
					carts[lane].discard();
				}
				SimpleBlockTestLog.info("BUG-003 rod reach (cable.rod_reach_max {}): {}", ConnectingRods.reachMax(), log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	@GameTest(structure = YARD, maxTicks = 200)
	public void rodGripsCableUpToFiveBlocksUp(GameTestHelper helper) {
		rodReach(helper, 3, 4, 5);
	}

	@GameTest(structure = YARD, maxTicks = 200)
	public void rodDoesNotGripCableSixBlocksUp(GameTestHelper helper) {
		rodReach(helper, 6);
	}
}
