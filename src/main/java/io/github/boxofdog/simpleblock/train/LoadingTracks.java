package io.github.boxofdog.simpleblock.train;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.feature.MinecartCushions;

/**
 * M06 Loading Track: a freight station that is useful before there is anything to load from (the
 * skip and its transfers are M07).
 *
 * <ul>
 *   <li><b>Docking:</b> a freight cart (a plain minecart without a cushion: cargo, empty, or with a
 *       rod) that rolls onto a loading track stops in the middle of it and docks; its front (where its
 *       folding gate is) turns to the track's facing, whichever way it came. Its whole train is held
 *       ({@link Couplings}): an engine in it stops and is switched off, a cable hauling it stops (a coal
 *       winch's lever goes to NEUTRAL). Engines and cushioned carts pass over without docking.</li>
 *   <li><b>The stop:</b> docking raises the rail's hinged stop behind the cart; release (or the cart
 *       going away) folds it ({@link LoadingTrackBlock#RAISED}).</li>
 *   <li><b>Release:</b> use the rail (with an empty hand; the raised stop behind the cart can be clicked
 *       from behind it) to let the train go. Nothing starts it: start the engine, or set the winch, again. A released train's
 *       carts do not dock at that track again until each has left it.</li>
 *   <li><b>Front gate</b> (drawn by the client): every freight cart has one, at the end that faces the
 *       track's way while docked. Its front is kept on the same end of the cart as it runs round
 *       curves and back. The gate stays closed in M06; opening it onto a skip is M07.</li>
 * </ul>
 * Dock, release and front are saved with the cart ({@code mixin/train/AbstractMinecartMixin}).
 */
public final class LoadingTracks {
	/** A released cart docks again at the same track only once it is this far away (or has left it). */
	private static final double RELEASE_CLEAR = 16.0;

	/** The freight state of a minecart (on every minecart; used only by freight carts). */
	public interface Freight {
		/** The way the cart's front (its gate) points, or null before it has any. Synced to clients. */
		@Nullable Direction simpleBlock$front();

		void simpleBlock$setFront(@Nullable Direction front);

		/** M07: whether the front gate is folded down as a ramp (into a skip). Synced, not saved. */
		boolean simpleBlock$gateOpen();

		/** M07: folds the gate down for {@code ticks} more ticks (0 closes it now); a skip renews it each tick while it tips the cart. */
		void simpleBlock$holdGateOpen(int ticks);

		/** The track this cart was last released from (it does not dock there until it has left it), or null. */
		@Nullable BlockPos simpleBlock$released();

		void simpleBlock$setReleased(@Nullable BlockPos released);

		/** Not saved: whether it has been on its release track since (so leaving it clears the release). */
		boolean simpleBlock$onReleased();

		void simpleBlock$setOnReleased(boolean on);

		/** Not saved: the rail block it was on last tick, and that rail's shape (for keeping the front through curves). */
		@Nullable BlockPos simpleBlock$lastRail();

		@Nullable RailShape simpleBlock$lastShape();

		void simpleBlock$setLastRail(@Nullable BlockPos pos, @Nullable RailShape shape);
	}

	private LoadingTracks() {
	}

	public static Freight freight(AbstractMinecart cart) {
		return (Freight) cart;
	}

	/** Whether {@code cart} is a freight cart: a plain minecart without a cushion. */
	public static boolean isFreight(AbstractMinecart cart) {
		return cart instanceof Minecart minecart && !MinecartCushions.isFitted(minecart);
	}

	private static Component message(String key, String fallback) {
		return Component.translatableWithFallback("simple_block.loading_track." + key, fallback);
	}

	// ---- Every freight cart's tick ----

