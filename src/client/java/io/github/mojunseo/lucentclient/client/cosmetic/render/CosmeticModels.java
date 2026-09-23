package io.github.mojunseo.lucentclient.client.cosmetic.render;

import io.github.mojunseo.lucentclient.LucentClient;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Model layers for cosmetics. Each one is a player mesh with every vanilla cube removed, so the
 * cosmetic parts hang off the player's head or body and follow its animation.
 *
 * <p>Model space: +x is the player's left, +y is down, +z is behind the player. Wing sizes and
 * texture offsets must match tools/gen_cosmetic_textures.py.
 */
public final class CosmeticModels {
	public static final Map<String, ModelLayerLocation> LAYERS = Map.of(
			"feathered_wings", layer("feathered_wings"),
			"dragon_wings", layer("dragon_wings"),
			"butterfly_wings", layer("butterfly_wings"),
			"top_hat", layer("top_hat"),
			"crown", layer("crown"),
			"halo", layer("halo"));

	private CosmeticModels() {
	}

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(LucentClient.id(name), "main");
	}

	public static void register() {
		Map<String, Supplier<LayerDefinition>> definitions = Map.of(
				"feathered_wings", CosmeticModels::featheredWings,
				"dragon_wings", CosmeticModels::dragonWings,
				"butterfly_wings", CosmeticModels::butterflyWings,
				"top_hat", CosmeticModels::topHat,
				"crown", CosmeticModels::crown,
				"halo", CosmeticModels::halo);
		definitions.forEach((name, definition) -> ModelLayerRegistry.registerModelLayer(LAYERS.get(name), definition::get));
	}

	private static MeshDefinition emptyPlayer() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		mesh.getRoot().clearRecursively();
		return mesh;
	}

	private static PartPose pose(float x, float y, float z, float xRot, float yRot, float zRot, float scale) {
		return new PartPose(x, y, z, xRot, yRot, zRot, scale, scale, 1.0F);
	}

	/** Adds a left and a right wing to the body; {@code builder} builds one side, {@code side} is +1 or -1. */
	private static void bothWings(PartDefinition body, float x, float y, float z, WingBuilder builder) {
		builder.build(body.addOrReplaceChild("left_wing", CubeListBuilder.create(), PartPose.offset(x, y, z)), 1);
		builder.build(body.addOrReplaceChild("right_wing", CubeListBuilder.create(), PartPose.offset(-x, y, z)), -1);
	}

	@FunctionalInterface
	private interface WingBuilder {
		void build(PartDefinition shoulder, int side);
	}

	/** A bone along the wing (+x on the left, -x on the right). */
	private static CubeListBuilder bone(int u, int v, float length, float thickness, int side) {
		CubeListBuilder cubes = CubeListBuilder.create().texOffs(u, v);
		if (side < 0) cubes.mirror();
		float start = side > 0 ? 0.0F : -length;
		return cubes.addBox(start, -thickness / 2, -thickness / 2, length, thickness, thickness);
	}

	/** A flat, 0-thick part lying in the wing plane, hanging down from its pivot. */
	private static CubeListBuilder flat(int u, int v, float width, float height, float xOffset, int side) {
		CubeListBuilder cubes = CubeListBuilder.create().texOffs(u, v);
		if (side < 0) cubes.mirror();
		float start = side > 0 ? xOffset : -xOffset - width;
		return cubes.addBox(start, 0.0F, 0.0F, width, height, 0.0F);
	}

	// --- Feathered wings: three bones with layered flight feathers and coverts. ---

	private static LayerDefinition featheredWings() {
		MeshDefinition mesh = emptyPlayer();
		bothWings(mesh.getRoot().getChild("body"), 1.5F, 1.5F, 2.4F, (shoulder, side) -> {
			PartDefinition humerus = shoulder.addOrReplaceChild("humerus", bone(32, 0, 7.0F, 2.0F, side), PartPose.ZERO);
			PartDefinition forearm = humerus.addOrReplaceChild("forearm", bone(32, 4, 8.0F, 1.6F, side), PartPose.offset(7.0F * side, 0.0F, 0.0F));
			PartDefinition hand = forearm.addOrReplaceChild("hand", bone(32, 8, 6.0F, 1.2F, side), PartPose.offset(8.0F * side, 0.0F, 0.0F));

			// Primaries fan out from the hand, longest at the tip.
			for (int i = 0; i < 6; i++) {
				float angle = -(0.18F + i * 0.22F) * side;
				hand.addOrReplaceChild("primary_" + i, flat(0, 0, 4.0F, 20.0F, -2.0F, side),
						pose((0.5F + i * 1.1F) * side, 0.0F, 0.06F * i, 0.04F * i, 0.0F, angle, 0.85F + i * 0.05F));
			}
			// Secondaries hang from the forearm, slightly under the primaries.
			for (int i = 0; i < 7; i++) {
				forearm.addOrReplaceChild("secondary_" + i, flat(8, 0, 4.0F, 16.0F, -2.0F, side),
						pose((0.6F + i * 1.15F) * side, 0.0F, -0.1F + 0.03F * i, 0.03F, 0.0F, -(0.02F + i * 0.025F) * side, 1.0F - i * 0.02F));
			}
			// Tertials near the body, shorter and angled inwards.
			for (int i = 0; i < 4; i++) {
				humerus.addOrReplaceChild("tertial_" + i, flat(8, 0, 4.0F, 16.0F, -2.0F, side),
						pose((1.0F + i * 1.6F) * side, 0.0F, -0.1F + 0.03F * i, 0.02F, 0.0F, (0.16F - i * 0.04F) * side, 0.7F + i * 0.04F));
			}
			// Coverts sit on the back side of the wing, over the roots of the flight feathers.
			for (int i = 0; i < 7; i++) {
				forearm.addOrReplaceChild("covert_" + i, flat(16, 0, 3.0F, 9.0F, -1.5F, side),
						pose((0.4F + i * 1.15F) * side, -0.3F, 0.25F, 0.0F, 0.0F, -0.06F * side, 1.0F));
			}
			for (int i = 0; i < 4; i++) {
				humerus.addOrReplaceChild("covert_" + i, flat(16, 0, 3.0F, 9.0F, -1.5F, side),
						pose((0.8F + i * 1.6F) * side, -0.3F, 0.25F, 0.0F, 0.0F, 0.05F * side, 0.9F));
			}
			for (int i = 0; i < 4; i++) {
				hand.addOrReplaceChild("covert_" + i, flat(22, 0, 3.0F, 6.0F, -1.5F, side),
						pose((0.5F + i * 1.4F) * side, -0.2F, 0.3F, 0.0F, 0.0F, -(0.2F + i * 0.22F) * side, 1.0F));
			}
			// Small marginal coverts along the leading edge.
			for (int i = 0; i < 6; i++) {
				PartDefinition parent = i < 3 ? humerus : forearm;
				float x = i < 3 ? 1.0F + i * 2.0F : 0.5F + (i - 3) * 2.5F;
				parent.addOrReplaceChild("marginal_" + i, flat(22, 0, 3.0F, 6.0F, -1.5F, side),
						pose(x * side, -1.0F, 0.4F, 0.0F, 0.0F, -0.1F * side, 0.6F));
			}
		});
		return LayerDefinition.create(mesh, 64, 32);
	}

	// --- Dragon wings: arm, forearm and four fingers with membranes between them. ---

	static final float[] DRAGON_FINGER_ANGLES = {0.25F, 0.7F, 1.15F, 1.6F};
	static final float[] DRAGON_FINGER_LENGTHS = {16.0F, 15.0F, 13.0F, 10.0F};
	private static final int[][] DRAGON_MEMBRANES = {{0, 0, 16, 8}, {0, 8, 15, 8}, {0, 16, 13, 7}};

	private static LayerDefinition dragonWings() {
		MeshDefinition mesh = emptyPlayer();
		bothWings(mesh.getRoot().getChild("body"), 1.5F, 1.0F, 2.4F, (shoulder, side) -> {
			PartDefinition arm = shoulder.addOrReplaceChild("arm", bone(32, 0, 6.0F, 2.0F, side), PartPose.ZERO);
			// Trailing membrane from the last finger back to the waist, attached to the arm.
			arm.addOrReplaceChild("trailing_membrane", flat(0, 24, 15.0F, 11.0F, 0.0F, side), PartPose.ZERO);
			PartDefinition forearm = arm.addOrReplaceChild("forearm", bone(32, 4, 9.0F, 1.5F, side), PartPose.offset(6.0F * side, 0.0F, 0.0F));
			PartDefinition wrist = forearm.addOrReplaceChild("wrist", CubeListBuilder.create(), PartPose.offset(9.0F * side, 0.0F, 0.0F));
			// A claw pointing up at the wrist.
			wrist.addOrReplaceChild("claw", CubeListBuilder.create().texOffs(50, 0).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 2.5F, 1.0F),
					PartPose.rotation(0.0F, 0.0F, 0.3F * side));
			for (int i = 0; i < DRAGON_FINGER_ANGLES.length; i++) {
				PartDefinition finger = wrist.addOrReplaceChild("finger_" + i, bone(0, 40, DRAGON_FINGER_LENGTHS[i], 0.8F, side),
						PartPose.rotation(0.0F, 0.0F, DRAGON_FINGER_ANGLES[i] * side));
				if (i < DRAGON_MEMBRANES.length) {
					int[] m = DRAGON_MEMBRANES[i];
					finger.addOrReplaceChild("membrane", flat(m[0], m[1], m[2], m[3], 0.0F, side), PartPose.ZERO);
				}
			}
		});
		return LayerDefinition.create(mesh, 64, 64);
	}

	// --- Butterfly wings: a fore and a hind wing on each side. ---

	private static LayerDefinition butterflyWings() {
		MeshDefinition mesh = emptyPlayer();
		bothWings(mesh.getRoot().getChild("body"), 0.5F, 4.5F, 2.4F, (shoulder, side) -> {
			shoulder.addOrReplaceChild("hinge", bone(32, 0, 1.0F, 1.0F, side), PartPose.ZERO);
			shoulder.addOrReplaceChild("fore", flat(0, 0, 14.0F, 12.0F, 0.0F, side), PartPose.offset(0.0F, -11.0F, 0.0F));
			shoulder.addOrReplaceChild("hind", flat(0, 12, 10.0F, 10.0F, 0.0F, side), PartPose.offset(0.0F, -1.0F, 0.1F));
		});
		return LayerDefinition.create(mesh, 64, 32);
	}

	// --- Head cosmetics. The head is 8x8x8 with its top at y = -8. ---

	private static LayerDefinition topHat() {
		MeshDefinition mesh = emptyPlayer();
		mesh.getRoot().getChild("head").addOrReplaceChild("top_hat", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.5F, -9.0F, -5.5F, 11.0F, 1.0F, 11.0F)
				.texOffs(0, 12).addBox(-4.0F, -17.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static LayerDefinition crown() {
		MeshDefinition mesh = emptyPlayer();
		PartDefinition crown = mesh.getRoot().getChild("head").addOrReplaceChild("crown", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.75F, -10.5F, -4.75F, 9.5F, 2.5F, 0.5F)
				.texOffs(0, 4).addBox(-4.75F, -10.5F, 4.25F, 9.5F, 2.5F, 0.5F)
				.texOffs(0, 8).addBox(-4.75F, -10.5F, -4.25F, 0.5F, 2.5F, 8.5F)
				.texOffs(20, 8).addBox(4.25F, -10.5F, -4.25F, 0.5F, 2.5F, 8.5F),
				PartPose.ZERO);
		// Points along the rim: corners and the middle of each side.
		float[][] points = {{-4.5F, -4.5F}, {4.0F, -4.5F}, {-4.5F, 4.0F}, {4.0F, 4.0F}, {-0.25F, -4.75F}, {-0.25F, 4.25F}, {-4.75F, -0.25F}, {4.25F, -0.25F}};
		for (int i = 0; i < points.length; i++) {
			crown.addOrReplaceChild("point_" + i,
					CubeListBuilder.create().texOffs(40, 0).addBox(points[i][0], -12.0F, points[i][1], 0.5F, 1.5F, 0.5F),
					PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static LayerDefinition halo() {
		MeshDefinition mesh = emptyPlayer();
		PartDefinition halo = mesh.getRoot().getChild("head").addOrReplaceChild("halo", CubeListBuilder.create(),
				PartPose.offset(0.0F, -12.0F, 0.0F));
		// An octagonal ring: eight segments rotated around the vertical axis.
		for (int i = 0; i < 8; i++) {
			halo.addOrReplaceChild("segment_" + i,
					CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -0.5F, -5.0F, 4.0F, 1.0F, 1.0F),
					PartPose.rotation(0.0F, i * (float) Math.PI / 4.0F, 0.0F));
		}
		return LayerDefinition.create(mesh, 16, 16);
	}
}
