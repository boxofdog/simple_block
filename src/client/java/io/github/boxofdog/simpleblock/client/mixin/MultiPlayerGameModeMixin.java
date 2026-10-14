package io.github.boxofdog.simpleblock.client.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.boxofdog.simpleblock.cable.CableContent;
import io.github.boxofdog.simpleblock.cable.CableDrumBlock;
import io.github.boxofdog.simpleblock.cable.CableDrumBlockEntity;
import io.github.boxofdog.simpleblock.network.CrankPayload;

/**
 * M05: left-click held on a cable drum's crank feeds the cable (owner's crank controls) instead of
 * starting to break the drum. While the crosshair is on the crank, starting or continuing to break
 * the drum is replaced by a {@link CrankPayload} every 4 ticks (as often as a held use key repeats);
 * left-clicking the rest of the drum breaks it as before. The server checks each payload. On a coal
 * winch (the lever where the crank was) only a new left-click sends one: it sets FEED, or NEUTRAL again.
 */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private long simpleBlock$lastCrank = -4L;

	@Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$crankInsteadOfBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (this.simpleBlock$onCrank(pos, true)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
	private void simpleBlock$keepCranking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (this.simpleBlock$onCrank(pos, false)) {
			cir.setReturnValue(false);
		}
	}

	/**
	 * Whether the crosshair is on the crank of the drum at {@code pos}; if so, turns it (at most every 4
	 * ticks). A coal winch's lever is there instead: only a new click ({@code start}) sets it to FEED.
	 */
	@Unique
	private boolean simpleBlock$onCrank(BlockPos pos, boolean start) {
		if (this.minecraft.level == null || this.minecraft.player == null
			|| !(this.minecraft.hitResult instanceof BlockHitResult hit) || !hit.getBlockPos().equals(pos)) {
			return false;
		}
		BlockState state = this.minecraft.level.getBlockState(pos);
		if (!state.is(CableContent.CABLE_DRUM) || !CableDrumBlock.isCrank(state, pos, hit.getLocation())) {
			return false;
		}
		boolean lever = this.minecraft.level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.showsLever();
		long now = this.minecraft.level.getGameTime();
		if (lever ? start : now - this.simpleBlock$lastCrank >= 4 || now < this.simpleBlock$lastCrank) {
			this.simpleBlock$lastCrank = now;
			ClientPlayNetworking.send(new CrankPayload(pos, hit.getLocation()));
			LocalPlayer player = this.minecraft.player;
			player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
		}
		return true;
	}
}
