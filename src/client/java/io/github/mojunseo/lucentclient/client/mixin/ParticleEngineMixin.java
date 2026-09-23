package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
	@Shadow
	@Final
	private Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void lucentclient$limit(Particle particle, CallbackInfo ci) {
		if (!ModuleManager.PARTICLES.isEnabled()) return;
		int existing = particlesToAdd.size();
		for (ParticleGroup<?> group : particles.values()) existing += group.size();
		if (ModuleManager.PARTICLES.drops(particle, existing)) ci.cancel();
	}
}
