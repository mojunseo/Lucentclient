package io.github.mojunseo.lucentclient.client.cosmetic.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mojunseo.lucentclient.client.cosmetic.Cosmetic;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticType;
import io.github.mojunseo.lucentclient.client.cosmetic.PlayerCosmetics;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.Items;

import java.util.Map;

/** Draws wings, hats and halos. Capes reuse the vanilla cape layer (see AvatarRendererMixin). */
public class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private final CosmeticModel wings;
	private final CosmeticModel halo;
	private final Map<String, CosmeticModel> hats;

	public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet models) {
		super(renderer);
		this.wings = new CosmeticModel(models.bakeLayer(CosmeticModels.WINGS));
		this.halo = new CosmeticModel(models.bakeLayer(CosmeticModels.HALO));
		this.hats = Map.of(
				"top_hat", new CosmeticModel(models.bakeLayer(CosmeticModels.TOP_HAT)),
				"crown", new CosmeticModel(models.bakeLayer(CosmeticModels.CROWN)));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
		PlayerCosmetics cosmetics = state.getData(CosmeticRenderState.COSMETICS);
		if (cosmetics == null || cosmetics.isEmpty() || state.isInvisible) return;
		int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);

		Cosmetic wingsCosmetic = cosmetics.get(CosmeticType.WINGS);
		if (wingsCosmetic != null && !state.chestEquipment.is(Items.ELYTRA)) {
			collector.submitModel(wings, state, poseStack, RenderTypes.entityCutout(wingsCosmetic.texturePath()),
					lightCoords, overlay, wingsCosmetic.tint(), null, state.outlineColor);
		}

		Cosmetic hat = cosmetics.get(CosmeticType.HAT);
		if (hat != null && state.headEquipment.isEmpty() && hats.containsKey(hat.id())) {
			collector.submitModel(hats.get(hat.id()), state, poseStack, RenderTypes.entityCutout(hat.texturePath()),
					lightCoords, overlay, hat.tint(), null, state.outlineColor);
		}

		Cosmetic haloCosmetic = cosmetics.get(CosmeticType.HALO);
		if (haloCosmetic != null) {
			// Full brightness so the halo glows in the dark.
			collector.submitModel(halo, state, poseStack, RenderTypes.entityCutout(haloCosmetic.texturePath()),
					LightCoordsUtil.FULL_BRIGHT, overlay, haloCosmetic.tint(), null, state.outlineColor);
		}
	}
}
