package io.github.mojunseo.lucentclient.client.cosmetic.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Player-shaped model whose only cubes are cosmetic parts, so it poses exactly like the player.
 * Wing and halo parts, when present, are animated on top of that.
 */
public class CosmeticModel extends PlayerModel {
	private final @Nullable Wing left;
	private final @Nullable Wing right;
	private final @Nullable ModelPart halo;
	private final String kind;

	public CosmeticModel(ModelPart root, String kind) {
		super(root, false);
		this.kind = kind;
		this.left = body.hasChild("left_wing") ? new Wing(body.getChild("left_wing")) : null;
		this.right = body.hasChild("right_wing") ? new Wing(body.getChild("right_wing")) : null;
		this.halo = head.hasChild("halo") ? head.getChild("halo") : null;
	}

	/** The joints of one wing; missing joints are null. */
	private record Wing(ModelPart shoulder, @Nullable ModelPart elbow, @Nullable ModelPart wrist) {
		Wing(ModelPart shoulder) {
			this(shoulder, find(shoulder, "humerus", "forearm"), find(shoulder, "humerus", "forearm", "hand"));
		}

		private static @Nullable ModelPart find(ModelPart part, String... path) {
			for (String name : path) {
				if (!part.hasChild(name)) return null;
				part = part.getChild(name);
			}
			return part;
		}
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);
		float time = state.ageInTicks;
		CosmeticMotion motion = state.getData(CosmeticRenderState.MOTION);
		if (left != null && right != null && motion != null) {
			switch (kind) {
				case "feathered_wings" -> animateFeathered(motion);
				case "dragon_wings" -> animateDragon(motion);
				case "butterfly_wings" -> animateButterfly(motion);
				default -> {
				}
			}
		}
		if (halo != null) {
			halo.yRot = time * 0.05F;
			halo.y += Mth.sin(time * 0.1F) * 0.4F;
		}
	}

	private void animateFeathered(CosmeticMotion motion) {
		// fold: 0 = spread wide, 1 = tucked against the back.
		float fold = Mth.lerp(motion.crouch(), 0.35F - motion.moving() * 0.1F, 0.85F);
		float flap = Mth.sin(motion.wingPhase()) * (0.04F + motion.moving() * 0.07F);
		for (int side = -1; side <= 1; side += 2) {
			Wing wing = side > 0 ? left : right;
			wing.shoulder.yRot = -(0.35F + fold * 0.75F) * side;
			wing.shoulder.zRot = (-0.2F + flap) * side;
			wing.shoulder.xRot = 0.05F;
			if (wing.elbow != null) wing.elbow.yRot = -fold * 0.9F * side;
			if (wing.wrist != null) wing.wrist.yRot = fold * 0.6F * side;
		}
	}

	private void animateDragon(CosmeticMotion motion) {
		float fold = Mth.lerp(motion.crouch(), 0.4F - motion.moving() * 0.1F, 0.9F);
		float flap = Mth.sin(motion.wingPhase() * 0.8F) * (0.03F + motion.moving() * 0.06F);
		for (int side = -1; side <= 1; side += 2) {
			Wing wing = side > 0 ? left : right;
			wing.shoulder.yRot = -(0.45F + fold * 0.8F) * side;
			wing.shoulder.zRot = (-0.35F + flap) * side;
		}
	}

	private void animateButterfly(CosmeticMotion motion) {
		// Flutter that lingers open and closed; faster while moving.
		float beat = Mth.sin(motion.flutterPhase());
		float closed = beat * beat * beat * 0.5F + 0.5F;
		float open = 0.3F + closed * 1.0F;
		for (int side = -1; side <= 1; side += 2) {
			Wing wing = side > 0 ? left : right;
			wing.shoulder.yRot = -open * side;
			wing.shoulder.zRot = -0.1F * side;
		}
	}
}
