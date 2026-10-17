package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.1 {
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
//?} else {
/*import java.util.List;
*///?}

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
	//? if >=26.1 {
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
	// Before 26.1 there is no render state to patch; the per-frame rain/snow column lists this
	// populates (ColumnInstance is package-private, so these are typed as raw lists) are cleared
	// here instead, right after collectColumnInstances() builds them.
	//?} else {
	/*@Inject(method = "collectColumnInstances", at = @At("TAIL"))
	private void lucentclient$hideWeather(net.minecraft.world.level.Level level, int lightCoords, float partialTicks,
			net.minecraft.world.phys.Vec3 cameraPos, int radius, List<?> rain, List<?> snow, CallbackInfo ci) {
		if (ModuleManager.WEATHER.hidesRain()) rain.clear();
		if (ModuleManager.WEATHER.hidesSnow()) snow.clear();
	}
	*///?}
}
