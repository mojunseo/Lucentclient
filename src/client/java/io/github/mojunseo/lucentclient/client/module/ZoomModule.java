package io.github.mojunseo.lucentclient.client.module;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class ZoomModule extends Module {
	private static final float ZOOM_FACTOR = 4.0F;

	private KeyMapping key;
	private boolean zooming;
	private boolean savedSmoothCamera;

	public ZoomModule() {
		super("zoom", true);
	}

	public void setKey(KeyMapping key) {
		this.key = key;
	}

	@Override
	public void tick(Minecraft minecraft) {
		setZooming(minecraft, key != null && key.isDown() && minecraft.gui.screen() == null);
	}

	@Override
	protected void onDisable() {
		setZooming(Minecraft.getInstance(), false);
	}

	private void setZooming(Minecraft minecraft, boolean zooming) {
		if (this.zooming == zooming) return;
		this.zooming = zooming;
		// Cinematic camera smooths out mouse movement while zoomed in.
		if (zooming) {
			savedSmoothCamera = minecraft.options.smoothCamera;
			minecraft.options.smoothCamera = true;
		} else {
			minecraft.options.smoothCamera = savedSmoothCamera;
		}
	}

	public float modifyFov(float fov) {
		return zooming ? fov / ZOOM_FACTOR : fov;
	}
}
