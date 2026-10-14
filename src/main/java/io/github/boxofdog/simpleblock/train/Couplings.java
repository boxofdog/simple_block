package io.github.boxofdog.simpleblock.train;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import io.github.boxofdog.simpleblock.feature.Feedback;

/**
 * M05 train coupling (owner's choice: an iron chain). Use an iron chain on one minecart, then on
 * another within {@value #REACH} blocks: the chain is used up and couples them, end to end, about
 * {@value #REST} blocks apart (centre to centre). Use shears on a coupled cart to take off the coupling
 * nearest where you clicked; the chain drops. The plain minecart (freight, cushioned or with a rod) and
 * the Cobblestone Engine couple; each has two ends, so at most two couplings. A destroyed cart drops its chains;
 * a coupling pulled further than {@value #BREAK} blocks apart (a derailment) snaps and drops its chain.
 *
 * <p>Each server tick, after every entity has moved, the carts of a train are kept together: a train
 * that something drives this tick ({@link #command}: the engine, or a cable-hauled cart) moves its
 * driver as commanded and every other car follows its neighbour towards the driver; a held train stays
 * put; a train nothing drives behaves as carts joined by rods (push one and the rest come along). A
 * train with a coupled cart that is not loaded is held still, so nothing is dragged into or out of
 * unloaded chunks.
 *
 * <p>Couplings are saved on both carts by entity UUID; their entity ids are synced so clients can draw
 * the chains. The state lives on {@link Coupled}, implemented by {@code mixin/train/AbstractMinecartMixin}.
 *
 * <p>M06: a train with a car docked at a loading track ({@link LoadingTracks}) is held, whatever
 * drives it, until the track releases it.
 */
public final class Couplings {
	/** Centre-to-centre distance of coupled carts. */
	public static final double REST = 1.6;
	/** How far apart two carts may be when they are coupled. */
	public static final double REACH = 3.0;
	/** A coupling stretched further than this snaps. */
	public static final double BREAK = 4.0;
	/** Share of the gap error a follower corrects each tick. */
	private static final double STIFFNESS = 0.5;
	private static final double MAX_CORRECTION = 0.25;
	/** How long a first chain click waits for the second, in ticks. */
	private static final long PENDING_TICKS = 600;

	/** A minecart's couplings and the train controls' state. */
	public interface Coupled {
		@Nullable UUID simpleBlock$coupling(int slot);

		void simpleBlock$setCoupling(int slot, @Nullable UUID partner);

		/** The partner's entity id as clients know it, or -1. */
		int simpleBlock$couplingId(int slot);

		void simpleBlock$setCouplingId(int slot, int id);

		/** Where a held cart is pinned, or null. */
		@Nullable Vec3 simpleBlock$holdPos();

		void simpleBlock$setHoldPos(@Nullable Vec3 pos);

		/** Whether a driver commanded this cart's train last tick. */
		boolean simpleBlock$controlled();

		void simpleBlock$setControlled(boolean controlled);

		/** M06: the loading track this cart is docked at (its train is held), or null. Saved. */
		@Nullable BlockPos simpleBlock$dock();

		void simpleBlock$setDock(@Nullable BlockPos dock);
	}

	private record Pending(UUID cart, long time) {
	}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static final Map<ResourceKey<Level>, Set<AbstractMinecart>> ACTIVE = new HashMap<>();
	/** This tick's commands: train root to velocity (null: hold). */
	private static final Map<ResourceKey<Level>, Map<AbstractMinecart, @Nullable Vec3>> COMMANDS = new HashMap<>();

