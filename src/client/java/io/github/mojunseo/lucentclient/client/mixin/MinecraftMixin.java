package io.github.mojunseo.lucentclient.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	// Brand the window title so it's obvious the client is loaded.
	@Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
	private void lucentclient$createTitle(CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(cir.getReturnValue() + " | Lucent Client");
	}
}
