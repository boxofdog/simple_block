package io.github.boxofdog.simpleblock.skip;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import io.github.boxofdog.simpleblock.cable.CableHoists;
import io.github.boxofdog.simpleblock.cable.CableHook;
import io.github.boxofdog.simpleblock.cable.CableRoutes;
import io.github.boxofdog.simpleblock.feature.MinecartCargo;
import io.github.boxofdog.simpleblock.tuning.Tuning;

/**
 * M07 upright ore skip: a vertical carrier hoisted on a drum's cable (owner's brief, sections 1 to 4).
 *
 * <ul>
 *   <li><b>Shape:</b> one block wide and deep; three blocks of body (the open chute at the bottom, the
 *       two-block ore bin above) and the bridle as a fourth block on top. All four blocks collide:
 *       the hitbox is 1 x 4 x 1 ({@link SkipContent#ORE_SKIP}). Its position is the bottom middle of
 *       its column. Seen from the front ({@link #facing()}, the side with the chute's mouth and the
 *       station) it looks like a minecart on its side, wider than its block: the cell on each side
 *       ({@link #lateral()}) must be clear wherever it goes, though nothing collides there.</li>
 *   <li><b>Cargo:</b> one slot of up to 512 of one commodity ({@link SkipCargo}), opened with an empty
 *       hand ({@link SkipMenu}); cargo in hand loads straight in. Broken, it drops the skip and its
 *       cargo as ordinary stacks.</li>
 *   <li><b>Hoisting:</b> holding a drum's cable end (picked up from under the shaft's anchor), use the
 *       skip to hang it on the cable ({@link CableHoists}); sneak-use with an empty hand, while it rests
 *       on something, to take it off. The skip only ever moves straight up or down: wound by the
 *       drum's drive, or, with no cable, settling onto what is under it. It never moves into a block,
 *       a side cell that is not clear, a living thing or a cart, nor above the anchor's roller; it
 *       never moves while it is not in an entity-ticking chunk.</li>
 *   <li><b>Stations:</b> at a height where a freight cart is docked on a Loading Rail in front of it,
 *       it stops and the two exchange cargo ({@link SkipStations}).</li>
 * </ul>
 */
public class OreSkip extends VehicleEntity implements SkipCargo.Store {
	/** Body and bridle (the bridle is there only while the skip hangs on a cable: owner's playtest patch §3). */
	public static final double HEIGHT = 4.0;
	/** The body alone: chute and bin. */
	public static final double BODY_HEIGHT = 3.0;
	/** Client: door animation progress, 0 shut to 1 open, now and last tick. */
	public float lidAnim;
	public float lidAnimO;
	public float gateAnim;
	public float gateAnimO;
	/** Ticks a door takes to open or close (as {@link SkipStations#DOOR_TICKS}). */
	private static final float DOOR_STEP = 0.1F;
	/** Collision checks are made on a box this much smaller than the cell, so touching is not colliding. */
	private static final double SKIN = 1.0E-4;

