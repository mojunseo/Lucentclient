package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.cosmetic.Cosmetic;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticType;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticsManager;
import io.github.mojunseo.lucentclient.client.cosmetic.PlayerCosmetics;
import io.github.mojunseo.lucentclient.client.cosmetic.render.CosmeticMotion;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
import io.github.mojunseo.lucentclient.client.cosmetic.render.CosmeticRenderState;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
//?} else {
/*import io.github.mojunseo.lucentclient.client.compat.CosmeticDataHolder;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
*///?}

//? if >=26.1 {
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"))
	private void lucentclient$extractCosmetics(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
		if (!(entity instanceof AbstractClientPlayer player)) {
			state.setData(CosmeticRenderState.COSMETICS, null);
			state.setData(CosmeticRenderState.MOTION, null);
			return;
		}
		PlayerCosmetics cosmetics = CosmeticsManager.get(player.getUUID());
		state.setData(CosmeticRenderState.COSMETICS, cosmetics);
		state.setData(CosmeticRenderState.MOTION, cosmetics.isEmpty() ? null : CosmeticMotion.update(state));

		// Swap the skin's cape so the vanilla cape layer (and its physics) draws ours.
		Cosmetic cape = cosmetics.get(CosmeticType.CAPE);
		if (cape != null) {
			PlayerSkin skin = state.skin;
			state.skin = new PlayerSkin(skin.body(), new ClientAsset.ResourceTexture(cape.texture()), skin.elytra(), skin.model(), skin.secure());
		}
	}
}
//?} else {
/*@Mixin(PlayerRenderer.class)
public class AvatarRendererMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
			at = @At("TAIL"))
	private void lucentclient$extractCosmetics(AbstractClientPlayer player, PlayerRenderState state, float partialTicks, CallbackInfo ci) {
		CosmeticDataHolder holder = (CosmeticDataHolder) state;
		PlayerCosmetics cosmetics = CosmeticsManager.get(player.getUUID());
		holder.lucentclient$setCosmeticData("cosmetics", cosmetics);
		holder.lucentclient$setCosmeticData("motion", cosmetics.isEmpty() ? null : CosmeticMotion.update(state));

		// Swap the skin's cape so the vanilla cape layer (and its physics) draws ours.
		Cosmetic cape = cosmetics.get(CosmeticType.CAPE);
		if (cape != null) {
			PlayerSkin skin = state.skin;
			state.skin = new PlayerSkin(skin.texture(), skin.textureUrl(), cape.texture(), skin.elytraTexture(), skin.model(), skin.secure());
		}
	}
}
*///?}
