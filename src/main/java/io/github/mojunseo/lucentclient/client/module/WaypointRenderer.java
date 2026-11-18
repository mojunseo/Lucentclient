package io.github.mojunseo.lucentclient.client.module;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.mojunseo.lucentclient.client.compat.Mc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
*///?}

/**
 * Draws {@link ModuleManager#WAYPOINTS}'s markers in the world: a small colored pillar plus a
 * floating name, for every waypoint in the player's current dimension and within its render
 * distance. Registered once from {@code LucentClientClient}.
 *
 * <p>Before 26.1, world-space quads are drawn straight into a {@code VertexConsumer} fetched from
 * the frame's {@code MultiBufferSource}, and a floating name needs its own billboard rotation and
 * {@code Font.drawInBatch} call, and the line buffer must be flushed by hand since lines aren't
 * part of the normal terrain batch order. From 26.1 on there is no such buffer; custom geometry
 * and name tags instead go through the deferred {@code SubmitNodeCollector}, which already knows
 * how to face text toward the camera and flushes on its own.
 */
public final class WaypointRenderer {
	private static final float PILLAR_HEIGHT = 40.0F;
	private static final float PILLAR_RADIUS = 0.15F;

	private WaypointRenderer() {
	}

	public static void register() {
		//? if >=26.1 {
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			if (!ModuleManager.WAYPOINTS.isEnabled()) return;
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.level == null) return;
			String dimension = Mc.dimensionId(minecraft.level);
			double maxDistance = ModuleManager.WAYPOINTS.renderDistance();
			Vec3 cameraPos = context.levelState().cameraRenderState.pos;
			for (Waypoint waypoint : ModuleManager.WAYPOINTS.waypoints()) {
				if (!waypoint.dimension().equals(dimension)) continue;
				double dx = waypoint.x() - cameraPos.x;
				double dy = waypoint.y() - cameraPos.y;
				double dz = waypoint.z() - cameraPos.z;
				double distanceSq = dx * dx + dy * dy + dz * dz;
				if (distanceSq > maxDistance * maxDistance) continue;

				PoseStack poseStack = context.poseStack();
				poseStack.pushPose();
				poseStack.translate(dx, dy, dz);
				if (ModuleManager.WAYPOINTS.showBeam()) {
					context.submitNodeCollector().submitCustomGeometry(poseStack, Mc.linesRenderType(),
							(pose, buffer) -> drawPillar(pose, buffer, waypoint.color()));
				}
				//? if >=26.2 {
				context.submitNodeCollector().submitNameTag(poseStack, new Vec3(0, PILLAR_HEIGHT + 0.5, 0), 0,
						Component.literal(waypoint.name()), true, 15728880, context.levelState().cameraRenderState);
				//?} else {
				/*context.submitNodeCollector().submitNameTag(poseStack, new Vec3(0, PILLAR_HEIGHT + 0.5, 0), 0,
						Component.literal(waypoint.name()), true, 15728880, distanceSq, context.levelState().cameraRenderState);
				*///?}
				poseStack.popPose();
			}
		});
		//?} else {
		/*WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			if (!ModuleManager.WAYPOINTS.isEnabled()) return;
			Minecraft minecraft = Minecraft.getInstance();
			PoseStack poseStack = context.matrixStack();
			MultiBufferSource consumers = context.consumers();
			if (minecraft.level == null || poseStack == null || consumers == null) return;
			String dimension = Mc.dimensionId(minecraft.level);
			double maxDistance = ModuleManager.WAYPOINTS.renderDistance();
			Vec3 cameraPos = context.camera().getPosition();
			Quaternionf cameraRotation = context.camera().rotation();
			VertexConsumer lineBuffer = consumers.getBuffer(Mc.linesRenderType());
			boolean drewAny = false;
			for (Waypoint waypoint : ModuleManager.WAYPOINTS.waypoints()) {
				if (!waypoint.dimension().equals(dimension)) continue;
				double dx = waypoint.x() - cameraPos.x;
				double dy = waypoint.y() - cameraPos.y;
				double dz = waypoint.z() - cameraPos.z;
				if (dx * dx + dy * dy + dz * dz > maxDistance * maxDistance) continue;
				drewAny = true;

				if (ModuleManager.WAYPOINTS.showBeam()) {
					poseStack.pushPose();
					poseStack.translate(dx, dy, dz);
					drawPillar(poseStack.last(), lineBuffer, waypoint.color());
					poseStack.popPose();
				}

				poseStack.pushPose();
				poseStack.translate(dx, dy + PILLAR_HEIGHT + 0.5, dz);
				poseStack.mulPose(cameraRotation);
				poseStack.scale(0.025F, -0.025F, 0.025F);
				Matrix4f matrix = poseStack.last().pose();
				Component label = Component.literal(waypoint.name());
				float textX = -minecraft.font.width(label) / 2.0F;
				minecraft.font.drawInBatch(label, textX, 0, -1, false, matrix, consumers, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
				poseStack.popPose();
			}
			if (drewAny && consumers instanceof MultiBufferSource.BufferSource bufferSource) bufferSource.endLastBatch();
		});
		*///?}
	}

	/** A thin cross of four vertical lines, so the beam reads from any viewing angle. */
	private static void drawPillar(PoseStack.Pose pose, VertexConsumer buffer, int color) {
		line(pose, buffer, color, -PILLAR_RADIUS, 0, 0, -PILLAR_RADIUS, PILLAR_HEIGHT, 0);
		line(pose, buffer, color, PILLAR_RADIUS, 0, 0, PILLAR_RADIUS, PILLAR_HEIGHT, 0);
		line(pose, buffer, color, 0, 0, -PILLAR_RADIUS, 0, PILLAR_HEIGHT, -PILLAR_RADIUS);
		line(pose, buffer, color, 0, 0, PILLAR_RADIUS, 0, PILLAR_HEIGHT, PILLAR_RADIUS);
	}

	private static void line(PoseStack.Pose pose, VertexConsumer buffer, int color,
			float x0, float y0, float z0, float x1, float y1, float z1) {
		buffer.addVertex(pose, x0, y0, z0).setColor(color).setNormal(pose, 0.0F, 1.0F, 0.0F);
		buffer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, 0.0F, 1.0F, 0.0F);
	}
}