	private static final EntityDataAccessor<Direction> DATA_FACING = SynchedEntityData.defineId(OreSkip.class, EntityDataSerializers.DIRECTION);
	private static final EntityDataAccessor<ItemStack> DATA_CARGO = SynchedEntityData.defineId(OreSkip.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(OreSkip.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<BlockPos>> DATA_DRUM = SynchedEntityData.defineId(OreSkip.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	/** Where the hoisting cable comes from: the last anchor's roller (for drawing the cable). */
	private static final EntityDataAccessor<Optional<BlockPos>> DATA_ANCHOR = SynchedEntityData.defineId(OreSkip.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	/** Bit 0: the top intake's lid is open; bit 1: the discharge gate is open. */
	private static final EntityDataAccessor<Byte> DATA_DOORS = SynchedEntityData.defineId(OreSkip.class, EntityDataSerializers.BYTE);

	/** Not saved: the station exchange in progress. */
	final SkipStations.Exchange exchange = new SkipStations.Exchange();
	/** Not saved: the height of the station it last stopped at; it passes that one until it has moved away. */
	double passingY = Double.NaN;
	/** Falling with no cable: its speed, blocks per tick (not saved: a skip loaded in mid-air starts falling again from rest). */
	private double fallSpeed;
	private int landings;
	/** Not saved: why it last could not move (for tests). */
	@Nullable String lastHalt;

	public OreSkip(EntityType<? extends OreSkip> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_FACING, Direction.NORTH);
		builder.define(DATA_CARGO, ItemStack.EMPTY);
		builder.define(DATA_COUNT, 0);
		builder.define(DATA_DRUM, Optional.empty());
		builder.define(DATA_ANCHOR, Optional.empty());
		builder.define(DATA_DOORS, (byte) 0);
	}

	// ---- State ----

	/** The side the chute opens to and a station's cart stands on. */
	public Direction facing() {
		return this.entityData.get(DATA_FACING);
	}

	public void setFacing(Direction facing) {
		this.entityData.set(DATA_FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH);
		this.setYRot(facing.toYRot());
	}

	/** The axis its silhouette is wide along, at right angles to its front. */
	public Direction lateral() {
		return this.facing().getClockWise();
	}

	@Override
	public Item cargo() {
		return this.count() > 0 ? this.entityData.get(DATA_CARGO).getItem() : Items.AIR;
	}

	@Override
	public int count() {
		return this.entityData.get(DATA_COUNT);
	}

	@Override
	public void setCargo(Item item, int count) {
		boolean empty = count <= 0 || item == Items.AIR;
		this.entityData.set(DATA_CARGO, empty ? ItemStack.EMPTY : new ItemStack(item));
		this.entityData.set(DATA_COUNT, empty ? 0 : Math.min(count, SkipCargo.CAPACITY));
	}

	/** The drum whose cable it hangs on, or null. */
	public @Nullable BlockPos drum() {
		return this.entityData.get(DATA_DRUM).orElse(null);
	}

	void setDrum(@Nullable BlockPos drum, @Nullable BlockPos anchor) {
		this.entityData.set(DATA_DRUM, Optional.ofNullable(drum));
		this.entityData.set(DATA_ANCHOR, Optional.ofNullable(anchor));
		this.refreshDimensions();
	}

	/** Whether it hangs on a cable, and so has its bridle (and the fourth, collidable block). */
	public boolean bridled() {
		return this.drum() != null;
	}

	/** Its height: the body, and the bridle while it hangs. */
	public double height() {
		return this.bridled() ? HEIGHT : BODY_HEIGHT;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return EntityDimensions.fixed(1.0F, (float) this.height());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_DRUM.equals(accessor)) {
			this.refreshDimensions();
		}
	}

	/** The anchor at the head of the shaft its cable runs over, or null (for drawing the cable). */
	public @Nullable BlockPos anchor() {
		return this.entityData.get(DATA_ANCHOR).orElse(null);
	}

	public boolean lidOpen() {
		return (this.entityData.get(DATA_DOORS) & 1) != 0;
	}

	public boolean gateOpen() {
		return (this.entityData.get(DATA_DOORS) & 2) != 0;
	}

	void setDoors(boolean lid, boolean gate) {
		byte doors = (byte) ((lid ? 1 : 0) | (gate ? 2 : 0));
		if (this.entityData.get(DATA_DOORS) != doors) {
			this.entityData.set(DATA_DOORS, doors);
		}
	}

	public @Nullable String lastHalt() {
		return this.lastHalt;
	}

	/** Its mass in empty-minecart units: the skip and its load. */
	public double mass() {
		return Tuning.getDouble("skip.mass") + Tuning.getDouble("skip.cargo_mass") * this.count() / SkipCargo.CAPACITY;
	}

	/** Its column's block at its bottom (the chute). */
	public BlockPos column() {
		return BlockPos.containing(this.getX(), this.getY() + 0.5, this.getZ());
	}

	// ---- Room to be in ----

	/** The skip's box (body and bridle) standing with its bottom at {@code y}, a hair inside its cell. */
	AABB boxAt(double y) {
		return this.boxAt(y, this.height());
	}

	/** The skip's box {@code height} tall with its bottom at {@code y}, a hair inside its cell. */
	AABB boxAt(double y, double height) {
		return new AABB(this.getX() - 0.5, y, this.getZ() - 0.5, this.getX() + 0.5, y + height, this.getZ() + 0.5).deflate(SKIN);
	}

	/** Why the skip cannot stand with its bottom at {@code y}, or null if it can. */
	@Nullable String obstruction(double y) {
		return obstruction(this.level(), this, this.boxAt(y), this.lateral());
	}

	/**
	 * Why a skip cannot be in {@code box} (with its silhouette's side cells along {@code lateral}): a
	 * block with a collision shape in its column or in either side cell, or a living thing, cart or
	 * other skip in its column.
	 */
	static @Nullable String obstruction(Level level, @Nullable Entity self, AABB box, Direction lateral) {
		if (blocked(level, self, box) || blocked(level, self, box.move(lateral.getStepX(), 0.0, lateral.getStepZ()))
			|| blocked(level, self, box.move(-lateral.getStepX(), 0.0, -lateral.getStepZ()))) {
			return "blocked";
		}
		boolean occupied = !level.getEntities(self, box, entity -> entity.isAlive() && !entity.isSpectator()
			&& (entity instanceof LivingEntity || entity instanceof AbstractMinecart || entity instanceof OreSkip)).isEmpty();
		return occupied ? "occupied" : null;
	}

	private static boolean blocked(Level level, @Nullable Entity self, AABB box) {
		for (VoxelShape shape : level.getBlockCollisions(self, box)) {
			if (!shape.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** Whether it rests on something (it could not go down a little). */
	boolean supported() {
		return this.obstruction(this.getY() - 0.05) != null;
	}

	// ---- Ticking ----

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel level)) {
			// The doors move over DOOR_TICKS ticks towards what the server says (a hinge and a slide, drawn by the renderer).
			this.lidAnimO = this.lidAnim;
			this.gateAnimO = this.gateAnim;
			this.lidAnim = step(this.lidAnim, this.lidOpen());
			this.gateAnim = step(this.gateAnim, this.gateOpen());
			return;
		}
		if (this.getHurtTime() > 0) {
			this.setHurtTime(this.getHurtTime() - 1);
		}
		if (this.getDamage() > 0.0F) {
			this.setDamage(this.getDamage() - 1.0F);
		}
		BlockPos drum = this.drum();
		CableHoists.Hoist hoist = drum == null ? null : CableHoists.hoist(level, drum, this.getUUID());
		if (drum != null && hoist == null) {
			// The cable was let go (the route changed, the drum was broken): it settles onto what is under it.
			this.setDrum(null, null);
		}
		// A station first: an exchange starting here holds the skip where it is.
		SkipStations.tick(level, this);
		if (hoist != null) {
			String halt = null;
			double dy = -hoist.speed();
			// A drive passing through zero as it reverses can leave a speed of a few billionths: that is standing still.
			if (Math.abs(dy) > 1.0E-6) {
				halt = this.exchange.busy() ? "docked" : this.move(dy, hoist.roller().y - CableHoists.APEX_GAP);
			}
			this.lastHalt = halt != null ? halt : this.lastHalt;
			CableHoists.report(level, hoist.drum(), this.getId(), this.getY(), this.mass(), halt);
		} else if (!this.exchange.busy()) {
			this.fall();
		}
		if (hoist != null || this.exchange.busy()) {
			this.fallSpeed = 0.0;
		}
		if (!Double.isNaN(this.passingY) && Math.abs(this.getY() - this.passingY) > 0.5) {
			this.passingY = Double.NaN;
		}
	}

	private static float step(float anim, boolean open) {
		return open ? Math.min(1.0F, anim + DOOR_STEP) : Math.max(0.0F, anim - DOOR_STEP);
	}

	/**
	 * One tick of being wound {@code dy} (up positive) by the cable: moves as far as it can, stopping
	 * at a station on the way. Returns why it stopped short, or null if it moved freely.
	 */
	private @Nullable String move(double dy, double apexLimit) {
		double y = this.getY();
		double target = y + dy;
		String halt = null;
		if (dy > 0.0 && target + HEIGHT > apexLimit) {
			// Up to the head of the shaft: the apex stops just under the anchor's pulley.
			target = Math.max(y, apexLimit - HEIGHT);
			halt = "end";
		}
		// A station on the way: stop exactly there (the nearest one in the direction of travel).
		Double station = null;
		for (double h : SkipStations.heightsBetween(y, target)) {
			if ((Double.isNaN(this.passingY) || Math.abs(h - this.passingY) > 1.0E-6) && SkipStations.station(this.level(), this, h) != null
				&& (station == null || Math.abs(h - y) < Math.abs(station - y))) {
				station = h;
			}
		}
		if (station != null) {
			target = station;
			halt = "docked";
		}
		double reached = this.reach(y, target);
		if (Math.abs(reached - target) > 1.0E-9) {
			halt = "blocked";
		}
		if (reached != y) {
			this.setPos(this.getX(), reached, this.getZ());
		}
		if ("docked".equals(halt)) {
			this.passingY = reached;
		}
		return halt == null && reached == y ? "blocked" : halt;
	}

	/**
	 * With no cable: comes down onto whatever is under it, like a dropped anchor (M07 playtest): faster
	 * every tick ({@code skip.fall_gravity}, up to {@code skip.fall_max_speed}), with a heavy metallic
	 * clang when it strikes what stops it, however far it fell, as loud as it hit hard; nothing for a
	 * skip that only settles a little (one let go standing on something stays where it is, silent).
	 * Its cargo stays in it.
	 */
	private void fall() {
		double y = this.getY();
		this.fallSpeed = Math.min(this.fallSpeed + Tuning.getDouble("skip.fall_gravity"), Tuning.getDouble("skip.fall_max_speed"));
		double target = y - this.fallSpeed;
		double reached = this.reach(y, target);
		if (reached != y) {
			this.setPos(this.getX(), reached, this.getZ());
		}
		if (reached > target + 1.0E-9) {
			// Stopped by what is under it.
			double impact = this.fallSpeed;
			this.fallSpeed = 0.0;
			if (impact >= LANDING_SPEED && this.level() instanceof ServerLevel level) {
				this.landings++;
				float volume = (float) Math.min(1.5, 0.5 + impact);
				level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, volume, 0.6F);
				level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, volume * 0.6F, 0.7F);
			}
		}
	}

