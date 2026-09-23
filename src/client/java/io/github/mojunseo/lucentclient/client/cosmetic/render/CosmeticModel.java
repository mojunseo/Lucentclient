package io.github.mojunseo.lucentclient.client.cosmetic.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/** Player-shaped model whose only cubes are cosmetic parts, so it poses exactly like the player. */
public class CosmeticModel extends PlayerModel {
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart halo;

	public CosmeticModel(ModelPart root) {
		super(root, false);
		this.leftWing = body.hasChild("left_wing") ? body.getChild("left_wing") : null;
		this.rightWing = body.hasChild("right_wing") ? body.getChild("right_wing") : null;
		this.halo = head.hasChild("halo") ? head.getChild("halo") : null;
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);
		float time = state.ageInTicks;
		if (leftWing != null && rightWing != null) {
			// Wings sweep back from the shoulders and flap gently; faster while moving.
			float flap = Mth.sin(time * (0.12F + state.walkAnimationSpeed * 0.25F)) * 0.22F;
			float spread = state.isCrouching ? 0.9F : 0.45F;
			leftWing.yRot = -spread - flap;
			rightWing.yRot = spread + flap;
			leftWing.zRot = -0.1F;
			rightWing.zRot = 0.1F;
		}
		if (halo != null) {
			halo.yRot = time * 0.05F;
			halo.y += Mth.sin(time * 0.1F) * 0.4F;
		}
	}
}
