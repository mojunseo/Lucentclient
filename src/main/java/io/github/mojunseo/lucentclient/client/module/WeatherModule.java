package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;

/** Stops drawing rain and/or snow. The weather itself (and its sounds) is unchanged. */
public class WeatherModule extends Module {
	private final BooleanSetting hideRain = setting(new BooleanSetting("hide_rain", true));
	private final BooleanSetting hideSnow = setting(new BooleanSetting("hide_snow", true));

	public WeatherModule() {
		super("weather", Category.PERFORMANCE, false);
	}

	public boolean hidesRain() {
		return isEnabled() && hideRain.enabled();
	}

	public boolean hidesSnow() {
		return isEnabled() && hideSnow.enabled();
	}
}
