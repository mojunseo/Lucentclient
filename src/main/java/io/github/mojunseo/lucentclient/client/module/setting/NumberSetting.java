package io.github.mojunseo.lucentclient.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Mth;

import java.util.Locale;

/** A number between {@code min} and {@code max}, snapped to {@code step}. */
public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;
	private final String suffix;

	public NumberSetting(String id, double defaultValue, double min, double max, double step, String suffix) {
		super(id, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		this.suffix = suffix;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public float floatValue() {
		return value.floatValue();
	}

	public int intValue() {
		return (int) Math.round(value);
	}

	/** Position between min (0) and max (1). */
	public double fraction() {
		return (value - min) / (max - min);
	}

	public void setFraction(double fraction) {
		set(min + Mth.clamp(fraction, 0.0, 1.0) * (max - min));
	}

	@Override
	public void set(Double value) {
		double snapped = Math.round((value - min) / step) * step + min;
		super.set(Mth.clamp(snapped, min, max));
	}

	public String display() {
		boolean whole = step >= 1.0;
		return (whole ? String.valueOf(intValue()) : String.format(Locale.ROOT, "%.1f", value)) + suffix;
	}

	@Override
	public void read(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) set(json.getAsDouble());
	}

	@Override
	public JsonElement write() {
		return new JsonPrimitive(value);
	}
}
