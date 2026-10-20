package io.github.boxofdog.simpleblock.train;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.feature.MinecartCushions;
import io.github.boxofdog.simpleblock.tuning.Tuning;
import io.github.boxofdog.simpleblock.feature.Feedback;

/**
 * M05 Cobblestone Engine: vanilla's furnace minecart, reworked into a small coal-fired locomotive.
 *
 * <ul>
 *   <li><b>Making one:</b> use a furnace on an empty minecart (no cargo, cushion, rod or rider); it
 *       becomes an engine where it stands, keeping its couplings. The crafted furnace-minecart recipe
 *       is disabled. Breaking an engine drops a minecart and a furnace (never an engine item).</li>
 *   <li><b>Fuel:</b> use coal or charcoal on it (3,600 ticks of driving each, up to 32,000). Fuel burns
 *       only while it drives.</li>
 *   <li><b>From the ground:</b> use it with an empty hand. Stopped, it sets off away from you along
 *       its track; running, it brakes to a stop.</li>
 *   <li><b>From the seat</b> (owner's choice): a player sitting in any cushioned cart coupled to it
 *       drives it. Holding forward powers the train towards where they look (along the track: forwards
 *       or back); holding back brakes. Their input overrides the ground setting while held.</li>
 *   <li><b>M15 selector lever</b> on the back ({@link EngineSelector}): THROUGH, NEUTRAL or SERVICE at
 *       Loading Rail stations ({@link FreightStations}).</li>
 * </ul>
 *
 * <p>Traction (tuning {@code train.*}, masses in {@link TrainPhysics}): the engine's force accelerates
 * the whole train's mass; every car on a climbing rail pulls back with {@code train.grade_force} per
 * unit of its mass (and pushes on a falling one); rolling resistance takes a little; the speed tops out
 * at {@code train.max_speed}. A level, well-built line lets an engine pull a long train; a sustained
 * climb stalls it, even with empty carts behind it (an engine alone still climbs, slowly). A train that
 * stops, or stalls, holds where it is with its brakes on. Nothing depends on what the carts carry.
 */
public final class CobblestoneEngine {
	public static final int FUEL_PER_ITEM = 3600;
	public static final int MAX_FUEL = 32000;

	/** An engine's state, on {@link MinecartFurnace} ({@code mixin/train/MinecartFurnaceMixin}). */
	public interface Engine {
		int simpleBlock$fuel();

		void simpleBlock$setFuel(int fuel);

		/** Whether it was set going from the ground (cleared by stopping it). */
		boolean simpleBlock$running();

		void simpleBlock$setRunning(boolean running);

		/** Unit horizontal direction it drives in, or zero before it has ever started. */
		Vec3 simpleBlock$heading();

		void simpleBlock$setHeading(Vec3 heading);

		/** Train speed along the heading, blocks per tick (never negative). */
		double simpleBlock$speed();

		void simpleBlock$setSpeed(double speed);

		/** Whether it is burning coal right now (synced: smoke and the lit firebox). */
		void simpleBlock$setBurning(boolean burning);

		/** M15: the rear selector lever's position ({@link EngineSelector.Mode}). Synced and saved. */
		EngineSelector.Mode simpleBlock$selector();

		void simpleBlock$setSelector(EngineSelector.Mode mode);

		/** M15: the game time from which a SERVICE selection may start transfers (the arming delay). Saved. */
		long simpleBlock$armedAt();

		void simpleBlock$setArmedAt(long time);

		/** M15: whether a freight transfer has begun since SERVICE was last chosen (for the automatic return to NEUTRAL). Saved. */
		boolean simpleBlock$serviceBegan();

		void simpleBlock$setServiceBegan(boolean began);

		/** M15: the terminal Loading Rail it last passed without docking (looked at once per arrival), or null. Not saved. */
		@Nullable BlockPos simpleBlock$passed();

		void simpleBlock$setPassed(@Nullable BlockPos rail);
	}

	private CobblestoneEngine() {
	}

	public static Engine engine(MinecartFurnace engine) {
		return (Engine) engine;
	}

