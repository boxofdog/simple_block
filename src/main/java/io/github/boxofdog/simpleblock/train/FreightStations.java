package io.github.boxofdog.simpleblock.train;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.cable.ConnectingRods;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.skip.SkipStations;

/**
 * M15 multi-wagon freight stations for Cobblestone Engine trains (a train with an engine, not hauled by a
 * cable: cable trains keep the single-cart Loading Rail rules of {@link LoadingTracks}).
 *
 * <ul>
 *   <li><b>A station</b> is a straight, level run of Loading Rails {@value #SPACING} track blocks apart,
 *       ordinary rail between. The forward-most (terminal) Loading Rail, the one with no Loading Rail
 *       {@value #SPACING} blocks further on in the train's way, is where the train stops: its leading car
 *       on the terminal rail and every car behind it pulled to a berth {@value #SPACING} blocks behind the
 *       one in front (couplings stretched from {@value Couplings#REST} to {@value #SPACING} blocks while
 *       docked). A freight wagon on a Loading Rail berth is docked there; engines, seats and wagons past
 *       the end of the run stand in the formation and do nothing.</li>
 *   <li><b>Docking</b> happens only with the selector at SERVICE: on arrival, if some wagon has a transfer
 *       it could start; or by hand, choosing SERVICE with the train standing over a station (then
 *       whatever its hoppers' lids say, ready for buttons and levers). A station that cannot be lined up
 *       (a curve, a slope, missing track, a cart in the way, an unloaded chunk) is passed.</li>
 *   <li><b>Each wagon</b>, after the arming delay, loads if an open hopper over it offers cargo it takes,
 *       else unloads if an open receiving hopper beside it takes its cargo, else does nothing; and keeps
 *       to that way for the visit. Loading ends at 256 or when the hopper has given nothing for
 *       {@value #GRACE_TICKS} ticks; unloading ends empty or when the hopper has taken nothing for as long.
 *       When every transfer that began has ended, the selector returns itself to NEUTRAL.</li>
 *   <li><b>Leaving SERVICE</b> stops every transfer at once (none resumes until SERVICE is chosen
 *       again); THROUGH lets the train go (the couplings ease back to their own spacing as it moves).</li>
 * </ul>
 */
public final class FreightStations {
	/** Track blocks between a station's Loading Rails, and between docked cars. */
	public static final int SPACING = 2;
	/** Ticks a transfer may go without moving an item before it is over. */
	public static final int GRACE_TICKS = 24;
	/** How far (blocks) SERVICE chosen by hand may move any car of a standing train to line it up. */
	public static final double HAND_ALIGN = 1.6;

	/** A wagon's transfer at a station. */
	public enum Service {
		/** Not begun: chooses its way when service starts. */
		NONE,
		LOAD,
		UNLOAD,
		/** Over (finished, or stopped): nothing more until service is asked for again. */
		DONE;

		public static Service of(int index) {
			return index >= 0 && index < values().length ? values()[index] : NONE;
		}

		boolean active() {
			return this == LOAD || this == UNLOAD;
		}
	}

	/** Each wagon's hoppers' buttons, pressed last tick (a transfer starts on the press). */
	private static final Map<BlockPos, Boolean> PRESSED = new HashMap<>();

	private FreightStations() {
	}

	// ---- Which trains ----

	/** The engine that runs {@code cart}'s train at stations: its first engine (lowest id), or null for a train without one, or one a cable hauls. */
	public static @Nullable MinecartFurnace leadEngine(List<AbstractMinecart> train) {
		MinecartFurnace lead = null;
		for (AbstractMinecart car : train) {
			if (car instanceof Minecart minecart && ConnectingRods.isGripping(minecart)) {
				return null;
			}
			if (car instanceof MinecartFurnace engine && (lead == null || engine.getId() < lead.getId())) {
				lead = engine;
			}
		}
		return lead;
	}

	/** Whether {@code cart}'s train is an engine train served by stations (and not by single-cart Loading Rail docking). */
	public static boolean stationTrain(AbstractMinecart cart) {
		return leadEngine(Couplings.consist(cart)) != null;
	}

