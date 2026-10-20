package io.github.boxofdog.simpleblock.metallurgy;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M08: the furnace's inventories and its work. Three places, each kept exactly:
 * <ul>
 *   <li>the <b>ore reservoir</b>: up to {@value #RESERVOIR_CAPACITY} raw iron, copper or gold, one
 *       kind at a time, held as an item and a count (like the M07 skip, so no oversized stack ever
 *       exists);</li>
 *   <li>the <b>fuel</b> slot: one ordinary stack of Blocks of Coal, Blocks of Charcoal or Dried Kelp
 *       Blocks ({@link #isFuel});</li>
 *   <li>the <b>mold</b> slot: one ordinary stack (64) of the cast metal blocks, one metal at a time.</li>
 * </ul>
 * While there are nine ore in the reservoir and the mold takes their block, the furnace burns fuel and
 * casts: {@code blast_furnace.cast_ticks} of burning turn nine raw ore into one block of the metal.
 * When it cannot cast (fewer than nine, the mold full or holding another metal, no fuel) it waits:
 * the fire is banked, so neither the casting progress nor the fuel block already burning is lost.
 * The remainder under nine stays in the reservoir. Everything is saved with the furnace.
 */
public class BlastFurnaceBlockEntity extends BlockEntity {
	/** Owner-settled reservoir size (M08 brief). */
	public static final int RESERVOIR_CAPACITY = 516;
	public static final int ORE_PER_BLOCK = 9;
	public static final int FUEL_SLOT = 0;
	public static final int MOLD_SLOT = 1;
	/** M08 playtest: eighths of the mold the molten metal is drawn in. */
	static final int FILL_STEPS = 8;
	/** What each raw metal casts into. */
	public static final Map<Item, Item> CASTS = Map.of(Items.RAW_IRON, Items.IRON_BLOCK, Items.RAW_COPPER,
		Items.COPPER_BLOCK.weathering().unaffected(), Items.RAW_GOLD, Items.GOLD_BLOCK);

	private Item ore = Items.AIR;
	private int count;
	/** Burning ticks left in the current fuel block, and what it started with (for the flame). */
	private int burn;
	private int burnTotal;
	/** Ticks of burning towards the next cast. */
	private int progress;
	/** Ticks the finished cast is still shown solid, before the next fill shows (not saved). */
	private int hold;
	/** Set by a creative player breaking the furnace: it then drops no furnace item. */
	boolean brokenInCreative;
	final SimpleContainer items = new SimpleContainer(2) {
		@Override
		public void setChanged() {
			super.setChanged();
			BlastFurnaceBlockEntity.this.setChanged();
		}
	};
	/** The freight transfers in progress, by rail (not saved: they start over if there is still cargo to move). */
	final BlastFurnaceFreight.Intakes intakes = new BlastFurnaceFreight.Intakes();

	public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(MetallurgyContent.BLAST_FURNACE_ENTITY, pos, state);
	}

	public static boolean isOre(Item item) {
		return CASTS.containsKey(item);
	}

	public static boolean isFuel(ItemStack stack) {
		return stack.is(MetallurgyContent.CHARCOAL_BLOCK_ITEM) || stack.is(Items.COAL_BLOCK) || stack.is(Items.DRIED_KELP_BLOCK);
	}

	/** Burning ticks one fuel block gives the furnace. */
	public static int burnTicks(ItemStack fuel) {
		if (fuel.is(Items.DRIED_KELP_BLOCK)) {
			return Tuning.getInt("blast_furnace.kelp_block_burn_ticks");
		}
		return isFuel(fuel) ? Tuning.getInt("blast_furnace.coal_block_burn_ticks") : 0;
	}

	/** The raw ore in the reservoir, {@link Items#AIR} when empty. */
	public Item ore() {
		return this.count > 0 ? this.ore : Items.AIR;
	}

	public int count() {
		return this.count;
	}

	public int burn() {
		return this.burn;
	}

	public int burnTotal() {
		return this.burnTotal;
	}

	public int progress() {
		return this.progress;
	}

	/** The fuel and mold slots ({@link #FUEL_SLOT}, {@link #MOLD_SLOT}). */
	public SimpleContainer items() {
		return this.items;
	}

	public ItemStack fuel() {
		return this.items.getItem(FUEL_SLOT);
	}

	public ItemStack mold() {
		return this.items.getItem(MOLD_SLOT);
	}

	/** Sets the reservoir (tests and transfers); a count of 0 empties it. */
	public void setOre(Item item, int count) {
		this.count = Math.max(0, Math.min(RESERVOIR_CAPACITY, count));
		this.ore = this.count > 0 ? item : Items.AIR;
		this.setChanged();
	}

	/** How many of {@code item} the reservoir could take now: none if it is not raw metal, another one is in, or it is full. */
	public int room(Item item) {
		if (!isOre(item) || this.count > 0 && item != this.ore) {
			return 0;
		}
		return RESERVOIR_CAPACITY - this.count;
	}

	/** Puts up to {@code n} of {@code item} in; returns how many went in. */
	public int deposit(Item item, int n) {
		int moved = Math.min(n, this.room(item));
		if (moved > 0) {
			this.setOre(item, this.count + moved);
		}
		return moved;
	}

	/** Takes up to {@code max} out as one ordinary stack (never more than the item's stack size). */
	public ItemStack withdraw(int max) {
		Item item = this.ore();
		int n = Math.min(Math.min(max, this.count), item.getDefaultMaxStackSize());
		if (n <= 0) {
			return ItemStack.EMPTY;
		}
		this.setOre(item, this.count - n);
		return new ItemStack(item, n);
	}

	/** The block the next nine ore would cast, if the mold has room for it. */
	private boolean canCast() {
		if (this.count < ORE_PER_BLOCK) {
			return false;
		}
		ItemStack mold = this.mold();
		Item cast = CASTS.get(this.ore);
		return mold.isEmpty() || mold.is(cast) && mold.getCount() < mold.getMaxStackSize();
	}

	/** For tests: sets the burning time left in the current fuel block. */
	public void setBurnForTest(int burn) {
		this.burn = burn;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BlastFurnaceBlockEntity furnace) {
		if (level instanceof ServerLevel serverLevel) {
			BlastFurnaceFreight.tick(serverLevel, furnace);
		}
		boolean working = furnace.canCast();
		if (working && furnace.burn <= 0) {
			ItemStack fuel = furnace.fuel();
			if (isFuel(fuel)) {
				furnace.burn = furnace.burnTotal = burnTicks(fuel);
				fuel.shrink(1);
				furnace.items.setChanged();
			}
		}
		if (working && furnace.burn > 0) {
			furnace.burn--;
			furnace.progress++;
			if (furnace.progress >= Tuning.getInt("blast_furnace.cast_ticks")) {
				furnace.progress = 0;
				Item cast = CASTS.get(furnace.ore);
				furnace.setOre(furnace.ore, furnace.count - ORE_PER_BLOCK);
				ItemStack mold = furnace.mold();
				if (mold.isEmpty()) {
					furnace.items.setItem(MOLD_SLOT, new ItemStack(cast));
				} else {
					mold.grow(1);
					furnace.items.setChanged();
				}
				furnace.hold = Tuning.getInt("blast_furnace.cast_hold_ticks");
				level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.8F, 0.7F);
			}
			furnace.setChanged();
		}
		if (furnace.hold > 0) {
			furnace.hold--;
		}
		boolean lit = working && furnace.burn > 0;
		// M08 playtest: the mold fills with the cast's real progress (shown from just after the finished
		// block's moment, at once caught up); metal runs from the tap down the channel while it fills.
		int fill = furnace.hold > 0 || furnace.progress <= 0 ? 0
			: Math.min(FILL_STEPS, (int) Math.ceil(furnace.progress * (double) FILL_STEPS / Tuning.getInt("blast_furnace.cast_ticks")));
		boolean pouring = lit && fill > 0;
		if (state.getValue(BlastFurnaceBlock.LIT) != lit || state.getValue(BlastFurnaceBlock.POURING) != pouring) {
			level.setBlock(pos, state.setValue(BlastFurnaceBlock.LIT, lit).setValue(BlastFurnaceBlock.POURING, pouring), Block.UPDATE_ALL);
		}
		// The channel shows the metal running, the mold the molten metal rising and then the cast block.
		Direction facing = state.getValue(BlastFurnaceBlock.FACING);
		BlockPos channelPos = BlastFurnaceStructure.channel(pos, facing);
		BlockState channel = level.getBlockState(channelPos);
		if (channel.is(MetallurgyContent.BLAST_FURNACE_CHANNEL) && channel.getValue(BlastFurnaceChannelBlock.POURING) != pouring) {
			level.setBlock(channelPos, channel.setValue(BlastFurnaceChannelBlock.POURING, pouring), Block.UPDATE_CLIENTS);
		}
		BlockPos moldPos = BlastFurnaceStructure.mold(pos, facing);
		BlockState mold = level.getBlockState(moldPos);
		BlastFurnaceMoldBlock.Content content = BlastFurnaceMoldBlock.Content.of(furnace.mold());
		if (mold.is(MetallurgyContent.BLAST_FURNACE_MOLD) && (mold.getValue(BlastFurnaceMoldBlock.CONTENT) != content
			|| mold.getValue(BlastFurnaceMoldBlock.FILL) != fill || mold.getValue(BlastFurnaceMoldBlock.POURING) != pouring)) {
			level.setBlock(moldPos, mold.setValue(BlastFurnaceMoldBlock.CONTENT, content).setValue(BlastFurnaceMoldBlock.FILL, fill)
				.setValue(BlastFurnaceMoldBlock.POURING, pouring), Block.UPDATE_CLIENTS);
		}
	}

	/** The furnace is going (broken, not unloaded): everything in it drops as ordinary stacks. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level == null) {
			return;
		}
		while (this.count > 0) {
			Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.withdraw(Integer.MAX_VALUE));
		}
		Containers.dropContents(this.level, pos, this.items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (this.count > 0) {
			output.store("ore", Item.CODEC, this.ore.builtInRegistryHolder());
			output.putInt("count", this.count);
		}
		output.putInt("burn", this.burn);
		output.putInt("burn_total", this.burnTotal);
		output.putInt("progress", this.progress);
		net.minecraft.world.ContainerHelper.saveAllItems(output, this.items.getItems());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		Item item = input.read("ore", Item.CODEC).map(holder -> holder.value()).orElse(Items.AIR);
		this.count = isOre(item) ? Math.max(0, Math.min(RESERVOIR_CAPACITY, input.getIntOr("count", 0))) : 0;
		this.ore = this.count > 0 ? item : Items.AIR;
		this.burn = Math.max(0, input.getIntOr("burn", 0));
		this.burnTotal = Math.max(this.burn, input.getIntOr("burn_total", 0));
		this.progress = Math.max(0, input.getIntOr("progress", 0));
		this.items.getItems().replaceAll(stack -> ItemStack.EMPTY);
		net.minecraft.world.ContainerHelper.loadAllItems(input, this.items.getItems());
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		this.saveAdditional(output);
		return output.buildResult();
	}

	@Override
	public String toString() {
		return "BlastFurnace[" + BuiltInRegistries.ITEM.getKey(this.ore()) + " x" + this.count + ", fuel " + this.fuel() + ", mold " + this.mold()
			+ " at " + this.getBlockPos() + "]";
	}
}
