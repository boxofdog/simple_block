package io.github.boxofdog.simpleblock.metallurgy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.train.Couplings;
import io.github.boxofdog.simpleblock.train.LoadingTrackBlock;
import io.github.boxofdog.simpleblock.train.LoadingTracks;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M08: freight carts tipping into the furnace's hopper, the M07 way a cart tips into a skip. A cart
 * docked on a Loading Rail (end-on or side-on) <b>beside the furnace, level with the top of the
 * hopper's rim</b>, opens its gate on the furnace's side and its cargo pours diagonally down over the
 * rim into the hopper. Twelve rail positions qualify, three along each side of the furnace. The space
 * over the rim between the cart and the hopper must be open.
 *
 * <p>The furnace takes only raw iron, copper or gold, one kind at a time, up to its 516: a cart with
 * anything else, another metal than the furnace holds, or a full furnace keeps its gate shut and its
 * cargo. Each tick moves {@code blast_furnace.transfer_per_tick} items, taken from the cart and given
 * to the furnace at once, so a transfer cut short leaves every item in one or the other. (The ore
 * skip's chute empties into the same hopper: {@code skip/SkipStations}.)
 */
public final class BlastFurnaceFreight {
	/** Ticks a cart's gate takes to open before the ore runs. */
	static final int DOOR_TICKS = 10;

	/** A furnace's intakes in progress: ticks each rail's cart has been tipping, and the items taken in (tests). */
	static final class Intakes {
		final Map<BlockPos, Integer> tipping = new HashMap<>();
		int moved;
	}

	/** A cart docked beside the hopper, and the way from it to the furnace. */
	public record CartStation(BlockPos rail, Minecart cart, Direction towardFurnace) {
	}

	private BlastFurnaceFreight() {
	}

	/** The carts docked at the furnace's twelve rail positions, with nothing over the rim in the way. */
	public static List<CartStation> cartStations(ServerLevel level, BlockPos controller) {
		List<CartStation> stations = new ArrayList<>();
		int rimY = controller.getY() + BlastFurnaceStructure.HOPPER_LAYER - BlastFurnaceStructure.CONTROLLER_LAYER + 1;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			Direction along = side.getClockWise();
			for (int l = -1; l <= 1; l++) {
				BlockPos over = new BlockPos(controller.getX(), rimY, controller.getZ()).relative(side).relative(along, l);
				BlockPos rail = over.relative(side);
				if (LoadingTrackBlock.front(level.getBlockState(rail)) == null
					|| !level.getBlockState(over).getCollisionShape(level, over).isEmpty()) {
					continue;
				}
				List<Minecart> carts = level.getEntitiesOfClass(Minecart.class, new AABB(rail).inflate(0.5),
					cart -> cart.isAlive() && LoadingTracks.isFreight(cart) && rail.equals(Couplings.coupled(cart).simpleBlock$dock()));
				if (!carts.isEmpty()) {
					stations.add(new CartStation(rail.immutable(), carts.getFirst(), side.getOpposite()));
				}
			}
		}
		return stations;
	}

	/** How many of a docked cart's cargo the furnace could take now. */
	public static int work(BlastFurnaceBlockEntity furnace, Minecart minecart) {
		MinecartCargo.Cart cart = MinecartCargo.cart(minecart);
		return cart.simpleBlock$count() <= 0 ? 0 : Math.min(cart.simpleBlock$count(), furnace.room(cart.simpleBlock$cargo()));
	}

	/** Every server tick of a furnace: its docked carts tip in what it takes. */
	static void tick(ServerLevel level, BlastFurnaceBlockEntity furnace) {
		Intakes intakes = furnace.intakes;
		List<CartStation> stations = cartStations(level, furnace.getBlockPos());
		intakes.tipping.keySet().removeIf(rail -> stations.stream().noneMatch(station -> station.rail().equals(rail)));
		for (CartStation station : stations) {
			int work = work(furnace, station.cart());
			if (work <= 0) {
				intakes.tipping.remove(station.rail());
				continue;
			}
			int ticks = intakes.tipping.merge(station.rail(), 1, Integer::sum);
			LoadingTracks.freight(station.cart()).simpleBlock$holdGateOpen(station.towardFurnace(), 3);
			if (ticks == 1) {
				level.playSound(null, station.rail(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.8F);
			}
			if (ticks <= DOOR_TICKS) {
				continue;
			}
			MinecartCargo.Cart cart = MinecartCargo.cart(station.cart());
			Item item = cart.simpleBlock$cargo();
			int n = Math.min(Tuning.getInt("blast_furnace.transfer_per_tick"), work);
			cart.simpleBlock$setCargo(item, cart.simpleBlock$count() - n);
			furnace.deposit(item, n);
			intakes.moved += n;
			if (level.getGameTime() % 4 == 0) {
				level.playSound(null, station.rail(), SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 0.6F, 0.9F);
			}
		}
	}

	/** For tests: items the furnace has taken in from carts since it was loaded. */
	public static int moved(BlastFurnaceBlockEntity furnace) {
		return furnace.intakes.moved;
	}
}
