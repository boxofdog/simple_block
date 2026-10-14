package io.github.boxofdog.simpleblock.effigy;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M12 Monster Effigies: what is registered. Per {@link EffigyType}: the compressed monster block (nine
 * full stacks of the drop, {@link CompressionRecipe}), the effigy block ({@link EffigyBlock}, two high,
 * with {@link EffigyBlockEntity} holding its angle) and the effigy item ({@link EffigyItem}, placed
 * through {@link EffigyPlacementMenu}). The figure each effigy shows is a model on
 * {@link #FIGURE_MODEL}, drawn turned to the effigy's angle by the client.
 */
public final class EffigyContent {
	private static final Map<EffigyType, Block> COMPRESSED = new EnumMap<>(EffigyType.class);
	private static final Map<EffigyType, Item> COMPRESSED_ITEMS = new EnumMap<>(EffigyType.class);
	private static final Map<EffigyType, EffigyBlock> EFFIGIES = new EnumMap<>(EffigyType.class);
	private static final Map<EffigyType, Item> EFFIGY_ITEMS = new EnumMap<>(EffigyType.class);

	static {
		for (EffigyType type : EffigyType.values()) {
			Block compressed = block(type.getSerializedName() + "_block", Block::new, compressedProperties(type));
			COMPRESSED.put(type, compressed);
			COMPRESSED_ITEMS.put(type, register(type.getSerializedName() + "_block",
				key -> new BlockItem(compressed, new Item.Properties().useBlockDescriptionPrefix().setId(key))));
			EffigyBlock effigy = (EffigyBlock) block(type.getSerializedName() + "_effigy", properties -> new EffigyBlock(type, properties),
				BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
					// Obsidian's hardness; a blast resistance no explosion overcomes (bedrock's).
					.strength(50.0F, 3600000.0F).sound(SoundType.BONE_BLOCK).noOcclusion().noLootTable().pushReaction(PushReaction.IMMOVEABLE)
					.forceSolidOn());
			EFFIGIES.put(type, effigy);
			EFFIGY_ITEMS.put(type, register(type.getSerializedName() + "_effigy",
				key -> new EffigyItem(type, new Item.Properties().useBlockDescriptionPrefix().setId(key))));
		}
	}

	/** Not placeable: carries the four figures' models ({@code type}, {@code half}) for the effigy renderer. */
	public static final EffigyFigureBlock FIGURE_MODEL = (EffigyFigureBlock) block("effigy_figure", EffigyFigureBlock::new,
		BlockBehaviour.Properties.of().noCollision().noOcclusion().noLootTable().instabreak());

	public static final BlockEntityType<EffigyBlockEntity> EFFIGY_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("effigy"),
		new BlockEntityType<>(EffigyBlockEntity::new, Set.copyOf(EFFIGIES.values())));
	public static final MenuType<EffigyPlacementMenu> PLACEMENT_MENU = Registry.register(BuiltInRegistries.MENU, id("effigy_placement"),
		new MenuType<>(EffigyPlacementMenu::new, FeatureFlags.VANILLA_SET));
	public static final RecipeSerializer<CompressionRecipe> COMPRESSION = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
		id("compression"), CompressionRecipe.SERIALIZER);

	private EffigyContent() {
	}

	private static BlockBehaviour.Properties compressedProperties(EffigyType type) {
		return switch (type) {
			case ZOMBIE -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.8F).sound(SoundType.SLIME_BLOCK);
			case SKELETON -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.XYLOPHONE)
				.requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK);
			case CREEPER -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.SAND);
			case ENDERMAN -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(3.0F, 6.0F)
				.sound(SoundType.AMETHYST);
		};
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

	public static Block compressedBlock(EffigyType type) {
		return COMPRESSED.get(type);
	}

	public static Item compressedItem(EffigyType type) {
		return COMPRESSED_ITEMS.get(type);
	}

	public static EffigyBlock effigyBlock(EffigyType type) {
		return EFFIGIES.get(type);
	}

	public static Item effigyItem(EffigyType type) {
		return EFFIGY_ITEMS.get(type);
	}

	/** Registers everything (class initialisation), the creative-tab entries and the fear of effigies. */
	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			Item after = Items.BONE_BLOCK;
			for (EffigyType type : EffigyType.values()) {
				output.insertAfter(after, compressedItem(type));
				after = compressedItem(type);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			for (EffigyType type : EffigyType.values()) {
				output.accept(effigyItem(type));
			}
		});
		Effigies.register();
	}
}
