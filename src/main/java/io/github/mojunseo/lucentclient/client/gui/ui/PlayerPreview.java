package io.github.mojunseo.lucentclient.client.gui.ui;

import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import io.github.mojunseo.lucentclient.client.compat.GuiGraphicsExtractor;
*///?}
//? if >=26.1 {
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//?} else {
/*import net.minecraft.client.gui.screens.inventory.InventoryScreen;
*///?}
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PlayerPreview {
	private PlayerPreview() {
	}

	/** Like InventoryScreen's preview, but turned by an arbitrary yaw so the back is visible too. */
	public static void extract(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1, int size, float yaw) {
		//? if >=26.1 {
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
		//?} else {
		/*
		// No render-state based entity drawing before 26.1: turn the entity itself, draw with the
		// classic InventoryScreen helper, then put its rotation back the way renderEntityInInventory
		// (vanilla's own mouse-follow variant) restores it afterwards.
		float savedBodyRot = entity.yBodyRot;
		float savedYRot = entity.getYRot();
		float savedXRot = entity.getXRot();
		float savedHeadRotO = entity.yHeadRotO;
		float savedHeadRot = entity.yHeadRot;
		entity.yBodyRot = 180.0F + yaw;
		entity.setYRot(0.0F);
		entity.setXRot(0.0F);
		entity.yHeadRot = entity.getYRot();
		entity.yHeadRotO = entity.getYRot();
		Vector3f translation = new Vector3f(0.0F, entity.getBbHeight() / 2.0F + 0.0625F, 0.0F);
		float scale = size / entity.getScale();
		InventoryScreen.renderEntityInInventory(graphics.delegate(), (x0 + x1) / 2.0F, (y0 + y1) / 2.0F, scale,
				translation, new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(), entity);
		entity.yBodyRot = savedBodyRot;
		entity.setYRot(savedYRot);
		entity.setXRot(savedXRot);
		entity.yHeadRotO = savedHeadRotO;
		entity.yHeadRot = savedHeadRot;
		*///?}
	}
}
