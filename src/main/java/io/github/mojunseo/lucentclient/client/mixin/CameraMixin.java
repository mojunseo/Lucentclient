package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void lucentclient$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(ModuleManager.ZOOM.modifyFov(cir.getReturnValue()));
	}
}
