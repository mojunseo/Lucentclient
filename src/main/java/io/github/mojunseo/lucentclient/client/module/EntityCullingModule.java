package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Skips drawing entities and block entities (chests, signs, banners, ...) beyond a set distance. */
public class EntityCullingModule extends Module {
	private final NumberSetting entityDistance = setting(new NumberSetting("entity_distance", 64, 16, 160, 8, "m"));
	private final NumberSetting blockEntityDistance = setting(new NumberSetting("block_entity_distance", 48, 16, 128, 8, "m"));
	private final BooleanSetting keepPlayers = setting(new BooleanSetting("keep_players", true));

	public EntityCullingModule() {
		super("entity_culling", Category.PERFORMANCE, false);
	}

	public boolean cullsEntity(Entity entity, double camX, double camY, double camZ) {
		if (!isEnabled()) return false;
		if (entity == Minecraft.getInstance().getCameraEntity()) return false;
		if (keepPlayers.enabled() && entity instanceof Player) return false;
		double limit = entityDistance.get();
		return entity.distanceToSqr(camX, camY, camZ) > limit * limit;
	}

	public boolean cullsBlockEntity(BlockPos pos, Vec3 camera) {
		if (!isEnabled()) return false;
		double limit = blockEntityDistance.get();
		return Vec3.atCenterOf(pos).distanceToSqr(camera) > limit * limit;
	}
}