	/** Server, at the end of a freight cart's own tick: keeps its front, docks it at a loading track. */
	public static void tick(AbstractMinecart cart) {
		if (!(cart.level() instanceof ServerLevel level) || !isFreight(cart)) {
			return;
		}
		Freight freight = freight(cart);
		BlockPos pos = cart.getCurrentBlockPosOrRailBelow();
		BlockState state = level.getBlockState(pos);
		followFront(cart, freight, pos, state);

		Couplings.Coupled coupled = Couplings.coupled(cart);
		BlockPos dock = coupled.simpleBlock$dock();
		if (dock != null) {
			if (LoadingTrackBlock.front(level.getBlockState(dock)) == null) {
				// The track is gone: nothing holds the train any more.
				coupled.simpleBlock$setDock(null);
			} else {
				stopEngines(cart);
			}
			return;
		}
		BlockPos released = freight.simpleBlock$released();
		if (released != null) {
			if (released.equals(pos)) {
				freight.simpleBlock$setOnReleased(true);
				return;
			}
			if (!freight.simpleBlock$onReleased() && released.distSqr(pos) < RELEASE_CLEAR * RELEASE_CLEAR) {
				return;
			}
			freight.simpleBlock$setReleased(null);
			freight.simpleBlock$setOnReleased(false);
		}
		Direction front = LoadingTrackBlock.front(state);
		if (front != null) {
			dock(level, cart, pos, front);
		}
	}

