package io.github.boxofdog.simpleblock.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * MINE-01: natural deepslate and every deepslate ore need an iron, diamond or netherite pickaxe;
 * crafted deepslate and stone-depth ores keep their vanilla requirements; creative is unaffected.
 */
public class MiningGameTests {
	static final List<Block> GATED = List.of(Blocks.DEEPSLATE, Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_COPPER_ORE,
		Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
		Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
	static final List<Item> TOO_WEAK = List.of(Items.AIR, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.COPPER_PICKAXE,
		Items.GOLDEN_PICKAXE, Items.IRON_SHOVEL);
	static final List<Item> STRONG_ENOUGH = List.of(Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
	static final List<Block> CRAFTED = List.of(Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE_SLAB, Blocks.COBBLED_DEEPSLATE_STAIRS,
		Blocks.COBBLED_DEEPSLATE_WALL, Blocks.POLISHED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.POLISHED_DEEPSLATE_STAIRS,
		Blocks.POLISHED_DEEPSLATE_WALL, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.DEEPSLATE_BRICK_STAIRS,
		Blocks.DEEPSLATE_BRICK_WALL, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_TILE_SLAB,
		Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_WALL, Blocks.CRACKED_DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE);

	private static String name(Object blockOrItem) {
		return blockOrItem instanceof Block block ? BuiltInRegistries.BLOCK.getKey(block).getPath()
			: BuiltInRegistries.ITEM.getKey((Item) blockOrItem).getPath();
	}

	/** Vanilla's mining progress per tick, without any Simple Block rule. */
	private static float vanillaProgress(ServerPlayer player, BlockState state, ServerLevel level, BlockPos pos) {
		return player.getDestroySpeed(state) / state.getDestroySpeed(level, pos) / (player.hasCorrectToolForDrops(state) ? 30 : 100);
	}

	/** MINE-01, MINE-02: every gated block, every tool tier, survival. */
	@GameTest
	public void deepslateAndDeepslateOresNeedIronPickaxe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
		List<String> problems = new ArrayList<>();
		for (Block block : GATED) {
			level.setBlockAndUpdate(pos, block.defaultBlockState());
			BlockState state = level.getBlockState(pos);
			for (Item tool : TOO_WEAK) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
				float progress = state.getDestroyProgress(player, level, pos);
				if (progress != 0.0F || player.hasCorrectToolForDrops(state)) {
					problems.add(name(block) + " with " + name(tool) + ": progress " + progress + ", drops " + player.hasCorrectToolForDrops(state));
				}
			}
			for (Item tool : STRONG_ENOUGH) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
				float progress = state.getDestroyProgress(player, level, pos);
				if (progress <= 0.0F || !player.hasCorrectToolForDrops(state) || progress != vanillaProgress(player, state, level, pos)) {
					problems.add(name(block) + " with " + name(tool) + ": progress " + progress + ", drops " + player.hasCorrectToolForDrops(state));
				}
			}
		}
		SimpleBlockTestLog.info("MINE-01/02: {} gated blocks x {} weak tools blocked, x {} iron+ pickaxes at vanilla speed; problems {}",
			GATED.size(), TOO_WEAK.size(), STRONG_ENOUGH.size(), problems);
		TestPlayers.remove(player);
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/**
	 * MINE-01: the authoritative server-side break, driven through the same packet handler a client
	 * uses. A survival player digs natural deepslate for 100 ticks with a stone pickaxe (vanilla
	 * breaks it in about 23) and reports the break: the server refuses and the block stays. The
	 * refused break must not jam the player's next break: a stone block reported early (before
	 * the server's own count reaches it) still breaks. With an iron pickaxe the deepslate breaks and
	 * drops cobbled deepslate.
	 */
	@GameTest(maxTicks = 300)
	public void serverRejectsBreakingDeepslateWithStonePickaxe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		// Fake players get no movement physics; mining while airborne is five times slower.
		player.setOnGround(true);
		BlockPos deepslateA = new BlockPos(2, 1, 1);
		BlockPos plainStone = new BlockPos(0, 1, 1);
		BlockPos deepslateB = new BlockPos(1, 1, 2);
		helper.setBlock(deepslateA, Blocks.DEEPSLATE);
		helper.setBlock(plainStone, Blocks.STONE);
		helper.setBlock(deepslateB, Blocks.DEEPSLATE);
		int maxY = level.getMaxY();
		int[] sequence = {0};
		java.util.function.BiConsumer<BlockPos, ServerboundPlayerActionPacket.Action> act = (rel, action) ->
			player.gameMode.handleBlockBreakAction(helper.absolutePos(rel), action, Direction.UP, maxY, ++sequence[0]);

