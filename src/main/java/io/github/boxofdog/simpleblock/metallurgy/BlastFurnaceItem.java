package io.github.boxofdog.simpleblock.metallurgy;

import java.util.List;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * M08: the furnace item places the whole structure at once ({@link BlastFurnaceStructure}): the block
 * clicked is the middle of its front at the bottom (where the mold is), facing the player, and the
 * furnace reaches two blocks back, one to each side and four up. Every one of those 36 blocks must be
 * free (air, plants, water, ...) and clear of entities; otherwise nothing is placed and the item is kept.
 */
public class BlastFurnaceItem extends BlockItem {
	public BlastFurnaceItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		if (!context.canPlace()) {
			return InteractionResult.FAIL;
		}
		Level level = context.getLevel();
		Player player = context.getPlayer();
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockPos controller = BlastFurnaceStructure.controllerFor(context.getClickedPos(), facing);
		List<BlockPos> blocks = BlastFurnaceStructure.blocks(controller);
		CollisionContext collision = CollisionContext.placementContext(player);
		for (BlockPos pos : blocks) {
			BlockState state = BlastFurnaceStructure.stateAt(controller, pos, facing);
			if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced(context) && !pos.equals(context.getClickedPos())
				|| !level.isUnobstructed(state, pos, collision)) {
				if (player != null && !level.isClientSide()) {
					player.sendOverlayMessage(Component.translatableWithFallback("simple_block.blast_furnace.no_room",
						"The furnace needs a clear space 3 blocks wide, 3 deep and 4 high"));
				}
				return InteractionResult.FAIL;
			}
		}
		if (!level.isClientSide()) {
			for (BlockPos pos : blocks) {
				level.setBlock(pos, BlastFurnaceStructure.stateAt(controller, pos, facing), Block.UPDATE_ALL);
			}
			ItemStack stack = context.getItemInHand();
			if (player instanceof ServerPlayer serverPlayer) {
				CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, controller, stack);
			}
			level.gameEvent(GameEvent.BLOCK_PLACE, controller, GameEvent.Context.of(player, level.getBlockState(controller)));
		}
		level.playSound(player, controller, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
		context.getItemInHand().consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