	/** The train's selector: SERVICE only if every engine says so; THROUGH if any does; else NEUTRAL. */
	public static EngineSelector.Mode mode(List<AbstractMinecart> train) {
		boolean allService = true;
		boolean any = false;
		for (AbstractMinecart car : train) {
			if (car instanceof MinecartFurnace engine) {
				any = true;
				EngineSelector.Mode mode = EngineSelector.mode(engine);
				if (mode == EngineSelector.Mode.THROUGH) {
					return EngineSelector.Mode.THROUGH;
				}
				allService &= mode == EngineSelector.Mode.SERVICE;
			}
		}
		return any && allService ? EngineSelector.Mode.SERVICE : EngineSelector.Mode.NEUTRAL;
	}

	/** Whether {@code wagon}'s train is in SERVICE and past the arming delay of every engine. */
	static boolean armed(ServerLevel level, AbstractMinecart wagon) {
		List<AbstractMinecart> train = Couplings.consist(wagon);
		if (mode(train) != EngineSelector.Mode.SERVICE) {
			return false;
		}
		for (AbstractMinecart car : train) {
			if (car instanceof MinecartFurnace engine && level.getGameTime() < CobblestoneEngine.engine(engine).simpleBlock$armedAt()) {
				return false;
			}
		}
		return true;
	}

	// ---- Every engine tick ----

	/**
	 * Server, each tick of an engine: its train's station work, run by the lead engine. Returns whether the
	 * train is docked (it is then held).
	 */
	static boolean tick(ServerLevel level, MinecartFurnace engine) {
		List<AbstractMinecart> train = Couplings.consist(engine);
		MinecartFurnace lead = leadEngine(train);
		if (lead == null) {
			return Couplings.docked(engine);
		}
		boolean docked = false;
		for (AbstractMinecart car : train) {
			docked |= Couplings.coupled(car).simpleBlock$dock() != null;
		}
		if (lead != engine) {
			return docked;
		}
		if (docked) {
			serve(level, lead, train);
			return true;
		}
		if (mode(train) == EngineSelector.Mode.SERVICE) {
			return arrive(level, lead, train);
		}
		return false;
	}

	// ---- Arriving ----

	/** A moving train in SERVICE: docks if its leading car has reached the terminal rail of a station with work for it. */
	private static boolean arrive(ServerLevel level, MinecartFurnace lead, List<AbstractMinecart> train) {
		CobblestoneEngine.Engine state = CobblestoneEngine.engine(lead);
		Vec3 heading = state.simpleBlock$heading();
		if (heading.lengthSqr() < 0.5) {
			return false;
		}
		Direction way = Direction.getApproximateNearest(heading.x, 0.0, heading.z);
		List<AbstractMinecart> order = inOrder(train, way);
		AbstractMinecart front = order.getFirst();
		BlockPos rail = front.getCurrentBlockPosOrRailBelow();
		BlockPos passed = state.simpleBlock$passed();
		if (passed != null && !passed.equals(rail)) {
			state.simpleBlock$setPassed(null);
		}
		if (!terminal(level, rail, way) || rail.equals(passed)) {
			return false;
		}
		double along = front.position().subtract(Vec3.atBottomCenterOf(rail)).dot(Vec3.atLowerCornerOf(way.getUnitVec3i()));
		if (along < -0.05) {
			return false;
		}
		Plan plan = Plan.of(level, rail, way, order);
		if (plan == null || !plan.hasWork(level)) {
			state.simpleBlock$setPassed(rail.immutable());
			return false;
		}
		plan.dock(level);
		state.simpleBlock$setPassed(rail.immutable());
		return true;
	}

	/** Whether the Loading Rail at {@code rail} ends a station in {@code way}: straight along it, and no Loading Rail {@value #SPACING} blocks on. */
	static boolean terminal(ServerLevel level, BlockPos rail, Direction way) {
		Direction front = LoadingTrackBlock.front(level.getBlockState(rail));
		return front != null && front.getAxis() == way.getAxis() && LoadingTrackBlock.front(level.getBlockState(rail.relative(way, SPACING))) == null;
	}

