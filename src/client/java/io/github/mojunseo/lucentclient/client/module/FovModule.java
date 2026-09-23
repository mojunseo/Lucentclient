package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

/**
 * Replaces the vanilla dynamic FOV, which is driven by movement speed attributes, with separate
 * amounts for walking, sprinting, Speed, Slowness and flying. The camera still eases towards the
 * result, so changes stay smooth.
 */
public class FovModule extends Module {
	private final NumberSetting walk = setting(new NumberSetting("fov_walk", 0, 0, 20, 1, "%"));
	private final NumberSetting sprint = setting(new NumberSetting("fov_sprint", 15, 0, 40, 1, "%"));
	private final NumberSetting speed = setting(new NumberSetting("fov_speed", 10, 0, 30, 1, "%"));
	private final NumberSetting slowness = setting(new NumberSetting("fov_slowness", 5, 0, 20, 1, "%"));
	private final NumberSetting flying = setting(new NumberSetting("fov_flying", 10, 0, 30, 1, "%"));
	private final BooleanSetting bowZoom = setting(new BooleanSetting("fov_bow", true));

	public FovModule() {
		super("fov", false);
	}

	public float modifier(AbstractClientPlayer player) {
		float modifier = 1.0F;
		boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4;
		if (player.isSprinting()) {
			modifier += sprint.floatValue() / 100.0F;
		} else if (moving) {
			modifier += walk.floatValue() / 100.0F;
		}
		MobEffectInstance speedEffect = player.getEffect(MobEffects.SPEED);
		if (speedEffect != null) modifier += (speedEffect.getAmplifier() + 1) * speed.floatValue() / 100.0F;
		MobEffectInstance slownessEffect = player.getEffect(MobEffects.SLOWNESS);
		if (slownessEffect != null) modifier -= (slownessEffect.getAmplifier() + 1) * slowness.floatValue() / 100.0F;
		if (player.getAbilities().flying) modifier += flying.floatValue() / 100.0F;

		if (bowZoom.enabled() && player.isUsingItem() && player.getUseItem().is(Items.BOW)) {
			float draw = Math.min(player.getTicksUsingItem() / 20.0F, 1.0F);
			modifier *= 1.0F - Mth.square(draw) * 0.15F;
		}
		return Mth.clamp(modifier, 0.5F, 1.5F);
	}
}
