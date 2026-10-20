package io.github.boxofdog.simpleblock.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import io.github.boxofdog.simpleblock.SimpleBlock;

/** M13: the chest screen's Sort button asks the server to sort the chest open in that screen ({@code containerId}). */
public record SortChestPayload(int containerId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SortChestPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "sort_chest"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SortChestPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,
		SortChestPayload::containerId, SortChestPayload::new);

	@Override
	public CustomPacketPayload.Type<SortChestPayload> type() {
		return TYPE;
	}
}
