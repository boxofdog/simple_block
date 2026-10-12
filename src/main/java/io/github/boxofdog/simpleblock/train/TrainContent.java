package io.github.boxofdog.simpleblock.train;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M05 trains: chain couplings ({@link Couplings}) and the Cobblestone Engine ({@link CobblestoneEngine}).
 * Nothing new is craftable: the engine is vanilla's furnace minecart, made by using a furnace on a
 * minecart. {@link #ENGINE_MODEL} exists only to carry the engine's model for its renderer: no item,
 * no drops, no collision.
 */
public final class TrainContent {
	public static final Block ENGINE_MODEL = technical("cobblestone_engine_model");

	private TrainContent() {
	}

	private static Block technical(String path) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, path));
		return Registry.register(BuiltInRegistries.BLOCK, key,
			new Block(BlockBehaviour.Properties.of().noCollision().noOcclusion().noLootTable().instabreak().setId(key)));
	}

	/** Registers everything (class initialisation) and the train bookkeeping. */
	public static void register() {
		Couplings.register();
	}
}