	/** The cars of a train from the front (furthest in {@code way}) back. */
	static List<AbstractMinecart> inOrder(List<AbstractMinecart> train, Direction way) {
		Vec3 unit = Vec3.atLowerCornerOf(way.getUnitVec3i());
		List<AbstractMinecart> order = new ArrayList<>(train);
		order.sort(Comparator.comparingDouble((AbstractMinecart car) -> car.position().dot(unit)).reversed());
		return order;
	}

	/** A train lined up on a station: each car's berth, and the Loading Rail under it, if it has one. */
	record Plan(List<AbstractMinecart> cars, List<BlockPos> berths, List<Boolean> rails, double y) {
		/**
		 * The berths of {@code cars} (front first) behind the terminal rail {@code terminal} in {@code way},
		 * or null if they cannot be had: track missing, bent or sloping, a chunk not ticking, another cart in the way.
		 */
		static @Nullable Plan of(ServerLevel level, BlockPos terminal, Direction way, List<AbstractMinecart> cars) {
			Direction back = way.getOpposite();
			List<BlockPos> berths = new ArrayList<>();
			List<Boolean> rails = new ArrayList<>();
			boolean station = true;
			int length = SPACING * (cars.size() - 1);
			for (int step = 0; step <= length; step++) {
				BlockPos at = terminal.relative(back, step);
				if (!level.isPositionEntityTicking(at) || !straightRail(level.getBlockState(at), way.getAxis())) {
					return null;
				}
				if (step % SPACING == 0) {
					berths.add(at.immutable());
					station &= LoadingTrackBlock.front(level.getBlockState(at)) != null;
					rails.add(station);
				}
			}
			double y = cars.getFirst().getY();
			for (int i = 0; i < cars.size(); i++) {
				AbstractMinecart car = cars.get(i);
				Vec3 spot = new Vec3(berths.get(i).getX() + 0.5, y, berths.get(i).getZ() + 0.5);
				AABB box = car.getBoundingBox().move(spot.subtract(car.position())).deflate(0.05);
				if (!level.getEntitiesOfClass(AbstractMinecart.class, box, other -> other.isAlive() && !cars.contains(other)).isEmpty()) {
					return null;
				}
			}
			return new Plan(List.copyOf(cars), berths, rails, y);
		}

		/** Whether some freight wagon on a Loading Rail berth has a transfer it could start (its hoppers' lids as they are). */
		boolean hasWork(ServerLevel level) {
			for (int i = 0; i < this.cars.size(); i++) {
				if (this.rails.get(i) && this.cars.get(i) instanceof Minecart wagon && LoadingTracks.isFreight(wagon)
					&& choose(level, wagon, this.berths.get(i), null) != Service.NONE) {
					return true;
				}
			}
			return false;
		}

		/** Pulls every car onto its berth, docks the wagons on Loading Rails, and holds the train. */
		void dock(ServerLevel level) {
			boolean any = false;
			for (int i = 0; i < this.cars.size(); i++) {
				AbstractMinecart car = this.cars.get(i);
				BlockPos berth = this.berths.get(i);
				Vec3 spot = new Vec3(berth.getX() + 0.5, this.y, berth.getZ() + 0.5);
				car.setPos(spot);
				car.setDeltaMovement(Vec3.ZERO);
				Couplings.coupled(car).simpleBlock$setHoldPos(spot);
				if (this.rails.get(i) && car instanceof Minecart wagon && LoadingTracks.isFreight(wagon)) {
					Couplings.coupled(wagon).simpleBlock$setDock(berth);
					LoadingTracks.Freight freight = LoadingTracks.freight(wagon);
					freight.simpleBlock$setFront(LoadingTrackBlock.front(level.getBlockState(berth)));
					freight.simpleBlock$setService(Service.NONE);
					freight.simpleBlock$setPort(null);
					LoadingTracks.setStop(level, berth, true);
					any = true;
				}
			}
			if (any) {
				LoadingTracks.stopEngines(this.cars.getFirst());
				Couplings.noteActive(this.cars.getFirst());
				level.playSound(null, this.berths.getFirst(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.7F);
			}
		}
	}

