package io.github.mojunseo.lucentclient.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
//? if >=26.1 {
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;
*///?}

/**
 * Everything that differs between the Minecraft versions Lucent Client is built for goes through
 * here, so the rest of the mod can be written once. Branches are selected by Stonecutter comments.
 */
public final class Mc {
	private Mc() {
	}

	// Renamed between eras: MOVEMENT_SPEED/MOVEMENT_SLOWDOWN before 26.1, SPEED/SLOWNESS from 26.1 on.
	//? if >=26.1 {
	public static final Holder<MobEffect> SPEED = MobEffects.SPEED;
	public static final Holder<MobEffect> SLOWNESS = MobEffects.SLOWNESS;
	//?} else {
	/*public static final Holder<MobEffect> SPEED = MobEffects.MOVEMENT_SPEED;
	public static final Holder<MobEffect> SLOWNESS = MobEffects.MOVEMENT_SLOWDOWN;
	*///?}

	public static @Nullable Screen screen(Minecraft minecraft) {
		//? if >=26.2 {
		return minecraft.gui.screen();
		//?} else {
		/*return minecraft.screen;
		*///?}
	}

	public static void setScreen(Minecraft minecraft, @Nullable Screen screen) {
		//? if >=26.2 {
		minecraft.gui.setScreen(screen);
		//?} else {
		/*minecraft.setScreen(screen);
		*///?}
	}

	//? if >=26.1 {
	public static Identifier mobEffectSprite(Holder<MobEffect> effect) {
		//? if >=26.2 {
		return net.minecraft.client.gui.Hud.getMobEffectSprite(effect);
		//?} else {
		/*return net.minecraft.client.gui.Gui.getMobEffectSprite(effect);
		*///?}
	}
	//?} else {
	/*public static TextureAtlasSprite mobEffectSprite(Holder<MobEffect> effect) {
		return Minecraft.getInstance().getMobEffectTextures().get(effect);
	}
	*///?}

	public static com.mojang.blaze3d.pipeline.RenderTarget mainRenderTarget(Minecraft minecraft) {
		//? if >=26.2 {
		return minecraft.gameRenderer.mainRenderTarget();
		//?} else {
		/*return minecraft.getMainRenderTarget();
		*///?}
	}

	// Submits a model tinted with an ARGB color. Before 26.1 there is no deferred render-node
	// collector, so this draws straight into a VertexConsumer fetched from the buffer source, and
	// there is no per-model outline pass in that era, so the outline color goes unused.
	//? if >=26.1 {
	public static <S> void submitModel(SubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack,
			RenderType renderType, int light, int overlay, int tint, int outline) {
		//? if >=26.3 {
		collector.submitModel(model, state, poseStack, renderType, light, overlay, tint, null, outline);
		//?} else {
		/*collector.submitModel(model, state, poseStack, renderType, light, overlay, tint, null, outline, null);
		*///?}
	}
	//?} else {
	/*public static <S extends net.minecraft.client.renderer.entity.state.EntityRenderState> void submitModel(
			MultiBufferSource bufferSource, EntityModel<S> model, S state, PoseStack poseStack,
			RenderType renderType, int light, int overlay, int tint, int outline) {
		VertexConsumer buffer = bufferSource.getBuffer(renderType);
		model.renderToBuffer(poseStack, buffer, light, overlay, tint);
	}
	*///?}
}
