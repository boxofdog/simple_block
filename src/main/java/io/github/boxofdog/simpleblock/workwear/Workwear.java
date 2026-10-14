package io.github.boxofdog.simpleblock.workwear;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M10 workwear rules, shared by server and client (the client predicts its own player's climbing,
 * mining and locked slots from the same equipment).
 *
 * <ul>
 *   <li><b>Overalls take two slots.</b> Worn in the chest slot, they keep the legs slot empty: leggings
 *       cannot be put on (slot, right-click), and any that get there anyway (a dispenser, putting the
 *       overalls on over leggings) are moved into the inventory at once, or dropped at the player's feet
 *       if it is full ({@link #reconcile}). They open the backpack row ({@link #backpack}).</li>
 *   <li><b>Bonuses</b> ({@link #bonus}): Spiked Boots alone give Spelunker's Speed, Mining Boots alone
 *       Mule. With the Headlamp and Overalls, the full outfit's bonus replaces the boots' own: Trail
 *       Blazer (keeps the climbing) and Old Pro (keeps Mule's carrying). Shown as effects.</li>
 *   <li><b>Climbing</b> ({@link #climbMultiplier}): Spelunker's Speed and Trail Blazer climb ladders,
 *       vines and ropes {@code workwear.climb_speed} times as fast; no second multiplier.</li>
 *   <li><b>Mule</b> ({@link #capacityBonus}): commodities may be taken in up to
 *       {@code workwear.mule_extra_ingots} ingots over the ordinary 64; the commodity part of the load
 *       over 64 slows walking, up to {@code workwear.mule_max_slowdown} at the most it may carry.</li>
 *   <li><b>Overload lock</b> ({@link #lockedSlots}): while commodities are carried beyond what the
 *       ordinary 64 would admit, the worn pieces whose removal would end the active bonus cannot be
 *       taken off or swapped: the Mining Boots for Mule; Headlamp, Overalls (both slots) and Mining
 *       Boots for Old Pro. Equipment alone over 64 locks nothing.</li>
 *   <li><b>Old Pro mining</b> ({@link #miningTool}): a pickaxe in the offhand mines blocks it is right
 *       for whenever the main hand's item is not; the main hand keeps attacking.</li>
 * </ul>
 */
public final class Workwear {
	public enum Bonus {
		NONE, SPELUNKERS_SPEED, TRAIL_BLAZER, MULE, OLD_PRO
	}

	private static final net.minecraft.resources.Identifier MULE_SLOWDOWN = WorkwearContent.id("mule_slowdown");

	private Workwear() {
	}

	private static boolean wears(LivingEntity entity, EquipmentSlot slot, Item item) {
		return entity.getItemBySlot(slot).is(item);
	}

	/** Whether the Overalls are worn: they take the legs slot too and open the backpack row. */
	public static boolean overalls(LivingEntity entity) {
		return wears(entity, EquipmentSlot.CHEST, WorkwearContent.OVERALLS);
	}

	/** Whether the player has the backpack row (inventory 9-17): wearing the Overalls. */
	public static boolean backpack(@Nullable Player player) {
		return player != null && overalls(player);
	}

	public static boolean headlamp(LivingEntity entity) {
		return wears(entity, EquipmentSlot.HEAD, WorkwearContent.HEADLAMP);
	}

	public static Bonus bonus(LivingEntity entity) {
		boolean outfit = headlamp(entity) && overalls(entity);
		if (wears(entity, EquipmentSlot.FEET, WorkwearContent.SPIKED_BOOTS)) {
			return outfit ? Bonus.TRAIL_BLAZER : Bonus.SPELUNKERS_SPEED;
		}
		if (wears(entity, EquipmentSlot.FEET, WorkwearContent.MINING_BOOTS)) {
			return outfit ? Bonus.OLD_PRO : Bonus.MULE;
		}
		return Bonus.NONE;
	}

	/** How much faster this entity climbs (1 without Spiked Boots). */
	public static double climbMultiplier(LivingEntity entity) {
		Bonus bonus = bonus(entity);
		return bonus == Bonus.SPELUNKERS_SPEED || bonus == Bonus.TRAIL_BLAZER ? Tuning.getDouble("workwear.climb_speed") : 1.0;
	}

	/** Whether the player carries like a mule (Mining Boots, alone or as Old Pro). */
	public static boolean mule(Player player) {
		Bonus bonus = bonus(player);
		return bonus == Bonus.MULE || bonus == Bonus.OLD_PRO;
	}

	/** Extra room for commodities over the ordinary cap, in 1/216 ingot. */
	public static int capacityBonus(Player player) {
		return mule(player) ? (int) Math.round(Tuning.getDouble("workwear.mule_extra_ingots") * HeavyLoad.UNITS_PER_INGOT) : 0;
	}

	/**
	 * Commodity weight carried beyond what the ordinary 64 would admit: the load over 64, as far as
	 * commodities make it up (equipment alone over 64 is never overload).
	 */
	public static int commodityOverload(Player player) {
		int over = HeavyLoad.carried(player) - HeavyLoad.CAPACITY;
		return over <= 0 ? 0 : Math.min(over, HeavyLoad.carriedCommodities(player));
	}

	/** The worn slots that cannot be emptied or swapped now (commodity overload holding a bonus). */
	public static Set<EquipmentSlot> lockedSlots(Player player) {
		if (HeavyLoad.bypasses(player) || commodityOverload(player) <= 0) {
			return Set.of();
		}
		return switch (bonus(player)) {
			case OLD_PRO -> EnumSet.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
			case MULE -> EnumSet.of(EquipmentSlot.FEET);
			default -> Set.of();
		};
	}

	public static boolean locked(Player player, EquipmentSlot slot) {
		return lockedSlots(player).contains(slot);
	}

	/** Whether nothing may be put in the legs slot: the Overalls take it. */
	public static boolean legsReserved(LivingEntity entity) {
		return overalls(entity);
	}

	/**
	 * The item this player mines {@code state} with: with Old Pro, an offhand pickaxe that is right for
	 * the block when the main hand's item is not; otherwise the main hand's.
	 */
	public static ItemStack miningTool(Player player, BlockState state) {
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if (bonus(player) != Bonus.OLD_PRO || !off.is(ItemTags.PICKAXES) || main.getDestroySpeed(state) > 1.0F) {
			return main;
		}
		return off.getDestroySpeed(state) > 1.0F ? off : main;
	}

	// ---- Server, every tick ----

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tick(player);
			}
		});
	}

	static void tick(ServerPlayer player) {
		reconcile(player);
		showBonus(player);
		slowMule(player);
	}

	/** Overalls worn: anything in the legs slot moves into the inventory (or drops at the player's feet). */
	public static void reconcile(Player player) {
		if (!legsReserved(player)) {
			return;
		}
		ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
		if (!legs.isEmpty()) {
			player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
			HeavyLoad.exempt(() -> player.getInventory().placeItemBackInInventory(legs, Prediction.SERVER_ONLY));
		}
	}

	private static final List<Holder<MobEffect>> EFFECTS = List.of(WorkwearContent.SPELUNKERS_SPEED, WorkwearContent.TRAIL_BLAZER,
		WorkwearContent.MULE, WorkwearContent.OLD_PRO);

	/** The effect shown for a bonus, or null. */
	public static @Nullable Holder<MobEffect> effect(Bonus bonus) {
		return switch (bonus) {
			case SPELUNKERS_SPEED -> WorkwearContent.SPELUNKERS_SPEED;
			case TRAIL_BLAZER -> WorkwearContent.TRAIL_BLAZER;
			case MULE -> WorkwearContent.MULE;
			case OLD_PRO -> WorkwearContent.OLD_PRO;
			case NONE -> null;
		};
	}

	/** The active bonus is shown as its effect (no particles, lasting while worn); any other workwear effect goes. */
	private static void showBonus(ServerPlayer player) {
		Holder<MobEffect> shown = effect(bonus(player));
		for (Holder<MobEffect> effect : EFFECTS) {
			if (effect.equals(shown)) {
				if (!player.hasEffect(effect)) {
					player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, true, false, true));
				}
			} else if (player.hasEffect(effect)) {
				player.removeEffect(effect);
			}
		}
	}

	/** The Mule's slowdown: none at the ordinary cap, {@code workwear.mule_max_slowdown} at the most it may carry. */
	public static double muleSlowdown(Player player) {
		int bonus = capacityBonus(player);
		if (bonus <= 0) {
			return 0.0;
		}
		double fraction = Math.min(1.0, commodityOverload(player) / (double) bonus);
		return fraction * Tuning.getDouble("workwear.mule_max_slowdown");
	}

	private static void slowMule(ServerPlayer player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		double slowdown = muleSlowdown(player);
		AttributeModifier current = speed.getModifier(MULE_SLOWDOWN);
		if (slowdown <= 0.0) {
			if (current != null) {
				speed.removeModifier(MULE_SLOWDOWN);
			}
		} else if (current == null || Math.abs(current.amount() + slowdown) > 1.0E-4) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(MULE_SLOWDOWN, -slowdown, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}
}
