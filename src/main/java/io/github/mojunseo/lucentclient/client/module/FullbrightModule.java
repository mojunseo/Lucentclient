package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;

public class FullbrightModule extends Module {
	private final NumberSetting strength = setting(new NumberSetting("strength", 100, 10, 100, 5, "%"));

	public FullbrightModule() {
		super("fullbright", Category.OTHER, false);
	}

	/** How strongly to apply the night-vision lighting, 0..1. */
	public float strength() {
		return strength.floatValue() / 100.0F;
	}
}
