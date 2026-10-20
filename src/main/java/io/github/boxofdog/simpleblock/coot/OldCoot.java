package io.github.boxofdog.simpleblock.coot;

import org.jspecify.annotations.Nullable;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * M14 the Old Coot: a short, squat old miner who has lived in an abandoned mineshaft long enough to call its
 * ore and its cave spiders his own. Villager-like (he is no monster: {@code MobCategory.MISC}, like a villager),
 * persistent once spawned, and always holding his iron pickaxe (which he never drops as equipment).
 *
 * <p>Stage 1: the entity, his attributes (villager health, zombie walking pace, a player's iron-pickaxe hit)
 * and a quiet wander. His voice is a villager's, pitched up towards a witch's (owner, 2026-10-20).
 */
public class OldCoot extends PathfinderMob {
	public static final double HEALTH = 20.0;
	/** A zombie's walking pace. */
	public static final double SPEED = 0.23;
	/** A player's hit with an iron pickaxe. */
	public static final double DAMAGE = 4.0;
	/** Villager voice, raised towards a witch's register: villagers speak at 0.8-1.2, this at 1.2-1.4. */
	public static final float VOICE_PITCH = 1.3F;

	public OldCoot(EntityType<? extends OldCoot> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, HEALTH)
			.add(Attributes.MOVEMENT_SPEED, SPEED)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
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
		// His own pickaxe never drops as equipment: his death drop is a worn one (stage 4).
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public float getVoicePitch() {
		return VOICE_PITCH + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.VILLAGER_AMBIENT;
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
