package io.github.boxofdog.simpleblock.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import io.github.boxofdog.simpleblock.SimpleBlock;

/** FOOD-01 HUD: the server tells the client its player's current saturation (see {@code feature/SaturationSync}). */
public record SaturationPayload(float saturation) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SaturationPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "saturation"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SaturationPayload> CODEC =
		StreamCodec.composite(ByteBufCodecs.FLOAT, SaturationPayload::saturation, SaturationPayload::new);

	@Override
	public CustomPacketPayload.Type<SaturationPayload> type() {
		return TYPE;
	}
}
