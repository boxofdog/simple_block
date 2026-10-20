package io.github.boxofdog.simpleblock.metallurgy;

import java.util.Set;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M08 metallurgy: what is registered. The blast furnace ({@link BlastFurnaceBlock}, its
 * {@link BlastFurnacePartBlock structure blocks}, {@link BlastFurnaceChannelBlock channel} and
 * {@link BlastFurnaceMoldBlock mold}, {@link BlastFurnaceBlockEntity}, {@link BlastFurnaceMenu})
 * is crafted (owner-settled shaped recipe with two Bricks blocks, and its mirror) and placed whole from
 * its item ({@link BlastFurnaceItem}). The Block of Charcoal (nine charcoal) is a fuel like the Block of
 * Coal.
 */
public final class MetallurgyContent {
	private static final BlockBehaviour.Properties BRICKWORK = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
		.instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.STONE).noOcclusion()
		.pushReaction(PushReaction.IMMOVEABLE);

	public static final Block BLAST_FURNACE = block("blast_furnace", BlastFurnaceBlock::new,
		BRICKWORK.lightLevel(state -> state.getValue(BlastFurnaceBlock.LIT) ? 13 : 0));
	public static final Block BLAST_FURNACE_PART = block("blast_furnace_part", BlastFurnacePartBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
			.strength(3.5F, 6.0F).sound(SoundType.STONE).noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE));
	/** M13 BUG-011: the channel and mold glow while metal runs through them (light 9 and 11), and only then. */
	public static final int CHANNEL_POUR_LIGHT = 9;
	public static final int MOLD_POUR_LIGHT = 11;
	public static final Block BLAST_FURNACE_CHANNEL = block("blast_furnace_channel", BlastFurnaceChannelBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.METAL)
			.noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE)
			.lightLevel(state -> state.getValue(BlastFurnaceChannelBlock.POURING) ? CHANNEL_POUR_LIGHT : 0));
	public static final Block BLAST_FURNACE_MOLD = block("blast_furnace_mold", BlastFurnaceMoldBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(SoundType.METAL)
			.noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE)
			.lightLevel(state -> state.getValue(BlastFurnaceMoldBlock.POURING) ? MOLD_POUR_LIGHT : 0));
	public static final Item BLAST_FURNACE_ITEM = register("blast_furnace",
		key -> new BlastFurnaceItem(BLAST_FURNACE, new Item.Properties().stacksTo(1).useBlockDescriptionPrefix().setId(key)));

	public static final Block CHARCOAL_BLOCK = block("charcoal_block", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
		.instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F));
	public static final Item CHARCOAL_BLOCK_ITEM = register("charcoal_block", key -> new BlockItem(CHARCOAL_BLOCK,
		new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_COAL_BLOCK).useBlockDescriptionPrefix().setId(key)));

	public static final BlockEntityType<BlastFurnaceBlockEntity> BLAST_FURNACE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		id("blast_furnace"), new BlockEntityType<>(BlastFurnaceBlockEntity::new, Set.of(BLAST_FURNACE)));
	public static final MenuType<BlastFurnaceMenu> BLAST_FURNACE_MENU = Registry.register(BuiltInRegistries.MENU, id("blast_furnace"),
		new MenuType<>(BlastFurnaceMenu::new, FeatureFlags.VANILLA_SET));

	private MetallurgyContent() {
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, path);
	}

	private static Block block(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(path));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item register(String path, Function<ResourceKey<Item>, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(path));
		Item item = factory.apply(key);
		if (item instanceof BlockItem blockItem) {
			blockItem.registerBlocks(Item.BY_BLOCK, item);
		}
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	/** Registers everything (class initialisation), the creative-tab entries and the charcoal block's flammability. */
	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output ->
			output.insertAfter(Items.FURNACE, BLAST_FURNACE_ITEM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
			output.insertAfter(Items.COAL_BLOCK, CHARCOAL_BLOCK_ITEM));
		FlammableBlockRegistry.getDefaultInstance().add(CHARCOAL_BLOCK, 5, 5);
	}
}
