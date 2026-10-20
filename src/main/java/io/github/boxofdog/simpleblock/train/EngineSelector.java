package io.github.boxofdog.simpleblock.train;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.feature.MinecartCushions;

/**
 * M15 the Cobblestone Engine's rear selector lever: three detents, THROUGH (left), NEUTRAL (centre) and
 * SERVICE (right). It governs freight service at Loading Rails ({@link FreightStations}), never the
 * engine's driving.
 *
 * <ul>
 *   <li><b>From the seat:</b> Space, once per press, steps THROUGH → NEUTRAL → SERVICE → THROUGH, for a
 *       rider in a cushioned cart coupled straight to the engine, or in the only cushioned cart of the
 *       engine's train. A rider who could mean more than one engine moves none.</li>
 *   <li><b>By hand:</b> hitting the lever steps it one detent left (towards THROUGH), using it one
 *       detent right (towards SERVICE), within ordinary reach; the ends stay at the ends.</li>
 *   <li><b>Changes:</b> leaving SERVICE stops every transfer of the train at once; THROUGH also lets go of
 *       the station hold. Choosing SERVICE is a new request for service: the wagons choose afresh, after
 *       {@link #ARM_TICKS} ticks (so a quick press on through to THROUGH moves nothing).</li>
 * </ul>
 */
public final class EngineSelector {
	/** Ticks after SERVICE is chosen before a transfer may start. */
	public static final int ARM_TICKS = 20;
	/**
	 * The lever's pivot in the engine's model frame (pixels; x runs along the cart from the smokestack to
	 * the firebox door at the back, y up from the rail, z across). It stands on the back of the cart, its
	 * handle above the end wall.
	 */
	public static final double LEVER_X = 16.5;
	public static final double LEVER_Y = 8.0;
	public static final double LEVER_Z = 12.5;
	/** Where on the cart (model frame, blocks from its middle) a hit or use works the lever: the back end, round the lever. */
	private static final double ZONE_X_MIN = 0.3;
	private static final double ZONE_Y_MIN = 0.15;
	private static final double ZONE_HALF_WIDTH = 0.25;

	public enum Mode {
		THROUGH, NEUTRAL, SERVICE;

		public static Mode of(int index) {
			return index >= 0 && index < values().length ? values()[index] : THROUGH;
		}

		Mode cycled() {
			return values()[(this.ordinal() + 1) % values().length];
		}

		Mode left() {
			return values()[Math.max(0, this.ordinal() - 1)];
		}

		Mode right() {
			return values()[Math.min(values().length - 1, this.ordinal() + 1)];
		}
	}

	/** Whether each rider held Space last tick (one step per press). */
	private static final Map<UUID, Boolean> JUMPING = new HashMap<>();

	private EngineSelector() {
	}

