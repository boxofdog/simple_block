package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.cable.CableAnchorBlock;
import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrive;
import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.cable.CableHook;
import io.github.boxofdog.simpleblock.cable.CableLines;
import io.github.boxofdog.simpleblock.cable.ConnectingRods;

/**
 * M07 playtest change 2 (owner's decisions, 2026-10-14): two drums and one cable between them, worked
 * from either end. The yard line is {@link CableLoopGameTests#line}: the first drum at x 1, anchors at
 * x 7, 15 and 23 (y 3), rails along z 4 at y 1 from x 2. The second drum stands at the end of the line,
 * in place of the rail at x 26.
 */
public class TwoDrumGameTests {
	private static final String YARD = "simple_block-gametest:cable_yard";
	private static final BlockPos HOME = CableLoopGameTests.DRUM;
	private static final BlockPos FAR = new BlockPos(26, 1, 4);

	/** An empty drum at {@code pos}, as a player would place it. */
	private static CableDrumBlockEntity placeDrum(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos.below(), Blocks.STONE);
		helper.setBlock(pos, CableContent.CABLE_DRUM.defaultBlockState());
		return CableGameTests.drum(helper, pos);
	}

	/** Picks up the first drum's end, runs it through the anchors and uses the second drum with it. */
	private static void attach(GameTestHelper helper, ServerPlayer player, BlockPos far) {
		pickUp(helper, player);
		for (BlockPos anchor : CableLoopGameTests.ANCHORS) {
			CableGameTests.useBlock(helper, player, anchor, false);
		}
		CableGameTests.useBlock(helper, player, far, false);
	}

	/** Walks to the first drum's cable end and picks it up (a held end is found within 4 blocks of its holder). */
	private static void pickUp(GameTestHelper helper, ServerPlayer player) {
		CableHook hook = CableGameTests.hook(helper, HOME);
		player.teleportTo(hook.getX(), hook.getY(), hook.getZ() + 1.0);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.interactOn(hook, InteractionHand.MAIN_HAND, hook.position());
	}

	private static Vec3 crankHit(GameTestHelper helper, BlockPos relative) {
		BlockPos pos = helper.absolutePos(relative);
		BlockState state = helper.getLevel().getBlockState(pos);
		Direction side = CableDrumBlock.crankSide(state);
		double axleY = state.getValue(CableDrumBlock.HANGING) ? 6.5 / 16.0 : 9.5 / 16.0;
		return Vec3.atLowerCornerOf(pos).add(0.5 + side.getStepX() * 0.55, axleY, 0.5 + side.getStepZ() * 0.55);
	}

	/** A right-click on a drum's crank or lever (pull towards that drum). */
	static void rightClick(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
		BlockPos pos = helper.absolutePos(relative);
		Vec3 hit = crankHit(helper, relative);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
			new BlockHitResult(hit, CableDrumBlock.crankSide(helper.getLevel().getBlockState(pos)), pos, false));
	}

	/** A left-click on a drum's crank or lever (send away), as the client's payload: the player is first put beside it. */
	static void leftClick(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
		BlockPos pos = helper.absolutePos(relative);
		Vec3 stand = Vec3.atBottomCenterOf(pos.south(2));
		player.teleportTo(stand.x, stand.y, stand.z);
		CableDrive.feed(player, pos, crankHit(helper, relative));
	}

	private static CableLines.Line line(GameTestHelper helper) {
		CableLines.Line line = CableLines.line(helper.getLevel(), helper.absolutePos(HOME));
		if (line == null) {
			throw helper.assertionException(net.minecraft.network.chat.Component.literal("no cable line"));
		}
		return line;
	}

	/**
	 * TWIN-01, hand cranks: the end used on an empty drum at the end of the straight line is made fast
	 * to it (no wheel, no return guides, the hook gone, the second drum's barrel wound, both drums turned
	 * along the line). Its crank pulls a gripping cart towards it, the first drum's pulls it back, and
	 * when both are cranked the last turn wins. Sneak-using the second drum takes the end off: an open
	 * route again, the end back at the last anchor and the coil still on the first drum.
	 */
	@GameTest(structure = YARD, maxTicks = 600)
	public void twoDrumsCrankFromEitherEnd(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(12, 1, 6));
		CableDrumBlockEntity home = CableLoopGameTests.line(helper, player, 1);
		CableDrumBlockEntity[] far = new CableDrumBlockEntity[1];
		Minecart[] cart = new Minecart[1];
		double[] marks = new double[4];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				far[0] = placeDrum(helper, FAR);
				attach(helper, player, FAR);
				helper.assertTrue(home.looped(), "driven like a loop");
				helper.assertValueEqual(home.endDrum(), helper.absolutePos(FAR), "the cable ends on the second drum");
				helper.assertValueEqual(far[0].farFrom(), helper.absolutePos(HOME), "the second drum holds the first's end");
				helper.assertValueEqual(line(helper).far(), helper.absolutePos(FAR), "the line knows its second drum");
				helper.assertValueEqual(CableLoopGameTests.returnState(helper, CableLoopGameTests.ANCHORS[2]), CableAnchorBlock.Return.NONE,
					"no return wheel");
				helper.assertValueEqual(CableLoopGameTests.returnState(helper, CableLoopGameTests.ANCHORS[0]), CableAnchorBlock.Return.NONE,
					"no return guides");
				helper.assertTrue(helper.getBlockState(FAR).getValue(CableDrumBlock.WOUND), "the second drum's barrel shows cable");
				helper.assertValueEqual(helper.getBlockState(FAR).getValue(CableDrumBlock.FACING), Direction.WEST, "the second drum faces the line");
				helper.assertTrue(home.hasCoil() && !far[0].hasCoil(), "one coil, on the first drum");
				helper.assertTrue(!far[0].showsLever(), "hand-cranked: the second drum has a crank");
				log.add("end made fast to the drum at x 26: no wheel or guides, barrel wound, facing west, one coil on the first drum");
				cart[0] = CableLoopGameTests.rodCart(helper, 12.5);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(ConnectingRods.isGripping(cart[0]), "gripping");
				marks[0] = CableLoopGameTests.x(helper, cart[0]);
			}))
			.thenExecuteFor(40, CableGameTests.step(failed, () -> {
				if (helper.getTick() % 4 == 0) {
					rightClick(helper, player, FAR);
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[1] = CableLoopGameTests.x(helper, cart[0]);
				helper.assertTrue(marks[1] > marks[0] + 1.0, "the second drum's crank pulls the cart towards it: x " + marks[0] + " -> " + marks[1]);
			}))
			.thenExecuteFor(40, CableGameTests.step(failed, () -> {
				if (helper.getTick() % 4 == 0) {
					rightClick(helper, player, HOME);
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[2] = CableLoopGameTests.x(helper, cart[0]);
				helper.assertTrue(marks[2] < marks[1] - 1.0, "the first drum's crank pulls it back: x " + marks[1] + " -> " + marks[2]);
			}))
			.thenExecuteFor(40, CableGameTests.step(failed, () -> {
				if (helper.getTick() % 4 == 0) {
					// Both ends cranked in the same tick, the second drum last: its turn wins.
					rightClick(helper, player, HOME);
					rightClick(helper, player, FAR);
				}
			}))
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[3] = CableLoopGameTests.x(helper, cart[0]);
				helper.assertTrue(marks[3] > marks[2] + 1.0, "the last turn wins: x " + marks[2] + " -> " + marks[3]);
				log.add(String.format("cart x %.2f -> %.2f (second drum's crank) -> %.2f (first drum's) -> %.2f (both, second last)", marks[0], marks[1],
					marks[2], marks[3]));
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				CableGameTests.useBlock(helper, player, FAR, true);
				helper.assertTrue(!home.looped() && home.endDrum() == null, "the end is off the second drum");
				helper.assertTrue(far[0].farFrom() == null, "the second drum is a plain drum again");
				helper.assertTrue(line(helper).far() == null && !line(helper).looped(), "the line is an open route");
				helper.assertValueEqual(home.route().size(), 3, "the route stays laid out");
				helper.assertTrue(home.hasCoil(), "the coil stays on the first drum");
				helper.assertTrue(!helper.getBlockState(FAR).getValue(CableDrumBlock.WOUND), "the second drum's barrel is bare again");
			}))
			.thenIdle(3)
			.thenExecute(CableGameTests.step(failed, () -> {
				CableHook hook = CableGameTests.hook(helper, HOME);
				helper.assertValueEqual(hook.endPos(), helper.absolutePos(CableLoopGameTests.ANCHORS[2]), "the end is back at the last anchor");
				log.add("sneak-use on the second drum: open route of 3 anchors, end back at x 23, coil kept");
				SimpleBlockTestLog.info("TWIN-01: {}", log);
				cart[0].discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * TWIN-02, coal winch: the second drum shows the winch's lever (no motor). Its right-click sets PULL
	 * from its end (FEED at the winch, shown mirrored on both levers) and the cart runs to that end, where
	 * the lever goes back to NEUTRAL; its left-click sends the cart back; a click at the winch then takes
	 * over (last command wins). Coal burns only at the winch; the second drum takes no coal or furnace.
	 * Breaking the second drum takes the end off and drops nothing of the winch's.
	 */
	@GameTest(structure = YARD, maxTicks = 1200)
	public void coalWinchWorksFromTheSecondDrum(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(12, 1, 6));
		CableDrumBlockEntity home = CableLoopGameTests.line(helper, player, 1);
		CableDrumBlockEntity[] far = new CableDrumBlockEntity[1];
		Minecart[] cart = new Minecart[1];
		double[] marks = new double[4];
		int[] burn = new int[2];
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FURNACE));
				CableGameTests.useBlock(helper, player, HOME, Direction.UP);
				for (int i = 0; i < 2; i++) {
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL));
					CableGameTests.useBlock(helper, player, HOME, Direction.UP);
				}
				helper.assertTrue(home.powered() && line(helper).coal() == 2, "the first drum is a coal winch with 2 coal");
				far[0] = placeDrum(helper, FAR);
				attach(helper, player, FAR);
				helper.assertTrue(far[0].showsLever() && !far[0].powered(), "the second drum shows the lever, without the motor");
				helper.assertTrue(!helper.getBlockState(FAR).getValue(CableDrumBlock.POWERED), "no furnace motor on the second drum");

				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 3));
				CableGameTests.useBlock(helper, player, FAR, Direction.UP);
				helper.assertValueEqual(player.getMainHandItem().getCount(), 3, "the second drum takes no coal");
				helper.assertValueEqual(line(helper).coal(), 2, "coal stays at the winch");
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FURNACE));
				CableGameTests.useBlock(helper, player, FAR, Direction.UP);
				helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "the second drum takes no furnace");
				helper.assertTrue(!far[0].powered(), "still no furnace");
				log.add("second drum: lever without motor; coal and furnace refused (coal at the winch 2)");
				cart[0] = CableLoopGameTests.rodCart(helper, 12.5);
			}))
			.thenIdle(5)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertTrue(ConnectingRods.isGripping(cart[0]), "gripping");
				marks[0] = CableLoopGameTests.x(helper, cart[0]);
				rightClick(helper, player, FAR);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "a right-click at the second drum is FEED at the winch");
				helper.assertValueEqual(far[0].lever(), CableLines.Lever.PULL, "and PULL on the second drum's own lever");
				helper.assertValueEqual(home.lever(), CableLines.Lever.FEED, "the winch's lever shows FEED");
			}))
			.thenWaitUntil(() -> helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "the lever back to NEUTRAL at the end"))
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[1] = CableLoopGameTests.x(helper, cart[0]);
				CableLines.Line line = line(helper);
				helper.assertTrue("end".equals(line.lastStop()) || "track_end".equals(line.lastStop()), "stopped at the end of the line: " + line.lastStop());
				helper.assertTrue(marks[1] > 22.0 && marks[1] < 25.5, "at the second drum's end, short of it at x 26.5: x " + marks[1]);
				helper.assertValueEqual(far[0].lever(), CableLines.Lever.NEUTRAL, "the second drum's lever is back too");
				burn[0] = line.burn();
				helper.assertTrue(line.coal() == 1 && burn[0] < 1600, "the winch's coal burnt: coal " + line.coal() + ", burn " + burn[0]);
				log.add(String.format("PULL at the second drum: x %.2f -> %.2f, stopped (end), coal at the winch 1 + %d ticks", marks[0], marks[1], burn[0]));
				leftClick(helper, player, FAR);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.PULL, "a left-click at the second drum is PULL at the winch");
				helper.assertValueEqual(far[0].lever(), CableLines.Lever.FEED, "FEED on its own lever");
			}))
			.thenIdle(40)
			.thenExecute(CableGameTests.step(failed, () -> {
				marks[2] = CableLoopGameTests.x(helper, cart[0]);
				helper.assertTrue(marks[2] < marks[1] - 1.0, "sent back from the second drum: x " + marks[1] + " -> " + marks[2]);
				// The winch's own right-click is PULL: the same as the setting, so NEUTRAL; then FEED takes over.
				rightClick(helper, player, HOME);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "PULL again at the winch: NEUTRAL");
				leftClick(helper, player, HOME);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.FEED, "the winch's left-click takes over: FEED");
				rightClick(helper, player, FAR);
				helper.assertValueEqual(line(helper).lever(), CableLines.Lever.NEUTRAL, "PULL at the second drum is FEED at the winch, again: NEUTRAL");
				log.add(String.format("left-click at the second drum: back to x %.2f; then the last click from either end wins", marks[2]));
				burn[1] = line(helper).burn() + 1600 * line(helper).coal();
			}))
			.thenIdle(20)
			.thenExecute(CableGameTests.step(failed, () -> {
				helper.assertValueEqual(line(helper).burn() + 1600 * line(helper).coal(), burn[1], "no coal burns at NEUTRAL");
				helper.getLevel().destroyBlock(helper.absolutePos(FAR), true, player);
				helper.assertTrue(!home.looped() && home.endDrum() == null, "breaking the second drum takes the end off");
				helper.assertTrue(home.powered() && home.hasCoil(), "the winch keeps its furnace and coil");
				List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FAR)).inflate(3.0));
				helper.assertValueEqual(TrainGameTests.total(drops, Items.FURNACE), 0, "no furnace from the second drum");
				helper.assertValueEqual(TrainGameTests.total(drops, CableContent.CABLE_COIL), 0, "no coil from the second drum");
				helper.assertValueEqual(TrainGameTests.total(drops, CableContent.CABLE_DRUM_ITEM), 1, "the drum itself");
				log.add("second drum broken: end off, the winch keeps furnace and coil; drops: the drum only");
				SimpleBlockTestLog.info("TWIN-02: {}", log);
				drops.forEach(ItemEntity::discard);
				cart[0].discard();
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}

	/**
	 * TWIN-03, refusals: the end is not made fast to a drum off the line (not straight), to a drum with
	 * its own coil, or beyond the longest span; the existing loop with its return wheel still closes and
	 * then refuses a second drum.
	 */
	@GameTest(structure = YARD, maxTicks = 200)
	public void secondDrumRefusals(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(12, 1, 6));
		CableDrumBlockEntity home = CableLoopGameTests.line(helper, player, 1);
		List<String> log = new ArrayList<>();
		boolean[] failed = {false};
		helper.startSequence()
			.thenIdle(2)
			.thenExecute(CableGameTests.step(failed, () -> {
				BlockPos aside = new BlockPos(26, 1, 7);
				CableDrumBlockEntity offLine = placeDrum(helper, aside);
				attach(helper, player, aside);
				helper.assertTrue(!home.looped() && offLine.farFrom() == null, "a drum off the line is refused");
				log.add("drum off the line: refused");
				CableGameTests.useBlock(helper, player, CableLoopGameTests.ANCHORS[2], false);

				BlockPos wound = new BlockPos(26, 1, 4);
				CableDrumBlockEntity withCoil = placeDrum(helper, wound);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CableContent.CABLE_COIL));
				CableGameTests.useBlock(helper, player, wound, Direction.UP);
				helper.assertTrue(withCoil.hasCoil(), "coil fitted to the would-be second drum");
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				pickUp(helper, player);
				CableGameTests.useBlock(helper, player, wound, false);
				helper.assertTrue(!home.looped() && withCoil.farFrom() == null, "a drum with its own coil is refused");
				log.add("drum with a coil: refused");
				CableGameTests.useBlock(helper, player, CableLoopGameTests.ANCHORS[2], false);

				BlockPos distant = new BlockPos(29, 1, 4);
				CableDrumBlockEntity tooFar = placeDrum(helper, distant);
				helper.setBlock(new BlockPos(26, 1, 4), Blocks.AIR);
				pickUp(helper, player);
				int saved = (int) io.github.boxofdog.simpleblock.tuning.Tuning.getInt("cable.max_span");
				io.github.boxofdog.simpleblock.tuning.Tuning.set("cable.max_span", 5);
				CableGameTests.useBlock(helper, player, distant, false);
				io.github.boxofdog.simpleblock.tuning.Tuning.set("cable.max_span", saved);
				helper.assertTrue(!home.looped() && tooFar.farFrom() == null, "a span longer than the longest is refused");
				log.add("span of 6.3 with the longest at 5: refused");
				CableGameTests.useBlock(helper, player, CableLoopGameTests.ANCHORS[2], false);

				CableLoopGameTests.close(helper, player, CableLoopGameTests.ANCHORS[2]);
				helper.assertTrue(home.looped() && home.endDrum() == null, "a loop with its return wheel still closes");
				helper.assertValueEqual(CableLoopGameTests.returnState(helper, CableLoopGameTests.ANCHORS[2]), CableAnchorBlock.Return.WHEEL,
					"the loop's wheel");
				log.add("the one-drum loop closes as before");
				SimpleBlockTestLog.info("TWIN-03: {}", log);
				TestPlayers.remove(player);
			}))
			.thenSucceed();
	}
}
