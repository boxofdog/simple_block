package io.github.boxofdog.simpleblock.client.train;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.client.cable.CableGeometry;
import io.github.boxofdog.simpleblock.train.Couplings;

/**
 * M05: draws the iron chains between coupled minecarts (of every kind), with vanilla's chain texture
 * as two crossed strips like the chain block. Each cart draws its half of each coupling, from its end to
 * the middle, so a chain stays whole while both carts are drawn and half-visible when one is culled.
 * The hooks are in {@code mixin/AbstractMinecartRendererMixin}; the pieces live on the cart's render
 * state ({@code mixin/MinecartRenderStateMixin}).
 */
public final class CouplingChains {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/iron_chain.png");
	/** Height of the chain above the rail, and how far in from a cart's centre it starts. */
	private static final double HEIGHT = 0.3;
	private static final double END = 0.55;
	/** Half the strip's width: 3 pixels across, like the chain block. */
	private static final float HALF_WIDTH = 1.5F / 16.0F;

	/** A minecart render state's chain pieces, relative to the cart. */
	public interface Holder {
		List<CableGeometry.Piece> simpleBlock$chains();
	}

	private CouplingChains() {
	}

	public static void extract(AbstractMinecart cart, MinecartRenderState state, float partialTicks) {
		List<CableGeometry.Piece> pieces = ((Holder) state).simpleBlock$chains();
		pieces.clear();
		Vec3 origin = cart.getPosition(partialTicks);
		for (int slot = 0; slot < 2; slot++) {
			int id = Couplings.coupled(cart).simpleBlock$couplingId(slot);
			Entity partner = id < 0 ? null : cart.level().getEntity(id);
			if (partner == null) {
				continue;
			}
			Vec3 other = partner.getPosition(partialTicks);
			Vec3 offset = other.subtract(origin);
			double distance = offset.horizontalDistance();
			if (distance < 2.0 * END) {
				continue;
			}
			Vec3 direction = offset.scale(1.0 / distance);
			Vec3 start = origin.add(direction.scale(END)).add(0.0, HEIGHT, 0.0);
			Vec3 middle = origin.lerp(other, 0.5).add(0.0, HEIGHT, 0.0);
			CableGeometry.addRun(pieces, cart.level(), origin, start, middle);
		}
	}

	/** With the pose at the cart's position. */
	public static void submit(MinecartRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		List<CableGeometry.Piece> pieces = ((Holder) state).simpleBlock$chains();
		if (pieces.isEmpty()) {
			return;
		}
		List<CableGeometry.Piece> copy = List.copyOf(pieces);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) -> {
			for (CableGeometry.Piece piece : copy) {
				strips(pose, buffer, piece);
			}
		});
	}

	/** Two strips crossed along the piece, at 45 degrees to the ground, with the chain block's two halves of the texture. */
	private static void strips(PoseStack.Pose pose, VertexConsumer buffer, CableGeometry.Piece p) {
		float dx = p.x2() - p.x1();
		float dy = p.y2() - p.y1();
		float dz = p.z2() - p.z1();
		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float h = (float) Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-4F || h < 1.0E-4F) {
			return;
		}
		// Across the chain: level with the ground ("side") and square to it ("up").
		float sx = -dz / h;
		float sz = dx / h;
		float ux = -dy / length * dx / h;
		float uy = h / length;
		float uz = -dy / length * dz / h;
		float r = 0.70710677F;
		strip(pose, buffer, p, (sx + ux) * r, uy * r, (sz + uz) * r, 0.0F);
		strip(pose, buffer, p, (sx - ux) * r, -uy * r, (sz - uz) * r, 3.0F / 16.0F);
	}

	private static void strip(PoseStack.Pose pose, VertexConsumer buffer, CableGeometry.Piece p, float ax, float ay, float az, float u0) {
		float u1 = u0 + 3.0F / 16.0F;
		float wx = ax * HALF_WIDTH;
		float wy = ay * HALF_WIDTH;
		float wz = az * HALF_WIDTH;
		vertex(pose, buffer, p.x1() - wx, p.y1() - wy, p.z1() - wz, u0, p.v1(), p.light());
		vertex(pose, buffer, p.x2() - wx, p.y2() - wy, p.z2() - wz, u0, p.v2(), p.light());
		vertex(pose, buffer, p.x2() + wx, p.y2() + wy, p.z2() + wz, u1, p.v2(), p.light());
		vertex(pose, buffer, p.x1() + wx, p.y1() + wy, p.z1() + wz, u1, p.v1(), p.light());
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int light) {
		buffer.addVertex(pose, x, y, z)
			.setColor(255, 255, 255, 255)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}
}
