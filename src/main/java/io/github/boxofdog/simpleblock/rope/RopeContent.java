package io.github.boxofdog.simpleblock.rope;

import java.util.Set;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
 * M10 Rope and Spelunker's Rope (owner-settled shaped recipes: three string down the middle column; a
 * copper cable anchor over a Rope). The anchor, the hanging rope and its ground coil are blocks with no
 * drops of their own: taking a line down gives back what it was made of ({@link SpelunkerRopes}).
 */
public final class RopeContent {
	private static final BlockBehaviour.Properties LINE = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noCollision().strength(0.4F)
		.sound(SoundType.WOOL).noOcclusion().noLootTable().pushReaction(PushReaction.POPPED);

	public static final Block ROPE_ANCHOR = block("spelunker_rope_anchor", RopeAnchorBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).noCollision().strength(0.8F).sound(SoundType.COPPER).noOcclusion()
			.noLootTable().pushReaction(PushReaction.POPPED));
	public static final Block ROPE_BLOCK = block("spelunker_rope", properties -> new RopeLineBlock(properties, false), LINE);
	public static final Block ROPE_COIL = block("spelunker_rope_coil", properties -> new RopeLineBlock(properties, true), LINE);

	public static final Item ROPE = register("rope", key -> new RopeItem(new Item.Properties().setId(key)));
	public static final Item SPELUNKER_ROPE = register("spelunker_rope", key -> new SpelunkerRopeItem(new Item.Properties().stacksTo(16).setId(key)));

	public static final BlockEntityType<RopeAnchorBlockEntity> ROPE_ANCHOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
		id("spelunker_rope_anchor"), new BlockEntityType<>(RopeAnchorBlockEntity::new, Set.of(ROPE_ANCHOR)));

	public static final ResourceKey<EntityType<?>> ROPE_SHOT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("spelunker_rope_shot"));
	/** M13: a Spelunker's Rope shot from a bow; a thrown item's size and tracking. */
	public static final EntityType<RopeShot> ROPE_SHOT = Registry.register(BuiltInRegistries.ENTITY_TYPE, ROPE_SHOT_KEY,
		EntityType.Builder.<RopeShot>of(RopeShot::new, MobCategory.MISC)
			.noLootTable()
			.sized(0.25F, 0.25F)
			.clientTrackingRange(4)
			.updateInterval(10)
			.build(ROPE_SHOT_KEY));

	private RopeContent() {
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
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
	}

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output ->
			output.insertAfter(Items.LEAD, ROPE, SPELUNKER_ROPE));
	}
}
