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
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceBlockEntity;
import io.github.boxofdog.simpleblock.metallurgy.BlastFurnaceStructure;
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
 *       <td>the skip's lid opens and the cart's gate on the skip's side opens into the intake (an end gate
 *       folds down as a ramp, a side gate swings up and out)</td></tr>
 *   <tr><td>one level below the chute, kitty-corner (owner's playtest patch §1), with the chute's lip on
 *       the cart's rim</td><td>{@link Mode#UNLOAD} skip to cart</td>
 *       <td>the skip's discharge gate slides open; the cart's gates stay shut</td></tr>
 * </table>
 *
 * <p>M08 adds a third: a skip whose bottom is level with the top of a blast furnace's hopper rim, the
 * hopper in front of it, empties its chute into the furnace ({@link Mode#FURNACE}; the discharge gate
 * opens; the furnace takes only raw metal of the kind it holds, up to its 516).
 *
 * <p>A station is a Loading Rail in front of the skip ({@link OreSkip#facing()}) at one of those two
 * positions, whichever way the rail runs: end-on (a docked cart's end gate towards the skip) or side-on
 * (its long side gate towards it, so each car of a coupled train can be worked without uncoupling),
 * with a freight cart docked on it ({@link Couplings.Coupled#simpleBlock$dock()}), and only for a skip hung on
 * its cable (it has its bridle then). Any other height, side or rail, a cart only passing, a cart beside
 * the skip's bottom block at the same level, or a solid block over the cart in the chute's way is not a
 * station. A skip being wound past a station height stops there exactly (the coal winch's lever goes to
 * NEUTRAL); a skip stopped at one exchanges while there is something to move: the source has cargo, the
 * destination has room for it and holds
 * nothing else. It moves {@code skip.transfer_per_tick} items a tick, each tick taking from one and
 * giving to the other at once, so an exchange cut short (the cart released or broken, the rail broken,
 * the chunk unloading) leaves every item in one or the other. The doors open for
 * {@value #DOOR_TICKS} ticks first and close for as many after; the skip will not move until they are
 * shut. M07 playtest: once a cart's exchange has finished (nothing more to move) and the doors are
 * shut, that cart is let go and its rail's stop folds, so a skip wound on past it cannot work it again;
 * a refused or interrupted exchange leaves it docked. Only that car is let go: the others of a coupled
 * train dock there in turn. Nothing restarts the winch or the train.
 */
public final class SkipStations {
	/** Ticks the lid, gate or ramp takes to open or close. */
	static final int DOOR_TICKS = 10;

	public enum Mode {
		/** Cart to skip, the cart level with the bridle. */
		LOAD,
		/** Skip to cart, the cart one level below the chute, the chute's lip on its rim. */
		UNLOAD,
		/** M08: skip to the blast furnace, the chute over the hopper's rim. */
		FURNACE
	}

	/** A station: the rail and its docked cart, or for {@link Mode#FURNACE} the hopper block (no cart), and the direction. */
	public record Station(BlockPos rail, @Nullable Minecart cart, Mode mode) {
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
		/** M07 playtest: the cart worked, and whether the exchange ran to its end (nothing more to move) rather than being cut short. */
		@Nullable Minecart cart;
		boolean completed;

		boolean busy() {
			return this.phase != Phase.NONE;
		}
	}

	private SkipStations() {
	}

	/**
	 * Owner's playtest patch §1: where a skip stops to fill a cart through its chute. The cart stands on
	 * a Loading Rail in front of the skip one level lower (kitty-corner below the chute), and the skip
	 * hangs with its bottom this far above that rail block, so that the bottom of the chute's lip (1 px
	 * above the skip's bottom) rests on the cart's rim. A vanilla minecart's rim is 12 px above its rail
	 * block: the cart is drawn 1 px above the rail, its body's origin 6 px above that, its walls' tops
	 * 5 px above the origin ({@code MinecartModel}, {@code AbstractMinecartRenderer}).
	 */
	public static final double UNLOAD_OFFSET = 11.0 / 16.0;
	/** Loading: the cart stands level with the bridle, the fourth block (skip bottom + 3). */
	public static final int LOAD_OFFSET = 3;

	/** Every height a skip could stop at for a station between {@code from} and {@code to} (either way; ends exclusive of {@code from}). */
	static List<Double> heightsBetween(double from, double to) {
		List<Double> heights = new java.util.ArrayList<>();
		double low = Math.min(from, to);
		double high = Math.max(from, to);
		for (int k = (int) Math.floor(low) - 1; k <= (int) Math.ceil(high) + 1; k++) {
			for (double h : new double[] {k, k + UNLOAD_OFFSET}) {
				boolean between = to > from ? h > from + 1.0E-6 && h <= to + 1.0E-6 : h < from - 1.0E-6 && h >= to - 1.0E-6;
				if (between) {
					heights.add(h);
				}
			}
		}
		return heights;
	}

	/**
	 * The station of a skip standing with its bottom at {@code bottomY}, if there is one. Only a skip
	 * hung on its cable (with its bridle) has stations, and only at these exact heights, in front of it
	 * ({@link OreSkip#facing()}), on a Loading Rail facing into the shaft with a freight cart docked:
	 * <ul>
	 *   <li>{@link Mode#LOAD}: bottom on a whole block, the rail level with the bridle (bottom + 3).</li>
	 *   <li>{@link Mode#UNLOAD}: bottom {@link #UNLOAD_OFFSET} above the rail block, which is one level
	 *       below the chute (the chute's lip on the cart's rim), and nothing solid in front of the chute,
	 *       over the cart, for the ore to pass through.</li>
	 * </ul>
	 * A cart beside the skip's bottom block at the same level is not a station: the chute cannot reach it.
	 */
	public static @Nullable Station station(Level level, OreSkip skip, double bottomY) {
		if (!skip.bridled()) {
			return null;
		}
		Direction front = skip.facing();
		Mode mode;
		BlockPos rail;
		if (Math.abs(bottomY - Math.rint(bottomY)) < 1.0E-6) {
			mode = Mode.LOAD;
			rail = BlockPos.containing(skip.getX(), Math.rint(bottomY) + LOAD_OFFSET, skip.getZ()).relative(front);
			if (LoadingTrackBlock.front(level.getBlockState(rail)) == null) {
				return furnaceStation(level, skip, bottomY);
			}
		} else if (Math.abs(bottomY - UNLOAD_OFFSET - Math.rint(bottomY - UNLOAD_OFFSET)) < 1.0E-6) {
			mode = Mode.UNLOAD;
			rail = BlockPos.containing(skip.getX(), Math.rint(bottomY - UNLOAD_OFFSET), skip.getZ()).relative(front);
			if (!level.getBlockState(rail.above()).getCollisionShape(level, rail.above()).isEmpty()) {
				return null;
			}
		} else {
			return null;
		}
		// Any Loading Rail there, end-on or side-on to the skip: the cart's gate on the skip's side opens
		// (every cart has four: the owner's cart redesign).
		if (LoadingTrackBlock.front(level.getBlockState(rail)) == null) {
			return null;
		}
		List<Minecart> carts = level.getEntitiesOfClass(Minecart.class, new AABB(rail).inflate(0.5),
			cart -> cart.isAlive() && LoadingTracks.isFreight(cart) && rail.equals(Couplings.coupled(cart).simpleBlock$dock()));
		return carts.isEmpty() ? null : new Station(rail.immutable(), carts.getFirst(), mode);
	}

	/**
	 * M08: a skip standing with its bottom level with the top of a blast furnace's hopper rim, the hopper
	 * in front of it ({@link OreSkip#facing()}): its chute's lip reaches over the rim. The space over the
	 * rim in front of the chute must be open.
	 */
	private static @Nullable Station furnaceStation(Level level, OreSkip skip, double bottomY) {
		BlockPos overRim = BlockPos.containing(skip.getX(), Math.rint(bottomY), skip.getZ()).relative(skip.facing());
		BlockPos hopper = overRim.below();
		if (BlastFurnaceStructure.hopperAt(level, hopper) == null || !level.getBlockState(overRim).getCollisionShape(level, overRim).isEmpty()) {
			return null;
		}
		return new Station(hopper.immutable(), null, Mode.FURNACE);
	}

	/** How many items could move now at {@code station}: from its source, as many as its destination has room for. */
	public static int work(OreSkip skip, Station station) {
		if (station.mode() == Mode.FURNACE) {
			BlastFurnaceBlockEntity furnace = BlastFurnaceStructure.hopperAt(skip.level(), station.rail());
			return furnace == null || skip.count() <= 0 ? 0 : Math.min(skip.count(), furnace.room(skip.cargo()));
		}
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
					exchange.cart = station.cart();
					exchange.completed = false;
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
					// Done (nothing more to move, the station still there), or cut short.
					exchange.completed = same && exchange.moved > 0;
					close(level, exchange);
				} else if (station.mode() == Mode.FURNACE) {
					Item item = skip.cargo();
					skip.setCargo(item, skip.count() - n);
					BlastFurnaceStructure.hopperAt(level, station.rail()).deposit(item, n);
					exchange.moved += n;
					if (level.getGameTime() % 4 == 0) {
						level.playSound(null, station.rail(), SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 0.6F, 0.9F);
					}
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
					// M07 playtest: a finished exchange with a cart lets it go, its stop folding, so that the
					// skip wound on past it cannot work the same cart again (it does not dock there again until
					// it has left). A refused or interrupted one keeps it docked.
					if (exchange.completed && exchange.cart != null && exchange.rail != null && exchange.mode != Mode.FURNACE) {
						LoadingTracks.releaseCar(level, exchange.cart, exchange.rail);
					}
					exchange.cart = null;
					exchange.completed = false;
					exchange.phase = Exchange.Phase.NONE;
					exchange.rail = null;
					exchange.mode = null;
				}
			}
		}
		// The doors: the lid and the cart's ramp for loading, the discharge gate for unloading.
		boolean open = exchange.phase == Exchange.Phase.OPENING || exchange.phase == Exchange.Phase.MOVING;
		skip.setDoors(open && exchange.mode == Mode.LOAD, open && exchange.mode != Mode.LOAD);
		if (open && exchange.mode == Mode.LOAD && station != null && same) {
			// The cart's gate that faces the skip: an end gate (ramp) or a side gate (lifted), as it stands.
			LoadingTracks.freight(station.cart()).simpleBlock$holdGateOpen(skip.facing().getOpposite(), 3);
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

	/** M11: whether a skip near {@code cart} is exchanging with it now (filling or emptying it). */
	public static boolean working(ServerLevel level, Minecart cart) {
		return !level.getEntitiesOfClass(OreSkip.class, cart.getBoundingBox().inflate(4.0),
			skip -> skip.exchange.busy() && skip.exchange.cart == cart).isEmpty();
	}
}
