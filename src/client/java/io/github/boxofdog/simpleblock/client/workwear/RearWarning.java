package io.github.boxofdog.simpleblock.client.workwear;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.SimpleBlock;
import io.github.boxofdog.simpleblock.tuning.Tuning;
import io.github.boxofdog.simpleblock.workwear.Workwear;

/**
 * M10 Old Pro, client: a red glow along the bottom edge of the screen while a hostile monster behind the
 * player (more than {@code workwear.rear_warning_angle} degrees from where they face, horizontally), within
 * {@code workwear.rear_warning_range} blocks, has a clear line from its eyes to the player's neck, in
 * their second block of height (1.5 above the feet). A wall, or anything solid, in the way: no glow,
 * whether or not the monster could walk round. Nearer is brighter. Not a radar: nothing in front, nothing
 * out of sight.
 */
public final class RearWarning {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "rear_warning");
	/** The neck: in the second block of height. */
	public static final double NECK = 1.5;
	private static float target;
	private static float shown;

	private RearWarning() {
	}

	/** How strongly the glow shows now, 0 (none) to 1 (for the client test). */
	public static float strength() {
		return target;
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			target = minecraft.player == null ? 0.0F : threat(minecraft.player);
			shown += (target - shown) * 0.35F;
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, ID, RearWarning::extract);
	}

	/** The strongest warning any monster behind the player gives now. */
	public static float threat(LocalPlayer player) {
		if (Workwear.bonus(player) != Workwear.Bonus.OLD_PRO) {
			return 0.0F;
		}
		double range = Tuning.getDouble("workwear.rear_warning_range");
		double behind = Math.cos(Math.toRadians(Tuning.getDouble("workwear.rear_warning_angle")));
		Vec3 look = player.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize();
		Vec3 neck = player.position().add(0.0, NECK, 0.0);
		float strongest = 0.0F;
		for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range), mob -> mob instanceof Enemy && mob.isAlive())) {
			Vec3 to = mob.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
			double distance = mob.getEyePosition().distanceTo(neck);
			if (distance > range || to.lengthSqr() < 1.0E-6 || to.normalize().dot(look) > behind) {
				continue;
			}
			HitResult hit = player.level().clip(new ClipContext(mob.getEyePosition(), neck, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
			if (hit.getType() == HitResult.Type.MISS) {
				strongest = Math.max(strongest, (float) (1.0 - 0.6 * distance / range));
			}
		}
		return strongest;
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (shown < 0.02F || minecraft.gui.hud.isHidden()) {
			return;
		}
		int height = Mth.ceil(graphics.guiHeight() * 0.12F);
		int alpha = Mth.clamp(Math.round(170 * shown), 0, 255);
		graphics.fillGradient(0, graphics.guiHeight() - height, graphics.guiWidth(), graphics.guiHeight(), 0x00D01010, (alpha << 24) | 0xD01010);
	}
}
