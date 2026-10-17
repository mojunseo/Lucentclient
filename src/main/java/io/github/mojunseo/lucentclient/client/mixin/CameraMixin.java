package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Before 26.1, FOV is computed by GameRenderer#getFov(Camera, float, boolean), not Camera#calculateFov.
//? if >=26.1 {
import net.minecraft.client.Camera;

@Mixin(Camera.class)
public class CameraMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void lucentclient$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(ModuleManager.ZOOM.modifyFov(cir.getReturnValue()));
	}
}
//?} else {
/*import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class CameraMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void lucentclient$zoom(Camera camera, float partialTick, boolean usesFovSetting, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(ModuleManager.ZOOM.modifyFov(cir.getReturnValue()));
	}
}
*///?}
