package io.github.boxofdog.simpleblock.mixin.sleep;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SF-01: sleeping never skips the night or clears a storm.
 *
 * <p>Vanilla already has a "nobody can sleep through the night" mode (game rule
 * {@code players_sleeping_percentage} above 100): beds still set the respawn point, players can
 * still lie down and get up, and the sleeper is told "No amount of rest can pass this night". This
 * mixin forces that mode on every level instead of relying on a game rule players can change.
 *
 * <p>A mixin is used rather than Fabric's {@code EntitySleepEvents.ALLOW_RESETTING_TIME} because
 * that event leaves vanilla's sleep announcements on, so players would be told the night is being
 * skipped when it is not.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
	/** Drives vanilla's own feedback: the "sleep.not_possible" message and no skip announcements. */
	@ModifyReturnValue(method = "canSleepThroughNights", at = @At("RETURN"))
	private boolean simple_block$neverSleepThroughNights(boolean original) {
		return false;
	}

	/**
	 * The tick-time skip (advance the clock to morning, wake everyone, clear rain/thunder) only checks
	 * the sleeping percentage, so make it respect {@link ServerLevel#canSleepThroughNights()} too.
	 */
	@ModifyExpressionValue(
			method = "tick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z")
	)
	private boolean simple_block$onlySkipWhenAllowed(boolean enoughSleeping) {
		return enoughSleeping && ((ServerLevel) (Object) this).canSleepThroughNights();
	}
}
