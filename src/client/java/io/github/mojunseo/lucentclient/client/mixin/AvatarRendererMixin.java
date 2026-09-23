package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.cosmetic.Cosmetic;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticType;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticsManager;
import io.github.mojunseo.lucentclient.client.cosmetic.PlayerCosmetics;
import io.github.mojunseo.lucentclient.client.cosmetic.render.CosmeticRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"))
	private void lucentclient$extractCosmetics(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		if (!(entity instanceof AbstractClientPlayer player)) {
			state.setData(CosmeticRenderState.COSMETICS, null);
			return;
		}
		PlayerCosmetics cosmetics = CosmeticsManager.get(player.getUUID());
		state.setData(CosmeticRenderState.COSMETICS, cosmetics);

		// Swap the skin's cape so the vanilla cape layer (and its physics) draws ours.
		Cosmetic cape = cosmetics.get(CosmeticType.CAPE);
		if (cape != null) {
			PlayerSkin skin = state.skin;
			state.skin = new PlayerSkin(skin.body(), new ClientAsset.ResourceTexture(cape.texture()), skin.elytra(), skin.model(), skin.secure());
		}
	}
}
