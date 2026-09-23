package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	//? if >=26.2 {
	private void lucentclient$hideWeather(net.minecraft.client.multiplayer.ClientLevel level, float partialTicks, Vec3 cameraPos,
			WeatherRenderState state, CallbackInfo ci) {
	//?} else {
	/*private void lucentclient$hideWeather(net.minecraft.world.level.Level level, int ticks, float partialTicks, Vec3 cameraPos,
			WeatherRenderState state, CallbackInfo ci) {
	*///?}
		if (ModuleManager.WEATHER.hidesRain()) state.rainColumns.clear();
		if (ModuleManager.WEATHER.hidesSnow()) state.snowColumns.clear();
	}
}
