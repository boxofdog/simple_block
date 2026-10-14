package io.github.boxofdog.simpleblock.cable;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.network.CrankPayload;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.train.TrainPhysics;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * Cable haulage: the M05 hand crank and the M06 coal winch, driving closed loops.
 *
 * <p><b>The crank</b> (owner's controls, on a drum without a furnace). Aiming at it, hold left-click to
 * <b>feed</b> the cable away from the drum, or hold right-click (use) to <b>pull</b> it towards the
 * drum. A held use key repeats every 4 ticks; a held left-click on the crank never starts breaking
 * the drum: the client sends a {@link CrankPayload} every 4 ticks instead
 * ({@code client/mixin/MultiPlayerGameModeMixin}), checked here. The crank keeps turning
 * {@value #CRANK_HOLD} ticks after each. An empty drum just spins; a drum with an open route is locked
 * (only a closed loop moves cable).
 *
 * <p><b>The coal winch</b> (M06, {@link CoalWinch}): a latched lever instead. PULL or FEED drives the
 * strand at {@code winch.powered_speed} without anyone at the drum, speeding up and slowing down by
 * {@code winch.acceleration} a tick (so PULL straight to FEED reverses smoothly); NEUTRAL stops and
 * holds at once. Coal burns one tick of its {@code winch.burn_ticks_per_coal} only on ticks when a
 * gripping cart actually moved; the time left in the coal alight is kept exactly through stops and
 * saves. Every physical stop puts the lever back to NEUTRAL, and nothing restarts by itself: a cart
 * at the end of the line, docked at a loading track, off the rails, blocked, or at the edge of the
 * loaded world; the loop opened or broken; the coal run out; or no gripping cart in reach for
 * {@value #IDLE_LIMIT} ticks.
 *
 * <p><b>The loop.</b> On a closed loop ({@link CableLoops}) the drum is a capstan: the working strand
 * moves out or back and never piles up on the spool. Each cart whose rod grips the working strand
 * (only that strand) moves with it, with the carts coupled to it; when the drive stops they are held.
 * A cart stops short of either end: within {@code cable.end_clearance} blocks of the drum's exit or the
 * return wheel, or where the cable would leave its rod's reach; except that a cart with a Loading Rail
 * it will dock at no more than {@value #STATION_APPROACH} blocks ahead goes on onto it (M07 playtest
 * patch §10), so a station under or just before the last support is reached without cable past it. Hauling uphill is slower the heavier
 * the train: the speed is divided by {@code 1 + cable.grade_load x} the mass of the gripping trains on
 * a climbing rail (masses in {@link TrainPhysics}).
 *
 * <p><b>Long lines (M06).</b> The drive is kept in the level's {@link CableLines} and run once a level
 * tick for every line, whether or not its drum is loaded; carts move only in their own ticks, so only
 * while they are in entity-ticking chunks. A cart is held (and a coal winch's lever goes to NEUTRAL)
 * when its train, or the track just ahead of any of its cars, is not in entity-ticking chunks: it never
 * runs into, or drags anything out of, the unloaded world, and the rest of a long line does not need to
 * be loaded for the part near a player to work.
 */
public final class CableDrive {
	/** Ticks a crank keeps turning after one use: a held use key repeats every 4 ticks. */
	static final int CRANK_HOLD = 6;
	/** A coal winch running with no gripping cart in reach for this long stops (lever to NEUTRAL). */
	static final int IDLE_LIMIT = 40;
	/** A coal winch whose carts are driven but do not move for this long stops: they are blocked. */
	static final int STALL_LIMIT = 10;
	/** How far ahead a Loading Rail lets a cart on past the end checks for its last stretch (M07 patch §10). */
	static final double STATION_APPROACH = 1.5;

	private CableDrive() {
	}

	public static double crankSpeed() {
		return Tuning.getDouble("cable.crank_speed");
	}

	/** Right-click on the crank: keeps it pulling the cable towards the drum. Client and server both swing the arm. */
	static InteractionResult pull(Level level, BlockPos pos, Player player) {
		crank(level, pos, player, -1);
		return InteractionResult.SUCCESS;
	}

	/**
	 * Left-click on the crank, from {@link CrankPayload}: held, keeps it feeding the cable away; on a
	 * coal winch, one click on its lever sets FEED. Only if the click is real.
	 */
	public static void feed(ServerPlayer player, BlockPos pos, Vec3 hit) {
		ServerLevel level = player.level();
		if (!level.isLoaded(pos) || !player.isWithinBlockInteractionRange(pos, 1.0) || hit.distanceToSqr(Vec3.atCenterOf(pos)) > 2.0 * 2.0) {
			return;
		}
		BlockState state = level.getBlockState(pos);
		if (state.is(CableContent.CABLE_DRUM) && CableDrumBlock.isCrank(state, pos, hit)) {
			if (level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.farFrom() != null) {
				// M07 playtest change 2: a loop's far drum; left-click sends the cart away from it.
				FarDrums.work(level, drum, player, false);
			} else if (level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && drum.powered()) {
				// A coal winch's lever: a left-click sets FEED (again: NEUTRAL).
				CoalWinch.setLever(level, pos, player, CableLines.Lever.FEED);
			} else {
				crank(level, pos, player, 1);
			}
		}
	}

	static void register() {
		PayloadTypeRegistry.serverboundPlay().register(CrankPayload.TYPE, CrankPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CrankPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> feed(player, payload.pos(), payload.hit()));
		});
		ServerTickEvents.END_LEVEL_TICK.register(CableDrive::tickLevel);
	}

	/** Keeps the crank turning: +1 feeds the working strand away from the drum, -1 pulls it towards it. A coal winch has no crank. */
	private static void crank(Level level, BlockPos pos, Player player, int direction) {
		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof CableDrumBlockEntity drum && !drum.powered()) {
			crank(serverLevel, CableLines.get(serverLevel).getOrCreate(pos), pos, player, direction, !drum.route().isEmpty());
		}
	}

	/**
	 * Keeps the crank of {@code line} turning, from a crank at {@code at}: the drum's own, or (M07 playtest
	 * change 2) its loop's far drum, whose direction is already in the home drum's terms. The last turn
	 * from either end wins.
	 */
	static void crank(ServerLevel level, CableLines.Line line, BlockPos at, Player player, int direction, boolean routed) {
		boolean starting = line.crankTicks <= 0;
		line.crankTicks = CRANK_HOLD;
		line.crankDirection = direction;
		if (starting && !line.drivable() && routed) {
			level.playSound(null, at, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.8F, 0.6F);
			CableRoutes.tell(player, CableRoutes.message("crank_locked", "Close the loop, or hang a skip on the cable end, to haul"));
		}
	}

	// ---- Every level tick: each line's drive ----

	private static void tickLevel(ServerLevel level) {
		CableLines lines = CableLines.get(level);
		for (Map.Entry<BlockPos, CableLines.Line> entry : lines.all().entrySet()) {
			if (tickLine(level, entry.getKey(), entry.getValue())) {
				lines.changed();
			}
		}
	}

	/** One tick of a line's drive, after its carts have moved. Returns whether saved state changed. */
	static boolean tickLine(ServerLevel level, BlockPos pos, CableLines.Line line) {
		int input = line.crankTicks > 0 ? line.crankDirection : 0;
		if (line.crankTicks > 0) {
			line.crankTicks--;
		}
		boolean moved = line.moved;
		line.lastMoved = moved;
		boolean gripped = line.gripped;
		String halt = line.halt;
		line.moved = false;
		line.gripped = false;
		line.halt = null;
		line.lastAlongs = line.alongs;
		line.alongs = new HashMap<>();
		if (line.blockedSign != 0 && line.lastAlongs.keySet().stream().noneMatch(line.blockedCarts::contains)) {
			// The blocked way (BUG-002) belongs to the carts that stopped there: once none of them grips, it goes.
			line.blockedSign = 0;
		}
		line.lastLoadForward = line.loadForward;
		line.lastLoadBackward = line.loadBackward;
		line.loadForward = 0.0;
		line.loadBackward = 0.0;
		boolean changed = false;
		double speed;
		if (line.powered) {
			changed = tickWinch(level, pos, line, moved, gripped, halt);
			int sign = line.lever.sign;
			if (sign == 0) {
				speed = 0.0;
			} else {
				double load = sign > 0 ? line.lastLoadForward : line.lastLoadBackward;
				double target = sign * Tuning.getDouble("winch.powered_speed") / (1.0 + load);
				double step = Tuning.getDouble("winch.acceleration");
				speed = line.speed + Math.clamp(target - line.speed, -step, step);
			}
		} else if (line.drivable()) {
			// A loop, or (M07) a skip hung on the cable end: PULL (-1) lifts it, FEED (+1) lowers it.
			double load = input > 0 ? line.lastLoadForward : line.lastLoadBackward;
			speed = input == 0 ? 0.0 : input * crankSpeed() / (1.0 + load);
		} else {
			// An empty drum just spins; one with an open route is locked.
			speed = line.points.size() < 2 ? input * crankSpeed() : 0.0;
		}
		line.speed = (float) speed;
		if (speed != 0.0 && level.getGameTime() % 5 == 0 && level.isLoaded(pos)) {
			level.playSound(null, pos, line.powered ? SoundEvents.FURNACE_FIRE_CRACKLE : SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.4F);
		}
		return changed;
	}

	/** The coal winch's lever and coal for this tick. Returns whether saved state changed. */
	private static boolean tickWinch(ServerLevel level, BlockPos pos, CableLines.Line line, boolean moved, boolean gripped, @Nullable String halt) {
		if (line.lever == CableLines.Lever.NEUTRAL) {
			line.idleTicks = 0;
			line.stallTicks = 0;
			return false;
		}
		boolean changed = false;
		if (moved) {
			line.blockedSign = 0;
		}
		// Coal burns only for ticks the load actually moved.
		if (moved && line.burn > 0) {
			line.burn--;
			changed = true;
		}
		String stop = null;
		if (!line.drivable()) {
			stop = "open";
		} else if (halt != null) {
			stop = halt;
		} else {
			line.idleTicks = gripped ? 0 : line.idleTicks + 1;
			boolean driven = gripped && Math.abs(line.speed) > 0.02;
			line.stallTicks = driven && !moved ? line.stallTicks + 1 : 0;
			if (line.idleTicks > IDLE_LIMIT) {
				stop = "idle";
			} else if (line.stallTicks > STALL_LIMIT) {
				stop = "blocked";
			}
		}
		if (stop == null && line.burn == 0) {
			if (line.coal > 0) {
				line.coal--;
				line.burn = Tuning.getInt("winch.burn_ticks_per_coal");
				changed = true;
			} else {
				stop = "no_fuel";
			}
		}
		if (stop != null) {
			CoalWinch.toNeutral(level, pos, line, stop);
			changed = true;
		}
		return changed;
	}

	// ---- Carts ----

	/**
	 * A cart whose rod grips a working strand (server, in the cart's own tick): if the strand belongs to
	 * a loop, the cart (and its train) is moved with the cable or held. Returns whether it is under a
	 * loop's control.
	 */
	static boolean driveCart(ServerLevel level, Minecart cart, CableIndex.Grip grip) {
		CableLines.Line line = CableLines.line(level, grip.drum());
		if (line == null || !line.looped || grip.direction().lengthSqr() < 1.0E-6) {
			return false;
		}
		line.gripped = true;
		Vec3 outward = grip.direction();
		double mass = Couplings.consistMass(cart);
		double gradeLoad = Tuning.getDouble("cable.grade_load") * mass;
		int grade = TrainPhysics.gradeSign(cart, outward);
		if (grade > 0) {
			line.loadForward += gradeLoad;
		} else if (grade < 0) {
			line.loadBackward += gradeLoad;
		}
		// Has the cart got on along the strand since last tick, on the speed it was given? A cart pressed
		// against something in its way (moving a little and pushed back) has not: at least a quarter of
		// the way.
		Double before = line.lastAlongs.get(cart.getId());
		line.alongs.put(cart.getId(), grip.along());
		if (before != null && Math.abs(grip.along() - before) > Math.max(0.003, 0.25 * Math.abs(line.speed))) {
			line.moved = true;
		}
		double speed = line.speed;
		if (speed == 0.0) {
			Couplings.command(cart, null);
			return true;
		}
		String stop = stop(level, cart, grip, speed);
		if (stop != null) {
			line.halt = stop;
			Couplings.command(cart, null);
		} else {
			Couplings.command(cart, outward.scale(speed));
		}
		return true;
	}

	/**
	 * Why moving {@code speed} along the strand must not happen now, or null if it may: the train is
	 * docked, off the rails, at the end of its track (M06), at an end of the line, about to leave its
	 * rod's reach, or not all in (or about to run out of) entity-ticking chunks.
	 */
	/** Height of a cart on the rail at {@code rail} at the horizontal position of {@code at} (along a slope, part way up it). */
	private static double railHeight(ServerLevel level, BlockPos rail, Vec3 at) {
		double y = rail.getY() + 0.0625;
		BlockState state = level.getBlockState(rail);
		if (state.getBlock() instanceof BaseRailBlock block) {
			double fx = Mth.clamp(at.x - rail.getX(), 0.0, 1.0);
			double fz = Mth.clamp(at.z - rail.getZ(), 0.0, 1.0);
			y += switch (state.getValue(block.getShapeProperty())) {
				case ASCENDING_EAST -> fx;
				case ASCENDING_WEST -> 1.0 - fx;
				case ASCENDING_SOUTH -> fz;
				case ASCENDING_NORTH -> 1.0 - fz;
				default -> 0.0;
			};
		}
		return y;
	}

	/** The rail at {@code pos}, or just below or above it (a slope), or null. */
	private static @Nullable BlockPos railNear(ServerLevel level, BlockPos pos) {
		for (BlockPos candidate : new BlockPos[] {pos, pos.below(), pos.above()}) {
			if (BaseRailBlock.isRail(level, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Whether a Loading Rail this cart would dock at is within {@value #STATION_APPROACH} blocks ahead
	 * ({@code forward}: a unit step in its direction of travel), on its own level of track.
	 */
	private static boolean stationAhead(ServerLevel level, Minecart cart, Vec3 forward) {
		BlockPos here = cart.getCurrentBlockPosOrRailBelow();
		for (double step = 0.5; step <= STATION_APPROACH + 1.0E-6; step += 0.5) {
			BlockPos pos = BlockPos.containing(cart.position().add(forward.scale(step))).atY(here.getY());
			if (!pos.equals(here) && LoadingTracks.willDockAt(level, cart, pos)) {
				return true;
			}
		}
		return false;
	}

	private static @Nullable String stop(ServerLevel level, Minecart cart, CableIndex.Grip grip, double speed) {
		if (Couplings.docked(cart)) {
			return "docked";
		}
		if (!TrainPhysics.onRail(cart)) {
			return "derailed";
		}
		// M07 playtest patch §10: the last stretch onto a Loading Rail it will dock at is allowed even
		// inside an end zone; docking (on entering the rail) stops it there.
		boolean station = stationAhead(level, cart, grip.direction().scale(Math.signum(speed)));
		double clearance = Tuning.getDouble("cable.end_clearance");
		double next = grip.along() + speed;
		// Never further into an end zone; a cart already in one may always move out of it.
		if (!station && (next < clearance && speed < 0.0 || next > grip.length() - clearance && speed > 0.0)) {
			return "end";
		}
		Vec3 ahead = cart.position().add(grip.direction().scale(speed + Math.signum(speed) * 0.5));
		BlockPos aheadRail = railNear(level, BlockPos.containing(ahead));
		if (aheadRail == null) {
			return "track_end";
		}
		// BUG-004: judge the reach ahead from where the cart will be on the track there (up or down a
		// climb), not from its present height.
		ahead = new Vec3(ahead.x, railHeight(level, aheadRail, ahead), ahead.z);
		CableIndex.Grip still = CableIndex.grip(level, ahead, ConnectingRods.TOLERANCE, ConnectingRods.reachMin(), ConnectingRods.reachMax());
		if (!station && (still == null || !still.drum().equals(grip.drum()))) {
			return "end";
		}
		if (!Couplings.allTicking(level, cart, grip.direction().scale(speed + Math.signum(speed) * 1.0))) {
			return "unloaded";
		}
		return null;
	}
}
