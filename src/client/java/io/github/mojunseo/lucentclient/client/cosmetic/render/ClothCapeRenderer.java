package io.github.mojunseo.lucentclient.client.cosmetic.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Draws a cape as a finely subdivided cloth instead of the vanilla single box. The cape keeps the
 * vanilla orientation and physics (it is drawn in the vanilla cape part's space) and adds a curve
 * that increases towards the hem plus travelling ripples along and across it.
 *
 * <p>Cape space matches the vanilla cape box: x in [-5, 5], hanging along +y for 16 pixels, with the
 * outer face at z = -1 and the inner face at z = 0. Texture layout is the vanilla 64x32 cape layout.
 */
public final class ClothCapeRenderer {
	private static final int COLUMNS = 20;
	private static final int ROWS = 32;
	private static final float WIDTH = 10.0F;
	private static final float LENGTH = 16.0F;
	private static final float THICKNESS = 1.0F;
	private static final float PIXEL = 1.0F / 16.0F;

	/** Values copied out of the render state, since drawing happens later than the layer's submit. */
	public record Params(float time, float lean, float flap, float moving, int light) {
		public static Params of(AvatarRenderState state, int light) {
			return new Params(state.ageInTicks, state.capeLean, state.capeFlap,
					Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F), light);
		}
	}

	private ClothCapeRenderer() {
	}

	public static void render(PoseStack.Pose pose, VertexConsumer buffer, Params params) {
		// Center line of the cloth, sampled per row: y/z position and bend angle (radians, towards -z).
		float[] lineY = new float[ROWS + 1];
		float[] lineZ = new float[ROWS + 1];
		float[] angle = new float[ROWS + 1];
		float energy = 0.35F + params.moving * 0.9F + Mth.clamp(params.lean / 80.0F, 0.0F, 1.0F);
		float bend = 0.12F + Mth.clamp(params.lean, 0.0F, 150.0F) * 0.0015F;
		float y = 0.0F;
		float z = -THICKNESS / 2;
		for (int row = 0; row <= ROWS; row++) {
			float s = row / (float) ROWS;
			float ripple = Mth.sin(s * 7.5F - params.time * (0.18F + params.moving * 0.25F)) * 0.07F * energy * s;
			angle[row] = bend * s + ripple;
			lineY[row] = y;
			lineZ[row] = z;
			float step = LENGTH / ROWS;
			y += step * Mth.cos(angle[row]);
			z -= step * Mth.sin(angle[row]);
		}

		// Cross-width flutter: small out-of-plane offset per column, strongest at the hem.
		float[][] offsetZ = new float[ROWS + 1][COLUMNS + 1];
		for (int row = 0; row <= ROWS; row++) {
			float s = row / (float) ROWS;
			for (int column = 0; column <= COLUMNS; column++) {
				float x = column / (float) COLUMNS * WIDTH - WIDTH / 2;
				offsetZ[row][column] = Mth.sin(x * 0.7F + params.time * 0.3F + s * 3.0F) * 0.35F * energy * s * s;
			}
		}

		float half = THICKNESS / 2;
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				float x0 = column / (float) COLUMNS * WIDTH - WIDTH / 2;
				float x1 = (column + 1) / (float) COLUMNS * WIDTH - WIDTH / 2;
				// Outer face: normal points away from the body (towards -z in cape space).
				quad(pose, buffer, params.light,
						vertex(x0, row, column, lineY, lineZ, angle, offsetZ, -half),
						vertex(x1, row, column + 1, lineY, lineZ, angle, offsetZ, -half),
						vertex(x1, row + 1, column + 1, lineY, lineZ, angle, offsetZ, -half),
						vertex(x0, row + 1, column, lineY, lineZ, angle, offsetZ, -half),
						outerU(x0), outerU(x1), row, true, angle);
				// Inner face, facing the body.
				quad(pose, buffer, params.light,
						vertex(x1, row, column + 1, lineY, lineZ, angle, offsetZ, half),
						vertex(x0, row, column, lineY, lineZ, angle, offsetZ, half),
						vertex(x0, row + 1, column, lineY, lineZ, angle, offsetZ, half),
						vertex(x1, row + 1, column + 1, lineY, lineZ, angle, offsetZ, half),
						innerU(x1), innerU(x0), row, false, angle);
			}
		}

		// Side and bottom edges, textured with the vanilla edge columns.
		for (int row = 0; row < ROWS; row++) {
			edge(pose, buffer, params.light, -WIDTH / 2, 0, row, lineY, lineZ, angle, offsetZ, 0.5F / 64.0F);
			edge(pose, buffer, params.light, WIDTH / 2, COLUMNS, row, lineY, lineZ, angle, offsetZ, 11.5F / 64.0F);
		}
		for (int column = 0; column < COLUMNS; column++) {
			float x0 = column / (float) COLUMNS * WIDTH - WIDTH / 2;
			float x1 = (column + 1) / (float) COLUMNS * WIDTH - WIDTH / 2;
			float[] a = vertex(x0, ROWS, column, lineY, lineZ, angle, offsetZ, -half);
			float[] b = vertex(x1, ROWS, column + 1, lineY, lineZ, angle, offsetZ, -half);
			float[] c = vertex(x1, ROWS, column + 1, lineY, lineZ, angle, offsetZ, half);
			float[] d = vertex(x0, ROWS, column, lineY, lineZ, angle, offsetZ, half);
			float u0 = (11.0F + (x0 + WIDTH / 2)) / 64.0F;
			float u1 = (11.0F + (x1 + WIDTH / 2)) / 64.0F;
			float v = 0.5F / 32.0F;
			float ny = Mth.cos(angle[ROWS]);
			float nz = -Mth.sin(angle[ROWS]);
			put(pose, buffer, a, u0, v, params.light, 0.0F, ny, nz);
			put(pose, buffer, b, u1, v, params.light, 0.0F, ny, nz);
			put(pose, buffer, c, u1, v, params.light, 0.0F, ny, nz);
			put(pose, buffer, d, u0, v, params.light, 0.0F, ny, nz);
		}
	}

	/** Position of a grid point, pushed along the cloth normal by {@code depth} pixels. */
	private static float[] vertex(float x, int row, int column, float[] lineY, float[] lineZ, float[] angle, float[][] offsetZ, float depth) {
		float a = angle[row];
		// Normal of the center line, pointing to +z (towards the body) for depth > 0.
		float ny = Mth.sin(a);
		float nz = Mth.cos(a);
		float flutter = offsetZ[row][column];
		return new float[]{x, lineY[row] + ny * (depth - flutter), lineZ[row] + nz * (depth - flutter)};
	}

	private static float outerU(float x) {
		// Outer (north) face at (1, 1), 10x16, u increasing with x like a vanilla box face.
		return (1.0F + (x + WIDTH / 2)) / 64.0F;
	}

	private static float innerU(float x) {
		// Inner (south) face at (12, 1), mirrored.
		return (12.0F + (WIDTH / 2 - x)) / 64.0F;
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int light, float[] a, float[] b, float[] c, float[] d,
			float uA, float uB, int row, boolean outer, float[] angle) {
		float v0 = (1.0F + row * LENGTH / ROWS) / 32.0F;
		float v1 = (1.0F + (row + 1) * LENGTH / ROWS) / 32.0F;
		float mid = (angle[row] + angle[row + 1]) / 2;
		float sign = outer ? -1.0F : 1.0F;
		float ny = Mth.sin(mid) * sign;
		float nz = Mth.cos(mid) * sign;
		put(pose, buffer, a, uA, v0, light, 0.0F, ny, nz);
		put(pose, buffer, b, uB, v0, light, 0.0F, ny, nz);
		put(pose, buffer, c, uB, v1, light, 0.0F, ny, nz);
		put(pose, buffer, d, uA, v1, light, 0.0F, ny, nz);
	}

	private static void edge(PoseStack.Pose pose, VertexConsumer buffer, int light, float x, int column, int row,
			float[] lineY, float[] lineZ, float[] angle, float[][] offsetZ, float u) {
		float half = THICKNESS / 2;
		float[] a = vertex(x, row, column, lineY, lineZ, angle, offsetZ, -half);
		float[] b = vertex(x, row, column, lineY, lineZ, angle, offsetZ, half);
		float[] c = vertex(x, row + 1, column, lineY, lineZ, angle, offsetZ, half);
		float[] d = vertex(x, row + 1, column, lineY, lineZ, angle, offsetZ, -half);
		float v0 = (1.0F + row * LENGTH / ROWS) / 32.0F;
		float v1 = (1.0F + (row + 1) * LENGTH / ROWS) / 32.0F;
		float nx = Math.signum(x);
		put(pose, buffer, a, u, v0, light, nx, 0.0F, 0.0F);
		put(pose, buffer, b, u, v0, light, nx, 0.0F, 0.0F);
		put(pose, buffer, c, u, v1, light, nx, 0.0F, 0.0F);
		put(pose, buffer, d, u, v1, light, nx, 0.0F, 0.0F);
	}

	private static void put(PoseStack.Pose pose, VertexConsumer buffer, float[] p, float u, float v, int light, float nx, float ny, float nz) {
		buffer.addVertex(pose, p[0] * PIXEL, p[1] * PIXEL, p[2] * PIXEL)
				.setColor(-1)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, nx, ny, nz);
	}
}
