package io.github.boxofdog.simpleblock.coot;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M14 the Old Coot's mindless tunnelling. Now and then, when he is calm, he looks along a random level
 * direction from where he stands for the first solid block at his feet; if that is ordinary stone or
 * deepslate inside his working territory he digs a short passage into it, two blocks high and a few
 * blocks deep, a block at a time: walk up to it, swing the pickaxe (each blow a real impact sound and
 * crack), and after enough blows the block is gone, with no drop and no experience. He never digs down,
 * up, or anything but those two blocks, and leaves alone any block next to water or lava or under
 * something that would fall. At the end of a passage he now and then puts up a real torch in the dark
 * (a few per home, for all time).
 */
public final class CootMineGoal extends Goal {
	/** Blocks he looks along for a wall to start on. */
	private static final int LOOK = 6;
	/** How far he reaches with the pick, from his feet to the middle of the block. */
	private static final double REACH = 3.0;
	/** Ticks between blows: a slow, steady old man's rhythm. */
	private static final int BLOW = 12;
	/** Blows to break stone; deepslate, being harder, takes half as many again. */
	private static final int STONE_BLOWS = 6;
	private static final float TORCH_CHANCE = 0.2F;
	private static final int TORCH_DARK = 4;

	private final OldCoot coot;
	private Direction direction;
	/** The foot block of the passage slice being dug. */
	private BlockPos slice;
	private int slicesLeft;
	private BlockPos block;
	private int blows;
	private int needed;
	private int nextBlow;
	private int stuck;

	public CootMineGoal(OldCoot coot) {
		this.coot = coot;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	/** The only blocks he mines: ordinary stone and ordinary deepslate. */
	public static boolean mineable(BlockState state) {
		return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE);
	}

