package io.github.boxofdog.simpleblock.metallurgy;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.boxofdog.simpleblock.feature.HeavyLoad;
import io.github.boxofdog.simpleblock.tuning.Tuning;
import io.github.boxofdog.simpleblock.feature.Feedback;

/**
 * M08: the furnace's screen, laid out like a vanilla furnace: the ore reservoir where the input goes,
 * the fuel slot under it, the mold where the result goes, the flame and the casting arrow between.
 *
 * <p>The fuel and mold are ordinary slots (the mold only gives, and taking cast blocks is limited by
 * the heavy-load cap like any other intake). The reservoir holds up to 516 and so is not a vanilla
 * slot (see {@code skip/SkipMenu}): its item and count are synced data and the screen's clicks on it
 * are menu buttons. Left-click with an empty cursor takes a stack (within the cap), right-click half;
 * holding raw metal, left-click puts all of it that fits in, right-click one; shift-click takes a stack
 * into the inventory. Shift-clicking raw metal or fuel in the inventory puts it in.
 */
public class BlastFurnaceMenu extends AbstractContainerMenu {
	public static final int BUTTON_LEFT = 0;
	public static final int BUTTON_RIGHT = 1;
	public static final int BUTTON_SHIFT = 2;
	/** The vanilla furnace screen's positions: input (the reservoir), fuel and result (the mold). */
	public static final int RESERVOIR_X = 56;
	public static final int RESERVOIR_Y = 17;
	private static final int DATA_ORE = 0;
	private static final int DATA_COUNT = 1;
	private static final int DATA_BURN = 2;
	private static final int DATA_BURN_TOTAL = 3;
	private static final int DATA_PROGRESS = 4;
	private static final int DATA_CAST_TICKS = 5;
	private static final int DATA_SIZE = 6;
	private static final int SLOTS = 2;

	private final @Nullable BlastFurnaceBlockEntity furnace;
	private final ContainerData data;

