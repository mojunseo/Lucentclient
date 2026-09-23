package io.github.mojunseo.lucentclient.client.module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

	/** Called every frame while the module is enabled and the HUD is visible. */
	public void extractHud(Minecraft minecraft, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
	}
}