	/** Speed (blocks per tick) a falling skip must hit something at for the clang: more than a little settling. */
	static final double LANDING_SPEED = 0.15;

	/** For tests: how many times this skip has landed with a clang. */
	public int landings() {
		return this.landings;
	}

	/**
	 * How far from {@code from} towards {@code to} the skip has room to go: checked in sixteenths of a
	 * block, then the last sixteenth narrowed down, so it ends touching what stopped it.
	 */
	private double reach(double from, double to) {
		double reached = from;
		double step = Math.signum(to - from) / 16.0;
		while (Math.abs(to - reached) > 1.0E-9) {
			double next = Math.abs(to - reached) < Math.abs(step) ? to : reached + step;
			if (this.obstruction(next) != null) {
				double clear = reached;
				double blocked = next;
				for (int i = 0; i < 12; i++) {
					double mid = (clear + blocked) / 2.0;
					if (this.obstruction(mid) == null) {
						clear = mid;
					} else {
						blocked = mid;
					}
				}
				// Touching: settle on the sixteenth grid if that is clear (the box is a hair inside its cell).
				double snapped = Math.rint(clear * 16.0) / 16.0;
				return Math.abs(snapped - clear) < 1.0E-3 && this.obstruction(snapped) == null ? snapped : clear;
			}
			reached = next;
		}
		return reached;
	}

