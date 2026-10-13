package io.github.boxofdog.simpleblock.metallurgy;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M08: every block of the furnace except its controller. It has no item and drops nothing itself:
 * using it uses the furnace, breaking it breaks the whole furnace ({@link BlastFurnaceBlock#dismantle}),
 * and picking it gives the furnace item. Only the middle of the top layer has a model (the hopper);
 * the controller draws the rest.
 */
public class BlastFurnacePartBlock extends Block {
	/** Offset from the controller across x and z (0..2, the controller at 1), and the layer (0..3, the controller's is 1). */
	public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 2);
	public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, BlastFurnaceStructure.LAYERS - 1);
	public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 2);
	/** The furnace's facing (its front), for the shape of the blocks along the front. */
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape FULL = Shapes.block();

	public BlastFurnacePartBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(DX, 0).setValue(LAYER, 0).setValue(DZ, 0).setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DX, LAYER, DZ, FACING);
	}

	/**
	 * The block's shape follows the furnace's silhouette along its front, where the tower stands back:
	 * the hearth and the spout in the middle of the front, the open mold on its left (the casting
	 * corner), the footing on its right, and the shaft's overhang above them. Elsewhere it is a full
	 * block. Shapes are made facing north, then turned.
	 */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		int wx = state.getValue(DX) - 1;
		int wz = state.getValue(DZ) - 1;
		// The block's place in the furnace as if it faced north (front row z -1, the mold's side x -1).
		int x = switch (facing) {
			case SOUTH -> -wx;
			case EAST -> wz;
			case WEST -> -wz;
			default -> wx;
		};
		int z = switch (facing) {
			case SOUTH -> -wz;
			case EAST -> -wx;
			case WEST -> wx;
			default -> wz;
		};
		if (z != -1) {
			return FULL;
		}
		int layer = state.getValue(LAYER);
		VoxelShape north;
		if (layer == 0) {
			north = switch (x) {
				// The mold, and the hearth's corner behind it.
				case -1 -> Shapes.or(Block.box(1.0, 0.0, 1.0, 13.0, 7.0, 13.0), Block.box(3.0, 0.0, 10.0, 16.0, 16.0, 16.0));
				// The tap arch with the spout and the runner in front of it.
				case 0 -> Shapes.or(Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0), Block.box(0.0, 0.0, 1.0, 16.0, 9.0, 8.0));
				// Footing, and the hearth's corner.
				default -> Shapes.or(Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0), Block.box(0.0, 0.0, 10.0, 13.0, 16.0, 16.0));
			};
		} else if (layer < BlastFurnaceStructure.HOPPER_LAYER) {
			north = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 16.0);
		} else {
			north = Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 16.0);
		}
		return rotate(north, facing);
	}

	/** Turns a shape made for a furnace facing north to {@code facing}. */
	private static VoxelShape rotate(VoxelShape north, Direction facing) {
		if (facing == Direction.NORTH) {
			return north;
		}
		VoxelShape[] turned = {Shapes.empty()};
		north.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
			VoxelShape box = switch (facing) {
				case SOUTH -> Shapes.box(1.0 - x2, y1, 1.0 - z2, 1.0 - x1, y2, 1.0 - z1);
				case EAST -> Shapes.box(1.0 - z2, y1, x1, 1.0 - z1, y2, x2);
				default -> Shapes.box(z1, y1, 1.0 - x2, z2, y2, 1.0 - x1);
			};
			turned[0] = Shapes.or(turned[0], box);
		});
		return turned[0];
	}

	private static @Nullable BlockPos controller(BlockState state, BlockPos pos) {
		return pos.offset(1 - state.getValue(DX), BlastFurnaceStructure.CONTROLLER_LAYER - state.getValue(LAYER), 1 - state.getValue(DZ));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return BlastFurnaceBlock.open(level, controller(state, pos), player);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlastFurnaceBlock.markBrokenBy(level, controller(state, pos), player);
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		BlastFurnaceBlock.dismantle(level, controller(state, pos));
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(MetallurgyContent.BLAST_FURNACE_ITEM);
	}
}