	/** Whether {@code state} is a rail, straight and level along {@code axis} (a Loading Rail is). */
	private static boolean straightRail(BlockState state, Direction.Axis axis) {
		if (!(state.getBlock() instanceof BaseRailBlock rail)) {
			return false;
		}
		RailShape shape = state.getValue(rail.getShapeProperty());
		return axis == Direction.Axis.X ? shape == RailShape.EAST_WEST : shape == RailShape.NORTH_SOUTH;
	}

	// ---- The selector's changes ----

	/** SERVICE chosen: a new request. A docked train's wagons choose afresh; a train standing over a station lines up on it. */
	static void requestService(ServerLevel level, MinecartFurnace engine) {
		List<AbstractMinecart> train = Couplings.consist(engine);
		MinecartFurnace lead = leadEngine(train);
		if (lead == null) {
			return;
		}
		for (AbstractMinecart car : train) {
			if (Couplings.coupled(car).simpleBlock$dock() != null) {
				LoadingTracks.freight(car).simpleBlock$setService(Service.NONE);
				LoadingTracks.freight(car).simpleBlock$setPort(null);
			}
		}
		if (CobblestoneEngine.engine(lead).simpleBlock$speed() > 1.0E-3 || mode(train) != EngineSelector.Mode.SERVICE) {
			return;
		}
		// A standing train lines up on the station under it (also one whose wagons were docked one by one
		// before they were coupled to the engine).
		Plan plan = handPlan(level, lead, train);
		if (plan != null) {
			for (AbstractMinecart car : train) {
				BlockPos old = Couplings.coupled(car).simpleBlock$dock();
				if (old != null && !plan.berths().contains(old)) {
					LoadingTracks.setStop(level, old, false);
				}
				Couplings.coupled(car).simpleBlock$setDock(null);
			}
			plan.dock(level);
		}
	}

	/**
	 * The station a standing train is over, lined up for SERVICE chosen by hand: of the line-ups that move no
	 * car more than {@value #HAND_ALIGN} blocks (no shoving a train along the track), the one that moves the
	 * cars least. Null if there is none.
	 */
	private static @Nullable Plan handPlan(ServerLevel level, MinecartFurnace lead, List<AbstractMinecart> train) {
		Plan best = null;
		double bestShift = Double.MAX_VALUE;
		for (Direction way : Direction.Plane.HORIZONTAL) {
			List<AbstractMinecart> order = inOrder(train, way);
			for (AbstractMinecart car : order) {
				BlockPos rail = car.getCurrentBlockPosOrRailBelow();
				Direction front = LoadingTrackBlock.front(level.getBlockState(rail));
				if (front == null || front.getAxis() != way.getAxis()) {
					continue;
				}
				BlockPos terminal = rail;
				while (LoadingTrackBlock.front(level.getBlockState(terminal.relative(way, SPACING))) != null) {
					terminal = terminal.relative(way, SPACING);
				}
				Plan plan = Plan.of(level, terminal, way, order);
				if (plan == null) {
					continue;
				}
				double shift = 0.0;
				for (int i = 0; i < order.size(); i++) {
					BlockPos berth = plan.berths().get(i);
					shift = Math.max(shift, Math.hypot(order.get(i).getX() - (berth.getX() + 0.5), order.get(i).getZ() - (berth.getZ() + 0.5)));
				}
				if (shift <= HAND_ALIGN && shift < bestShift) {
					best = plan;
					bestShift = shift;
				}
			}
		}
		return best;
	}

