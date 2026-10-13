package io.github.boxofdog.simpleblock.skip;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M07: a skip and a freight cart exchanging cargo at the M06 Loading Rail (owner's brief section 4).
 * There is no other station block. <b>Which section of the skip the docked cart stands beside decides
 * the direction</b>, never what either of them holds:
 *
 * <table>
 *   <caption>Direction by height</caption>
 *   <tr><th>The docked cart is level with</th><th>Exchange</th><th>Shown as</th></tr>
 *   <tr><td>the bridle (the 4th block, above the body)</td><td>{@link Mode#LOAD} cart to skip</td>
 *       <td>the skip's lid opens and the cart's front gate folds down as a ramp into the intake</td></tr>
 *   <tr><td>the chute (the bottom block)</td><td>{@link Mode#UNLOAD} skip to cart</td>
 *       <td>the skip's discharge gate slides open; the cart's front stays shut</td></tr>
 * </table>
 *
 * <p>A station is a Loading Rail in the cell in front of the skip ({@link OreSkip#facing()}) at one of
 * those two heights, facing the skip (a docked cart's front, where its gate is, points into the shaft),
 * with a freight cart docked on it ({@link Couplings.Coupled#simpleBlock$dock()}). Any other height,
 * side or rail, or a cart that is only passing, is not a station. A skip being wound past a station
 * height stops there exactly (the coal winch's lever goes to NEUTRAL); a skip stopped at one exchanges
 * while there is something to move: the source has cargo, the destination has room for it and holds
 * nothing else. It moves {@code skip.transfer_per_tick} items a tick, each tick taking from one and
 * giving to the other at once, so an exchange cut short (the cart released or broken, the rail broken,
 * the chunk unloading) leaves every item in one or the other. The doors open for
 * {@value #DOOR_TICKS} ticks first and close for as many after; the skip will not move until they are
 * shut. Nothing restarts the winch or the train: the player releases the cart at the rail and sets
 * the winch again.
 */
public final class SkipStations {
	/** Ticks the lid, gate or ramp takes to open or close. */
	static final int DOOR_TICKS = 10;

	public enum Mode {
		/** Cart to skip, the cart level with the bridle. */
		LOAD,
		/** Skip to cart, the cart level with the chute. */
		UNLOAD
	}

	/** A station: the rail, its docked cart and the direction. */
	public record Station(BlockPos rail, Minecart cart, Mode mode) {
	}

	/** A skip's exchange in progress (not saved: after loading it starts again if there is still something to move). */
	static final class Exchange {
		enum Phase { NONE, OPENING, MOVING, CLOSING }

		Phase phase = Phase.NONE;
		int ticks;
		@Nullable BlockPos rail;
		@Nullable Mode mode;
		/** Items moved in this exchange (for tests and messages). */
		int moved;

		boolean busy() {
			return this.phase != Phase.NONE;
		}
	}

	private SkipStations() {
	}

	/** The station of a skip standing with its bottom at {@code bottomY}, if there is one. */
	public static @Nullable Station station(Level level, OreSkip skip, double bottomY) {
		if (Math.abs(bottomY - Math.rint(bottomY)) > 1.0E-6) {
			return null;
		}
		BlockPos column = BlockPos.containing(skip.getX(), Math.rint(bottomY), skip.getZ());
		Direction front = skip.facing();
		for (Mode mode : Mode.values()) {
			BlockPos rail = column.above(mode == Mode.LOAD ? 3 : 0).relative(front);
			if (LoadingTrackBlock.front(level.getBlockState(rail)) != front.getOpposite()) {
				continue;
			}
			List<Minecart> carts = level.getEntitiesOfClass(Minecart.class, new AABB(rail).inflate(0.5),
				cart -> cart.isAlive() && LoadingTracks.isFreight(cart) && rail.equals(Couplings.coupled(cart).simpleBlock$dock()));
			if (!carts.isEmpty()) {
				return new Station(rail.immutable(), carts.getFirst(), mode);
			}
		}
		return null;
	}

	/** How many items could move now at {@code station}: from its source, as many as its destination has room for. */
	public static int work(OreSkip skip, Station station) {
		MinecartCargo.Cart cart = MinecartCargo.cart(station.cart());
		if (station.mode() == Mode.LOAD) {
			if (cart.simpleBlock$count() <= 0) {
				return 0;
			}
			return Math.min(cart.simpleBlock$count(), SkipCargo.room(skip, new net.minecraft.world.item.ItemStack(cart.simpleBlock$cargo())));
		}
		if (skip.count() <= 0 || cart.simpleBlock$count() > 0 && cart.simpleBlock$cargo() != skip.cargo()) {
			return 0;
		}
		return Math.min(skip.count(), MinecartCargo.CAPACITY - cart.simpleBlock$count());
	}

	/** Every server tick of a skip, before it moves: runs its exchange at the station it stands at, if any. */
	static void tick(ServerLevel level, OreSkip skip) {
		Exchange exchange = skip.exchange;
		Station station = station(level, skip, skip.getY());
		boolean same = station != null && station.rail().equals(exchange.rail) && station.mode() == exchange.mode;
		switch (exchange.phase) {
			case NONE -> {
				if (station != null && work(skip, station) > 0) {
					exchange.phase = Exchange.Phase.OPENING;
					exchange.ticks = DOOR_TICKS;
					exchange.rail = station.rail();
					exchange.mode = station.mode();
					exchange.moved = 0;
					level.playSound(null, station.rail(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.8F);
				}
			}
			case OPENING -> {
				if (!same) {
					close(level, exchange);
				} else if (--exchange.ticks <= 0) {
					exchange.phase = Exchange.Phase.MOVING;
				}
			}
			case MOVING -> {
				int n = same ? Math.min(Tuning.getInt("skip.transfer_per_tick"), work(skip, station)) : 0;
				if (n <= 0) {
					close(level, exchange);
				} else {
					MinecartCargo.Cart cart = MinecartCargo.cart(station.cart());
					if (station.mode() == Mode.LOAD) {
						Item item = cart.simpleBlock$cargo();
						cart.simpleBlock$setCargo(item, cart.simpleBlock$count() - n);
						skip.setCargo(item, skip.count() + n);
					} else {
						Item item = skip.cargo();
						skip.setCargo(item, skip.count() - n);
						cart.simpleBlock$setCargo(item, cart.simpleBlock$count() + n);
					}
					exchange.moved += n;
					if (level.getGameTime() % 4 == 0) {
						level.playSound(null, station.rail(), SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 0.6F, 0.9F);
					}
				}
			}
			case CLOSING -> {
				if (--exchange.ticks <= 0) {
					exchange.phase = Exchange.Phase.NONE;
					exchange.rail = null;
					exchange.mode = null;
				}
			}
		}
		// The doors: the lid and the cart's ramp for loading, the discharge gate for unloading.
		boolean open = exchange.phase == Exchange.Phase.OPENING || exchange.phase == Exchange.Phase.MOVING;
		skip.setDoors(open && exchange.mode == Mode.LOAD, open && exchange.mode == Mode.UNLOAD);
		if (open && exchange.mode == Mode.LOAD && station != null && same) {
			LoadingTracks.freight(station.cart()).simpleBlock$holdGateOpen(3);
		}
	}

	private static void close(ServerLevel level, Exchange exchange) {
		exchange.phase = Exchange.Phase.CLOSING;
		exchange.ticks = DOOR_TICKS;
		if (exchange.rail != null) {
			level.playSound(null, exchange.rail, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.8F);
		}
	}

	/** For tests: the items moved by the skip's current or last exchange, and its phase. */
	public static int moved(OreSkip skip) {
		return skip.exchange.moved;
	}

	public static boolean busy(OreSkip skip) {
		return skip.exchange.busy();
	}
}
