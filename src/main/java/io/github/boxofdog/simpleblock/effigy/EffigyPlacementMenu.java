package io.github.boxofdog.simpleblock.effigy;

import org.jspecify.annotations.Nullable;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * M12: the confirmation screen before an effigy is placed ("Are you sure? Monster Effigies cannot be
 * picked back up after being destroyed.", Place / Cancel). The spot and the angle are fixed when the
 * screen opens ({@link EffigyItem#useOn}); the screen has no slots, its two buttons are menu buttons.
 *
 * <p>"Place" is checked again on the server, then: the player still holds that effigy in that hand,
 * is still within reach, may build there, and both blocks are still free. If all hold, the effigy is
 * placed at that spot and angle and one item is used up, once; if not, nothing is placed or used. Either
 * way the screen closes, so a second click (or a click arriving after the screen closed for distance,
 * a changed held item or a disconnect) does nothing. "Cancel" closes it and changes nothing.
 */
public class EffigyPlacementMenu extends AbstractContainerMenu {
	public static final int BUTTON_PLACE = 0;
	public static final int BUTTON_CANCEL = 1;

	private final DataSlot type = DataSlot.standalone();
	private final @Nullable BlockPos lower;
	private final float yaw;
	private final InteractionHand hand;
	private boolean done;

	/** Counts of what the buttons did, for the game tests. */
	public static long placed;
	public static long refused;

	/** The client's menu. */
	public EffigyPlacementMenu(int containerId, Inventory inventory) {
		this(containerId, EffigyType.ZOMBIE, null, 0.0F, InteractionHand.MAIN_HAND);
	}

	private EffigyPlacementMenu(int containerId, EffigyType type, @Nullable BlockPos lower, float yaw, InteractionHand hand) {
		super(EffigyContent.PLACEMENT_MENU, containerId);
		this.type.set(type.ordinal());
		this.lower = lower;
		this.yaw = yaw;
		this.hand = hand;
		this.addDataSlot(this.type);
	}

	/** The server's menu, for an effigy of {@code type} at {@code lower} facing {@code yaw}, held in {@code hand}. */
	public static EffigyPlacementMenu server(int containerId, Inventory inventory, EffigyType type, BlockPos lower, float yaw, InteractionHand hand) {
		return new EffigyPlacementMenu(containerId, type, lower.immutable(), yaw, hand);
	}

	/** Which effigy this screen is about (synced to the client for its picture). */
	public EffigyType type() {
		EffigyType[] types = EffigyType.values();
		return types[Math.floorMod(this.type.get(), types.length)];
	}

	/** Whether an effigy can stand with its lower half at {@code lower}: both blocks inside the world, replaceable and clear of entities. */
	public static boolean hasRoom(Level level, BlockPos lower, Player player) {
		BlockPos upper = lower.above();
		if (!level.isInWorldBounds(lower) || !level.isInWorldBounds(upper)) {
			return false;
		}
		if (!level.getBlockState(lower).canBeReplaced() || !level.getBlockState(upper).canBeReplaced()) {
			return false;
		}
		BlockState any = EffigyContent.effigyBlock(EffigyType.ZOMBIE).defaultBlockState();
		CollisionContext collision = CollisionContext.placementContext(player);
		return level.isUnobstructed(any, lower, collision) && level.isUnobstructed(any, upper, collision);
	}

	private boolean holdsIt(Player player) {
		ItemStack held = player.getItemInHand(this.hand);
		return !held.isEmpty() && held.getItem() instanceof EffigyItem item && item.type() == this.type();
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (this.done || this.lower == null || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (buttonId == BUTTON_CANCEL) {
			this.done = true;
			serverPlayer.closeContainer();
			return true;
		}
		if (buttonId != BUTTON_PLACE) {
			return false;
		}
		this.done = true;
		Level level = serverPlayer.level();
		BlockPos lower = this.lower;
		ItemStack held = player.getItemInHand(this.hand);
		boolean ok = serverPlayer.isAlive() && this.holdsIt(player) && serverPlayer.isWithinBlockInteractionRange(lower, 1.0)
			&& level.mayInteract(serverPlayer, lower) && serverPlayer.mayUseItemAt(lower, Direction.UP, held) && hasRoom(level, lower, player);
		if (ok) {
			place(level, lower, this.type(), this.yaw, serverPlayer);
			CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, lower, held);
			held.consume(1, player);
			placed++;
		} else {
			refused++;
		}
		serverPlayer.closeContainer();
		return true;
	}

	/** Places an effigy of {@code type} with its lower half at {@code lower}, facing {@code yaw} (also used by the tests and commands). */
	public static void place(Level level, BlockPos lower, EffigyType type, float yaw, @Nullable Player player) {
		EffigyBlock block = EffigyContent.effigyBlock(type);
		BlockState lowerState = block.defaultBlockState().setValue(EffigyBlock.HALF, DoubleBlockHalf.LOWER);
		level.setBlock(lower, lowerState, Block.UPDATE_ALL);
		level.setBlock(lower.above(), block.defaultBlockState().setValue(EffigyBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
		if (level.getBlockEntity(lower) instanceof EffigyBlockEntity effigy) {
			effigy.setYaw(yaw);
			level.sendBlockUpdated(lower, lowerState, lowerState, Block.UPDATE_ALL);
		}
		level.playSound(null, lower, SoundEvents.BONE_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.gameEvent(GameEvent.BLOCK_PLACE, lower, GameEvent.Context.of(player, lowerState));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.lower == null) {
			return true;
		}
		return !this.done && player.isAlive() && this.holdsIt(player) && player.isWithinBlockInteractionRange(this.lower, 1.0);
	}
}
