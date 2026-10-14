package io.github.boxofdog.simpleblock.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block of hanging Spelunker's Rope, or ({@link #coil}) the coil of spare rope on the lowest block
 * ({@link SpelunkerRopes}). Climbable, nothing to walk into; it hangs from the rope or anchor above.
 * Its going takes the whole line down.
 */
public class RopeLineBlock extends Block {
	private static final VoxelShape ROPE = Block.box(6.5, 0.0, 6.5, 9.5, 16.0, 9.5);
	private static final VoxelShape COIL = Shapes.or(Block.box(3.0, 0.0, 3.0, 13.0, 4.0, 13.0), ROPE);
	private final boolean coil;

	public RopeLineBlock(BlockBehaviour.Properties properties, boolean coil) {
		super(properties);
		this.coil = coil;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.coil ? COIL : ROPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState above = level.getBlockState(pos.above());
		return above.is(RopeContent.ROPE_ANCHOR) || above.is(RopeContent.ROPE_BLOCK);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
		BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return directionToNeighbour == Direction.UP && !state.canSurvive(level, pos)
			? Blocks.AIR.defaultBlockState()
			: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return takeDown(level, pos, player);
	}

	/** Sneak-use with an empty hand: the whole line comes up into the inventory. */
	static InteractionResult takeDown(Level level, BlockPos pos, Player player) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel serverLevel) {
			SpelunkerRopes.retrieve(serverLevel, pos, null, player, pos, null);
			SpelunkerRopes.tell(player, SpelunkerRopes.message("taken_down", "Rope taken down"));
		}
		return InteractionResult.SUCCESS;
	}

	/** Gone in any other way (broken, washed away, pushed): the rest of the line comes down and drops here. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		// Rope turned to coil or back (letting the line out) is not the line going.
		if (!SpelunkerRopes.retrieving() && !SpelunkerRopes.isLine(level.getBlockState(pos))) {
			SpelunkerRopes.retrieve(level, pos.above(), pos, null, pos, null);
		}
	}
}
