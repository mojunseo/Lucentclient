package io.github.mojunseo.lucentclient.client.cosmetic.render;

import io.github.mojunseo.lucentclient.LucentClient;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.player.PlayerModel;

/**
 * Model layers for cosmetics. Each one is a player mesh with every vanilla cube removed, so the
 * cosmetic parts hang off the player's head or body and follow its animation.
 */
public final class CosmeticModels {
	public static final ModelLayerLocation WINGS = layer("wings");
	public static final ModelLayerLocation TOP_HAT = layer("top_hat");
	public static final ModelLayerLocation CROWN = layer("crown");
	public static final ModelLayerLocation HALO = layer("halo");

	private CosmeticModels() {
	}

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(LucentClient.id(name), "main");
	}

	public static void register() {
		ModelLayerRegistry.registerModelLayer(WINGS, CosmeticModels::wings);
		ModelLayerRegistry.registerModelLayer(TOP_HAT, CosmeticModels::topHat);
		ModelLayerRegistry.registerModelLayer(CROWN, CosmeticModels::crown);
		ModelLayerRegistry.registerModelLayer(HALO, CosmeticModels::halo);
	}

	private static PartDefinition emptyPlayer(MeshDefinition mesh) {
		return mesh.getRoot().clearRecursively();
	}

	private static LayerDefinition wings() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition body = emptyPlayer(mesh).getChild("body");
		// Flat 16x20 planes; the texture's alpha gives each design its shape.
		body.addOrReplaceChild("left_wing",
				CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -8.0F, 0.0F, 16.0F, 20.0F, 0.0F),
				PartPose.offset(1.0F, 2.0F, 2.5F));
		body.addOrReplaceChild("right_wing",
				CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-16.0F, -8.0F, 0.0F, 16.0F, 20.0F, 0.0F),
				PartPose.offset(-1.0F, 2.0F, 2.5F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	private static LayerDefinition topHat() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition head = emptyPlayer(mesh).getChild("head");
		head.addOrReplaceChild("top_hat", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.5F, -9.5F, -5.5F, 11.0F, 1.0F, 11.0F)
				.texOffs(0, 12).addBox(-4.0F, -17.5F, -4.0F, 8.0F, 8.0F, 8.0F),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static LayerDefinition crown() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition crown = emptyPlayer(mesh).getChild("head").addOrReplaceChild("crown", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.75F, -11.5F, -4.75F, 9.5F, 2.5F, 0.5F)
				.texOffs(0, 4).addBox(-4.75F, -11.5F, 4.25F, 9.5F, 2.5F, 0.5F)
				.texOffs(0, 8).addBox(-4.75F, -11.5F, -4.25F, 0.5F, 2.5F, 8.5F)
				.texOffs(20, 8).addBox(4.25F, -11.5F, -4.25F, 0.5F, 2.5F, 8.5F),
				PartPose.ZERO);
		// Points along the rim: corners and the middle of each side.
		float[][] points = {{-4.5F, -4.5F}, {4.0F, -4.5F}, {-4.5F, 4.0F}, {4.0F, 4.0F}, {-0.25F, -4.75F}, {-0.25F, 4.25F}, {-4.75F, -0.25F}, {4.25F, -0.25F}};
		for (int i = 0; i < points.length; i++) {
			crown.addOrReplaceChild("point_" + i,
					CubeListBuilder.create().texOffs(40, 0).addBox(points[i][0], -13.0F, points[i][1], 0.5F, 1.5F, 0.5F),
					PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static LayerDefinition halo() {
		MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
		PartDefinition halo = emptyPlayer(mesh).getChild("head").addOrReplaceChild("halo", CubeListBuilder.create(),
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
