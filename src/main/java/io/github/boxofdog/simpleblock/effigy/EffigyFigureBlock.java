package io.github.boxofdog.simpleblock.effigy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Not a placeable block: it only carries the four figures' models ({@code block/effigy/<type>}, two
 * blocks tall), which the client's effigy renderer draws turned to each effigy's angle (as
 * {@code cargo_fill} does for a cart's load).
 */
public class EffigyFigureBlock extends Block {
	public static final EnumProperty<EffigyType> TYPE = EnumProperty.create("type", EffigyType.class);

	public EffigyFigureBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, EffigyType.ZOMBIE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TYPE);
	}

	public BlockState figure(EffigyType type) {
		return this.defaultBlockState().setValue(TYPE, type);
	}
}
