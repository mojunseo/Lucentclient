package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.particle.ExplodeParticle;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.HugeExplosionSeedParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.WaterDropParticle;
import net.minecraft.util.RandomSource;

/** Fewer particles: a share of all new particles, a cap on how many exist, and switches for noisy kinds. */
public class ParticlesModule extends Module {
	private final NumberSetting amount = setting(new NumberSetting("particle_amount", 50, 0, 100, 5, "%"));
	private final NumberSetting max = setting(new NumberSetting("particle_max", 2000, 100, 16000, 100, ""));
	private final BooleanSetting blockBreaking = setting(new BooleanSetting("particles_block_breaking", true));
	private final BooleanSetting explosions = setting(new BooleanSetting("particles_explosions", true));
	private final BooleanSetting rainSplashes = setting(new BooleanSetting("particles_rain", true));

	private final RandomSource random = RandomSource.create();

	public ParticlesModule() {
		super("particles", Category.PERFORMANCE, false);
	}

	/** Whether a new particle should be dropped, given how many particles exist right now. */
	public boolean drops(Particle particle, int existing) {
		if (!isEnabled()) return false;
		if (!blockBreaking.enabled() && particle instanceof TerrainParticle) return true;
		if (!explosions.enabled() && (particle instanceof ExplodeParticle || particle instanceof HugeExplosionParticle
				|| particle instanceof HugeExplosionSeedParticle)) return true;
		if (!rainSplashes.enabled() && (particle instanceof WaterDropParticle || particle instanceof SplashParticle)) return true;
		if (existing >= max.intValue()) return true;
		return random.nextFloat() * 100.0F >= amount.floatValue();
	}
}
