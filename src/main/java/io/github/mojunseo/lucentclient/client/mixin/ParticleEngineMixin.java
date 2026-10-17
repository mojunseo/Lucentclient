package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
//? if >=26.1 {
import net.minecraft.client.particle.ParticleGroup;
//?}
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
	// Before 26.1, particles are stored directly as per-type Queues rather than ParticleGroups.
	//? if >=26.1 {
	@Shadow
	@Final
	private Map<ParticleRenderType, ParticleGroup<?>> particles;
	//?} else {
	/*@Shadow
	@Final
	private Map<ParticleRenderType, Queue<Particle>> particles;
	*///?}

	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void lucentclient$limit(Particle particle, CallbackInfo ci) {
		if (!ModuleManager.PARTICLES.isEnabled()) return;
		int existing = particlesToAdd.size();
		//? if >=26.1 {
		for (ParticleGroup<?> group : particles.values()) existing += group.size();
		//?} else {
		/*for (Queue<Particle> queue : particles.values()) existing += queue.size();
		*///?}
		if (ModuleManager.PARTICLES.drops(particle, existing)) ci.cancel();
	}
}
