package io.github.mojunseo.lucentclient.client.module;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public abstract class Module {
	private final String id;
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
	}

	public void write(JsonObject json) {
		json.addProperty("enabled", enabled);
	}
}
