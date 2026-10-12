package io.github.boxofdog.simpleblock.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M05: the client holds left-click on a cable drum's crank (where vanilla would start breaking the
 * block): feed the cable away from the drum. Sent every few ticks while held; {@code hit} is where the
 * crosshair meets the crank. The server checks it ({@code cable/CableDrive}).
 */
public record CrankPayload(BlockPos pos, Vec3 hit) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CrankPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "crank_feed"));
	public static final StreamCodec<ByteBuf, CrankPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, CrankPayload::pos, Vec3.STREAM_CODEC,
		CrankPayload::hit, CrankPayload::new);

	@Override
	public CustomPacketPayload.Type<CrankPayload> type() {
		return TYPE;
	}
}
