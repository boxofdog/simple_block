package io.github.boxofdog.simpleblock.coot;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
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
import net.minecraft.world.item.SpawnEggItem;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M14 the Old Coot ({@link OldCoot}) and his creative spawn egg: a testing convenience only (Creative
 * inventory, no recipe, no drop, no trade), which spawns a Coot anywhere at any light.
 */
public final class OldCootContent {
	public static final ResourceKey<EntityType<?>> OLD_COOT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("old_coot"));
	/** Short, broad and hunched: 0.75 blocks wide, 1.25 tall (his head top at 18.5 model pixels, into the hat's crown). */
	public static final EntityType<OldCoot> OLD_COOT = Registry.register(BuiltInRegistries.ENTITY_TYPE, OLD_COOT_KEY,
		EntityType.Builder.<OldCoot>of(OldCoot::new, MobCategory.MISC)
			.sized(0.75F, 1.25F)
			.eyeHeight(1.0F)
			.clientTrackingRange(10)
			.build(OLD_COOT_KEY));

	public static final ResourceKey<Item> SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, id("old_coot_spawn_egg"));
	public static final Item SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, SPAWN_EGG_KEY,
		new SpawnEggItem(new Item.Properties().spawnEgg(OLD_COOT).setId(SPAWN_EGG_KEY)));

	private OldCootContent() {
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, path);
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(OLD_COOT, OldCoot.createAttributes());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.insertAfter(Items.WITCH_SPAWN_EGG, SPAWN_EGG));
		CootResidents.register();
		CootAnger.register();
	}
}
