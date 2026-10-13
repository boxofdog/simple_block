package io.github.boxofdog.simpleblock.skip;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * M07: the ore skip's screen: its one cargo slot over the player's inventory.
 *
 * <p>The cargo slot is not a vanilla {@link Slot}: a vanilla slot holds a real {@link ItemStack}, and a
 * stack of 512 would be split, dragged, swapped and merged by vanilla's rules into places that hold 64.
 * Instead the menu shows the skip's item and count (two synced data values) in a slot drawn by the
 * screen, and the screen's clicks on it come to the server as menu buttons ({@link #clickMenuButton}):
 * <ul>
 *   <li><b>Left-click</b>: with an empty cursor, takes one stack (up to the item's stack size, within the
 *       heavy-load cap); holding cargo, puts all of it that fits in.</li>
 *   <li><b>Right-click</b>: with an empty cursor, takes half a stack; holding cargo, puts one in.</li>
 *   <li><b>Shift-click</b>: one stack into the player's inventory. Shift-click on cargo in the
 *       inventory puts it in the skip ({@link #quickMoveStack}).</li>
 * </ul>
 * Drags, double-click collection and number-key swaps only ever see the player's own slots, so they
 * never touch the skip. Everything is decided on the server.
 */
public class SkipMenu extends AbstractContainerMenu {
	public static final int BUTTON_LEFT = 0;
	public static final int BUTTON_RIGHT = 1;
	public static final int BUTTON_SHIFT = 2;
	/** Where the screen draws the cargo slot (as a vanilla slot's item position), and the player's inventory. */
	public static final int CARGO_X = 80;
	public static final int CARGO_Y = 20;
	public static final int INVENTORY_Y = 51;

	private final @Nullable OreSkip skip;
	private final ContainerData data;

	/** The client's menu: the cargo comes in the synced data. */
	public SkipMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null, new SimpleContainerData(2));
	}

	SkipMenu(int containerId, Inventory inventory, @Nullable OreSkip skip, ContainerData data) {
		super(SkipContent.SKIP_MENU, containerId);
		this.skip = skip;
		this.data = data;
		this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
		this.addDataSlots(data);
	}

	/** The server's menu for {@code skip}. */
	static SkipMenu server(int containerId, Inventory inventory, OreSkip skip) {
		return new SkipMenu(containerId, inventory, skip, new ContainerData() {
			@Override
			public int get(int index) {
				return index == 0 ? BuiltInRegistries.ITEM.getId(skip.cargo()) : skip.count();
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

	/** The item in the cargo slot, {@link Items#AIR} when empty (client: as last synced). */
	public Item cargo() {
		return this.count() > 0 ? BuiltInRegistries.ITEM.byId(this.data.get(0)) : Items.AIR;
	}

	public int count() {
		return this.data.get(1);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.skip == null || this.skip.isAlive() && player.distanceToSqr(this.skip.getBoundingBox().getCenter()) < 8.0 * 8.0;
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (this.skip == null || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		OreSkip skip = this.skip;
		ItemStack carried = this.getCarried();
		switch (buttonId) {
			case BUTTON_LEFT, BUTTON_RIGHT -> {
				boolean right = buttonId == BUTTON_RIGHT;
				if (carried.isEmpty()) {
					int stack = Math.min(skip.count(), skip.cargo().getDefaultMaxStackSize());
					int wanted = right ? (stack + 1) / 2 : stack;
					int allowed = SkipCargo.allowed(player, skip, wanted);
					if (allowed > 0) {
						this.setCarried(SkipCargo.withdraw(skip, allowed));
					} else if (skip.count() > 0) {
						serverPlayer.sendOverlayMessage(SkipCargo.message("too_heavy", "Too heavy: you can't carry any more %s",
							new ItemStack(skip.cargo()).getItemName()));
					}
				} else if (SkipCargo.room(skip, carried) > 0) {
					SkipCargo.deposit(skip, carried, right ? 1 : carried.getCount());
					this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
				} else {
					serverPlayer.sendOverlayMessage(SkipCargo.loadFromHand(skip, carried.copy()));
				}
			}
			case BUTTON_SHIFT -> serverPlayer.sendOverlayMessage(SkipCargo.unloadToInventory(skip, serverPlayer));
			default -> {
				return false;
			}
		}
		return true;
	}

	/** Shift-click on a stack in the player's inventory: as much of it as the skip takes goes in. */
	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (this.skip == null || slotIndex < 0 || slotIndex >= this.slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = this.slots.get(slotIndex);
		ItemStack stack = slot.getItem();
		if (!stack.isEmpty() && SkipCargo.deposit(this.skip, stack, stack.getCount()) > 0) {
			slot.setChanged();
		}
		// Nothing else to move: vanilla repeats a quick move only while this returns the same item.
		return ItemStack.EMPTY;
	}
}