	@Override
	public boolean canUse() {
		if (this.coot.getTarget() != null || this.coot.mineCooldown > 0 || !(this.coot.level() instanceof ServerLevel)) {
			return false;
		}
		this.coot.mineCooldown = OldCoot.MINE_LOOK_PAUSE;
		BlockPos feet = this.coot.blockPosition();
		Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(this.coot.getRandom());
		for (int turn = 0; turn < 4; turn++) {
			Direction dir = first;
			for (int i = 0; i < turn; i++) {
				dir = dir.getClockWise();
			}
			for (int step = 1; step <= LOOK; step++) {
				BlockPos at = feet.relative(dir, step);
				if (!this.coot.level().getBlockState(at).isAir() || !this.coot.level().getBlockState(at.above()).isAir()) {
					if (this.diggable(at) || this.diggable(at.above())) {
						this.direction = dir;
						this.slice = at;
						this.slicesLeft = 2 + this.coot.getRandom().nextInt(4);
						this.coot.passages++;
						return true;
					}
					break;
				}
			}
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		return this.slice != null && this.coot.getTarget() == null;
	}

	@Override
	public void start() {
		this.block = null;
		this.stuck = 0;
	}

	@Override
	public void stop() {
		if (this.block != null) {
			this.coot.level().destroyBlockProgress(this.coot.getId(), this.block, -1);
		}
		this.slice = null;
		this.block = null;
		this.coot.getNavigation().stop();
		this.coot.mineCooldown = OldCoot.MINE_PAUSE_MIN + this.coot.getRandom().nextInt(OldCoot.MINE_PAUSE_SPREAD);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private boolean diggable(BlockPos pos) {
		return diggable((ServerLevel) this.coot.level(), this.coot, pos);
	}

	/** Mineable, in his territory, nothing above to fall on him and no water or lava beside it to flood in. */
	public static boolean diggable(ServerLevel level, OldCoot coot, BlockPos pos) {
		if (!mineable(level.getBlockState(pos)) || !coot.inTerritory(pos) || !level.isLoaded(pos)) {
			return false;
		}
		if (level.getBlockState(pos.above()).getBlock() instanceof FallingBlock) {
			return false;
		}
		for (Direction side : Direction.values()) {
			if (!level.getFluidState(pos.relative(side)).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void tick() {
		ServerLevel level = (ServerLevel) this.coot.level();
		if (this.slice == null) {
			return;
		}
		if (this.block == null) {
			this.block = this.diggable(this.slice.above()) ? this.slice.above() : this.diggable(this.slice) ? this.slice : null;
			if (this.block == null) {
				this.nextSlice(level);
				return;
			}
			this.blows = 0;
			this.needed = level.getBlockState(this.block).is(Blocks.DEEPSLATE) ? STONE_BLOWS * 3 / 2 : STONE_BLOWS;
			this.nextBlow = BLOW;
		}
		this.coot.getLookControl().setLookAt(this.block.getX() + 0.5, this.block.getY() + 0.5, this.block.getZ() + 0.5);
		// Stand in the passage just short of the slice being dug.
		BlockPos stand = this.slice.relative(this.direction.getOpposite());
		double reach = this.coot.distanceToSqr(this.block.getX() + 0.5, this.block.getY() + 0.5, this.block.getZ() + 0.5);
		if (reach > REACH * REACH) {
			if (this.coot.getNavigation().isDone()) {
				// Right into the passage (the usual "within a block" would leave him short of the face).
				this.coot.getNavigation().moveTo(this.coot.getNavigation().createPath(stand, 0), 1.0);
			}
			if (++this.stuck > 200) {
				this.slice = null;
			}
			return;
		}
		this.coot.getNavigation().stop();
		if (--this.nextBlow > 0) {
			return;
		}
		this.nextBlow = BLOW;
		BlockState state = level.getBlockState(this.block);
		if (!mineable(state)) {
			level.destroyBlockProgress(this.coot.getId(), this.block, -1);
			this.block = null;
			return;
		}
		// The blow: the arm comes down, and the pick rings on the rock.
		this.coot.swingForAttack(InteractionHand.MAIN_HAND);
		SoundType sound = state.getSoundType();
		level.playSound(null, this.block, sound.getHitSound(), SoundSource.NEUTRAL, OldCoot.BLOW_VOLUME, sound.getPitch() * 0.8F);
		this.blows++;
		if (this.blows < this.needed) {
			level.destroyBlockProgress(this.coot.getId(), this.block, this.blows * 10 / this.needed);
			return;
		}
		level.destroyBlockProgress(this.coot.getId(), this.block, -1);
		level.destroyBlock(this.block, false, this.coot);
		this.coot.blocksMined++;
		this.block = null;
	}

	private void nextSlice(ServerLevel level) {
		BlockPos done = this.slice;
		if (--this.slicesLeft <= 0 || !level.getBlockState(done).isAir() || !level.getBlockState(done.above()).isAir()) {
			this.maybeTorch(level, done);
			this.slice = null;
			return;
		}
		BlockPos next = done.relative(this.direction);
		if (!this.diggable(next) && !this.diggable(next.above())) {
			this.maybeTorch(level, done);
			this.slice = null;
			return;
		}
		this.slice = next;
		this.stuck = 0;
	}

	/** Now and then a real torch on the floor where a passage ends. */
	private void maybeTorch(ServerLevel level, BlockPos at) {
		if (this.coot.getRandom().nextFloat() < TORCH_CHANCE) {
			placeTorch(level, this.coot, at);
		}
	}

	/**
	 * A torch on the floor here if the spot is empty and dark, a torch can stand there, and he may put one up
	 * (not too soon after the last, and his home's lifetime allowance not spent). Returns whether he did.
	 */
	public static boolean placeTorch(ServerLevel level, OldCoot coot, BlockPos at) {
		if (!level.getBlockState(at).isAir() || level.getBrightness(LightLayer.BLOCK, at) >= TORCH_DARK
			|| !Blocks.TORCH.defaultBlockState().canSurvive(level, at) || !coot.mayPlaceTorch()) {
			return false;
		}
		level.setBlockAndUpdate(at, Blocks.TORCH.defaultBlockState());
		coot.torchPlaced();
		return true;
	}
}
