package io.github.boxofdog.simpleblock.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import io.github.boxofdog.simpleblock.SimpleBlock;

/** INV-02: the client's Sort button asks the server to sort the player's storage rows. */
public record SortInventoryPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SortInventoryPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "sort_inventory"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SortInventoryPayload> CODEC = StreamCodec.unit(new SortInventoryPayload());

	@Override
	public CustomPacketPayload.Type<SortInventoryPayload> type() {
		return TYPE;
	}
}