	/** SERVICE left: every transfer of the train stops where it is; buttons' openings close. Nothing resumes until SERVICE is chosen again. */
	static void cancel(ServerLevel level, MinecartFurnace engine) {
		for (AbstractMinecart car : Couplings.consist(engine)) {
			LoadingTracks.Freight freight = LoadingTracks.freight(car);
			if (freight.simpleBlock$service() != Service.NONE || freight.simpleBlock$port() != null) {
				freight.simpleBlock$setService(Service.DONE);
				freight.simpleBlock$setPort(null);
			}
			if (car instanceof Minecart wagon) {
				FreightHoppers.stopDischarge(wagon);
			}
		}
		CobblestoneEngine.engine(engine).simpleBlock$setServiceBegan(false);
	}

	/** THROUGH: the train is let go. Its wagons will not dock at these rails again until they have left them. */
	static void release(ServerLevel level, MinecartFurnace engine) {
		List<AbstractMinecart> train = Couplings.consist(engine);
		boolean any = false;
		for (AbstractMinecart car : train) {
			Couplings.Coupled coupled = Couplings.coupled(car);
			LoadingTracks.Freight freight = LoadingTracks.freight(car);
			freight.simpleBlock$setService(Service.NONE);
			freight.simpleBlock$setPort(null);
			BlockPos dock = coupled.simpleBlock$dock();
			coupled.simpleBlock$setHoldPos(null);
			if (dock != null) {
				LoadingTracks.setStop(level, dock, false);
				coupled.simpleBlock$setDock(null);
				freight.simpleBlock$setReleased(dock);
				freight.simpleBlock$setOnReleased(dock.equals(car.getCurrentBlockPosOrRailBelow()));
				any = true;
			}
		}
		if (any) {
			level.playSound(null, engine.getX(), engine.getY(), engine.getZ(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.9F);
		}
	}

	// ---- Docked: the wagons' transfers ----

	/** What {@code wagon} at {@code rail} would do now: LOAD, UNLOAD, or NONE (nothing possible). {@code port} is its button-opened hopper. */
	static Service choose(ServerLevel level, Minecart wagon, BlockPos rail, @Nullable BlockPos port) {
		if (SkipStations.working(level, wagon)) {
			return Service.NONE;
		}
		BlockPos hopper = FreightPorts.hopperAbove(level, rail);
		if (hopper != null && FreightPorts.open(level, hopper, port) && level.getBlockEntity(hopper) instanceof HopperBlockEntity supply
			&& offers(supply, wagon)) {
			return Service.LOAD;
		}
		MinecartCargo.Cart cargo = MinecartCargo.cart(wagon);
		if (cargo.simpleBlock$count() > 0) {
			for (Direction side : FreightHoppers.order(level, rail)) {
				HopperBlockEntity receiver = FreightHoppers.receiver(level, rail, side, wagon);
				if (receiver != null && FreightPorts.open(level, receiver.getBlockPos(), port) && FreightHoppers.takes(receiver, cargo.simpleBlock$cargo())) {
					return Service.UNLOAD;
				}
			}
		}
		return Service.NONE;
	}

	/** Whether the hopper holds something the wagon would take (of its kind, with room). */
	private static boolean offers(HopperBlockEntity hopper, Minecart wagon) {
		MinecartCargo.Cart cargo = MinecartCargo.cart(wagon);
		for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
			ItemStack stack = hopper.getItem(slot);
			if (!stack.isEmpty() && MinecartCargo.room(cargo, stack.copyWithCount(1)) >= 1) {
				return true;
			}
		}
		return false;
	}