	private static void dock(ServerLevel level, AbstractMinecart cart, BlockPos pos, Direction front) {
		Couplings.coupled(cart).simpleBlock$setDock(pos.immutable());
		cart.setPos(pos.getX() + 0.5, cart.getY(), pos.getZ() + 0.5);
		cart.setDeltaMovement(Vec3.ZERO);
		freight(cart).simpleBlock$setFront(front);
		stopEngines(cart);
		Couplings.noteActive(cart);
		setStop(level, pos, true);
		level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.7F);
	}

	/** How often a raised stop checks that its cart is still docked. */
	private static final int STOP_CHECK_TICKS = 20;

	/** Raises or folds the rail's stop (a block state, so it is saved and seen by every client). */
	static void setStop(ServerLevel level, BlockPos pos, boolean raised) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof LoadingTrackBlock && state.getValue(LoadingTrackBlock.RAISED) != raised) {
			level.setBlock(pos, state.setValue(LoadingTrackBlock.RAISED, raised), Block.UPDATE_ALL);
		}
		if (raised) {
			level.scheduleTick(pos, state.getBlock(), STOP_CHECK_TICKS);
		}
	}

	/** A raised stop's check: folds it if no cart is docked here any more, else checks again later. */
	static void checkStop(ServerLevel level, BlockPos pos, BlockState state) {
		boolean held = !level.getEntitiesOfClass(AbstractMinecart.class, new AABB(pos).inflate(1.0),
			cart -> pos.equals(Couplings.coupled(cart).simpleBlock$dock())).isEmpty();
		if (held || !level.isPositionEntityTicking(pos)) {
			// Still docked, or its cart is not loaded to look at: keep it up and look again later.
			level.scheduleTick(pos, state.getBlock(), STOP_CHECK_TICKS);
		} else {
			level.setBlock(pos, state.setValue(LoadingTrackBlock.RAISED, false), Block.UPDATE_ALL);
		}
	}

	/** Every engine in a docked train is switched off and stopped: it must be started again by hand. */
	static void stopEngines(AbstractMinecart cart) {
		for (AbstractMinecart car : Couplings.consist(cart)) {
			if (car instanceof MinecartFurnace engine) {
				CobblestoneEngine.Engine state = CobblestoneEngine.engine(engine);
				state.simpleBlock$setRunning(false);
				state.simpleBlock$setSpeed(0.0);
				state.simpleBlock$setBurning(false);
			}
		}
	}

	// ---- Release ----

	/** A use of a loading track: lets go of the train docked there. */
	static InteractionResult release(Level level, BlockPos pos, Player player) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		List<AbstractMinecart> docked = serverLevel.getEntitiesOfClass(AbstractMinecart.class, new AABB(pos).inflate(1.0),
			cart -> pos.equals(Couplings.coupled(cart).simpleBlock$dock()));
		if (docked.isEmpty()) {
			serverPlayer.sendOverlayMessage(message("empty", "No train is held here"));
			setStop(serverLevel, pos, false);
			return InteractionResult.SUCCESS;
		}
		for (AbstractMinecart cart : docked) {
			releaseTrain(cart, pos);
		}
		setStop(serverLevel, pos, false);
		level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.9F);
		serverPlayer.sendOverlayMessage(message("released", "Released: start the engine or the winch to move off"));
		return InteractionResult.SUCCESS;
	}

	/** Lets go of every car of {@code cart}'s train; none docks at the track it is on, or at {@code track}, until it has left it. */
	public static void releaseTrain(AbstractMinecart cart, BlockPos track) {
		for (AbstractMinecart car : Couplings.consist(cart)) {
			Couplings.Coupled coupled = Couplings.coupled(car);
			BlockPos from = coupled.simpleBlock$dock() != null ? coupled.simpleBlock$dock() : track;
			if (coupled.simpleBlock$dock() != null && car.level() instanceof ServerLevel level) {
				setStop(level, coupled.simpleBlock$dock(), false);
			}
			coupled.simpleBlock$setDock(null);
			Freight freight = freight(car);
			freight.simpleBlock$setReleased(from.immutable());
			freight.simpleBlock$setOnReleased(from.equals(car.getCurrentBlockPosOrRailBelow()));
		}
	}

	// ---- The front through curves ----

	/**
	 * Keeps the cart's front on the same end of the cart as it runs: on a straight rail it stays along
	 * the rail; through a curve it turns with the cart (a curve joining ends {@code a} and {@code b}
	 * turns a front along {@code a} to one along {@code b} and back).
	 */
	private static void followFront(AbstractMinecart cart, Freight freight, BlockPos pos, BlockState state) {
		RailShape shape = state.getBlock() instanceof BaseRailBlock rail ? state.getValue(rail.getShapeProperty()) : null;
		BlockPos last = freight.simpleBlock$lastRail();
		if (shape == null || pos.equals(last)) {
			return;
		}
		RailShape lastShape = freight.simpleBlock$lastShape();
		freight.simpleBlock$setLastRail(pos.immutable(), shape);
		Direction front = freight.simpleBlock$front();
		if (front == null) {
			freight.simpleBlock$setFront(Direction.fromYRot(cart.getYRot()));
			return;
		}
		Direction[] ends = ends(shape);
		if (ends != null && ends[0].getAxis() != ends[1].getAxis()) {
			// Into a curve: turn with it.
			front = turn(front, ends);
		} else if (ends != null && front.getAxis() != ends[0].getAxis()) {
			// Out of a curve onto a straight at right angles to the front: the curve turns it back.
			Direction[] lastEnds = lastShape == null ? null : ends(lastShape);
			front = lastEnds != null && lastEnds[0].getAxis() != lastEnds[1].getAxis() ? turn(front, lastEnds) : front;
			if (front.getAxis() != ends[0].getAxis()) {
				front = ends[0];
			}
		}
		freight.simpleBlock$setFront(front);
	}

	private static Direction turn(Direction front, Direction[] ends) {
		if (front.getAxis() == ends[0].getAxis()) {
			return front == ends[0] ? ends[1].getOpposite() : ends[1];
		}
		if (front.getAxis() == ends[1].getAxis()) {
			return front == ends[1] ? ends[0].getOpposite() : ends[0];
		}
		return front;
	}

	/** The two directions a rail shape joins (horizontally). */
	private static Direction @Nullable [] ends(RailShape shape) {
		return switch (shape) {
			case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Direction[] {Direction.NORTH, Direction.SOUTH};
			case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> new Direction[] {Direction.EAST, Direction.WEST};
			case SOUTH_EAST -> new Direction[] {Direction.SOUTH, Direction.EAST};
			case SOUTH_WEST -> new Direction[] {Direction.SOUTH, Direction.WEST};
			case NORTH_WEST -> new Direction[] {Direction.NORTH, Direction.WEST};
			case NORTH_EAST -> new Direction[] {Direction.NORTH, Direction.EAST};
		};
	}
}
