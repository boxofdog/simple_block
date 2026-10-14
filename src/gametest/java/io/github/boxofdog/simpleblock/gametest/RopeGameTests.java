package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import io.github.boxofdog.simpleblock.rope.RopeContent;

/**
 * M10 Rope and Spelunker's Rope, in a column of the skip shaft cleared to open air: a stone support at
 * y 15, the floor at y 0.
 */
public class RopeGameTests {
	private static final String SHAFT = "simple_block-gametest:skip_shaft";
	private static final int X = 21;
	private static final int Z = 10;

	/** Clears the column (and its neighbours) and puts a stone support at the top and stone floor at the bottom. */
	private static void column(GameTestHelper helper) {
		for (int x = X - 3; x <= X + 1; x++) {
			for (int z = Z - 1; z <= Z + 1; z++) {
				for (int y = 0; y <= 15; y++) {
					helper.setBlock(new BlockPos(x, y, z), y == 0 ? Blocks.STONE : Blocks.AIR);
				}
			}
		}
		helper.setBlock(new BlockPos(X, 15, Z), Blocks.STONE);
	}

	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos relative, Direction face, ItemStack held, boolean sneaking) {
		BlockPos pos = helper.absolutePos(relative);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		player.setShiftKeyDown(sneaking);
		Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
		player.setShiftKeyDown(false);
	}

	/** The line under the anchor at {@code anchor}: rope blocks, coil at the bottom or not. */
	private static int[] line(GameTestHelper helper, BlockPos anchor) {
		int ropes = 0;
		int coils = 0;
		for (int y = anchor.getY() - 1; y >= 0; y--) {
			var state = helper.getBlockState(new BlockPos(anchor.getX(), y, anchor.getZ()));
			if (state.is(RopeContent.ROPE_BLOCK)) {
				ropes++;
			} else if (state.is(RopeContent.ROPE_COIL)) {
				coils++;
			}
		}
		return new int[] {ropes, coils};
	}

	private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
		int total = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	/**
	 * ROPE-01: under a block, the Spelunker's Rope hangs its 12 blocks; one Rope lets it down to the
	 * floor, the spare 11 lying coiled on the lowest block; more Rope is refused (nothing used); sneak-use
	 * takes it all up (1 Spelunker's Rope and 1 Rope back). Refused on a top face and with no room below.
	 * Rope and coil are climbable.
	 */
	@GameTest(structure = SHAFT, maxTicks = 40)
	public void spelunkersRopeHangsExtendsAndComesBack(GameTestHelper helper) {
		column(helper);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(X - 2, 1, Z));
		List<String> log = new ArrayList<>();
		BlockPos anchor = new BlockPos(X, 14, Z);
		use(helper, player, new BlockPos(X, 15, Z), Direction.DOWN, new ItemStack(RopeContent.SPELUNKER_ROPE), false);
		helper.assertTrue(helper.getBlockState(anchor).is(RopeContent.ROPE_ANCHOR), "anchor under the stone");
		int[] hung = line(helper, anchor);
		helper.assertTrue(hung[0] == 12 && hung[1] == 0, "12 blocks of rope, no coil: " + java.util.Arrays.toString(hung));
		helper.assertTrue(helper.getBlockState(new BlockPos(X, 1, Z)).isAir(), "air below the end");
		log.add("hung under a block: anchor and 12 rope (y 13-2), the end in the air");

		ItemStack ropes = new ItemStack(RopeContent.ROPE, 3);
		use(helper, player, new BlockPos(X, 5, Z), Direction.EAST, ropes, false);
		int[] extended = line(helper, anchor);
		helper.assertTrue(extended[0] == 12 && extended[1] == 1, "down to the floor, coil on the lowest block: " + java.util.Arrays.toString(extended));
		helper.assertTrue(helper.getBlockState(new BlockPos(X, 1, Z)).is(RopeContent.ROPE_COIL), "coil at y 1, on the floor");
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "one Rope used");
		use(helper, player, new BlockPos(X, 5, Z), Direction.EAST, player.getMainHandItem(), false);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "on the ground: no Rope used");
		helper.assertTrue(helper.getBlockState(new BlockPos(X, 0, Z)).is(Blocks.STONE), "the floor is untouched");
		log.add("one Rope: 1 more block to the floor, the spare 11 coiled on y 1; another Rope refused, not used; floor untouched");

		Zombie zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.EVENT);
		zombie.setNoAi(true);
		zombie.snapTo(helper.absoluteVec(new Vec3(X + 0.5, 6, Z + 0.5)), 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(zombie);
		boolean onRope = zombie.onClimbable();
		zombie.snapTo(helper.absoluteVec(new Vec3(X + 0.5, 1, Z + 0.5)), 0.0F, 0.0F);
		boolean onCoil = zombie.onClimbable();
		zombie.discard();
		helper.assertTrue(onRope && onCoil, "rope and coil are climbable: " + onRope + "/" + onCoil);
		log.add("climbable: on the rope " + onRope + ", on the coil " + onCoil);

		player.getInventory().clearContent();
		use(helper, player, new BlockPos(X, 7, Z), Direction.EAST, ItemStack.EMPTY, true);
		int[] gone = line(helper, anchor);
		helper.assertTrue(helper.getBlockState(anchor).isAir() && gone[0] == 0 && gone[1] == 0, "the whole line is taken up");
		helper.assertTrue(count(player, RopeContent.SPELUNKER_ROPE) == 1 && count(player, RopeContent.ROPE) == 1,
			"back: 1 Spelunker's Rope and 1 Rope, got " + count(player, RopeContent.SPELUNKER_ROPE) + " + " + count(player, RopeContent.ROPE));
		log.add("sneak-use: whole line taken up, 1 Spelunker's Rope + 1 Rope back");

		use(helper, player, new BlockPos(X, 0, Z), Direction.UP, new ItemStack(RopeContent.SPELUNKER_ROPE), false);
		helper.assertTrue(helper.getBlockState(new BlockPos(X, 1, Z)).isAir() && player.getMainHandItem().getCount() == 1, "refused on a top face");
		helper.setBlock(new BlockPos(X, 13, Z), Blocks.STONE);
		use(helper, player, new BlockPos(X, 15, Z), Direction.DOWN, player.getMainHandItem(), false);
		helper.assertTrue(helper.getBlockState(anchor).isAir() && player.getMainHandItem().getCount() == 1, "refused with no open air below");
		log.add("refused: on a top face, and with a block right under the anchor");
		SimpleBlockTestLog.info("ROPE-01: {}", log);
		TestPlayers.remove(player);
		helper.succeed();
	}

	/**
	 * ROPE-02: fixed on a side, the line hangs; a Rope lets it down to the floor (spare coiled); breaking a
	 * rope in the middle takes the whole line down and drops what it was made of there: 1 Spelunker's Rope
	 * and 1 Rope. Removing the support does the same.
	 */
	@GameTest(structure = SHAFT, maxTicks = 40)
	public void breakingAnyPartReturnsTheWholeLine(GameTestHelper helper) {
		column(helper);
		helper.setBlock(new BlockPos(X - 1, 15, Z), Blocks.AIR);
		helper.setBlock(new BlockPos(X - 1, 14, Z), Blocks.STONE);
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(X - 2, 1, Z));
		List<String> log = new ArrayList<>();
		// The stone at (X-1, 14) has its east face open: the anchor goes at (X, 14).
		helper.setBlock(new BlockPos(X, 15, Z), Blocks.AIR);
		use(helper, player, new BlockPos(X - 1, 14, Z), Direction.EAST, new ItemStack(RopeContent.SPELUNKER_ROPE), false);
		BlockPos anchor = new BlockPos(X, 14, Z);
		helper.assertTrue(helper.getBlockState(anchor).is(RopeContent.ROPE_ANCHOR), "anchor on the side");
		helper.assertValueEqual(line(helper, anchor)[0], 12, "12 rope");
		ItemStack ropes = new ItemStack(RopeContent.ROPE, 5);
		use(helper, player, new BlockPos(X, 9, Z), Direction.EAST, ropes, false);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "one Rope used");
		int[] down = line(helper, anchor);
		helper.assertTrue(down[0] + down[1] == 13 && down[1] == 1, "to the floor with a coil: " + java.util.Arrays.toString(down));
		log.add("on a side: 12 rope; one Rope: to the floor, 11 spare coiled");

		helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(X, 7, Z)), true, player);
		int[] gone = line(helper, anchor);
		helper.assertTrue(helper.getBlockState(anchor).isAir() && gone[0] == 0 && gone[1] == 0, "the whole line came down");
		List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(X, 7, Z))).inflate(2.0));
		int spelunker = TrainGameTests.total(drops, RopeContent.SPELUNKER_ROPE);
		int rope = TrainGameTests.total(drops, RopeContent.ROPE);
		helper.assertTrue(spelunker == 1 && rope == 1, "dropped 1 Spelunker's Rope and 1 Rope, got " + spelunker + " + " + rope);
		log.add("a rope broken mid-line: the whole line came down, dropping " + spelunker + " Spelunker's Rope + " + rope + " Rope there");

		drops.forEach(ItemEntity::discard);
		use(helper, player, new BlockPos(X - 1, 14, Z), Direction.EAST, new ItemStack(RopeContent.SPELUNKER_ROPE), false);
		helper.setBlock(new BlockPos(X - 1, 14, Z), Blocks.AIR);
		helper.assertTrue(helper.getBlockState(anchor).isAir() && line(helper, anchor)[0] == 0, "support gone: the line came down");
		int back = TrainGameTests.total(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(anchor)).inflate(2.0)),
			RopeContent.SPELUNKER_ROPE);
		helper.assertValueEqual(back, 1, "its Spelunker's Rope dropped at the anchor");
		log.add("support removed: line down, its Spelunker's Rope dropped");
		SimpleBlockTestLog.info("ROPE-02: {}", log);
		helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40.0)).forEach(ItemEntity::discard);
		TestPlayers.remove(player);
		helper.succeed();
	}
}
