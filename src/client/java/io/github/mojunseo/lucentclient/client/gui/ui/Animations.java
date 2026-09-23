package io.github.mojunseo.lucentclient.client.gui.ui;

import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;

/** Named values that ease towards a target each frame, for hover and toggle transitions. */
public final class Animations {
	private final Map<String, Float> values = new HashMap<>();
	private long lastFrame = Util.getMillis();
	private float step;

	/** Call once per frame before {@link #get}. */
	public void frame() {
		long now = Util.getMillis();
		float seconds = Math.min(0.1F, (now - lastFrame) / 1000.0F);
		lastFrame = now;
		step = 1.0F - (float) Math.exp(-seconds * 14.0F);
	}

	public float get(String key, float target) {
		float value = values.getOrDefault(key, target);
		value += (target - value) * step;
		values.put(key, value);
		return value;
	}

	public float get(String key, boolean target) {
		return get(key, target ? 1.0F : 0.0F);
	}
}
