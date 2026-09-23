package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
	private void lucentclient$fov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
		if (!ModuleManager.FOV.isEnabled()) return;
		AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
		// Leave the spyglass zoom alone.
		if (firstPerson && player.isScoping()) return;
		cir.setReturnValue(ModuleManager.FOV.modifier(player));
	}
}