	/** The docked train's tick (lead engine): buttons, choosing, the grace on stalled transfers, the return to NEUTRAL. */
	private static void serve(ServerLevel level, MinecartFurnace lead, List<AbstractMinecart> train) {
		boolean service = mode(train) == EngineSelector.Mode.SERVICE;
		boolean armed = service && armed(level, lead);
		boolean active = false;
		CobblestoneEngine.Engine engineState = CobblestoneEngine.engine(lead);
		for (AbstractMinecart car : train) {
			BlockPos rail = Couplings.coupled(car).simpleBlock$dock();
			if (rail == null || !(car instanceof Minecart wagon)) {
				continue;
			}
			LoadingTracks.Freight freight = LoadingTracks.freight(wagon);
			// A button press asks for one whole transfer through its hopper.
			for (BlockPos hopper : hoppers(level, rail, wagon)) {
				boolean pressed = FreightPorts.buttonPressed(level, hopper);
				boolean was = Boolean.TRUE.equals(PRESSED.put(hopper.immutable(), pressed));
				if (pressed && !was && armed && !freight.simpleBlock$service().active()) {
					freight.simpleBlock$setService(Service.NONE);
					freight.simpleBlock$setPort(hopper.immutable());
				}
			}
			Service now = freight.simpleBlock$service();
			if (now == Service.NONE && armed) {
				Service chosen = choose(level, wagon, rail, freight.simpleBlock$port());
				if (chosen != Service.NONE) {
					freight.simpleBlock$setService(chosen);
					engineState.simpleBlock$setServiceBegan(true);
					now = chosen;
				}
			} else if (now.active()) {
				MinecartCargo.Cart cargo = MinecartCargo.cart(wagon);
				int idle = freight.simpleBlock$idle() + 1;
				freight.simpleBlock$setIdle(idle);
				boolean finished = now == Service.LOAD ? cargo.simpleBlock$count() >= MinecartCargo.CAPACITY : cargo.simpleBlock$count() <= 0;
				if (finished || idle > GRACE_TICKS || !service) {
					freight.simpleBlock$setService(Service.DONE);
					freight.simpleBlock$setPort(null);
					FreightHoppers.stopDischarge(wagon);
					now = Service.DONE;
				}
			}
			active |= now.active();
		}
		if (service && engineState.simpleBlock$serviceBegan() && !active) {
			// Every transfer that began is over: back to NEUTRAL, the train still held.
			for (AbstractMinecart car : train) {
				if (car instanceof MinecartFurnace engine && EngineSelector.mode(engine) == EngineSelector.Mode.SERVICE) {
					CobblestoneEngine.engine(engine).simpleBlock$setSelector(EngineSelector.Mode.NEUTRAL);
					CobblestoneEngine.engine(engine).simpleBlock$setServiceBegan(false);
					level.playSound(null, engine.getX(), engine.getY(), engine.getZ(), SoundEvents.LEVER_CLICK, SoundSource.NEUTRAL, 0.5F, 0.6F);
				}
			}
		}
	}

	/** The freight hoppers of a wagon docked at {@code rail}: the one over it and those it could tip into. */
	private static List<BlockPos> hoppers(ServerLevel level, BlockPos rail, Minecart wagon) {
		List<BlockPos> found = new ArrayList<>();
		BlockPos above = FreightPorts.hopperAbove(level, rail);
		if (above != null) {
			found.add(above);
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos beside = rail.relative(side).below();
			if (level.getBlockEntity(beside) instanceof HopperBlockEntity) {
				found.add(beside);
			}
		}
		return found;
	}

	/** Whether {@code wagon} (docked) may take items from its hopper over it now: a station wagon loading, armed, through an open lid. */
	static boolean mayLoad(ServerLevel level, Minecart wagon, BlockPos hopper) {
		LoadingTracks.Freight freight = LoadingTracks.freight(wagon);
		if (!stationTrain(wagon)) {
			return FreightPorts.open(level, hopper, null);
		}
		return freight.simpleBlock$service() == Service.LOAD && armed(level, wagon) && FreightPorts.open(level, hopper, freight.simpleBlock$port());
	}

	/** Whether {@code wagon} (docked) may tip into the hopper at {@code hopper} now. */
	static boolean mayUnload(ServerLevel level, Minecart wagon, BlockPos hopper) {
		LoadingTracks.Freight freight = LoadingTracks.freight(wagon);
		if (!stationTrain(wagon)) {
			return FreightPorts.open(level, hopper, null);
		}
		return freight.simpleBlock$service() == Service.UNLOAD && armed(level, wagon) && FreightPorts.open(level, hopper, freight.simpleBlock$port());
	}

	/** An item moved for a station wagon: its transfer is alive. */
	static void moved(Minecart wagon) {
		LoadingTracks.freight(wagon).simpleBlock$setIdle(0);
	}
}
