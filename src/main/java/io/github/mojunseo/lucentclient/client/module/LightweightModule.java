package io.github.mojunseo.lucentclient.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.mojunseo.lucentclient.LucentClient;
import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.server.level.ParticleStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Low-end preset: sets Minecraft's video options to their cheapest values for as long as the module
 * is on, and puts the player's own values back when it is turned off. The player's values are kept
 * in the module's config, so they come back even if the game was closed in between.
 *
 * It also pulls in the client's own culling and particle-dropping modules, since those cut real
 * rendering and simulation work that no vanilla option touches. Whatever the player had those set
 * to before is remembered and restored, the same way the vanilla options are.
 */
public class LightweightModule extends Module {
	private final NumberSetting renderDistance = setting(new NumberSetting("lite_render_distance", 6, 2, 12, 1, ""));
	private final BooleanSetting uncapFps = setting(new BooleanSetting("lite_uncap_fps", true));
	private final BooleanSetting noClouds = setting(new BooleanSetting("lite_no_clouds", true));
	private final BooleanSetting minimalParticles = setting(new BooleanSetting("lite_minimal_particles", true));
	private final BooleanSetting flatLighting = setting(new BooleanSetting("lite_flat_lighting", true));

	/** One option this preset changes: its key in the saved backup, how to find it, and the cheap value. */
	private record Tweak<T>(String key, Function<Options, OptionInstance<T>> option, Function<LightweightModule, T> value) {
		T wanted(LightweightModule module) {
			return value.apply(module);
		}
	}

	private final List<Tweak<?>> tweaks = List.of(
			new Tweak<>("renderDistance", Options::renderDistance, m -> m.renderDistance.intValue()),
			new Tweak<>("simulationDistance", Options::simulationDistance, m -> Math.max(5, Math.min(8, m.renderDistance.intValue()))),
			new Tweak<>("entityDistanceScaling", Options::entityDistanceScaling, m -> 0.5),
			new Tweak<>("biomeBlendRadius", Options::biomeBlendRadius, m -> 0),
			new Tweak<>("mipmapLevels", Options::mipmapLevels, m -> 0),
			new Tweak<>("maxAnisotropyBit", Options::maxAnisotropyBit, m -> 1),
			new Tweak<>("textureFiltering", Options::textureFiltering, m -> TextureFilteringMethod.NONE),
			new Tweak<>("entityShadows", Options::entityShadows, m -> false),
			new Tweak<>("cutoutLeaves", Options::cutoutLeaves, m -> false),
			new Tweak<>("improvedTransparency", Options::improvedTransparency, m -> false),
			new Tweak<>("weatherRadius", Options::weatherRadius, m -> 3),
			new Tweak<>("chunkSectionFadeInTime", Options::chunkSectionFadeInTime, m -> 0.0),
			new Tweak<>("prioritizeChunkUpdates", Options::prioritizeChunkUpdates, m -> PrioritizeChunkUpdates.NONE),
			new Tweak<>("menuBackgroundBlurriness", Options::menuBackgroundBlurriness, m -> 0),
			new Tweak<>("vignette", Options::vignette, m -> false),
			new Tweak<>("framerateLimit", Options::framerateLimit, m -> m.uncapFps.enabled() ? Options.UNLIMITED_FRAMERATE_CUTOFF : null),
			new Tweak<>("enableVsync", Options::enableVsync, m -> m.uncapFps.enabled() ? false : null),
			new Tweak<>("cloudStatus", Options::cloudStatus, m -> m.noClouds.enabled() ? CloudStatus.OFF : null),
			new Tweak<>("particles", Options::particles, m -> m.minimalParticles.enabled() ? ParticleStatus.MINIMAL : null),
			new Tweak<>("ambientOcclusion", Options::ambientOcclusion, m -> m.flatLighting.enabled() ? false : null));

	/** The player's own values of the options this preset changed, as saved by each option's codec. */
	private JsonObject backup = new JsonObject();

	/** Whether entity culling / particle dropping were on before this preset turned them on, so they
	 *  can go back to that instead of just being force-disabled. Null means "not currently overridden". */
	private Boolean entityCullingBackup;
	private Boolean particlesBackup;

	public LightweightModule() {
		super("lightweight", Category.PERFORMANCE, false);
	}

	@Override
	public void tick(Minecraft minecraft) {
		boolean changed = false;
		for (Tweak<?> tweak : tweaks) changed |= apply(minecraft.options, tweak);
		if (changed) {
			minecraft.options.save();
			ModuleManager.save();
		}
	}

	@Override
	protected void onEnable() {
		// Cut real rendering/simulation work the vanilla options can't reach, remembering whatever
		// the player had these set to so onDisable() can give it back instead of just turning them off.
		if (entityCullingBackup == null) entityCullingBackup = ModuleManager.ENTITY_CULLING.isEnabled();
		if (particlesBackup == null) particlesBackup = ModuleManager.PARTICLES.isEnabled();
		ModuleManager.ENTITY_CULLING.setEnabled(true);
		ModuleManager.PARTICLES.setEnabled(true);
	}

	/** Sets one option to the preset's value, backing up the player's value the first time. */
	private <T> boolean apply(Options options, Tweak<T> tweak) {
		OptionInstance<T> option = tweak.option().apply(options);
		T wanted = tweak.wanted(this);
		if (wanted == null) {
			// This part of the preset is switched off: give the player's value back.
			return restore(option, tweak.key());
		}
		if (wanted.equals(option.get())) return false;
		if (!backup.has(tweak.key())) {
			option.codec().encodeStart(JsonOps.INSTANCE, option.get()).result().ifPresent(json -> backup.add(tweak.key(), json));
		}
		option.set(wanted);
		return true;
	}

	private <T> boolean restore(OptionInstance<T> option, String key) {
		JsonElement saved = backup.remove(key);
		if (saved == null) return false;
		option.codec().parse(JsonOps.INSTANCE, saved).result().ifPresent(option::set);
		return true;
	}

	@Override
	protected void onDisable() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.options != null) {
			boolean changed = false;
			for (Tweak<?> tweak : tweaks) changed |= restore(tweak.option().apply(minecraft.options), tweak.key());
			if (changed) minecraft.options.save();
		}
		if (!backup.isEmpty()) {
			LucentClient.LOGGER.warn("Lightweight mode: no option for saved values {}", new ArrayList<>(backup.keySet()));
			backup = new JsonObject();
		}
		if (entityCullingBackup != null) {
			ModuleManager.ENTITY_CULLING.setEnabled(entityCullingBackup);
			entityCullingBackup = null;
		}
		if (particlesBackup != null) {
			ModuleManager.PARTICLES.setEnabled(particlesBackup);
			particlesBackup = null;
		}
	}

	@Override
	public void read(JsonObject json) {
		if (json.get("backup") instanceof JsonObject saved) backup = saved.deepCopy();
		if (json.has("entityCullingBackup")) entityCullingBackup = json.get("entityCullingBackup").getAsBoolean();
		if (json.has("particlesBackup")) particlesBackup = json.get("particlesBackup").getAsBoolean();
		super.read(json);
	}

	@Override
	public void write(JsonObject json) {
		super.write(json);
		json.add("backup", backup.deepCopy());
		if (entityCullingBackup != null) json.addProperty("entityCullingBackup", entityCullingBackup);
		if (particlesBackup != null) json.addProperty("particlesBackup", particlesBackup);
	}
}
