package io.github.boxofdog.simpleblock.workwear;

import java.util.Map;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * M10 workwear: what is registered. Four owner-settled shaped recipes ({@code data/simple_block/recipe}):
 * <ul>
 *   <li><b>Headlamp</b> (five copper ingots round a lantern): a copper mining helmet with an upright
 *       lantern on top under a copper cap. Copper-helmet protection (2, just below iron's toughness of
 *       wear: copper durability), and steady light while worn ({@link Headlamps}).</li>
 *   <li><b>Overalls</b> (string, chest, string; leather chestplate; leather leggings): one garment worn
 *       in the chest slot that also takes the legs slot ({@link Workwear}); protection 5, a leather
 *       chestplate and leggings together. Wearing them opens the backpack row: 27 carrying slots
 *       become 36.</li>
 *   <li><b>Spiked Boots</b> (leather boots over pointed dripstone): leather-boot protection;
 *       Spelunker's Speed.</li>
 *   <li><b>Mining Boots</b> (leather boots over an iron ingot): iron-boot protection; Mule.</li>
 * </ul>
 * The named bonuses are shown as effects ({@link #SPELUNKERS_SPEED}, {@link #TRAIL_BLAZER},
 * {@link #MULE}, {@link #OLD_PRO}); a full outfit's replaces the boots' own.
 */
public final class WorkwearContent {
	private static ResourceKey<EquipmentAsset> asset(String path) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, id(path));
	}

	private static Map<ArmorType, Integer> defense(int boots, int legs, int chest, int helmet) {
		return Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, legs, ArmorType.CHESTPLATE, chest, ArmorType.HELMET, helmet, ArmorType.BODY, 0);
	}

	/** Copper's wear and the copper helmet's protection. */
	static final ArmorMaterial HEADLAMP_MATERIAL = new ArmorMaterial(11, defense(1, 3, 4, 2), 8, SoundEvents.ARMOR_EQUIP_COPPER, 0.0F, 0.0F,
		ItemTags.REPAIRS_COPPER_ARMOR, asset("headlamp"));
	/** A leather chestplate and leggings together: 3 + 2 on the one garment. */
	static final ArmorMaterial OVERALLS_MATERIAL = new ArmorMaterial(7, defense(1, 2, 5, 1), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
		ItemTags.REPAIRS_LEATHER_ARMOR, asset("overalls"));
	static final ArmorMaterial SPIKED_BOOTS_MATERIAL = new ArmorMaterial(7, defense(1, 2, 3, 1), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
		ItemTags.REPAIRS_LEATHER_ARMOR, asset("spiked_boots"));
	static final ArmorMaterial MINING_BOOTS_MATERIAL = new ArmorMaterial(11, defense(2, 2, 3, 1), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F,
		ItemTags.REPAIRS_LEATHER_ARMOR, asset("mining_boots"));

	/**
	 * Armor like a copper helmet's, but with no armor-layer asset: worn, vanilla draws the item's own 3D
	 * model on the head (as it does a carved pumpkin), helmet, lantern and cap together.
	 */
	public static final Item HEADLAMP = register("headlamp", key -> new Item(new Item.Properties()
		.durability(ArmorType.HELMET.getDurability(HEADLAMP_MATERIAL.durability()))
		.attributes(HEADLAMP_MATERIAL.createAttributes(ArmorType.HELMET))
		.enchantable(HEADLAMP_MATERIAL.enchantmentValue())
		.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(HEADLAMP_MATERIAL.equipSound()).build())
		.repairable(HEADLAMP_MATERIAL.repairIngredient()).setId(key)));
	public static final Item OVERALLS = register("overalls", key -> new Item(new Item.Properties()
		.humanoidArmor(OVERALLS_MATERIAL, ArmorType.CHESTPLATE).setId(key)));
	public static final Item SPIKED_BOOTS = register("spiked_boots", key -> new Item(new Item.Properties()
		.humanoidArmor(SPIKED_BOOTS_MATERIAL, ArmorType.BOOTS).setId(key)));
	public static final Item MINING_BOOTS = register("mining_boots", key -> new Item(new Item.Properties()
		.humanoidArmor(MINING_BOOTS_MATERIAL, ArmorType.BOOTS).setId(key)));

	/** Spiked Boots alone: faster climbing on ladders, vines and ropes. */
	public static final Holder<MobEffect> SPELUNKERS_SPEED = effect("spelunkers_speed", 0x9A7B4F);
	/** The full spelunking outfit: the same climbing, ghosted torches, ladders and ropes, worse skeleton aim. */
	public static final Holder<MobEffect> TRAIL_BLAZER = effect("trail_blazer", 0xE0B040);
	/** Mining Boots alone: commodities over the ordinary cap, slower the more is carried. */
	public static final Holder<MobEffect> MULE = effect("mule", 0x7A5A3A);
	/** The full mining outfit: Mule's carrying, offhand pickaxe mining and the rear warning. */
	public static final Holder<MobEffect> OLD_PRO = effect("old_pro", 0xB04030);

	private WorkwearContent() {
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, path);
	}

	private static Item register(String path, Function<ResourceKey<Item>, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(key));
	}

	private static Holder<MobEffect> effect(String path, int color) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id(path), new WorkwearEffect(color));
	}

	/** A named workwear bonus: shown in the effect list; what it does is done where it applies ({@link Workwear}). */
	static final class WorkwearEffect extends MobEffect {
		WorkwearEffect(int color) {
			super(MobEffectCategory.BENEFICIAL, color);
		}
	}

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			output.insertAfter(Items.LEATHER_BOOTS, HEADLAMP, OVERALLS, SPIKED_BOOTS, MINING_BOOTS);
		});
		Workwear.register();
		Headlamps.register();
		TrailMarkers.register();
	}
}
