package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
	// Fullbright: light everything as if the player had night vision.
	@Inject(method = "extract", at = @At("RETURN"))
	private void lucentclient$fullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		if (!ModuleManager.FULLBRIGHT.isEnabled()) return;
		state.nightVisionEffectIntensity = 1.0F;
		state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
		state.darknessEffectScale = 0.0F;
	}
}
