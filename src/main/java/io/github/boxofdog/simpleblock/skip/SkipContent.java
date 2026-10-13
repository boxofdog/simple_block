package io.github.boxofdog.simpleblock.skip;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import io.github.boxofdog.simpleblock.SimpleBlock;
import io.github.boxofdog.simpleblock.cable.CableContent;

/**
 * M07 ore skip: what is registered. The skip ({@link OreSkip}) is crafted (owner-settled shaped recipe:
 * a cable over an iron trapdoor over a minecart between two iron ingots), placed from its item
 * ({@link SkipItem}), opened as a one-slot screen ({@link SkipMenu}), hung on a drum's cable
 * ({@code cable/CableHoists}) and exchanges cargo at the Loading Rail ({@link SkipStations}).
 * {@link #SKIP_PART_MODEL} only carries the skip's part models for its renderer: no item, no drops, no
 * collision.
 */
public final class SkipContent {
	public static final Block SKIP_PART_MODEL = block("ore_skip_part", SkipPartBlock::new);

	public static final ResourceKey<EntityType<?>> ORE_SKIP_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("ore_skip"));
	/** One block wide and deep; three tall (the body), four once its bridle is on (it hangs on a cable): {@link OreSkip#getDimensions}. */
	public static final EntityType<OreSkip> ORE_SKIP = Registry.register(BuiltInRegistries.ENTITY_TYPE, ORE_SKIP_KEY,
		EntityType.Builder.<OreSkip>of(OreSkip::new, MobCategory.MISC)
			.noLootTable()
			.sized(1.0F, (float) OreSkip.BODY_HEIGHT)
			.clientTrackingRange(10)
			.updateInterval(1)
			.build(ORE_SKIP_KEY));

	public static final Item ORE_SKIP_ITEM = item("ore_skip");

	public static final MenuType<SkipMenu> SKIP_MENU = Registry.register(BuiltInRegistries.MENU, id("ore_skip"),
		new MenuType<>(SkipMenu::new, FeatureFlags.VANILLA_SET));

	private SkipContent() {
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, path);
	}

	private static Block block(String path, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(path));
		return Registry.register(BuiltInRegistries.BLOCK, key,
			factory.apply(BlockBehaviour.Properties.of().noCollision().noOcclusion().noLootTable().instabreak().setId(key)));
	}

	private static Item item(String path) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, new SkipItem(new Item.Properties().stacksTo(1).setId(key)));
	}

	/** Registers everything (class initialisation) and the creative-tab entry. */
	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
			output.insertAfter(CableContent.CONNECTING_ROD, ORE_SKIP_ITEM));
	}
}
