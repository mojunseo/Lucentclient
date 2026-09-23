package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void lucentclient$cull(Entity entity, Frustum culler, double camX, double camY, double camZ, float partialTicks,
			CallbackInfoReturnable<Boolean> cir) {
		if (ModuleManager.ENTITY_CULLING.cullsEntity(entity, camX, camY, camZ)) cir.setReturnValue(false);
	}
}
