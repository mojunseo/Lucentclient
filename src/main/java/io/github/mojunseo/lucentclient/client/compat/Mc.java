package io.github.mojunseo.lucentclient.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import org.jspecify.annotations.Nullable;

/**
 * Everything that differs between the Minecraft versions Lucent Client is built for goes through
 * here, so the rest of the mod can be written once. Branches are selected by Stonecutter comments.
 */
public final class Mc {
	private Mc() {
	}

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

	public static Identifier mobEffectSprite(Holder<MobEffect> effect) {
		//? if >=26.2 {
		return net.minecraft.client.gui.Hud.getMobEffectSprite(effect);
		//?} else {
		/*return net.minecraft.client.gui.Gui.getMobEffectSprite(effect);
		*///?}
	}

	/** Submits a model tinted with an ARGB color. */
	public static <S> void submitModel(SubmitNodeCollector collector, Model<? super S> model, S state, PoseStack poseStack,
			RenderType renderType, int light, int overlay, int tint, int outline) {
		//? if >=26.3 {
		collector.submitModel(model, state, poseStack, renderType, light, overlay, tint, null, outline);
		//?} else {
		/*collector.submitModel(model, state, poseStack, renderType, light, overlay, tint, null, outline, null);
		*///?}
	}
}
