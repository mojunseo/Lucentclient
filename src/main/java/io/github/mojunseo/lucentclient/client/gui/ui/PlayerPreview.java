package io.github.mojunseo.lucentclient.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PlayerPreview {
	private PlayerPreview() {
	}

	/** Like InventoryScreen's preview, but turned by an arbitrary yaw so the back is visible too. */
	public static void extract(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1, int size, float yaw) {
		EntityRenderer<? super LivingEntity, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
		EntityRenderState state = renderer.createRenderState(entity, 1.0F);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180.0F + yaw;
			living.yRot = 0.0F;
			living.xRot = 0.0F;
			living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
			living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
			living.scale = 1.0F;
		}
		Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
		graphics.entity(state, size, translation, new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(), x0, y0, x1, y1);
	}
}
