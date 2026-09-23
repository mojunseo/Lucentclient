package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.compat.Mc;
import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class ZoomModule extends Module {
	private final NumberSetting factor = setting(new NumberSetting("zoom_factor", 4.0, 2.0, 10.0, 0.5, "x"));
	private final BooleanSetting smoothCamera = setting(new BooleanSetting("smooth_camera", true));
	private final BooleanSetting scrollZoom = setting(new BooleanSetting("scroll_zoom", true));

	private KeyMapping key;
	private boolean zooming;
	private boolean savedSmoothCamera;
	/** The factor for the current zoom; the scroll wheel changes it until the key is released. */
	private float currentFactor;

	public ZoomModule() {
		super("zoom", Category.PVP, true);
	}

	public void setKey(KeyMapping key) {
		this.key = key;
	}

	@Override
	public void tick(Minecraft minecraft) {
		setZooming(minecraft, key != null && key.isDown() && Mc.screen(minecraft) == null);
	}

	@Override
	protected void onDisable() {
		setZooming(Minecraft.getInstance(), false);
	}

	private void setZooming(Minecraft minecraft, boolean zooming) {
		if (this.zooming == zooming) return;
		this.zooming = zooming;
		if (zooming) {
			currentFactor = factor.floatValue();
			savedSmoothCamera = minecraft.options.smoothCamera;
			// Cinematic camera smooths out mouse movement while zoomed in.
			if (smoothCamera.enabled()) minecraft.options.smoothCamera = true;
		} else {
			minecraft.options.smoothCamera = savedSmoothCamera;
		}
	}

	/** Returns true if the scroll was used to change the zoom and should not reach the hotbar. */
	public boolean onScroll(double amount) {
		if (!zooming || !scrollZoom.enabled() || amount == 0) return false;
		currentFactor = Mth.clamp(currentFactor * (amount > 0 ? 1.25F : 0.8F), 1.5F, 50.0F);
		return true;
	}

	public float modifyFov(float fov) {
		return zooming ? fov / currentFactor : fov;
	}
}
