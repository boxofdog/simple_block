package io.github.boxofdog.simpleblock.cable;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M13 (BUG-013): the coal winch's engine box as a small screen, laid out like a furnace's fuel slot with
 * its flame: one slot showing the coal in the winch, coal only, up to {@code winch.coal_capacity}, and the
 * player's inventory. Putting coal in or taking it out changes the line's coal itself (the winch's
 * one store of coal, {@link CableLines.Line}); the lever is not on this screen.
 */
public class CoalWinchMenu extends AbstractContainerMenu {
	public static final int COAL_X = 56;
	public static final int COAL_Y = 53;
	private static final int DATA_BURN = 0;
	private static final int DATA_BURN_TOTAL = 1;

	private final @Nullable BlockPos drum;
	private final ContainerData data;

	/** The client's menu. */
	public CoalWinchMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null, new SimpleContainer(1), new SimpleContainerData(2));
	}

	private CoalWinchMenu(int containerId, Inventory inventory, @Nullable BlockPos drum, Container coal, ContainerData data) {
		super(CableContent.COAL_WINCH_MENU, containerId);
		this.drum = drum;
		this.data = data;
		this.addSlot(new Slot(coal, 0, COAL_X, COAL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return CoalWinch.isCoal(stack);
			}

			@Override
			public int getMaxStackSize() {
				return Tuning.getInt("winch.coal_capacity");
			}

			@Override
			public int getMaxStackSize(ItemStack stack) {
				return Math.min(stack.getMaxStackSize(), this.getMaxStackSize());
			}
		});
		this.addStandardInventorySlots(inventory, 8, 84);
		this.addDataSlots(data);
	}

	/** The server's menu for the coal winch at {@code drum}. */
	public static CoalWinchMenu server(int containerId, Inventory inventory, ServerLevel level, BlockPos drum) {
		return new CoalWinchMenu(containerId, inventory, drum.immutable(), new LineCoal(level, drum.immutable()), new ContainerData() {
			@Override
			public int get(int index) {
				CableLines.Line line = CableLines.line(level, drum);
				return index == DATA_BURN ? line == null ? 0 : line.burn : Tuning.getInt("winch.burn_ticks_per_coal");
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

	/** The flame's height, 0..1: what is left of the coal alight. */
	public float litProgress() {
		int total = this.data.get(DATA_BURN_TOTAL);
		return total <= 0 ? 0.0F : Math.min(1.0F, (float) this.data.get(DATA_BURN) / total);
	}

	public boolean isLit() {
		return this.data.get(DATA_BURN) > 0;
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.drum == null) {
			return true;
		}
		return player.level().getBlockEntity(this.drum) instanceof CableDrumBlockEntity winch && winch.powered()
			&& player.distanceToSqr(Vec3.atCenterOf(this.drum)) < 8.0 * 8.0;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex < 0 || slotIndex >= this.slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = this.slots.get(slotIndex);
		ItemStack stack = slot.getItem();
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack original = stack.copy();
		if (slotIndex == 0) {
			if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (CoalWinch.isCoal(stack)) {
			if (!this.moveItemStackTo(stack, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
	}

	/**
	 * The winch's coal as a one-slot container: a stack of coal kept in step with the line's count (read
	 * from it whenever the count has changed elsewhere, written back to it whenever the slot changes, as
	 * vanilla's menus grow and shrink the stack they are given in place).
	 */
	private static final class LineCoal implements Container {
		private final ServerLevel level;
		private final BlockPos drum;
		private ItemStack stack = ItemStack.EMPTY;

		LineCoal(ServerLevel level, BlockPos drum) {
			this.level = level;
			this.drum = drum;
		}

		private CableLines.@Nullable Line line() {
			return CableLines.line(this.level, this.drum);
		}

		/** The stack, refreshed from the line if the coal changed there (burnt, or put in by hand). */
		private ItemStack stack() {
			CableLines.Line line = this.line();
			int coal = line == null ? 0 : line.coal;
			if (this.stack.getCount() != coal || coal > 0 && !this.stack.is(Items.COAL)) {
				this.stack = coal <= 0 ? ItemStack.EMPTY : new ItemStack(Items.COAL, coal);
			}
			return this.stack;
		}

		private void store() {
			CableLines.Line line = this.line();
			if (line == null) {
				return;
			}
			int coal = this.stack.is(Items.COAL) ? Math.min(this.stack.getCount(), Tuning.getInt("winch.coal_capacity")) : 0;
			if (line.coal != coal) {
				line.coal = coal;
				CableLines.get(this.level).changed();
				if (this.level.getBlockEntity(this.drum) instanceof CableDrumBlockEntity winch) {
					winch.copyDrive(line);
				}
			}
		}

		@Override
		public int getContainerSize() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return this.stack().isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			return this.stack();
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			ItemStack taken = this.stack().split(count);
			this.store();
			return taken;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			ItemStack taken = this.stack();
			this.stack = ItemStack.EMPTY;
			this.store();
			return taken;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			// Only coal is ever put here (the slot refuses anything else).
			this.stack = stack;
			this.store();
		}

		@Override
		public int getMaxStackSize() {
			return Tuning.getInt("winch.coal_capacity");
		}

		@Override
		public void setChanged() {
			this.store();
		}

		@Override
		public boolean stillValid(Player player) {
			return this.line() != null;
		}

		@Override
		public void clearContent() {
			this.stack = ItemStack.EMPTY;
			this.store();
		}
	}
}