	private Couplings() {
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(Couplings::endTick);
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof AbstractMinecart cart && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
				destroyed(cart, level);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PENDING.clear();
			ACTIVE.clear();
			COMMANDS.clear();
		});
	}

	public static Coupled coupled(AbstractMinecart cart) {
		return (Coupled) cart;
	}

	public static boolean hasCouplings(AbstractMinecart cart) {
		return coupled(cart).simpleBlock$coupling(0) != null || coupled(cart).simpleBlock$coupling(1) != null;
	}

	/** Called by every server-side minecart tick that has couplings or train control state. */
	public static void noteActive(AbstractMinecart cart) {
		if (cart.level() instanceof ServerLevel level) {
			ACTIVE.computeIfAbsent(level.dimension(), key -> Collections.newSetFromMap(new IdentityHashMap<>())).add(cart);
		}
	}

	/** A driver's order to hold its train still this tick (a parked or stalled engine). An order to move, from another driver in the same train, wins. */
	public static void commandHold(AbstractMinecart root) {
		command(root, null);
	}

	/** A driver's order for this tick: the train with {@code root} in it moves the root at {@code velocity}, or holds still (null). */
	public static void command(AbstractMinecart root, @Nullable Vec3 velocity) {
		if (root.level() instanceof ServerLevel level) {
			COMMANDS.computeIfAbsent(level.dimension(), key -> new IdentityHashMap<>()).put(root, velocity);
			noteActive(root);
		}
	}

	static @Nullable AbstractMinecart resolve(ServerLevel level, @Nullable UUID id) {
		return id != null && level.getEntity(id) instanceof AbstractMinecart cart && !cart.isRemoved() ? cart : null;
	}

	/** The loaded carts coupled to {@code start}, directly or through others, including itself; the start first. */
	public static List<AbstractMinecart> consist(AbstractMinecart start) {
		List<AbstractMinecart> found = new ArrayList<>();
		if (!(start.level() instanceof ServerLevel level)) {
			found.add(start);
			return found;
		}
		Set<AbstractMinecart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		ArrayDeque<AbstractMinecart> queue = new ArrayDeque<>(List.of(start));
		seen.add(start);
		while (!queue.isEmpty()) {
			AbstractMinecart cart = queue.poll();
			found.add(cart);
			for (int slot = 0; slot < 2; slot++) {
				AbstractMinecart partner = resolve(level, coupled(cart).simpleBlock$coupling(slot));
				if (partner != null && seen.add(partner)) {
					queue.add(partner);
				}
			}
		}
		return found;
	}

	/** M06: whether any car of {@code cart}'s train is docked at a loading track (the whole train is then held). */
	public static boolean docked(AbstractMinecart cart) {
		for (AbstractMinecart car : consist(cart)) {
			if (coupled(car).simpleBlock$dock() != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * M06: whether every car of {@code cart}'s train is loaded and in an entity-ticking chunk, and so is
	 * the track {@code ahead} of each (a move about to be made). A train that is not must not be driven:
	 * nothing is pulled into, or dragged out of, the unloaded world.
	 */
	public static boolean allTicking(ServerLevel level, AbstractMinecart cart, Vec3 ahead) {
		Set<AbstractMinecart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		ArrayDeque<AbstractMinecart> queue = new ArrayDeque<>(List.of(cart));
		seen.add(cart);
		while (!queue.isEmpty()) {
			AbstractMinecart car = queue.poll();
			if (!level.isPositionEntityTicking(car.blockPosition()) || !level.isPositionEntityTicking(BlockPos.containing(car.position().add(ahead)))) {
				return false;
			}
			for (int slot = 0; slot < 2; slot++) {
				UUID id = coupled(car).simpleBlock$coupling(slot);
				if (id == null) {
					continue;
				}
				AbstractMinecart partner = resolve(level, id);
				if (partner == null) {
					return false;
				}
				if (seen.add(partner)) {
					queue.add(partner);
				}
			}
		}
		return true;
	}

	/** Total mass of the train {@code cart} is part of ({@link TrainPhysics#mass}). */
	public static double consistMass(AbstractMinecart cart) {
		return consist(cart).stream().mapToDouble(TrainPhysics::mass).sum();
	}

	// ---- Coupling and uncoupling ----

	private static Component message(String key, String fallback) {
		return Component.translatableWithFallback("simple_block.train." + key, fallback);
	}

	/**
	 * A use on a minecart with a chain (couple) or shears (uncouple); null for anything else. The client
	 * swings the arm; the server decides.
	 */
	public static @Nullable InteractionResult interact(AbstractMinecart cart, Player player, InteractionHand hand, Vec3 location) {
		ItemStack held = player.getItemInHand(hand);
		boolean chain = held.is(Items.IRON_CHAIN);
		if (!chain && !(held.is(Items.SHEARS) && hasCouplings(cart))) {
			return null;
		}
		if (player instanceof ServerPlayer serverPlayer && cart.level() instanceof ServerLevel level) {
			if (chain) {
				chain(level, cart, serverPlayer, held);
			} else {
				uncouple(level, cart, cart.position().add(location));
			}
		}
		return InteractionResult.SUCCESS;
	}

	private static int freeSlot(AbstractMinecart cart) {
		return coupled(cart).simpleBlock$coupling(0) == null ? 0 : coupled(cart).simpleBlock$coupling(1) == null ? 1 : -1;
	}

	private static void chain(ServerLevel level, AbstractMinecart cart, ServerPlayer player, ItemStack chain) {
		Pending pending = PENDING.remove(player.getUUID());
		AbstractMinecart first = pending == null || level.getGameTime() - pending.time() > PENDING_TICKS ? null : resolve(level, pending.cart());
		if (first == null || first == cart) {
			if (freeSlot(cart) < 0) {
				Feedback.quiet(player, message("ends_taken", "Both ends of this cart are coupled"));
				return;
			}
			PENDING.put(player.getUUID(), new Pending(cart.getUUID(), level.getGameTime()));
			cart.playSound(SoundEvents.CHAIN_HIT, 0.8F, 1.2F);
			return;
		}
		if (first.distanceTo(cart) > REACH) {
			Feedback.quiet(player, message("too_far", "Too far apart to couple"));
			return;
		}
		if (cart.getUUID().equals(coupled(first).simpleBlock$coupling(0)) || cart.getUUID().equals(coupled(first).simpleBlock$coupling(1))) {
			return;
		}
		int firstSlot = freeSlot(first);
		int slot = freeSlot(cart);
		if (firstSlot < 0 || slot < 0) {
			Feedback.quiet(player, message("ends_taken", "Both ends of this cart are coupled"));
			return;
		}
		coupled(first).simpleBlock$setCoupling(firstSlot, cart.getUUID());
		coupled(cart).simpleBlock$setCoupling(slot, first.getUUID());
		chain.consume(1, player);
		cart.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
	}

	/** Takes off the coupling nearest {@code near}; its chain drops between the carts. */
	private static void uncouple(ServerLevel level, AbstractMinecart cart, Vec3 near) {
		int best = -1;
		double bestDistance = Double.MAX_VALUE;
		for (int slot = 0; slot < 2; slot++) {
			UUID id = coupled(cart).simpleBlock$coupling(slot);
			if (id == null) {
				continue;
			}
			AbstractMinecart partner = resolve(level, id);
			double distance = partner == null ? Double.MAX_VALUE / 2 : partner.position().distanceToSqr(near);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = slot;
			}
		}
		if (best >= 0) {
			AbstractMinecart partner = resolve(level, coupled(cart).simpleBlock$coupling(best));
			unlink(cart, best, partner);
			Vec3 at = partner == null ? cart.position() : cart.position().add(partner.position()).scale(0.5);
			dropChain(level, at);
			cart.playSound(SoundEvents.SHEARS_SNIP, 1.0F, 1.0F);
		}
	}

	private static void unlink(AbstractMinecart cart, int slot, @Nullable AbstractMinecart partner) {
		coupled(cart).simpleBlock$setCoupling(slot, null);
		coupled(cart).simpleBlock$setCouplingId(slot, -1);
		if (partner != null) {
			for (int other = 0; other < 2; other++) {
				if (cart.getUUID().equals(coupled(partner).simpleBlock$coupling(other))) {
					coupled(partner).simpleBlock$setCoupling(other, null);
					coupled(partner).simpleBlock$setCouplingId(other, -1);
				}
			}
		}
	}

	private static void dropChain(ServerLevel level, Vec3 at) {
		level.addFreshEntity(new ItemEntity(level, at.x, at.y + 0.5, at.z, new ItemStack(Items.IRON_CHAIN)));
	}

	/** A destroyed cart drops a chain for each coupling and frees its partners. */
	private static void destroyed(AbstractMinecart cart, ServerLevel level) {
		for (int slot = 0; slot < 2; slot++) {
			if (coupled(cart).simpleBlock$coupling(slot) != null) {
				unlink(cart, slot, resolve(level, coupled(cart).simpleBlock$coupling(slot)));
				dropChain(level, cart.position());
			}
		}
	}

	/**
	 * Moves a cart's couplings to its replacement (a minecart becoming a Cobblestone Engine), so the
	 * old cart drops no chains.
	 */
	public static void transfer(ServerLevel level, AbstractMinecart from, AbstractMinecart to) {
		for (int slot = 0; slot < 2; slot++) {
			UUID partnerId = coupled(from).simpleBlock$coupling(slot);
			if (partnerId == null) {
				continue;
			}
			coupled(to).simpleBlock$setCoupling(slot, partnerId);
			coupled(from).simpleBlock$setCoupling(slot, null);
			AbstractMinecart partner = resolve(level, partnerId);
			if (partner != null) {
				for (int other = 0; other < 2; other++) {
					if (from.getUUID().equals(coupled(partner).simpleBlock$coupling(other))) {
						coupled(partner).simpleBlock$setCoupling(other, to.getUUID());
					}
				}
			}
		}
	}

	// ---- Keeping trains together ----

	private static void endTick(ServerLevel level) {
		Set<AbstractMinecart> active = ACTIVE.remove(level.dimension());
		Map<AbstractMinecart, @Nullable Vec3> commands = COMMANDS.remove(level.dimension());
		if (active == null) {
			return;
		}
		if (commands == null) {
			commands = Map.of();
		}
		Set<AbstractMinecart> done = Collections.newSetFromMap(new IdentityHashMap<>());
		// Commanded trains first, so their order wins over the free-train rule.
		List<AbstractMinecart> order = new ArrayList<>(commands.keySet());
		order.addAll(active);
		for (AbstractMinecart start : new LinkedHashSet<>(order)) {
			if (done.contains(start) || start.isRemoved()) {
				continue;
			}
			Train train = Train.of(level, start);
			done.addAll(train.cars);
			if (train.docked()) {
				// M06: a train docked at a loading track stays put, whatever drives it.
				train.hold();
				continue;
			}
			// The driver: one ordering it to move if there is one (a cable hauling a parked engine's train), else one holding it.
			AbstractMinecart root = null;
			for (AbstractMinecart car : train.cars) {
				if (commands.containsKey(car) && (root == null || commands.get(root) == null && commands.get(car) != null)) {
					root = car;
				}
			}
			if (root != null) {
				Vec3 velocity = train.complete ? commands.get(root) : null;
				if (velocity == null) {
					train.hold();
				} else {
					train.drive(root, velocity);
				}
			} else {
				train.free();
			}
		}
	}

	/** The loaded carts of one train, with their couplings resolved. */
	private static final class Train {
		final ServerLevel level;
		final List<AbstractMinecart> cars = new ArrayList<>();
		/** Whether every coupling's partner is loaded. */
		boolean complete = true;

		private Train(ServerLevel level) {
			this.level = level;
		}

		static Train of(ServerLevel level, AbstractMinecart start) {
			Train train = new Train(level);
			Set<AbstractMinecart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
			ArrayDeque<AbstractMinecart> queue = new ArrayDeque<>(List.of(start));
			seen.add(start);
			while (!queue.isEmpty()) {
				AbstractMinecart car = queue.poll();
				train.cars.add(car);
				for (int slot = 0; slot < 2; slot++) {
					UUID id = coupled(car).simpleBlock$coupling(slot);
					if (id == null) {
						coupled(car).simpleBlock$setCouplingId(slot, -1);
						continue;
					}
					AbstractMinecart partner = resolve(level, id);
					if (partner == null) {
						if (areaLoaded(level, car.blockPosition())) {
							// Gone (destroyed while this cart was unloaded): it already dropped the chain.
							unlinkOneSide(car, slot);
						} else {
							train.complete = false;
							coupled(car).simpleBlock$setCouplingId(slot, -1);
						}
						continue;
					}
					if (partner.distanceTo(car) > BREAK) {
						unlink(car, slot, partner);
						dropChain(level, car.position().add(partner.position()).scale(0.5));
						car.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 1.0F);
						continue;
					}
					coupled(car).simpleBlock$setCouplingId(slot, partner.getId());
					if (seen.add(partner)) {
						queue.add(partner);
					}
				}
			}
			return train;
		}

		boolean docked() {
			for (AbstractMinecart car : this.cars) {
				if (coupled(car).simpleBlock$dock() != null) {
					return true;
				}
			}
			return false;
		}

		private static void unlinkOneSide(AbstractMinecart car, int slot) {
			coupled(car).simpleBlock$setCoupling(slot, null);
			coupled(car).simpleBlock$setCouplingId(slot, -1);
		}

		private static boolean areaLoaded(ServerLevel level, BlockPos pos) {
			int reach = (int) Math.ceil(BREAK);
			for (int dx = -reach; dx <= reach; dx += reach) {
				for (int dz = -reach; dz <= reach; dz += reach) {
					if (!level.isPositionEntityTicking(pos.offset(dx, 0, dz))) {
						return false;
					}
				}
			}
			return true;
		}

		void hold() {
			for (AbstractMinecart car : this.cars) {
				Coupled state = coupled(car);
				if (state.simpleBlock$holdPos() == null) {
					state.simpleBlock$setHoldPos(car.position());
				} else {
					car.setPos(state.simpleBlock$holdPos());
				}
				car.setDeltaMovement(Vec3.ZERO);
				state.simpleBlock$setControlled(true);
			}
		}

		void drive(AbstractMinecart root, Vec3 velocity) {
			Map<AbstractMinecart, Vec3> set = new IdentityHashMap<>();
			root.setDeltaMovement(velocity);
			set.put(root, velocity);
			ArrayDeque<AbstractMinecart> queue = new ArrayDeque<>(List.of(root));
			while (!queue.isEmpty()) {
				AbstractMinecart parent = queue.poll();
				for (int slot = 0; slot < 2; slot++) {
					AbstractMinecart child = resolve(this.level, coupled(parent).simpleBlock$coupling(slot));
					if (child == null || set.containsKey(child) || !this.cars.contains(child)) {
						continue;
					}
					Vec3 offset = child.position().subtract(parent.position());
					Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
					double distance = horizontal.length();
					Vec3 v = set.get(parent);
					if (distance > 1.0E-3) {
						Vec3 u = horizontal.scale(1.0 / distance);
						double correction = Math.clamp((distance - REST) * STIFFNESS, -MAX_CORRECTION, MAX_CORRECTION);
						v = v.subtract(u.scale(correction));
					}
					child.setDeltaMovement(v);
					set.put(child, v);
					queue.add(child);
				}
			}
			for (AbstractMinecart car : this.cars) {
				coupled(car).simpleBlock$setHoldPos(null);
				coupled(car).simpleBlock$setControlled(true);
			}
		}

		/** Nothing drives it: coupled carts move as if joined by rods; a train let go this tick stops. */
		void free() {
			for (AbstractMinecart car : this.cars) {
				Coupled state = coupled(car);
				if (state.simpleBlock$controlled()) {
					state.simpleBlock$setControlled(false);
					state.simpleBlock$setHoldPos(null);
					car.setDeltaMovement(Vec3.ZERO);
				}
			}
			for (AbstractMinecart car : this.cars) {
				for (int slot = 0; slot < 2; slot++) {
					AbstractMinecart partner = resolve(this.level, coupled(car).simpleBlock$coupling(slot));
					if (partner == null || partner.getId() < car.getId()) {
						continue;
					}
					Vec3 offset = partner.position().subtract(car.position());
					Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
					double distance = horizontal.length();
					if (distance < 1.0E-3) {
						continue;
					}
					Vec3 u = horizontal.scale(1.0 / distance);
					double closing = partner.getDeltaMovement().subtract(car.getDeltaMovement()).dot(u);
					double impulse = Math.clamp(((distance - REST) * STIFFNESS + closing) / 2.0, -MAX_CORRECTION, MAX_CORRECTION);
					car.setDeltaMovement(car.getDeltaMovement().add(u.scale(impulse)));
					partner.setDeltaMovement(partner.getDeltaMovement().subtract(u.scale(impulse)));
				}
			}
		}
	}
}
