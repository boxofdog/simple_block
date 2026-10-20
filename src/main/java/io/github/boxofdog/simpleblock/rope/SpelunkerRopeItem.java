package io.github.boxofdog.simpleblock.rope;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The Spelunker's Rope: used on the bottom or a side of a solid block, it fixes its anchor in the open
 * block there and lets its own 12 blocks of rope down ({@link SpelunkerRopes}). Used on a line it is
 * part of nothing: lines are extended with Rope.
 */
public class SpelunkerRopeItem extends Item {
	public SpelunkerRopeItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (!(level instanceof ServerLevel serverLevel)) {
			return SpelunkerRopes.problem(level, context.getClickedPos(), context.getClickedFace()) == null ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		if (!SpelunkerRopes.hang(serverLevel, context.getClickedPos(), context.getClickedFace(), player)) {
			return InteractionResult.FAIL;
		}
		context.getItemInHand().consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
