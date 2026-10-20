package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.rope.BowRope;
import io.github.boxofdog.simpleblock.rope.RopeContent;
import io.github.boxofdog.simpleblock.rope.RopeShot;

/**
 * M13 bow-launched Spelunker's Rope: the range against an arrow in open sky, then the real bow path in a
 * cleared column of the skip shaft (stone ceiling at y 15, floor at y 0).
 */
public class BowRopeGameTests {
	private static final String SHAFT = "simple_block-gametest:skip_shaft";
	private static final int X = 21;
	private static final int Z = 10;
	private static final int FULL_DRAW = 20;

	/**
	 * An arrow and a rope shot loosed side by side, high above everything, with the same draw and aim
	 * (15 degrees up, no spread): where each comes back down through its launch height. Full and half
	 * draws. The rope must fall modestly short of the arrow (between 60% and 90% of its distance), and a
	 * half draw short of a full one.
	 */
	@GameTest(maxTicks = 300)
	public void ropeFliesModestlyShorterThanAnArrow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Vec3 start = new Vec3(helper.absoluteVec(Vec3.ZERO).x, 200.0, helper.absoluteVec(Vec3.ZERO).z + 0.5);
		int chunkX = Mth.floor(start.x) >> 4;
		int chunkZ = Mth.floor(start.z) >> 4;
		for (int i = 0; i <= 10; i++) {
			level.setChunkForced(chunkX + i, chunkZ, true);
		}
		ServerPlayer shooter = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		double[] draws = {1.0, BowRope_power(10)};
		List<Projectile> flying = new ArrayList<>();
		double[][] landed = new double[4][];
		for (int d = 0; d < 2; d++) {
			float power = (float) draws[d];
			AbstractArrow arrow = EntityTypes.ARROW.create(level, EntitySpawnReason.TRIGGERED);
			RopeShot rope = new RopeShot(level, shooter, new ItemStack(RopeContent.SPELUNKER_ROPE));
			Vec3 at = start.add(0.0, 0.0, d * 4.0);
			Vec3 aim = new Vec3(Math.cos(Math.toRadians(15.0)), Math.sin(Math.toRadians(15.0)), 0.0);
			arrow.setPos(at);
			rope.setPos(at.add(0.0, 0.0, 2.0));
			arrow.shoot(aim.x, aim.y, aim.z, power * 3.0F, 0.0F);
			rope.shoot(aim.x, aim.y, aim.z, power * 3.0F * BowRope.SPEED, 0.0F);
			level.addFreshEntity(arrow);
			level.addFreshEntity(rope);
			flying.add(arrow);
			flying.add(rope);
		}
		double[][] last = new double[4][];
		helper.onEachTick(() -> {
			for (int i = 0; i < 4; i++) {
				Projectile p = flying.get(i);
				if (landed[i] != null) {
					continue;
				}
				Vec3 now = p.position();
				if (last[i] != null && now.y < 200.0 && last[i][1] >= 200.0) {
					double t = (last[i][1] - 200.0) / (last[i][1] - now.y);
					landed[i] = new double[] {last[i][0] + (now.x - last[i][0]) * t - start.x, p.tickCount};
				}
				last[i] = new double[] {now.x, now.y};
			}
		});
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(landed[0] != null && landed[1] != null && landed[2] != null && landed[3] != null, "all four back down"))
			.thenExecute(() -> {
				flying.forEach(Projectile::discard);
				for (int i = 0; i <= 10; i++) {
					level.setChunkForced(chunkX + i, chunkZ, false);
				}
				TestPlayers.remove(shooter);
				double fullRatio = landed[1][0] / landed[0][0];
				double halfRatio = landed[3][0] / landed[2][0];
				SimpleBlockTestLog.info("M13 BOW ROPE RANGE (15 degrees up, back to launch height): full draw: arrow {} blocks ({} ticks), rope {} ({} ticks), "
					+ "{}%; half draw ({}): arrow {}, rope {}, {}%", fmt(landed[0][0]), (int) landed[0][1], fmt(landed[1][0]), (int) landed[1][1],
					Math.round(fullRatio * 100), fmt(draws[1]), fmt(landed[2][0]), fmt(landed[3][0]), Math.round(halfRatio * 100));
				helper.assertTrue(fullRatio > 0.6 && fullRatio < 0.9, "full draw: rope modestly short of the arrow: " + fullRatio);
				helper.assertTrue(halfRatio > 0.6 && halfRatio < 0.9, "half draw: rope modestly short of the arrow: " + halfRatio);
				helper.assertTrue(landed[3][0] < landed[1][0] * 0.6, "a half draw falls well short of a full one: " + landed[3][0] + " vs " + landed[1][0]);
			})
			.thenSucceed();
	}

	private static double BowRope_power(int ticks) {
		return net.minecraft.world.item.BowItem.getPowerForTime(ticks);
	}

	private static String fmt(double value) {
		return String.format(java.util.Locale.ROOT, "%.1f", value);
	}

	/** The column of {@link RopeGameTests}, with the whole ceiling at y 15 stone (any upward shot meets an underside). */
	private static void column(GameTestHelper helper) {
		for (int x = X - 3; x <= X + 1; x++) {
			for (int z = Z - 1; z <= Z + 1; z++) {
				for (int y = 0; y <= 15; y++) {
					helper.setBlock(new BlockPos(x, y, z), y == 0 || y == 15 ? Blocks.STONE : Blocks.AIR);
				}
			}
		}
	}

	private static ServerPlayer archer(GameTestHelper helper, int arrows, int ropes) {
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(X - 2, 1, Z));
		player.getInventory().clearContent();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
		if (ropes > 0) {
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(RopeContent.SPELUNKER_ROPE, ropes));
		}
		if (arrows > 0) {
			player.getInventory().setItem(20, new ItemStack(Items.ARROW, arrows));
		}
		return player;
	}

	/** Aims at {@code target} (relative, a block centre plus offset), draws the bow for {@code ticks} and looses it; whether the draw started. */
	private static boolean shoot(GameTestHelper helper, ServerPlayer player, Vec3 target, int ticks) {
		Vec3 d = helper.absoluteVec(target).subtract(player.getEyePosition());
		player.setYRot((float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F);
		player.setXRot((float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG));
		ItemStack bow = player.getMainHandItem();
		player.gameMode.useItem(player, helper.getLevel(), bow, InteractionHand.MAIN_HAND);
		if (!player.isUsingItem()) {
			return false;
		}
		bow.releaseUsing(helper.getLevel(), player, bow.getUseDuration(player) - ticks);
		player.stopUsingItem();
		return true;
	}

	private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
		int total = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private static BlockPos anchor(GameTestHelper helper) {
		for (int x = X - 3; x <= X + 1; x++) {
			for (int z = Z - 1; z <= Z + 1; z++) {
				BlockPos pos = new BlockPos(x, 14, z);
				if (helper.getBlockState(pos).is(RopeContent.ROPE_ANCHOR)) {
					return pos;
				}
			}
		}
		return null;
	}

	private static int entities(GameTestHelper helper, Class<? extends net.minecraft.world.entity.Entity> type) {
		return helper.getLevel().getEntitiesOfClass(type, new AABB(helper.absoluteVec(new Vec3(X - 6, -2, Z - 6)), helper.absoluteVec(new Vec3(X + 6, 18, Z + 6))))
			.size();
	}

	/**
	 * The bow with rope in the offhand: a full draw at the ceiling hangs a rope there (12 blocks down), using
	 * one rope and no arrow, wearing the bow by one and spawning no arrow; a too-short draw shoots nothing;
	 * a shot at the floor (a top face) gives the rope back; with no arrows at all it still draws and shoots;
	 * with the rope gone from the offhand, the bow is vanilla again: it fires an arrow, and will not draw
	 * without one.
	 */
	@GameTest(structure = SHAFT, maxTicks = 200)
	public void bowShootsOffhandRope(GameTestHelper helper) {
		column(helper);
		List<String> log = new ArrayList<>();
		ServerPlayer player = archer(helper, 16, 3);
		Vec3 ceiling = new Vec3(X + 0.5, 15.0, Z + 0.5);
		helper.assertTrue(shoot(helper, player, ceiling, FULL_DRAW), "the bow draws");
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(anchor(helper) != null, "waiting for the rope to hang"))
			.thenExecute(() -> {
				BlockPos anchor = anchor(helper);
				int rope = 0;
				for (int y = 13; y > 0 && helper.getBlockState(new BlockPos(anchor.getX(), y, anchor.getZ())).is(RopeContent.ROPE_BLOCK); y--) {
					rope++;
				}
				helper.assertValueEqual(rope, 12, "the shot rope hangs its 12 blocks");
				helper.assertValueEqual(count(player, Items.ARROW), 16, "no arrow used");
				helper.assertValueEqual(player.getOffhandItem().getCount(), 2, "one rope used");
				helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "the bow wears as for an arrow");
				helper.assertValueEqual(entities(helper, AbstractArrow.class), 0, "no arrow in the air");
				log.add("full draw at the ceiling: anchor at " + anchor + " with 12 rope; arrows 16, ropes 2, bow damage 1, no arrow entity");

				helper.assertTrue(shoot(helper, player, ceiling, 1), "draws");
				helper.assertValueEqual(player.getOffhandItem().getCount(), 2, "a 1-tick draw shoots nothing");
				helper.assertValueEqual(entities(helper, RopeShot.class), 0, "nothing in the air");
				log.add("1-tick draw: nothing shot, nothing used");

				helper.assertTrue(shoot(helper, player, new Vec3(X - 0.5, 1.0, Z + 0.5), FULL_DRAW), "draws at the floor");
				helper.assertValueEqual(player.getOffhandItem().getCount(), 1, "the floor shot took a rope");
				helper.assertValueEqual(entities(helper, RopeShot.class), 1, "the floor shot is in the air");
			})
			.thenWaitUntil(() -> helper.assertValueEqual(entities(helper, RopeShot.class), 0, "waiting for the floor shot"))
			.thenExecute(() -> {
				helper.assertValueEqual(count(player, RopeContent.SPELUNKER_ROPE), 2, "the rope from the floor shot is given back");
				helper.assertTrue(helper.getBlockState(new BlockPos(X - 1, 1, Z)).isAir(), "nothing hung on the floor");
				log.add("shot at the floor (a top face): given back, ropes 2");

				// No arrows at all: rope still draws and shoots (the ceiling over the first anchor is taken; aim beside it).
				player.getInventory().setItem(20, ItemStack.EMPTY);
				helper.assertTrue(shoot(helper, player, new Vec3(X - 2.5, 15.0, Z - 0.5), FULL_DRAW), "draws with no arrows");
			})
			.thenWaitUntil(() -> helper.assertValueEqual(entities(helper, RopeShot.class), 0, "waiting for the arrowless shot"))
			.thenExecute(() -> {
				int anchors = 0;
				for (int x = X - 3; x <= X + 1; x++) {
					for (int z = Z - 1; z <= Z + 1; z++) {
						if (helper.getBlockState(new BlockPos(x, 14, z)).is(RopeContent.ROPE_ANCHOR)) {
							anchors++;
						}
					}
				}
				helper.assertValueEqual(anchors, 2, "the arrowless shot hung a second rope");
				helper.assertValueEqual(count(player, RopeContent.SPELUNKER_ROPE), 1, "ropes left");
				log.add("no arrows: drew and hung a second rope; ropes 1");

				// The rope out of the offhand: the bow is vanilla's again.
				player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				helper.assertFalse(shoot(helper, player, ceiling, FULL_DRAW), "no arrows, no rope: the bow does not draw");
				player.getInventory().setItem(20, new ItemStack(Items.ARROW, 4));
				helper.assertTrue(shoot(helper, player, new Vec3(X + 0.5, 8.0, Z + 1.5), FULL_DRAW), "draws with arrows");
				helper.assertValueEqual(count(player, Items.ARROW), 3, "an arrow used");
				helper.assertValueEqual(entities(helper, AbstractArrow.class), 1, "an arrow in the air");
				helper.assertValueEqual(entities(helper, RopeShot.class), 0, "no rope shot");
				log.add("rope out of the offhand: no draw without arrows; with arrows an ordinary arrow fired");
				SimpleBlockTestLog.info("M13 BOW ROPE: {}", log);
				TestPlayers.remove(player);
			})
			.thenSucceed();
	}
}
