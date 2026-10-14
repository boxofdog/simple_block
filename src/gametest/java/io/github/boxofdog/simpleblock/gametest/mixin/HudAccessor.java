package io.github.boxofdog.simpleblock.gametest.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only: lets the client smoke test read the action-bar message the player sees. */
@Mixin(Hud.class)
public interface HudAccessor {
	@Accessor("overlayMessageString")
	@Nullable Component simple_block$getOverlayMessage();

	/** M11: lets a test clear what an earlier step left showing before checking that nothing new appears. */
	@Accessor("overlayMessageString")
	void simple_block$setOverlayMessage(@Nullable Component message);
}
