package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=26.1 {
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?} else {
/*import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.injection.ModifyArg;
*///?}

// Fullbright: light everything as if the player had night vision. Before 26.1 there is no
// render-state extractor to patch, so that branch instead reaches into the two shader uniforms that
// drive night-vision-style brightening and darkness dimming directly, by position among
// LightTexture#updateLightTexture's AbstractUniform#set(float) calls (ordinals 3 = NightVisionFactor,
// 4 = DarknessScale; see that method's source for the full uniform sequence).
//? if >=26.1 {
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract", at = @At("RETURN"))
	private void lucentclient$fullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		if (!ModuleManager.FULLBRIGHT.isEnabled()) return;
		state.nightVisionEffectIntensity = Math.max(state.nightVisionEffectIntensity, ModuleManager.FULLBRIGHT.strength());
		state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
		state.darknessEffectScale = 0.0F;
	}
}
//?} else {
/*@Mixin(LightTexture.class)
public class LightmapRenderStateExtractorMixin {
	@ModifyArg(method = "updateLightTexture",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/AbstractUniform;set(F)V", ordinal = 3))
	private float lucentclient$fullbrightNightVision(float value) {
		return ModuleManager.FULLBRIGHT.isEnabled() ? Math.max(value, ModuleManager.FULLBRIGHT.strength()) : value;
	}

	@ModifyArg(method = "updateLightTexture",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/AbstractUniform;set(F)V", ordinal = 4))
	private float lucentclient$fullbrightDarkness(float value) {
		return ModuleManager.FULLBRIGHT.isEnabled() ? 0.0F : value;
	}
}
*///?}