		helper.startSequence()
			.thenExecute(() -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_PICKAXE));
				act.accept(deepslateA, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
			})
			.thenIdle(100)
			.thenExecute(() -> act.accept(deepslateA, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK))
			.thenIdle(20)
			.thenExecute(() -> {
				helper.assertBlockPresent(Blocks.DEEPSLATE, deepslateA);
				// Stone with a stone pickaxe takes 12 ticks; report it after 5 so the server finishes it itself.
				act.accept(plainStone, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
			})
			.thenIdle(5)
			.thenExecute(() -> act.accept(plainStone, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK))
			.thenWaitUntil(() -> helper.assertBlockNotPresent(Blocks.STONE, plainStone))
			.thenExecute(() -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
				act.accept(deepslateB, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
			})
			.thenIdle(20)
			.thenExecute(() -> act.accept(deepslateB, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK))
			.thenWaitUntil(() -> helper.assertBlockNotPresent(Blocks.DEEPSLATE, deepslateB))
			.thenWaitUntil(() -> {
				List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(deepslateB)).inflate(3),
					item -> item.getItem().is(Items.COBBLED_DEEPSLATE));
				helper.assertTrue(!drops.isEmpty(), "iron pickaxe should drop cobbled deepslate");
			})
			.thenExecute(() -> {
				SimpleBlockTestLog.info("MINE-01 server break: stone pickaxe 100 ticks -> deepslate refused and still present; next early-reported "
					+ "stone break still completes; iron pickaxe -> deepslate broken, cobbled deepslate dropped");
				TestPlayers.remove(player);
			})
			.thenSucceed();
	}

	/** MINE-03: crafted deepslate keeps vanilla requirements: a wooden pickaxe mines it at vanilla speed. */
	@GameTest
	public void craftedDeepslateKeepsVanillaRequirements(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
		List<String> problems = new ArrayList<>();
		for (Block block : CRAFTED) {
			level.setBlockAndUpdate(pos, block.defaultBlockState());
			BlockState state = level.getBlockState(pos);
			float progress = state.getDestroyProgress(player, level, pos);
			if (progress <= 0.0F || !player.hasCorrectToolForDrops(state) || progress != vanillaProgress(player, state, level, pos)) {
				problems.add(name(block) + ": progress " + progress + ", drops " + player.hasCorrectToolForDrops(state));
			}
		}
		SimpleBlockTestLog.info("MINE-03: {} crafted deepslate blocks mined by a wooden pickaxe at vanilla speed with drops; problems {}",
			CRAFTED.size(), problems);
		TestPlayers.remove(player);
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** MINE-02: stone-depth ores keep vanilla tiers (e.g. iron ore needs stone, diamond ore needs iron). */
	@GameTest
	public void stoneOresKeepVanillaTiers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.SURVIVAL, new BlockPos(1, 1, 1));
		BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
		Object[][] expectations = {
			{Blocks.COAL_ORE, Items.WOODEN_PICKAXE, true}, {Blocks.IRON_ORE, Items.WOODEN_PICKAXE, false},
			{Blocks.IRON_ORE, Items.STONE_PICKAXE, true}, {Blocks.COPPER_ORE, Items.STONE_PICKAXE, true},
			{Blocks.LAPIS_ORE, Items.STONE_PICKAXE, true}, {Blocks.GOLD_ORE, Items.STONE_PICKAXE, false},
			{Blocks.GOLD_ORE, Items.IRON_PICKAXE, true}, {Blocks.DIAMOND_ORE, Items.STONE_PICKAXE, false},
			{Blocks.DIAMOND_ORE, Items.IRON_PICKAXE, true}, {Blocks.REDSTONE_ORE, Items.IRON_PICKAXE, true},
			{Blocks.STONE, Items.WOODEN_PICKAXE, true}};
		List<String> problems = new ArrayList<>();
		for (Object[] row : expectations) {
			Block block = (Block) row[0];
			level.setBlockAndUpdate(pos, block.defaultBlockState());
			BlockState state = level.getBlockState(pos);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack((Item) row[1]));
			float progress = state.getDestroyProgress(player, level, pos);
			// Even the wrong tool still breaks a stone-depth ore slowly (vanilla), just without drops.
			if (player.hasCorrectToolForDrops(state) != (Boolean) row[2] || progress <= 0.0F
				|| progress != vanillaProgress(player, state, level, pos)) {
				problems.add(name(block) + " with " + name(row[1]) + ": drops " + player.hasCorrectToolForDrops(state) + ", progress " + progress);
			}
		}
		SimpleBlockTestLog.info("MINE-02: {} stone-depth ore/tool pairs keep vanilla tiers and speed; problems {}", expectations.length, problems);
		TestPlayers.remove(player);
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	/** MINE-01: creative players still break natural deepslate instantly with an empty hand. */
	@GameTest
	public void creativeBreaksDeepslateInstantly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = TestPlayers.create(helper, GameType.CREATIVE, new BlockPos(1, 1, 1));
		BlockPos rel = new BlockPos(2, 1, 2);
		BlockPos pos = helper.absolutePos(rel);
		level.setBlockAndUpdate(pos, Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState());
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, Direction.UP, level.getMaxY(), 1);
		helper.assertBlockNotPresent(Blocks.DEEPSLATE_DIAMOND_ORE, rel);
		SimpleBlockTestLog.info("MINE-01 creative: empty-handed creative player removed deepslate diamond ore instantly");
		TestPlayers.remove(player);
		helper.succeed();
	}
}
