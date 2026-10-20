package io.github.boxofdog.simpleblock.mixin.rope;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.rope.BowRope;

/** M13: a Spelunker's Rope in the offhand turns the main-hand bow into a rope launcher ({@link BowRope}). */
@Mixin(BowItem.class)
abstract class BowItemMixin {
	/** Draws with or without arrows. */
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$drawForRope(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (BowRope.armed(player, hand)) {
			player.startUsingItem(hand);
			cir.setReturnValue(InteractionResult.CONSUME);
		}
	}

	/** Looses a rope, never an arrow. */
	@Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$shootRope(ItemStack bow, Level level, LivingEntity entity, int remainingTime, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Player player && BowRope.armed(player, player.getUsedItemHand())) {
			int timeHeld = ((BowItem) (Object) this).getUseDuration(bow, entity) - remainingTime;
			cir.setReturnValue(BowRope.release(bow, level, player, timeHeld));
		}
	}
}
