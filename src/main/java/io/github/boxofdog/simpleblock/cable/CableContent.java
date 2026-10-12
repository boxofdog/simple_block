package io.github.boxofdog.simpleblock.cable;

import java.util.Set;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M04 cable infrastructure: what is registered, and the fixed lengths.
 *
 * <ul>
 *   <li><b>Cable</b> ({@link #CABLE}): {@value #BLOCKS_PER_CABLE} blocks of cable per item. Eight make
 *       a <b>cable coil</b> ({@link #CABLE_COIL}, {@value #BLOCKS_PER_COIL} blocks).</li>
 *   <li><b>Cable drum</b> ({@link CableDrumBlock}): a coil used on it winds the cable on and makes it a
 *       winch, with a loose end and hook ({@link CableHook}) to lay the cable out with.</li>
 *   <li><b>Copper cable anchor</b> ({@link CableAnchorBlock}): hangs from the underside of a wooden
 *       block ({@code #simple_block:cable_anchor_supports}) and carries the cable on a roller.</li>
 *   <li><b>Copper connecting rod</b> ({@link #CONNECTING_ROD}): fitted to a minecart, it grips a
 *       cable the cart passes under ({@link ConnectingRods}).</li>
 * </ul>
 *
 * <p>Three blocks exist only to carry models for renderers ({@link #CABLE_HOOK_MODEL},
 * {@link #CONNECTING_ROD_MODEL}, {@link #DRUM_PART_MODEL}): no item, no drops, no collision. Routing rules are in
 * {@link CableRoutes}.
 */
public final class CableContent {
	public static final int BLOCKS_PER_CABLE = 4;
	public static final int CABLES_PER_COIL = 8;
	public static final int BLOCKS_PER_COIL = BLOCKS_PER_CABLE * CABLES_PER_COIL;

	public static final TagKey<Block> ANCHOR_SUPPORTS = TagKey.create(Registries.BLOCK, id("cable_anchor_supports"));

	public static final Block CABLE_ANCHOR = block("cable_anchor", CableAnchorBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.COLOR_ORANGE).noCollision().noOcclusion().strength(0.5F).sound(SoundType.COPPER).pushReaction(PushReaction.POPPED));
	public static final Block CABLE_DRUM = block("cable_drum", CableDrumBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.WOOD).noOcclusion().strength(2.0F).sound(SoundType.WOOD).ignitedByLava());
	public static final Block CABLE_HOOK_MODEL = block("cable_hook", Block::new, technical());
	public static final Block CONNECTING_ROD_MODEL = block("connecting_rod_part", ConnectingRodPartBlock::new, technical());
	/** M05: the drum's turning parts (spool, crank), drawn by the drum renderer. */
	public static final Block DRUM_PART_MODEL = block("cable_drum_part", CableDrumPartBlock::new, technical());

	public static final BlockEntityType<CableAnchorBlockEntity> CABLE_ANCHOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		id("cable_anchor"), new BlockEntityType<>(CableAnchorBlockEntity::new, Set.of(CABLE_ANCHOR)));
	public static final BlockEntityType<CableDrumBlockEntity> CABLE_DRUM_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		id("cable_drum"), new BlockEntityType<>(CableDrumBlockEntity::new, Set.of(CABLE_DRUM)));

	public static final Item CABLE = item("cable", Item::new, new Item.Properties());
	public static final Item CABLE_COIL = item("cable_coil", Item::new, new Item.Properties().stacksTo(16));
	public static final Item CONNECTING_ROD = item("copper_connecting_rod", Item::new, new Item.Properties().stacksTo(16));
	public static final Item CABLE_ANCHOR_ITEM = item("cable_anchor", p -> new BlockItem(CABLE_ANCHOR, p), new Item.Properties().useBlockDescriptionPrefix());
	public static final Item CABLE_DRUM_ITEM = item("cable_drum", p -> new BlockItem(CABLE_DRUM, p), new Item.Properties().useBlockDescriptionPrefix());

	public static final ResourceKey<EntityType<?>> CABLE_HOOK_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("cable_hook"));
	/** The loose end of a drum's cable. Not saved: the drum puts it back when it is loaded. */
	public static final EntityType<CableHook> CABLE_HOOK = Registry.register(BuiltInRegistries.ENTITY_TYPE, CABLE_HOOK_KEY,
		EntityType.Builder.<CableHook>of(CableHook::new, MobCategory.MISC)
			.noLootTable()
			.noSave()
			.sized(0.375F, 0.5F)
			.clientTrackingRange(10)
			.updateInterval(1)
			.build(CABLE_HOOK_KEY));

	private CableContent() {
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, path);
	}

	private static BlockBehaviour.Properties technical() {
		return BlockBehaviour.Properties.of().noCollision().noOcclusion().noLootTable().instabreak();
	}

	private static Block block(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(path));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	private static Item item(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(path));
		Item item = factory.apply(properties.setId(key));
		if (item instanceof BlockItem blockItem) {
			blockItem.registerBlocks(Item.BY_BLOCK, item);
		}
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	/** Registers everything (class initialisation), the creative-tab entries and the loaded-cable bookkeeping. */
	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
			output.insertAfter(Items.MINECART, CABLE, CABLE_COIL, CABLE_DRUM_ITEM, CABLE_ANCHOR_ITEM, CONNECTING_ROD));
		CableIndex.register();
		CableDrive.register();
		SimpleBlock.LOGGER.info("Cable: {} blocks per cable, {} per coil", BLOCKS_PER_CABLE, BLOCKS_PER_COIL);
	}
}
