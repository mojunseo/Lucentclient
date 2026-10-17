package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.mojunseo.lucentclient.client.module.CpsTracker;
import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
//? if >=26.1 {
import net.minecraft.client.input.MouseButtonInfo;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void lucentclient$zoomScroll(long window, double scrollX, double scrollY, CallbackInfo ci) {
		if (Mc.screen(minecraft) == null && ModuleManager.ZOOM.isEnabled() && ModuleManager.ZOOM.onScroll(scrollY)) ci.cancel();
	}

	//? if >=26.1 {
	@Inject(method = "onButton", at = @At("HEAD"))
	private void lucentclient$countClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		if (action == InputConstants.PRESS && Mc.screen(minecraft) == null) CpsTracker.click(info.button());
	}
	//?} else {
	/*@Inject(method = "onPress", at = @At("HEAD"))
	private void lucentclient$countClick(long window, int button, int action, int mods, CallbackInfo ci) {
		if (action == InputConstants.PRESS && Mc.screen(minecraft) == null) CpsTracker.click(button);
	}
	*///?}
}
