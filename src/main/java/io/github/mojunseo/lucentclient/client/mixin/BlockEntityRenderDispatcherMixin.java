package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=26.1 {
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
	//? if >=26.1 {
	@Shadow
	private Vec3 cameraPos;

	//? if >=26.2 {
	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void lucentclient$cull(BlockEntity blockEntity, float partialTicks, ModelFeatureRenderer.CrumblingOverlay breakProgress,
			boolean isGloballyRendered, CallbackInfoReturnable<Object> cir) {
		// Globally rendered block entities (beacon beams, ...) are meant to be seen from afar.
		if (!isGloballyRendered) cull(blockEntity, cir);
	}
	//?} else {
	/*@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void lucentclient$cull(BlockEntity blockEntity, float partialTicks, ModelFeatureRenderer.CrumblingOverlay breakProgress,
			CallbackInfoReturnable<Object> cir) {
		// Beacon beams are meant to be seen from afar.
		if (!(blockEntity instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity)) cull(blockEntity, cir);
	}
	*///?}

	private void cull(BlockEntity blockEntity, CallbackInfoReturnable<Object> cir) {
		if (cameraPos != null && ModuleManager.ENTITY_CULLING.cullsBlockEntity(blockEntity.getBlockPos(), cameraPos)) {
			cir.setReturnValue(null);
		}
	}
	//?} else {
	/*// Before 26.1 there is no render-state extraction step; the classic render() call itself is
	// cancelled instead. Beacon beams are meant to be seen from afar.
	@Shadow
	private Camera camera;

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private <E extends BlockEntity> void lucentclient$cull(E blockEntity, float partialTicks, PoseStack poseStack,
			MultiBufferSource bufferSource, CallbackInfo ci) {
		if (blockEntity instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity) return;
		if (camera == null) return;
		if (ModuleManager.ENTITY_CULLING.cullsBlockEntity(blockEntity.getBlockPos(), camera.getPosition())) ci.cancel();
	}
	*///?}
}
