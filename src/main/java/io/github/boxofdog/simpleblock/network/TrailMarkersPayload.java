package io.github.boxofdog.simpleblock.network;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M10 Trail Blazer: the server tells a player wearing the full spelunking outfit which torches and
 * ladders near them were placed by players (see {@code workwear/TrailMarkers}); the client shows those
 * yellow and the rest blue.
 */
public record TrailMarkersPayload(List<BlockPos> placed) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TrailMarkersPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "trail_markers"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TrailMarkersPayload> CODEC =
		StreamCodec.composite(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(4096)), TrailMarkersPayload::placed, TrailMarkersPayload::new);

	@Override
	public CustomPacketPayload.Type<TrailMarkersPayload> type() {
		return TYPE;
	}
}