	/** The client's menu. */
	public BlastFurnaceMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null, new SimpleContainer(SLOTS), new SimpleContainerData(DATA_SIZE));
	}

	private BlastFurnaceMenu(int containerId, Inventory inventory, @Nullable BlastFurnaceBlockEntity furnace, Container items, ContainerData data) {
		super(MetallurgyContent.BLAST_FURNACE_MENU, containerId);
		this.furnace = furnace;
		this.data = data;
		this.addSlot(new Slot(items, BlastFurnaceBlockEntity.FUEL_SLOT, 56, 53) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return BlastFurnaceBlockEntity.isFuel(stack);
			}
		});
		this.addSlot(new Slot(items, BlastFurnaceBlockEntity.MOLD_SLOT, 116, 35) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});
		this.addStandardInventorySlots(inventory, 8, 84);
		this.addDataSlots(data);
	}

	/** The server's menu for {@code furnace}. */
	public static BlastFurnaceMenu server(int containerId, Inventory inventory, BlastFurnaceBlockEntity furnace) {
		return new BlastFurnaceMenu(containerId, inventory, furnace, furnace.items, new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case DATA_ORE -> BuiltInRegistries.ITEM.getId(furnace.ore());
					case DATA_COUNT -> furnace.count();
					case DATA_BURN -> furnace.burn();
					case DATA_BURN_TOTAL -> furnace.burnTotal();
					case DATA_PROGRESS -> furnace.progress();
					default -> Tuning.getInt("blast_furnace.cast_ticks");
				};
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return DATA_SIZE;
			}
		});
	}

	/** The raw metal in the reservoir, {@link Items#AIR} when empty (client: as last synced). */
	public Item ore() {
		return this.count() > 0 ? BuiltInRegistries.ITEM.byId(this.data.get(DATA_ORE)) : Items.AIR;
	}

	public int count() {
		return this.data.get(DATA_COUNT);
	}

	/** The flame's height, 0..1: what is left of the fuel block burning. */
	public float litProgress() {
		int total = this.data.get(DATA_BURN_TOTAL);
		return total <= 0 ? 0.0F : Math.min(1.0F, (float) this.data.get(DATA_BURN) / total);
	}

	public boolean isLit() {
		return this.data.get(DATA_BURN) > 0;
	}

	/** The casting arrow, 0..1: progress towards the next block. */
	public float castProgress() {
		int total = this.data.get(DATA_CAST_TICKS);
		return total <= 0 ? 0.0F : Math.min(1.0F, (float) this.data.get(DATA_PROGRESS) / total);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.furnace == null || !this.furnace.isRemoved()
			&& player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.furnace.getBlockPos())) < 8.0 * 8.0;
	}

	private static Component message(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("simple_block.blast_furnace." + key, fallback, args);
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (this.furnace == null || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		BlastFurnaceBlockEntity furnace = this.furnace;
		ItemStack carried = this.getCarried();
		switch (buttonId) {
			case BUTTON_LEFT, BUTTON_RIGHT -> {
				boolean right = buttonId == BUTTON_RIGHT;
				if (carried.isEmpty()) {
					int stack = Math.min(furnace.count(), furnace.ore().getDefaultMaxStackSize());
					int wanted = right ? (stack + 1) / 2 : stack;
					int allowed = furnace.count() <= 0 ? 0 : HeavyLoad.allowedCount(player, new ItemStack(furnace.ore(), wanted));
					if (allowed > 0) {
						this.setCarried(furnace.withdraw(allowed));
					} else if (furnace.count() > 0) {
						Feedback.quiet(serverPlayer, message("too_heavy", "Too heavy: you can't carry any more %s",
							new ItemStack(furnace.ore()).getItemName()));
					}
				} else if (furnace.room(carried.getItem()) > 0) {
					carried.shrink(furnace.deposit(carried.getItem(), right ? 1 : carried.getCount()));
					this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
				} else {
					Feedback.quiet(serverPlayer, refusal(furnace, carried.getItem()));
				}
			}
			case BUTTON_SHIFT -> {
				int stack = Math.min(furnace.count(), furnace.ore().getDefaultMaxStackSize());
				int allowed = stack <= 0 ? 0 : HeavyLoad.allowedCount(player, new ItemStack(furnace.ore(), stack));
				if (allowed > 0) {
					ItemStack taken = furnace.withdraw(allowed);
					player.getInventory().add(taken);
					if (!taken.isEmpty()) {
						furnace.deposit(taken.getItem(), taken.getCount());
					}
				} else if (stack > 0) {
					Feedback.quiet(serverPlayer, message("too_heavy", "Too heavy: you can't carry any more %s",
						new ItemStack(furnace.ore()).getItemName()));
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/** Why the reservoir will not take {@code item}. */
	static Component refusal(BlastFurnaceBlockEntity furnace, Item item) {
		if (!BlastFurnaceBlockEntity.isOre(item)) {
			return message("not_ore", "The furnace smelts raw iron, copper or gold");
		}
		if (furnace.count() > 0 && furnace.ore() != item) {
			return message("other_ore", "The furnace holds %s: it takes no other ore until that is used or taken out",
				new ItemStack(furnace.ore()).getItemName());
		}
		return message("full", "The furnace is full: %s / %s", furnace.count(), BlastFurnaceBlockEntity.RESERVOIR_CAPACITY);
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
		if (slotIndex < SLOTS) {
			// Fuel or cast blocks to the player's inventory.
			if (!this.moveItemStackTo(stack, SLOTS, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (this.furnace != null && this.furnace.room(stack.getItem()) > 0) {
			// Raw metal into the reservoir: as much as it takes; nothing else to move after.
			stack.shrink(this.furnace.deposit(stack.getItem(), stack.getCount()));
			slot.setChanged();
			return ItemStack.EMPTY;
		} else if (BlastFurnaceBlockEntity.isFuel(stack)) {
			if (!this.moveItemStackTo(stack, BlastFurnaceBlockEntity.FUEL_SLOT, BlastFurnaceBlockEntity.FUEL_SLOT + 1, false)) {
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
}
