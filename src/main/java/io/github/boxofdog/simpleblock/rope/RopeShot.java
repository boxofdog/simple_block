package io.github.boxofdog.simpleblock.rope;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * M13: a Spelunker's Rope shot from a bow ({@link BowRope}). It flies as an arrow does (same drag and
 * gravity) but leaves the bow slower, so it falls short of an arrow loosed with the same draw. It does
 * no damage. Hitting the bottom or a side of a solid block where a Spelunker's Rope could be fixed by
 * hand, it hangs there exactly as one fixed by hand. Anything else (the top of a block, no room, a mob,
 * the world's bottom, or flying on into unloaded ground) gives the rope back to the shooter, or drops it
 * where it ended if the shooter is gone: one rope in, one rope out.
 */
public class RopeShot extends ThrowableItemProjectile {
	/** Ticks a shot may fly before it is given back (a shot into unloaded ground stops there and would wait). */
	public static final int MAX_FLIGHT = 200;

	private boolean done;

	public RopeShot(EntityType<? extends RopeShot> type, Level level) {
		super(type, level);
	}

	public RopeShot(Level level, Player shooter, ItemStack rope) {
		super(RopeContent.ROPE_SHOT, shooter, level, rope);
	}

	@Override
	protected Item getDefaultItem() {
		return RopeContent.SPELUNKER_ROPE;
	}

	/** An arrow's gravity (thrown items fall at 0.03). */
	@Override
	protected double getDefaultGravity() {
		return 0.05;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && !this.isRemoved() && this.tickCount > MAX_FLIGHT) {
			this.giveBack();
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!(this.level() instanceof ServerLevel level) || this.done) {
			return;
		}
		Player shooter = this.getOwner() instanceof Player player ? player : null;
		if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
			&& SpelunkerRopes.hang(level, block.getBlockPos(), block.getDirection(), shooter)) {
			this.done = true;
			this.discard();
			return;
		}
		if (hit.getType() == HitResult.Type.ENTITY && shooter != null) {
			SpelunkerRopes.tell(shooter, SpelunkerRopes.message("no_room", "No room to hang a rope here"));
		}
		this.giveBack();
	}

	@Override
	protected void onBelowWorld() {
		this.giveBack();
		super.onBelowWorld();
	}

	/** The rope back to the shooter's inventory (dropped at the shooter's feet if it is full), or here if the shooter is gone. */
	private void giveBack() {
		if (this.done || !(this.level() instanceof ServerLevel level)) {
			return;
		}
		this.done = true;
		ItemStack rope = this.getItem().copy();
		if (this.getOwner() instanceof Player player && player.isAlive() && !player.isRemoved()) {
			player.getInventory().placeItemBackInInventory(rope, net.minecraft.util.Prediction.SERVER_ONLY);
		} else if (level.isInWorldBounds(this.blockPosition())) {
			Block.popResource(level, this.blockPosition(), rope);
		}
		this.discard();
	}
}
