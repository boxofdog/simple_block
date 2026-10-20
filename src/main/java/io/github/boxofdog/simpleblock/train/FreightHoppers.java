package io.github.boxofdog.simpleblock.train;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.skip.SkipStations;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M11: vanilla hoppers load and unload freight carts docked at a Loading Rail. Nothing new is placed:
 * an ordinary hopper, an ordinary freight cart, a Loading Rail.
 *
 * <ul>
 *   <li><b>In, from above.</b> A hopper pointing down over the Loading Rail (M15: up to three clear blocks above it),
 *       feeds the cart docked there through its open top, as a hopper feeds any container under it:
 *       at the hopper's own pace (one item every 8 ticks; M15: redstone does not lock a freight hopper), not out
 *       of loaded chunks or empty. The cart takes only cargo of the kind it holds, up to its 256; the
 *       hopper keeps what the cart refuses. Only a docked cart is fed (one passing under is not), and
 *       only while its gates are shut. ({@code mixin/train/HopperBlockEntityMixin}: the cart stands
 *       in as the container under the hopper.)</li>
 *   <li><b>Out, through a gate.</b> A hopper diagonally below the docked cart, beside the rail and one
 *       block down, so that its top is level with the rail and under the cart's gate on that side,
 *       takes the cargo: the gate on that side opens (an end gate folds down into a ramp, a side gate
 *       swings up and out), and after {@value #DOOR_TICKS} ticks {@code freight.hopper_discharge_per_tick}
 *       items a tick pour over the lip into the hopper, each taken from the cart and put in the hopper
 *       at once. The space over the hopper, between it and the gate, must be open: no block with a
 *       collision box, no rail, no other cart (so a coupled car's end gate never opens into its
 *       neighbour; side gates unload a train car by car). A hopper locked by redstone still takes items
 *       in, as it would from another hopper; it just passes nothing on.</li>
 *   <li><b>One way at a time.</b> A cart with a feeding hopper over it is a loading station and does
 *       not tip into hoppers; a cart being filled or emptied by a skip, or tipping into a blast furnace,
 *       does neither (those keep the gate they opened). Of several hoppers round a cart, the first that
 *       can take its cargo is used, in the order right side, left side, front end, back end (the
 *       cart's front is the rail's facing), and kept while it can take more; only that gate opens.</li>
 *   <li><b>Stopping.</b> A full or broken hopper, a blocked space, the cart released or broken, or the
 *       chunk unloading stops the flow where it is: every item is in the cart or the hopper, the gate
 *       closes. It starts again from shut when it can. A cart emptied this way, or filled to 256 from
 *       above, is let go as a skip lets go of the car it has worked ({@link LoadingTracks#releaseCar}).</li>
 * </ul>
 * A hopper under the Loading Rail gets nothing: the cart's floor is closed; vanilla hoppers draw only
 * from containers and dropped items above them. M15: a freight hopper's lid ({@link FreightPorts}) closes its
 * opening to the cart, and at an engine train's station each wagon loads or unloads only as its transfer
 * says ({@link FreightStations}); such a wagon is not let go when full or empty (the train waits for its lever).
 */
public final class FreightHoppers {
	/** Ticks a gate takes to open before the cargo runs. */
	public static final int DOOR_TICKS = 10;

	/** A cart's tipping in progress: the gate's side, ticks since it began opening, and the items moved (tests). */
	static final class Discharge {
		final Direction side;
		int ticks;

		Discharge(Direction side) {
			this.side = side;
		}
	}

	private static final Map<Minecart, Discharge> DISCHARGING = new WeakHashMap<>();
	/** For tests: the items each cart has tipped into hoppers since it was loaded. */
	private static final Map<Minecart, Integer> MOVED = new WeakHashMap<>();

	private FreightHoppers() {
	}

	// ---- In, from above ----

	/** The freight cart docked at the Loading Rail at {@code rail}, or null. */
	static @Nullable Minecart dockedAt(ServerLevel level, BlockPos rail) {
		if (LoadingTrackBlock.front(level.getBlockState(rail)) == null) {
			return null;
		}
		List<Minecart> carts = level.getEntitiesOfClass(Minecart.class, new AABB(rail).inflate(0.5),
			cart -> cart.isAlive() && LoadingTracks.isFreight(cart) && rail.equals(Couplings.coupled(cart).simpleBlock$dock()));
		return carts.isEmpty() ? null : carts.getFirst();
	}

	/**
	 * The container a hopper at {@code hopperPos} pushes into when vanilla finds none: the cart docked at
	 * the Loading Rail straight under a hopper that points down. Null otherwise.
	 */
	public static @Nullable Container intake(ServerLevel level, BlockPos hopperPos, BlockState hopper) {
		BlockPos rail = FreightPorts.railBelow(level, hopperPos, hopper);
		Minecart cart = rail == null ? null : dockedAt(level, rail);
		return cart == null ? null : new Intake(level, cart, rail.immutable(), hopperPos.immutable());
	}

	/** Whether a hopper over the Loading Rail at {@code rail} points down into it (the cart there is being loaded). */
	static boolean fedFromAbove(ServerLevel level, BlockPos rail) {
		return FreightPorts.hopperAbove(level, rail) != null;
	}

	/** A docked cart as the one-slot container under a hopper: it only takes, a whole offered stack or nothing. */
	static final class Intake implements Container {
		private final ServerLevel level;
		private final Minecart cart;
		private final BlockPos rail;
		private final BlockPos hopper;

		Intake(ServerLevel level, Minecart cart, BlockPos rail, BlockPos hopper) {
			this.level = level;
			this.cart = cart;
			this.rail = rail;
			this.hopper = hopper;
		}

		private boolean open() {
			return this.cart.isAlive() && this.rail.equals(Couplings.coupled(this.cart).simpleBlock$dock())
				&& !LoadingTracks.freight(this.cart).simpleBlock$gateOpen() && !SkipStations.working(this.level, this.cart)
				&& FreightStations.mayLoad(this.level, this.cart, this.hopper);
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return this.open() && MinecartCargo.room(MinecartCargo.cart(this.cart), stack) >= stack.getCount();
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			MinecartCargo.Cart cargo = MinecartCargo.cart(this.cart);
			if (stack.isEmpty() || MinecartCargo.room(cargo, stack) < stack.getCount()) {
				return;
			}
			cargo.simpleBlock$setCargo(stack.getItem(), cargo.simpleBlock$count() + stack.getCount());
			FreightStations.moved(this.cart);
			if (cargo.simpleBlock$count() >= MinecartCargo.CAPACITY && !FreightStations.stationTrain(this.cart)) {
				LoadingTracks.releaseCar(this.level, this.cart, this.rail);
			}
		}

		@Override
		public int getContainerSize() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return MinecartCargo.cart(this.cart).simpleBlock$count() <= 0;
		}

		/** Always empty, so a hopper puts its item in with {@link #setItem}. */
		@Override
		public ItemStack getItem(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public boolean canTakeItem(Container into, int slot, ItemStack stack) {
			return false;
		}

		@Override
		public void setChanged() {
		}

		@Override
		public boolean stillValid(Player player) {
			return false;
		}

		@Override
		public void clearContent() {
		}
	}

	// ---- Out, through a gate ----

	/** The hopper diagonally below the cart docked at {@code rail} on {@code side}, if the way into its top is open. */
	static @Nullable HopperBlockEntity receiver(ServerLevel level, BlockPos rail, Direction side, Minecart cart) {
		BlockPos over = rail.relative(side);
		if (!(level.getBlockEntity(over.below()) instanceof HopperBlockEntity hopper)) {
			return null;
		}
		BlockState space = level.getBlockState(over);
		if (!space.getCollisionShape(level, over).isEmpty() || space.getBlock() instanceof BaseRailBlock) {
			return null;
		}
		boolean otherCart = !level.getEntitiesOfClass(AbstractMinecart.class, new AABB(over).deflate(0.05), other -> other != cart && other.isAlive()).isEmpty();
		return otherCart ? null : hopper;
	}

	/** Whether {@code hopper} has room for at least one {@code item}. */
	static boolean takes(HopperBlockEntity hopper, Item item) {
		for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
			ItemStack in = hopper.getItem(slot);
			if (in.isEmpty() || in.is(item) && in.getComponentsPatch().isEmpty() && in.getCount() < in.getMaxStackSize()) {
				return true;
			}
		}
		return false;
	}

	/** The sides a cart docked at {@code rail} tries, in order: right side, left side, front end, back end. */
	static Direction[] order(ServerLevel level, BlockPos rail) {
		Direction front = LoadingTrackBlock.front(level.getBlockState(rail));
		if (front == null) {
			front = Direction.NORTH;
		}
		return new Direction[] {front.getClockWise(), front.getCounterClockWise(), front, front.getOpposite()};
	}

	/** Every tick of a freight cart docked at {@code rail}: tips its cargo into a hopper beside it, if one can take it. */
	static void discharge(ServerLevel level, Minecart cart, BlockPos rail) {
		MinecartCargo.Cart cargo = MinecartCargo.cart(cart);
		LoadingTracks.Freight freight = LoadingTracks.freight(cart);
		Discharge discharge = DISCHARGING.get(cart);
		Direction gate = freight.simpleBlock$gateSide();
		boolean station = FreightStations.stationTrain(cart);
		if (cargo.simpleBlock$count() <= 0 || !station && fedFromAbove(level, rail) || SkipStations.working(level, cart)
			|| station && freight.simpleBlock$service() != FreightStations.Service.UNLOAD
			|| discharge != null && gate != null && gate != discharge.side) {
			// Nothing to tip, a loading station, or another exchange has the gate: leave it be.
			DISCHARGING.remove(cart);
			return;
		}
		Item item = cargo.simpleBlock$cargo();
		if (discharge == null) {
			if (gate != null) {
				return;
			}
			for (Direction side : order(level, rail)) {
				HopperBlockEntity hopper = receiver(level, rail, side, cart);
				if (hopper != null && takes(hopper, item) && FreightStations.mayUnload(level, cart, hopper.getBlockPos())) {
					discharge = new Discharge(side);
					DISCHARGING.put(cart, discharge);
					level.playSound(null, rail, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.8F);
					break;
				}
			}
			if (discharge == null) {
				return;
			}
		}
		HopperBlockEntity hopper = receiver(level, rail, discharge.side, cart);
		if (hopper == null || !takes(hopper, item) || !FreightStations.mayUnload(level, cart, hopper.getBlockPos())) {
			// Full, gone or blocked: the gate shuts where it is.
			DISCHARGING.remove(cart);
			level.playSound(null, rail, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.8F);
			return;
		}
		freight.simpleBlock$holdGateOpen(discharge.side, 3);
		if (++discharge.ticks <= DOOR_TICKS) {
			return;
		}
		int offered = Math.min(Tuning.getInt("freight.hopper_discharge_per_tick"), cargo.simpleBlock$count());
		ItemStack left = HopperBlockEntity.addItem(null, hopper, new ItemStack(item, offered), Direction.UP);
		int moved = offered - left.getCount();
		if (moved <= 0) {
			return;
		}
		cargo.simpleBlock$setCargo(item, cargo.simpleBlock$count() - moved);
		MOVED.merge(cart, moved, Integer::sum);
		FreightStations.moved(cart);
		if (level.getGameTime() % 4 == 0) {
			level.playSound(null, rail, SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 0.6F, 0.9F);
		}
		if (cargo.simpleBlock$count() <= 0) {
			DISCHARGING.remove(cart);
			if (!station) {
				LoadingTracks.releaseCar(level, cart, rail);
			}
		}
	}

	/** M15: a wagon's tipping stops where it is (its station transfer is over or stopped); the gate shuts. */
	static void stopDischarge(Minecart cart) {
		if (DISCHARGING.remove(cart) != null) {
			LoadingTracks.freight(cart).simpleBlock$holdGateOpen(null, 0);
		}
	}

	/** For tests: the gate a cart is tipping through into a hopper, or null. */
	public static @Nullable Direction discharging(Minecart cart) {
		Discharge discharge = DISCHARGING.get(cart);
		return discharge == null ? null : discharge.side;
	}

	/** For tests: the items a cart has tipped into hoppers since it was loaded. */
	public static int moved(Minecart cart) {
		return MOVED.getOrDefault(cart, 0);
	}
}
