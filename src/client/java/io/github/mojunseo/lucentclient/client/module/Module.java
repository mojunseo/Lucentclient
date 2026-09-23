package io.github.mojunseo.lucentclient.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.mojunseo.lucentclient.client.module.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
	private final String id;
	private final List<Setting<?>> settings = new ArrayList<>();
	private boolean enabled;

	protected Module(String id, boolean enabledByDefault) {
		this.id = id;
		this.enabled = enabledByDefault;
	}

	public String id() {
		return id;
	}

	public Component name() {
		return Component.translatable("module.lucentclient." + id);
	}

	protected <S extends Setting<?>> S setting(S setting) {
		settings.add(setting);
		return setting;
	}

	public List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		if (!enabled) onDisable();
	}

	protected void onDisable() {
	}

	public void tick(Minecraft minecraft) {
	}

	public void read(JsonObject json) {
		if (json.has("enabled")) setEnabled(json.get("enabled").getAsBoolean());
		if (json.get("settings") instanceof JsonObject saved) {
			for (Setting<?> setting : settings) {
				JsonElement value = saved.get(setting.id());
				if (value != null) setting.read(value);
			}
		}
	}

	public void write(JsonObject json) {
		json.addProperty("enabled", enabled);
		JsonObject saved = new JsonObject();
		for (Setting<?> setting : settings) saved.add(setting.id(), setting.write());
		json.add("settings", saved);
	}
}
