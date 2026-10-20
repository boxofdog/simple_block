package io.github.boxofdog.simpleblock.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M13: the server tells the client that the screen about to open ({@code containerId}) is a chest's,
 * which can be sorted (a barrel's or an ender chest's uses the same screen and cannot), before the
 * screen itself opens.
 */
public record SortableChestPayload(int containerId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SortableChestPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "sortable_chest"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SortableChestPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,
		SortableChestPayload::containerId, SortableChestPayload::new);

	@Override
	public CustomPacketPayload.Type<SortableChestPayload> type() {
		return TYPE;
	}
}
