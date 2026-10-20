package io.github.boxofdog.simpleblock.train;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.boxofdog.simpleblock.feature.MinecartCargo;

/**
 * M13 (BUG-014, owner-approved 2026-10-19): a freight cart's cargo, inspected. An empty main hand used
 * on a freight cart (not sneaking, nothing else applying) opens this: one greyed-out slot showing what
 * the cart carries and how many, kept up to date while open. It has no slots at all (nothing can be
 * put in, taken or dragged) and no player inventory: the item and count are synced data.
 */
public class CargoInspectMenu extends AbstractContainerMenu {
	private static final int DATA_ITEM = 0;
	private static final int DATA_COUNT = 1;

	private final @Nullable Minecart cart;
	private final ContainerData data;

	/** The client's menu. */
	public CargoInspectMenu(int containerId, Inventory inventory) {
		this(containerId, null, new SimpleContainerData(2));
	}

	private CargoInspectMenu(int containerId, @Nullable Minecart cart, ContainerData data) {
		super(TrainContent.CARGO_INSPECT_MENU, containerId);
		this.cart = cart;
		this.data = data;
		this.addDataSlots(data);
	}

	/** The server's menu for {@code cart}. */
	public static CargoInspectMenu server(int containerId, Minecart cart) {
		return new CargoInspectMenu(containerId, cart, new ContainerData() {
			@Override
			public int get(int index) {
				MinecartCargo.Cart cargo = MinecartCargo.cart(cart);
				return index == DATA_ITEM ? BuiltInRegistries.ITEM.getId(cargo.simpleBlock$cargo()) : cargo.simpleBlock$count();
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return 2;
			}
		});
	}

	/** What the cart carries ({@link Items#AIR} when empty). */
	public Item cargo() {
		return this.count() > 0 ? BuiltInRegistries.ITEM.byId(this.data.get(DATA_ITEM)) : Items.AIR;
	}

	public int count() {
		return this.data.get(DATA_COUNT);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.cart == null || this.cart.isAlive() && player.distanceToSqr(this.cart) < 8.0 * 8.0;
	}
}
