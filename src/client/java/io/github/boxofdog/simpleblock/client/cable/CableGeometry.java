package io.github.boxofdog.simpleblock.client.cable;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import io.github.boxofdog.simpleblock.SimpleBlock;

/**
 * Draws cable as a thin square tube with {@code textures/entity/cable.png}: each of its four sides maps
 * the whole texture, u across the side and v along one block of length. Long runs are cut into pieces
 * of at most a block, each lit by the light where it is. A moving cable (M05 loops) shifts the
 * texture along with it.
 */
public final class CableGeometry {
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(SimpleBlock.MOD_ID, "textures/entity/cable.png");
	/** Half the cable's thickness: 1.5 pixels across. */
	private static final float RADIUS = 0.75F / 16.0F;

	/** One straight piece of cable, relative to the pose's origin, with its light and texture rows {@code v1} to {@code v2} (within 0..1). */
	public record Piece(float x1, float y1, float z1, float x2, float y2, float z2, int light, float v1, float v2) {
	}

	private CableGeometry() {
	}

	/** Cuts the straight run {@code from}-{@code to} (world positions) into lit pieces relative to {@code origin}. */
	public static void addRun(List<Piece> pieces, Level level, Vec3 origin, Vec3 from, Vec3 to) {
		addRun(pieces, level, origin, from, to, 0.0);
	}

	/**
	 * As {@link #addRun(List, Level, Vec3, Vec3, Vec3)}, with the texture starting at row {@code v} (in
	 * blocks of cable; it repeats every block). Pieces are cut where the texture repeats, so their rows
	 * stay within the texture. Returns the row at {@code to}, to carry on along a path.
	 */
	public static double addRun(List<Piece> pieces, Level level, Vec3 origin, Vec3 from, Vec3 to, double v) {
		double length = from.distanceTo(to);
		if (length < 1.0E-4) {
			return v;
		}
		double done = 0.0;
		Vec3 previous = from;
		while (done < length - 1.0E-6) {
			double row = v + done - Math.floor(v + done);
			double step = Math.min(length - done, 1.0 - row);
			if (step < 1.0E-4) {
				step = Math.min(length - done, 1.0);
				row = 0.0;
			}
			done += step;
			Vec3 next = from.lerp(to, done / length);
			Vec3 middle = previous.lerp(next, 0.5);
			int light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(middle));
			pieces.add(new Piece((float) (previous.x - origin.x), (float) (previous.y - origin.y), (float) (previous.z - origin.z),
				(float) (next.x - origin.x), (float) (next.y - origin.y), (float) (next.z - origin.z), light, (float) row, (float) (row + step)));
			previous = next;
		}
		return v + length;
	}

	public static void submit(List<Piece> pieces, PoseStack poseStack, SubmitNodeCollector collector) {
		if (pieces.isEmpty()) {
			return;
		}
		List<Piece> copy = List.copyOf(pieces);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) -> {
			for (Piece piece : copy) {
				tube(pose, buffer, piece);
			}
		});
	}

	private static void tube(PoseStack.Pose pose, VertexConsumer buffer, Piece p) {
		float dx = p.x2 - p.x1;
		float dy = p.y2 - p.y1;
		float dz = p.z2 - p.z1;
		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < 1.0E-4F) {
			return;
		}
		dx /= length;
		dy /= length;
		dz /= length;
		// Two axes across the cable: "side" level with the ground (unless the cable is vertical) and "up".
		float sx;
		float sy;
		float sz;
		if (Math.abs(dy) > 0.99F) {
			sx = 1.0F;
			sy = 0.0F;
			sz = 0.0F;
		} else {
			float h = (float) Math.sqrt(dx * dx + dz * dz);
			sx = -dz / h;
			sy = 0.0F;
			sz = dx / h;
		}
		float ux = sy * dz - sz * dy;
		float uy = sz * dx - sx * dz;
		float uz = sx * dy - sy * dx;
		float[][] corners = {
			{sx + ux, sy + uy, sz + uz}, {-sx + ux, -sy + uy, -sz + uz}, {-sx - ux, -sy - uy, -sz - uz}, {sx - ux, sy - uy, sz - uz}};
		for (int i = 0; i < 4; i++) {
			float[] a = corners[i];
			float[] b = corners[(i + 1) % 4];
			float nx = a[0] + b[0];
			float ny = a[1] + b[1];
			float nz = a[2] + b[2];
			vertex(pose, buffer, p.x1 + a[0] * RADIUS, p.y1 + a[1] * RADIUS, p.z1 + a[2] * RADIUS, 0.0F, p.v1, nx, ny, nz, p.light);
			vertex(pose, buffer, p.x2 + a[0] * RADIUS, p.y2 + a[1] * RADIUS, p.z2 + a[2] * RADIUS, 0.0F, p.v2, nx, ny, nz, p.light);
			vertex(pose, buffer, p.x2 + b[0] * RADIUS, p.y2 + b[1] * RADIUS, p.z2 + b[2] * RADIUS, 1.0F, p.v2, nx, ny, nz, p.light);
			vertex(pose, buffer, p.x1 + b[0] * RADIUS, p.y1 + b[1] * RADIUS, p.z1 + b[2] * RADIUS, 1.0F, p.v1, nx, ny, nz, p.light);
		}
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, float nx, float ny, float nz,
		int light) {
		buffer.addVertex(pose, x, y, z)
			.setColor(255, 255, 255, 255)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, nx, ny, nz);
	}
}
