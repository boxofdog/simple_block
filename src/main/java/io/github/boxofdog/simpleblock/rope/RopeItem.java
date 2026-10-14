package io.github.boxofdog.simpleblock.rope;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/** Rope: used (or held in use) on a Spelunker's Rope's line, each one lets it down 12 blocks more ({@link SpelunkerRopes}). */
public class RopeItem extends Item {
	public RopeItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		if (!state.is(RopeContent.ROPE_ANCHOR) && !SpelunkerRopes.isLine(state)) {
			return InteractionResult.PASS;
		}
		Player player = context.getPlayer();
		if (player != null && context.getLevel() instanceof ServerLevel level) {
			SpelunkerRopes.extend(level, context.getClickedPos(), player, context.getItemInHand());
		}
		return InteractionResult.SUCCESS;
	}
}
