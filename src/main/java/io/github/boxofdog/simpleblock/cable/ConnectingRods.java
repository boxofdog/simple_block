package io.github.boxofdog.simpleblock.cable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.MinecartCushions;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * The copper connecting rod, fitted to a minecart. It stands up from the cart like a tram's trolley
 * pole, sprung upwards, with a grip and roller head. Whenever the cart is under a cable laid out on a
 * route (within {@value #TOLERANCE} blocks sideways, {@code cable.rod_reach_min} to
 * {@code cable.rod_reach_max} blocks above the cart), the head rides up onto it and grips it; it lets go
 * where the cable leaves. The grip stays on across anchors: the head's jaw is open on the side away
 * from the anchors' hanger arms, so it rolls past their rollers instead of catching on them.
 *
 * <p>On an open route gripping does nothing to the cart. On a closed loop (M05) the cart moves with the
 * working strand ({@link CableDrive}); the rod never grips a loop's return strand. The cart remembers
 * the rod (synced, saved) and its current reach (synced, 0 when not gripping), on {@link Fitted},
 * implemented by {@code mixin/minecart/MinecartMixin}.
 *
 * <ul>
 *   <li>Fitting: use a connecting rod on a minecart. One rod is used up.</li>
 *   <li>Taking off: sneak and use the cart once its cushion and cargo are out (they come out first).</li>
 *   <li>Destroying the cart drops the rod.</li>
 * </ul>
 */
public final class ConnectingRods {
	/** How far sideways from the cart's middle the cable can run for the head to find it, in blocks. */
	public static final double TOLERANCE = 0.5;

	/** A minecart's rod state. */
	public interface Fitted {
		boolean simpleBlock$rod();

		void simpleBlock$setRod(boolean rod);

		/** Height of the gripped cable above the cart, or 0 when the rod grips nothing. */
		float simpleBlock$reach();

		void simpleBlock$setReach(float reach);
	}

	private ConnectingRods() {
	}

	public static Fitted fitted(Minecart minecart) {
		return (Fitted) minecart;
	}

	public static boolean hasRod(Minecart minecart) {
		return fitted(minecart).simpleBlock$rod();
	}

	public static boolean isGripping(Minecart minecart) {
		return fitted(minecart).simpleBlock$reach() > 0.0F;
	}

	public static double reachMin() {
		return Tuning.getDouble("cable.rod_reach_min");
	}

	public static double reachMax() {
		return Tuning.getDouble("cable.rod_reach_max");
	}

	/** Whether this use is about the rod: fitting one, or taking it off a cart with nothing else in it. */
	public static boolean handles(Minecart minecart, Player player, InteractionHand hand) {
		if (!player.isSecondaryUseActive()) {
			return player.getItemInHand(hand).is(CableContent.CONNECTING_ROD);
		}
		return hand == InteractionHand.MAIN_HAND && hasRod(minecart) && !MinecartCushions.isFitted(minecart)
			&& MinecartCargo.cart(minecart).simpleBlock$count() == 0;
	}

	public static InteractionResult interact(Minecart minecart, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			Fitted rod = fitted(minecart);
			if (player.isSecondaryUseActive()) {
				rod.simpleBlock$setRod(false);
				rod.simpleBlock$setReach(0.0F);
				serverPlayer.getInventory().placeItemBackInInventory(new ItemStack(CableContent.CONNECTING_ROD), Prediction.SERVER_ONLY);
				minecart.playSound(SoundEvents.COPPER_HIT, 1.0F, 0.8F);
				serverPlayer.sendOverlayMessage(CableRoutes.message("rod_removed", "Connecting rod taken off"));
			} else if (rod.simpleBlock$rod()) {
				serverPlayer.sendOverlayMessage(CableRoutes.message("rod_present", "This cart already has a connecting rod"));
			} else {
				rod.simpleBlock$setRod(true);
				player.getItemInHand(hand).consume(1, player);
				minecart.playSound(SoundEvents.COPPER_PLACE, 1.0F, 1.0F);
				serverPlayer.sendOverlayMessage(CableRoutes.message("rod_fitted", "Connecting rod fitted: it grips a cable the cart passes under"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Server, every tick of a cart with a rod: grips the cable above, if any, and (M05) moves with a
	 * closed loop's working strand ({@link CableDrive}).
	 */
	public static void tick(Minecart minecart) {
		if (!(minecart.level() instanceof ServerLevel level) || !hasRod(minecart)) {
			return;
		}
		Fitted rod = fitted(minecart);
		CableIndex.Grip grip = CableIndex.grip(level, minecart.position(), TOLERANCE, reachMin(), reachMax());
		float next = grip == null ? 0.0F : (float) grip.reach();
		if (next > 0.0F && rod.simpleBlock$reach() <= 0.0F) {
			minecart.playSound(SoundEvents.CHAIN_PLACE, 0.4F, 1.6F);
		}
		rod.simpleBlock$setReach(next);
		if (grip != null) {
			CableDrive.driveCart(level, minecart, grip);
		}
	}

	/** Drops the rod as an item and clears it; does nothing the second time. */
	public static void dropRod(Minecart minecart, ServerLevel level) {
		Fitted rod = fitted(minecart);
		if (rod.simpleBlock$rod()) {
			rod.simpleBlock$setRod(false);
			rod.simpleBlock$setReach(0.0F);
			minecart.spawnAtLocation(level, new ItemStack(CableContent.CONNECTING_ROD));
		}
	}

	public static void save(Fitted rod, ValueOutput output) {
		if (rod.simpleBlock$rod()) {
			output.putBoolean("simple_block:connecting_rod", true);
		}
	}

	public static void read(Fitted rod, ValueInput input) {
		rod.simpleBlock$setRod(input.getBooleanOr("simple_block:connecting_rod", false));
	}
}
