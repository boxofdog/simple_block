package io.github.boxofdog.simpleblock.gametest.mixin;

import io.netty.channel.ChannelFutureListener;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import io.github.boxofdog.simpleblock.gametest.ActionBarWatch;

/** Test only: counts the action-bar text each player is sent ({@link ActionBarWatch}). */
@Mixin(ServerCommonPacketListenerImpl.class)
abstract class ActionBarWatchMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"))
	private void simpleBlockTest$watch(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
		boolean actionBar = packet instanceof ClientboundSetActionBarTextPacket || packet instanceof ClientboundSystemChatPacket chat && chat.overlay();
		if (actionBar && (Object) this instanceof ServerGamePacketListenerImpl game) {
			ActionBarWatch.note(game.player, packet);
		}
	}
}
