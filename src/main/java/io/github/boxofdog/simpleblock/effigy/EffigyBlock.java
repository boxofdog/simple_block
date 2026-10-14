package io.github.boxofdog.simpleblock.effigy;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M12: a Monster Effigy, two blocks tall (like a door: the lower half holds its facing, the upper half
 * is the rest of the monument). Drawn by the client's effigy renderer at any horizontal angle.
 *
 * <ul>
 *   <li>Obsidian's hardness (50, a diamond pickaxe to mine it well) and a blast resistance no explosion
 *       overcomes; withers and the ender dragon cannot break it either ({@code #wither_immune},
 *       {@code #dragon_immune}).</li>
 *   <li>It drops nothing, however it is broken (no loot table: no effigy, no refund, Silk Touch or not).
 *       Breaking either half takes the other with it.</li>
 *   <li>Pistons cannot push or pull it (immovable, like obsidian); it is solid to fluids and falling blocks; it needs nothing
 *       under it.</li>
 *   <li>It is placed only through the confirmation screen ({@link EffigyItem}); creative pick-block gives
 *       the effigy item.</li>
 * </ul>
 */
public class EffigyBlock extends BaseEntityBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	/** The stake and the figure on it, a post in the middle of each half. */
	private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);

	private final EffigyType type;

	public EffigyBlock(EffigyType type, BlockBehaviour.Properties properties) {
		super(properties);
		this.type = type;
		this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
	}

	public EffigyType type() {
		return this.type;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new EffigyBlockEntity(pos, state) : null;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Either half alone is not an effigy: when the other half goes, this one goes too (dropping nothing). */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
		BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
		DoubleBlockHalf half = state.getValue(HALF);
		Direction toOther = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
		if (direction == toOther && !(neighbour.is(this) && neighbour.getValue(HALF) != half)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(EffigyContent.effigyItem(this.type));
	}

	/** The lower half's position, from either half. */
	public static BlockPos lower(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
	}
}
