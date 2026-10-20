package io.github.boxofdog.simpleblock.train;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M05 trains: chain couplings ({@link Couplings}) and the Cobblestone Engine ({@link CobblestoneEngine}).
 * Nothing new is craftable: the engine is vanilla's furnace minecart, made by using a furnace on a
 * minecart. {@link #ENGINE_MODEL} exists only to carry the engine's model for its renderer: no item,
 * no drops, no collision.
 *
 * <p>M06: the Loading Track ({@link LoadingTrackBlock}, {@link LoadingTracks}), crafted from a rail and a
 * cable anchor. (The freight cart's gates are part of every minecart's body model since the M07 cart
 * redesign, drawn by the client.)
 */
public final class TrainContent {
	public static final Block ENGINE_MODEL = technical("cobblestone_engine_model", Block::new);
	/** M11: the models of a freight cart's load, drawn by the minecart renderer. */
	public static final CargoFillBlock CARGO_FILL_MODEL = (CargoFillBlock) technical("cargo_fill", CargoFillBlock::new);
	/** M15: the engine's selector lever and the freight hoppers' lids, drawn by the client. */
	public static final FreightModelBlock FREIGHT_MODEL = (FreightModelBlock) technical("freight_model", FreightModelBlock::new);
	public static final Block LOADING_TRACK = block("loading_track", LoadingTrackBlock::new,
		BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(0.7F).sound(SoundType.METAL));
	public static final Item LOADING_TRACK_ITEM = item("loading_track", LOADING_TRACK);
	/** M13 (BUG-014): the read-only cargo inspection screen. */
	public static final net.minecraft.world.inventory.MenuType<CargoInspectMenu> CARGO_INSPECT_MENU = Registry.register(BuiltInRegistries.MENU,
		Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "cargo_inspect"),
		new net.minecraft.world.inventory.MenuType<>(CargoInspectMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

	private TrainContent() {
	}

	private static Block technical(String path, Function<BlockBehaviour.Properties, Block> factory) {
		return block(path, factory, BlockBehaviour.Properties.of().noCollision().noOcclusion().noLootTable().instabreak());
	}

	private static Block block(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, path));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item item(String path, Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, path));
		BlockItem item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(key));
		item.registerBlocks(Item.BY_BLOCK, item);
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	/** Registers everything (class initialisation) and the train bookkeeping. */
	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
			output.insertAfter(Items.RAIL, LOADING_TRACK_ITEM));
		Couplings.register();
		EngineSelector.register();
	}
}
