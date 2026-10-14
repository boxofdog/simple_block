package io.github.boxofdog.simpleblock.effigy;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * M12: the four Monster Effigies. Each is made from one vanilla mob drop (nine full stacks into its
 * compressed block, two blocks under the matching head into the effigy) and frightens exactly one
 * monster type: the zombie (baby zombies included, they are the same type), the skeleton, the creeper
 * and the enderman. Husks, drowned, strays, wither skeletons and every other mob are untouched.
 */
public enum EffigyType implements StringRepresentable {
	ZOMBIE(EntityTypes.ZOMBIE, Items.ROTTEN_FLESH, Items.ZOMBIE_HEAD),
	SKELETON(EntityTypes.SKELETON, Items.BONE, Items.SKELETON_SKULL),
	CREEPER(EntityTypes.CREEPER, Items.GUNPOWDER, Items.CREEPER_HEAD),
	ENDERMAN(EntityTypes.ENDERMAN, Items.ENDER_PEARL, Items.ENDER_EYE);

	private final EntityType<?> monster;
	private final Item drop;
	private final Item top;

	EffigyType(EntityType<?> monster, Item drop, Item top) {
		this.monster = monster;
		this.drop = drop;
		this.top = top;
	}

	/** The one monster type this effigy frightens. */
	public EntityType<?> monster() {
		return this.monster;
	}

	/** The drop nine full stacks of which make the compressed block. */
	public Item drop() {
		return this.drop;
	}

	/** The head (or Eye of Ender) on top of the effigy recipe. */
	public Item top() {
		return this.top;
	}

	@Override
	public String getSerializedName() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	/** The effigy type that frightens {@code entity}, or null. */
	public static @Nullable EffigyType frightening(Entity entity) {
		for (EffigyType type : values()) {
			if (entity.getType() == type.monster) {
				return type;
			}
		}
		return null;
	}
}