	public static Mode mode(MinecartFurnace engine) {
		return CobblestoneEngine.engine(engine).simpleBlock$selector();
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				boolean jump = player.getLastClientInput().jump();
				boolean was = Boolean.TRUE.equals(JUMPING.put(player.getUUID(), jump));
				if (jump && !was && player.getVehicle() instanceof Minecart seat && MinecartCushions.isFitted(seat)) {
					MinecartFurnace engine = seatEngine(seat);
					if (engine != null) {
						set(engine, mode(engine).cycled());
					}
				}
			}
		});
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (entity instanceof MinecartFurnace engine && !player.isSpectator() && aimsAtLever(player, engine)) {
				if (!level.isClientSide()) {
					set(engine, mode(engine).left());
				}
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}

	/**
	 * The engine a rider in {@code seat} works with Space: the one engine coupled straight to the seat, or
	 * the one engine of the train if the seat is its only cushioned cart. Null if none, or if it is not clear which.
	 */
	public static @Nullable MinecartFurnace seatEngine(Minecart seat) {
		if (!(seat.level() instanceof ServerLevel level)) {
			return null;
		}
		MinecartFurnace adjacent = null;
		int adjacentEngines = 0;
		for (int slot = 0; slot < 2; slot++) {
			if (Couplings.resolve(level, Couplings.coupled(seat).simpleBlock$coupling(slot)) instanceof MinecartFurnace engine) {
				adjacent = engine;
				adjacentEngines++;
			}
		}
		if (adjacentEngines == 1) {
			return adjacent;
		}
		if (adjacentEngines > 1) {
			return null;
		}
		List<AbstractMinecart> train = Couplings.consist(seat);
		MinecartFurnace only = null;
		int engines = 0;
		int seats = 0;
		for (AbstractMinecart car : train) {
			if (car instanceof MinecartFurnace engine) {
				only = engine;
				engines++;
			} else if (car instanceof Minecart minecart && MinecartCushions.isFitted(minecart)) {
				seats++;
			}
		}
		return engines == 1 && seats == 1 ? only : null;
	}

	/** A use of an engine: on the lever, steps it right (towards SERVICE). Null if the use is not on the lever. */
	static @Nullable InteractionResult use(MinecartFurnace engine, Player player, Vec3 location) {
		if (!onLever(engine, engine.position().add(location))) {
			return null;
		}
		if (!engine.level().isClientSide()) {
			set(engine, mode(engine).right());
		}
		return InteractionResult.SUCCESS;
	}

	/** {@code world} in the engine model's frame, in blocks from the cart's middle (the renderer turns the model by 180 - yaw about y). */
	public static Vec3 toModel(AbstractMinecart engine, Vec3 world) {
		double yaw = Math.toRadians(engine.getYRot());
		Vec3 d = world.subtract(engine.position());
		// The model's +x is (-cos, 0, -sin) of the yaw in the world, its +z (sin, 0, -cos).
		double x = -d.x * Math.cos(yaw) - d.z * Math.sin(yaw);
		double z = d.x * Math.sin(yaw) - d.z * Math.cos(yaw);
		return new Vec3(x, d.y, z);
	}

	/** The world position of a point of the engine's model frame (pixels). */
	public static Vec3 fromModel(AbstractMinecart engine, double px, double py, double pz) {
		double yaw = Math.toRadians(engine.getYRot());
		double x = px / 16.0 - 0.5;
		double z = pz / 16.0 - 0.5;
		return engine.position().add(-x * Math.cos(yaw) + z * Math.sin(yaw), py / 16.0, -x * Math.sin(yaw) - z * Math.cos(yaw));
	}

	/** Whether a point on the engine (in the world) is on its lever: the back end of the cart, round the lever. */
	static boolean onLever(AbstractMinecart engine, Vec3 world) {
		Vec3 local = toModel(engine, world);
		return local.x >= ZONE_X_MIN && local.y >= ZONE_Y_MIN && Math.abs(local.z - (LEVER_Z / 16.0 - 0.5)) <= ZONE_HALF_WIDTH;
	}

	/** Whether the player's look, within their reach, meets the engine on its lever. */
	static boolean aimsAtLever(Player player, MinecartFurnace engine) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.entityInteractionRange()));
		return engine.getBoundingBox().clip(eye, end).map(hit -> onLever(engine, hit)).orElse(false);
	}

	/** Moves the lever to {@code mode}, and does what leaving and entering positions mean. */
	public static void set(MinecartFurnace engine, Mode mode) {
		CobblestoneEngine.Engine state = CobblestoneEngine.engine(engine);
		Mode was = state.simpleBlock$selector();
		if (was == mode || !(engine.level() instanceof ServerLevel level)) {
			return;
		}
		state.simpleBlock$setSelector(mode);
		level.playSound(null, engine.getX(), engine.getY(), engine.getZ(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.5F,
			mode == Mode.SERVICE ? 0.7F : mode == Mode.NEUTRAL ? 0.6F : 0.5F);
		if (was == Mode.SERVICE) {
			FreightStations.cancel(level, engine);
		}
		if (mode == Mode.THROUGH) {
			FreightStations.release(level, engine);
		} else if (mode == Mode.SERVICE) {
			state.simpleBlock$setArmedAt(level.getGameTime() + ARM_TICKS);
			state.simpleBlock$setServiceBegan(false);
			FreightStations.requestService(level, engine);
		}
	}
}
