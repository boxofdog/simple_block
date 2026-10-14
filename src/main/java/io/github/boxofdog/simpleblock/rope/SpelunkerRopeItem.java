package io.github.boxofdog.simpleblock.rope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

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
		Direction face = context.getClickedFace();
		BlockPos support = context.getClickedPos();
		BlockPos anchor = support.relative(face);
		if (face == Direction.UP) {
			if (player != null && !level.isClientSide()) {
				SpelunkerRopes.tell(player, SpelunkerRopes.message("needs_hold", "Fix it under a block or on its side"));
			}
			return InteractionResult.FAIL;
		}
		if (!SpelunkerRopes.canHangOn(level, support, face) || !level.getBlockState(anchor).canBeReplaced()
			|| !level.getFluidState(anchor).isEmpty() || !SpelunkerRopes.open(level.getBlockState(anchor.below()))) {
			if (player != null && !level.isClientSide()) {
				SpelunkerRopes.tell(player, SpelunkerRopes.message("no_room", "No room to hang a rope here"));
			}
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel serverLevel) {
			level.setBlock(anchor, RopeContent.ROPE_ANCHOR.defaultBlockState().setValue(RopeAnchorBlock.SUPPORT, face.getOpposite()), Block.UPDATE_ALL);
			if (level.getBlockEntity(anchor) instanceof RopeAnchorBlockEntity entity) {
				int added = SpelunkerRopes.lower(serverLevel, anchor, entity);
				if (player != null) {
					SpelunkerRopes.tell(player, SpelunkerRopes.message("hung", "Rope hung: %s blocks down (Rope lets it down 12 more)", added));
				}
			}
			level.playSound(null, anchor, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
			context.getItemInHand().consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}
}
