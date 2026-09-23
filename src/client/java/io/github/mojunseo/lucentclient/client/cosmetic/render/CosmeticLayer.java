package io.github.mojunseo.lucentclient.client.cosmetic.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mojunseo.lucentclient.client.cosmetic.Cosmetic;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticType;
import io.github.mojunseo.lucentclient.client.cosmetic.PlayerCosmetics;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Draws every Lucent cosmetic on a player. The vanilla cape layer is suppressed while a Lucent cape is worn. */
public class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private final Map<String, CosmeticModel> models = new HashMap<>();
	/** Only used to find where the vanilla cape would hang; it is never drawn. */
	private final PlayerCapeModel capePose;
	private final ModelPart capePart;

	public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, EntityModelSet modelSet) {
		super(renderer);
		CosmeticModels.LAYERS.forEach((name, layer) -> models.put(name, new CosmeticModel(modelSet.bakeLayer(layer), name)));
		this.capePose = new PlayerCapeModel(modelSet.bakeLayer(ModelLayers.PLAYER_CAPE));
		this.capePart = capePose.body.getChild("cape");
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
		PlayerCosmetics cosmetics = state.getData(CosmeticRenderState.COSMETICS);
		if (cosmetics == null || cosmetics.isEmpty() || state.isInvisible) return;
		int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
		boolean elytra = state.chestEquipment.is(Items.ELYTRA);

		Cosmetic cape = cosmetics.get(CosmeticType.CAPE);
		if (cape != null && state.showCape && !elytra) {
			submitCape(poseStack, collector, lightCoords, state, cape);
		}

		Cosmetic wings = cosmetics.get(CosmeticType.WINGS);
		if (wings != null && !elytra) {
			// The wings are 0-thick planes, so back faces must be culled to avoid z-fighting.
			submit(collector, poseStack, state, wings, RenderTypes.entityCutoutCull(wings.texturePath()), lightCoords, overlay);
		}

		Cosmetic hat = cosmetics.get(CosmeticType.HAT);
		if (hat != null && state.headEquipment.isEmpty()) {
			submit(collector, poseStack, state, hat, RenderTypes.entityCutout(hat.texturePath()), lightCoords, overlay);
		}

		Cosmetic halo = cosmetics.get(CosmeticType.HALO);
		if (halo != null) {
			// Full brightness so the halo glows in the dark.
			submit(collector, poseStack, state, halo, RenderTypes.entityCutout(halo.texturePath()), LightCoordsUtil.FULL_BRIGHT, overlay);
		}
	}

	private void submit(SubmitNodeCollector collector, PoseStack poseStack, AvatarRenderState state, Cosmetic cosmetic,
			RenderType renderType, int light, int overlay) {
		CosmeticModel model = models.get(cosmetic.model());
		if (model == null) return;
		collector.submitModel(model, state, poseStack, renderType, light, overlay, cosmetic.tint(), null, state.outlineColor);
	}

	private void submitCape(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, Cosmetic cape) {
		capePose.setupAnim(state);
		poseStack.pushPose();
		if (!state.chestEquipment.isEmpty()) {
			// Same nudge the vanilla cape gets over a chestplate.
			poseStack.translate(0.0F, -0.053125F, 0.06875F);
		}
		capePose.root().translateAndRotate(poseStack);
		capePose.body.translateAndRotate(poseStack);
		capePart.translateAndRotate(poseStack);
		ClothCapeRenderer.Params params = ClothCapeRenderer.Params.of(state, lightCoords);
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(cape.texturePath()),
				(pose, buffer) -> ClothCapeRenderer.render(pose, buffer, params));
		poseStack.popPose();
	}
}