	private static Component message(String key, String fallback) {
		return Component.translatableWithFallback("simple_block.engine." + key, fallback);
	}

	// ---- Making one ----

	/** A furnace used on a minecart: turns an empty one into an engine. Null when the use is not about this. */
	public static @Nullable InteractionResult convert(Minecart cart, Player player, InteractionHand hand) {
		ItemStack furnace = player.getItemInHand(hand);
		if (!furnace.is(Items.FURNACE) || player.isSecondaryUseActive()) {
			return null;
		}
		if (player instanceof ServerPlayer serverPlayer && cart.level() instanceof ServerLevel level) {
			if (MinecartCargo.cart(cart).simpleBlock$count() > 0 || MinecartCushions.isFitted(cart) || ConnectingRods.hasRod(cart) || cart.isVehicle()) {
				Feedback.quiet(serverPlayer, message("not_empty", "Empty the cart first"));
				return InteractionResult.SUCCESS;
			}
			MinecartFurnace engine = EntityTypes.FURNACE_MINECART.create(level, EntitySpawnReason.CONVERSION);
			if (engine == null) {
				return InteractionResult.SUCCESS;
			}
			engine.snapTo(cart.getX(), cart.getY(), cart.getZ(), cart.getYRot(), cart.getXRot());
			engine.setCustomName(cart.getCustomName());
			Couplings.transfer(level, cart, engine);
			cart.discard();
			level.addFreshEntity(engine);
			furnace.consume(1, player);
			engine.playSound(SoundEvents.FIRECHARGE_USE, 0.6F, 0.8F);
		}
		return InteractionResult.SUCCESS;
	}

	// ---- Controls ----