	// ---- Players ----

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (!(this.level() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack held = player.getMainHandItem();
		CableHook hook = CableRoutes.heldHook(level, player);
		if (hook != null) {
			BlockPos bridle = BlockPos.containing(this.getX(), this.getY() + BODY_HEIGHT + 0.5, this.getZ());
			if (this.drum() != null) {
				serverPlayer.sendOverlayMessage(SkipCargo.message("already_hung", "This skip already hangs on a cable"));
			} else if (obstruction(level, this, this.boxAt(this.getY(), HEIGHT), this.lateral()) != null) {
				// No room for the bridle (the fourth block, or beside it): nothing is hung, nothing used.
				serverPlayer.sendOverlayMessage(Component.translatableWithFallback("simple_block.cable.blocked", "The cable would run through %s at %s",
					level.getBlockState(bridle).getBlock().getName(), bridle.toShortString()));
			} else {
				CableHoists.Attach attach = CableHoists.attach(level, hook, this.getUUID(), this.column(), this.getY() + HEIGHT);
				if (attach.hung()) {
					BlockPos drumPos = hook.drumPos();
					this.setDrum(drumPos, hook.endPos());
				}
				serverPlayer.sendOverlayMessage(attach.message());
			}
		} else if (player.isSecondaryUseActive() && held.isEmpty()) {
			serverPlayer.sendOverlayMessage(this.unhang(level));
		} else if (MinecartCargo.isCargo(held)) {
			serverPlayer.sendOverlayMessage(SkipCargo.loadFromHand(this, held));
		} else {
			serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> SkipMenu.server(id, inventory, this),
				Component.translatable("container.simple_block.ore_skip")));
		}
		return InteractionResult.SUCCESS;
	}

	/** Takes it off its cable, if it rests on something. */
	private Component unhang(ServerLevel level) {
		BlockPos drum = this.drum();
		if (drum == null) {
			return SkipCargo.status(this);
		}
		if (!this.supported()) {
			return SkipCargo.message("unhang_unsupported", "Lower the skip onto something before taking it off the cable");
		}
		CableHoists.detach(level, drum, this.getY() + HEIGHT);
		this.setDrum(null, null);
		level.playSound(null, this.column(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0F, 0.7F);
		return SkipCargo.message("unhung", "Skip taken off the cable");
	}

	// ---- Breaking ----

	@Override
	protected Item getDropItem() {
		return SkipContent.ORE_SKIP_ITEM;
	}

	/** Broken, killed or removed by a creative player: lets go of its cable and drops its cargo as ordinary stacks. */
	@Override
	public void remove(Entity.RemovalReason reason) {
		if (reason.shouldDestroy() && this.level() instanceof ServerLevel level && !this.isRemoved()) {
			BlockPos drum = this.drum();
			if (drum != null) {
				CableHoists.detach(level, drum, this.getY() + HEIGHT);
			}
			Item item = this.cargo();
			int left = this.count();
			this.setCargo(Items.AIR, 0);
			while (left > 0) {
				int stack = Math.min(left, item.getDefaultMaxStackSize());
				this.spawnAtLocation(level, new ItemStack(item, stack));
				left -= stack;
			}
		}
		super.remove(reason);
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return true;
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 128.0 * 128.0;
	}

	// ---- Saving ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.store("facing", Direction.CODEC, this.facing());
		if (this.count() > 0) {
			output.store("cargo", Item.CODEC, this.cargo().builtInRegistryHolder());
			output.putInt("count", this.count());
		}
		output.storeNullable("drum", BlockPos.CODEC, this.drum());
		output.storeNullable("anchor", BlockPos.CODEC, this.anchor());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setFacing(input.read("facing", Direction.CODEC).orElse(Direction.NORTH));
		Item item = input.read("cargo", Item.CODEC).map(holder -> holder.value()).orElse(Items.AIR);
		this.setCargo(item, Math.max(0, input.getIntOr("count", 0)));
		this.setDrum(input.read("drum", BlockPos.CODEC).orElse(null), input.read("anchor", BlockPos.CODEC).orElse(null));
	}

	@Override
	public String toString() {
		return "OreSkip[" + BuiltInRegistries.ITEM.getKey(this.cargo()) + " x" + this.count() + " at " + this.position() + "]";
	}
}
