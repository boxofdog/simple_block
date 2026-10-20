package io.github.boxofdog.simpleblock.metallurgy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * M08: the casting mold, a whole block at the end of the channel: an iron flask whose cavity is the
 * shape of a full block. M08 playtest (owner): it shows the casting as it happens. While a block is
 * being cast the cavity fills with molten metal from the bottom, eighth by eighth with the cast's real
 * progress ({@link #FILL}); above the molten metal stands the block the mold last cast, if it still
 * holds one ({@link #CONTENT}: its metal), so the next pour takes it over rather than the mold first
 * going empty. A finished cast shows as the solid block (fill 0) for a moment before the next fill
 * shows. Idle, it shows the last block it holds, or its open cavity once emptied. What it shows is only
 * the current cast: the stored count is in the furnace's screen.
 */
public class BlastFurnaceMoldBlock extends BlastFurnaceAttachmentBlock {
	public enum Content implements StringRepresentable {
		/** {@code molten} is no longer shown (the fill is): kept so that molds saved with it still load. */
		EMPTY("empty"), MOLTEN("molten"), IRON("iron"), COPPER("copper"), GOLD("gold");

		private final String name;

		Content(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		/** The metal of the block a mold holding {@code stack} shows (above any molten fill). */
		public static Content of(ItemStack stack) {
			if (stack.is(Items.IRON_BLOCK)) {
				return IRON;
			}
			if (stack.is(Items.GOLD_BLOCK)) {
				return GOLD;
			}
			return stack.isEmpty() ? EMPTY : COPPER;
		}
	}

	public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
	/** Eighths of the cavity filled with molten metal (0: none). */
	public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 8);
	/** M13 BUG-011: metal is running into the mold now (the furnace pours): the mold glows ({@link MetallurgyContent}). */
	public static final BooleanProperty POURING = BlastFurnaceBlock.POURING;

	public BlastFurnaceMoldBlock(BlockBehaviour.Properties properties) {
		super(properties, 2);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONTENT, Content.EMPTY).setValue(FILL, 0)
			.setValue(POURING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, CONTENT, FILL, POURING);
	}

	/**
	 * M13 BUG-012: while metal pours in, a cinder now and then rises from the molten surface, as from lava.
	 */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		// About one or two a second near the mold (a real client measured 46 in 10 s for every display tick, 6 for one in three).
		if (!state.getValue(POURING) || random.nextInt(2) != 0) {
			return;
		}
		double surface = pos.getY() + (1.0 + 14.0 * Math.max(1, state.getValue(FILL)) / BlastFurnaceBlockEntity.FILL_STEPS) / 16.0;
		level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.25 + random.nextDouble() * 0.5, surface, pos.getZ() + 0.25 + random.nextDouble() * 0.5,
			0.0, 0.0, 0.0);
	}
}