	/** A use on an engine: coal refuels, an empty hand starts or stops it, chains and shears couple. */
	public static InteractionResult interact(MinecartFurnace engine, Player player, InteractionHand hand, Vec3 location) {
		InteractionResult coupling = Couplings.interact(engine, player, hand, location);
		if (coupling != null) {
			return coupling;
		}
		ItemStack held = player.getItemInHand(hand);
		Engine state = engine(engine);
		if (held.is(ItemTags.FURNACE_MINECART_FUEL)) {
			if (player instanceof ServerPlayer serverPlayer) {
				if (state.simpleBlock$fuel() + FUEL_PER_ITEM > MAX_FUEL) {
					Feedback.quiet(serverPlayer, message("full", "The firebox is full"));
				} else {
					state.simpleBlock$setFuel(state.simpleBlock$fuel() + FUEL_PER_ITEM);
					held.consume(1, player);
					engine.playSound(SoundEvents.FURNACE_FIRE_CRACKLE, 1.0F, 1.0F);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		// M15: the rear selector lever, by hand.
		InteractionResult lever = EngineSelector.use(engine, player, location);
		if (lever != null) {
			return lever;
		}
		if (!held.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			if (state.simpleBlock$running()) {
				state.simpleBlock$setRunning(false);
			} else if (Couplings.docked(engine)) {
				Feedback.quiet(serverPlayer, FreightStations.stationTrain(engine)
					? message("docked_station", "Held at a station: set the lever to THROUGH to leave")
					: message("docked", "Held at a Loading Rail: use the rail to release it"));
			} else if (state.simpleBlock$fuel() <= 0) {
				Feedback.quiet(serverPlayer, message("no_fuel", "Needs coal"));
			} else {
				Vec3 away = engine.position().subtract(player.position());
				Vec3 heading = alongTrack(engine, away);
				if (heading != null) {
					state.simpleBlock$setHeading(heading);
					state.simpleBlock$setRunning(true);
					engine.playSound(SoundEvents.PISTON_EXTEND, 0.5F, 0.6F);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** {@code wanted} turned into a direction along the engine's track (its rail's axis, or its last heading on a curve). */
	private static @Nullable Vec3 alongTrack(MinecartFurnace engine, Vec3 wanted) {
		Vec3 axis = TrainPhysics.railAxis(engine);
		if (axis == null) {
			Vec3 heading = engine(engine).simpleBlock$heading();
			axis = heading.lengthSqr() > 0.5 ? heading : null;
		}
		if (axis == null) {
			return null;
		}
		double along = axis.dot(new Vec3(wanted.x, 0.0, wanted.z));
		if (Math.abs(along) < 1.0E-6) {
			return null;
		}
		return along > 0 ? axis : axis.scale(-1.0);
	}

	// ---- Driving ----

	/** Server, every tick: rider input, traction, fuel; the train is then moved by {@link Couplings}. */
	public static void tick(MinecartFurnace engine) {
		if (!(engine.level() instanceof ServerLevel level)) {
			return;
		}
		Engine state = engine(engine);
		if (FreightStations.tick(level, engine)) {
			// M06: docked at a loading track: switched off and held until released and started again (M15: at a
			// station, until the selector goes to THROUGH).
			state.simpleBlock$setRunning(false);
			state.simpleBlock$setSpeed(0.0);
			state.simpleBlock$setBurning(false);
			Couplings.commandHold(engine);
			return;
		}
		List<AbstractMinecart> train = Couplings.consist(engine);

		// A rider in the train overrides the ground setting while they hold forward or back.
		Vec3 wanted = null;
		boolean braking = false;
		for (AbstractMinecart car : train) {
			for (Entity passenger : car.getPassengers()) {
				if (passenger instanceof ServerPlayer player) {
					Input input = player.getLastClientInput();
					if (input.backward()) {
						braking = true;
					} else if (input.forward()) {
						wanted = player.getLookAngle();
					}
				}
			}
		}
		Vec3 heading = state.simpleBlock$heading();
		double speed = state.simpleBlock$speed();
		boolean power;
		if (braking) {
			power = false;
		} else if (wanted != null) {
			Vec3 direction = alongTrack(engine, wanted);
			if (direction != null && heading.lengthSqr() > 0.5 && direction.dot(heading) < 0 && speed > 1.0E-3) {
				braking = true;
				power = false;
			} else {
				if (direction != null) {
					heading = direction;
				}
				power = heading.lengthSqr() > 0.5;
			}
		} else {
			power = state.simpleBlock$running();
			braking = !power;
		}
		power &= state.simpleBlock$fuel() > 0;

		double mass = 0.0;
		double climb = 0.0;
		for (AbstractMinecart car : train) {
			double m = TrainPhysics.mass(car);
			mass += m;
			climb += m * TrainPhysics.gradeSign(car, heading);
		}
		double force = power ? Tuning.getDouble("train.engine_force") : 0.0;
		double resistance = climb * Tuning.getDouble("train.grade_force") + (speed > 0.0 ? mass * Tuning.getDouble("train.rolling_resistance") : 0.0);
		double acceleration = (force - resistance) / Math.max(mass, 1.0E-3);
		if (braking) {
			acceleration = Math.min(acceleration, -Tuning.getDouble("train.brake"));
		}
		speed = Math.clamp(speed + acceleration, 0.0, Tuning.getDouble("train.max_speed"));
		if (power) {
			state.simpleBlock$setFuel(state.simpleBlock$fuel() - 1);
		}
		state.simpleBlock$setHeading(heading);
		state.simpleBlock$setSpeed(speed);
		state.simpleBlock$setBurning(power);
		if (speed > 1.0E-4 && heading.lengthSqr() > 0.5) {
			Couplings.command(engine, heading.scale(speed));
		} else {
			Couplings.commandHold(engine);
		}
	}

	/** After the engine has moved: keeps its heading pointing the way it actually goes (round curves). */
	public static void followTrack(MinecartFurnace engine, Vec3 moved) {
		Engine state = engine(engine);
		Vec3 horizontal = new Vec3(moved.x, 0.0, moved.z);
		if (horizontal.lengthSqr() > 1.0E-6 && state.simpleBlock$heading().lengthSqr() > 0.5 && horizontal.dot(state.simpleBlock$heading()) > 0) {
			state.simpleBlock$setHeading(horizontal.normalize());
		}
	}
}
