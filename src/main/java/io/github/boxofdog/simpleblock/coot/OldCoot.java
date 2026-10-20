package io.github.boxofdog.simpleblock.coot;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * M14 the Old Coot: a short, hunched old miner who has lived in an abandoned mineshaft long enough to call
 * its ore and its cave spiders his own. Villager-like (he is no monster: {@code MobCategory.MISC}, like a
 * villager, and monsters and he leave each other alone), persistent once spawned, and always holding his
 * iron pickaxe.
 *
 * <p>His home: a natural resident belongs to his mineshaft's {@link CootHomes} record and works around the
 * place he first stood; an egg or command spawned Coot has no record, only his own spawn spot as a working
 * anchor. Either way he wanders his {@link #TERRITORY} at a zombie's pace and digs ({@link CootMineGoal}).
 *
 * <p>His anger ({@link CootAnger}): a player who mines ore within earshot, who he sees hit a cave spider,
 * or who hits him. He chases at a spider's pace and does not give up when he loses sight of them, only
 * when they get clean away ({@link #LOSE_DISTANCE} off, or past {@link #LEASH} from home, or out of sight
 * for {@link #LOSE_UNSEEN} ticks), and then walks home and goes back to work. He hits like a player with
 * an iron pickaxe. On death: 1-3 emeralds and a worn iron pickaxe (5-50% of its durability left).
 */
public class OldCoot extends PathfinderMob {
	public static final double HEALTH = 20.0;
	/** A zombie's walking pace. */
	public static final double SPEED = 0.23;
	/** A spider's pace while he chases: his speed times this. */
	public static final double CHASE = 0.3 / SPEED;
	/** Base damage 1, plus his iron pickaxe's +3: a player's iron-pickaxe hit (4). */
	public static final double DAMAGE = 1.0;
	/** Villager voice, raised towards a witch's register: villagers speak at 0.8-1.2, this at 1.2-1.4. */
	public static final float VOICE_PITCH = 1.3F;
	/** How far from his anchor he wanders and digs. */
	public static final int TERRITORY = 28;
	/** How far from his anchor a chase may go before he gives it up. */
	public static final int LEASH = 64;
	/** A quarry this far off has got away. */
	public static final double LOSE_DISTANCE = 40.0;
	/** A quarry out of his sight this long has got away. */
	public static final int LOSE_UNSEEN = 600;
	/** Ticks of rest after a passage. */
	public static final int MINE_PAUSE_MIN = 100;
	public static final int MINE_PAUSE_SPREAD = 300;
	/** Ticks before he looks again for a face to dig when he found none. */
	public static final int MINE_LOOK_PAUSE = 40;
	/** Pickaxe blows carry like a player's (heard to 16 blocks). */
	public static final float BLOW_VOLUME = 1.0F;
	/** Torches a natural home may ever get from its residents, or an egg-spawned Coot may ever put up. */
	public static final int TORCH_CAP = 6;
	/** Ticks between torches. */
	public static final int TORCH_PAUSE = 2400;
	public static final long NO_HOME = Long.MIN_VALUE;

	private long home = NO_HOME;
	private @Nullable BlockPos anchor;
	private int torches;
	private long lastTorch = Long.MIN_VALUE / 2;
	private @Nullable UUID angryAt;
	private int unseen;
	/** Ticks until he may start another passage. */
	public int mineCooldown = MINE_PAUSE_MIN;
	/** Passages begun and blocks mined (for tests and evidence). */
	public int passages;
	public int blocksMined;

	public OldCoot(EntityType<? extends OldCoot> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, HEALTH)
			.add(Attributes.MOVEMENT_SPEED, SPEED)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE)
			.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, CHASE, true));
		this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
		this.goalSelector.addGoal(5, new CootMineGoal(this));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason,
		@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
		this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
		this.setPersistenceRequired();
		return data;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
		// His own pickaxe never drops as equipment: his death drop is a worn one.
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	/** A natural resident: this mineshaft's (its start chunk, packed) and working around where he first stood. */
	void settle(long home, BlockPos anchor) {
		this.home = home;
		this.anchor = anchor.immutable();
		this.setHomeTo(this.anchor, TERRITORY);
	}

	/** His mineshaft's start chunk (packed), or {@link #NO_HOME} for an egg or command spawned Coot. */
	public long home() {
		return this.home;
	}

	public @Nullable BlockPos anchor() {
		return this.anchor;
	}

	public boolean inTerritory(BlockPos pos) {
		return this.anchor != null && this.anchor.distSqr(pos) <= (double) TERRITORY * TERRITORY;
	}

	public boolean isAngry() {
		return this.angryAt != null;
	}

	public boolean mayPlaceTorch() {
		if (!(this.level() instanceof ServerLevel level) || level.getGameTime() - this.lastTorch < TORCH_PAUSE) {
			return false;
		}
		if (this.home != NO_HOME) {
			CootHomes.Home record = CootHomes.get(level).find(this.home);
			return record != null && record.torches < TORCH_CAP;
		}
		return this.torches < TORCH_CAP;
	}

	/** Torches his home (or, egg spawned, he himself) may still put up, ever. */
	public int torchesLeft() {
		if (this.home != NO_HOME && this.level() instanceof ServerLevel level) {
			CootHomes.Home record = CootHomes.get(level).find(this.home);
			return record == null ? 0 : TORCH_CAP - record.torches;
		}
		return TORCH_CAP - this.torches;
	}

	public void torchPlaced() {
		ServerLevel level = (ServerLevel) this.level();
		this.lastTorch = level.getGameTime();
		this.torches++;
		if (this.home != NO_HOME) {
			CootHomes.Home record = CootHomes.get(level).find(this.home);
			if (record != null) {
				CootHomes.get(level).torchPlaced(record);
			}
		}
	}

	/** Torches he has put up (this Coot's own count). */
	public int torches() {
		return this.torches;
	}

	/** Angry with this player now (ore stolen within earshot, a cave spider hit before his eyes, or a blow to him). */
	public void provoke(Player player) {
		if (player.isCreative() || player.isSpectator() || !this.isAlive()) {
			return;
		}
		if (!player.getUUID().equals(this.angryAt)) {
			// A clear angry cry the moment he takes against someone.
			this.playSound(SoundEvents.VILLAGER_NO, 1.5F, this.getVoicePitch() * 0.9F);
		}
		this.angryAt = player.getUUID();
		this.unseen = 0;
		this.setTarget(player);
		if (this.anchor != null) {
			this.setHomeTo(this.anchor, LEASH);
		}
	}

	/** He has lost them: back home and to work. */
	public void calmDown() {
		this.angryAt = null;
		this.unseen = 0;
		this.setTarget(null);
		this.setAggressive(false);
		if (this.anchor != null) {
			this.setHomeTo(this.anchor, TERRITORY);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		if (this.anchor == null) {
			// An egg or command spawned Coot works around where he was put.
			this.anchor = this.blockPosition();
			this.setHomeTo(this.anchor, TERRITORY);
		}
		super.customServerAiStep(level);
		if (this.mineCooldown > 0) {
			this.mineCooldown--;
		}
		if (this.angryAt == null) {
			if (this.getTarget() != null) {
				this.setTarget(null);
			}
			return;
		}
		Player quarry = level.getPlayerByUUID(this.angryAt);
		if (quarry == null || !quarry.isAlive() || quarry.isCreative() || quarry.isSpectator() || quarry.level() != level) {
			this.calmDown();
			return;
		}
		this.unseen = this.getSensing().hasLineOfSight(quarry) ? 0 : this.unseen + 1;
		if (this.distanceTo(quarry) > LOSE_DISTANCE || this.unseen > LOSE_UNSEEN
			|| this.anchor.distSqr(quarry.blockPosition()) > (double) LEASH * LEASH) {
			this.calmDown();
			return;
		}
		// Stubborn: whatever cleared it, he still means them.
		if (this.getTarget() != quarry) {
			this.setTarget(quarry);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && source.getEntity() instanceof Player player && this.isAlive()) {
			this.provoke(player);
		}
		return hurt;
	}

	/** Only the player he is angry with is a target of his; nobody else ever is (monsters included). */
	@Override
	public boolean canAttack(LivingEntity target) {
		return target instanceof Player player && player.getUUID().equals(this.angryAt) && super.canAttack(target);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		this.spawnAtLocation(level, new ItemStack(Items.EMERALD, 1 + this.random.nextInt(3)));
		this.spawnAtLocation(level, wornPickaxe(this.random));
	}

	/** An iron pickaxe with 5-50% of its durability left (never broken). */
	public static ItemStack wornPickaxe(RandomSource random) {
		ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
		int max = pick.getMaxDamage();
		float left = 0.05F + random.nextFloat() * 0.45F;
		int remaining = Mth.clamp(Math.round(max * left), Math.max(1, Mth.ceil(max * 0.05F)), Mth.floor(max * 0.5F));
		pick.setDamageValue(max - remaining);
		return pick;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level && this.home != NO_HOME) {
			CootResidents.departed(level, this, this.home);
		}
	}

	@Override
	public void onRemoval(RemovalReason reason) {
		super.onRemoval(reason);
		// Killed or discarded: gone for good. Unloaded (to a chunk, with a player) or moved dimension: still living.
		if (reason.shouldDestroy() && this.level() instanceof ServerLevel level && this.home != NO_HOME) {
			CootResidents.departed(level, this, this.home);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.home != NO_HOME) {
			output.putLong("CootHome", this.home);
		}
		output.storeNullable("CootAnchor", BlockPos.CODEC, this.anchor);
		output.putInt("CootTorches", this.torches);
		output.putLong("CootLastTorch", this.lastTorch);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.home = input.getLongOr("CootHome", NO_HOME);
		this.anchor = input.read("CootAnchor", BlockPos.CODEC).orElse(null);
		this.torches = input.getIntOr("CootTorches", 0);
		this.lastTorch = input.getLongOr("CootLastTorch", Long.MIN_VALUE / 2);
		if (this.anchor != null) {
			this.setHomeTo(this.anchor, TERRITORY);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}

	@Override
	public float getVoicePitch() {
		return VOICE_PITCH + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return this.isAngry() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}
}
