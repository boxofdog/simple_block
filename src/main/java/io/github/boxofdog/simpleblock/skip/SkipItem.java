package io.github.boxofdog.simpleblock.skip;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;
import io.github.boxofdog.simpleblock.feature.Feedback;

/**
 * The ore skip item: used on the top of a block, it stands a skip on it, its front (chute) towards the
 * player. It needs its four blocks of height clear, and the cell on each side of them.
 */
public class SkipItem extends Item {
	public SkipItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getClickedFace() != Direction.UP) {
			return InteractionResult.FAIL;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos pos = context.getClickedPos().above();
		OreSkip skip = SkipContent.ORE_SKIP.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (skip == null) {
			return InteractionResult.FAIL;
		}
		skip.setFacing(context.getHorizontalDirection().getOpposite());
		skip.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		if (skip.obstruction(pos.getY()) != null) {
			if (context.getPlayer() instanceof ServerPlayer player) {
				Feedback.quiet(player, Component.translatableWithFallback("simple_block.skip.no_room",
					"No room: a skip needs four blocks of height, and a clear block on each side"));
			}
			return InteractionResult.FAIL;
		}
		level.addFreshEntity(skip);
		level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer()));
		context.getItemInHand().consume(1, context.getPlayer());
		return InteractionResult.SUCCESS;
	}
}
