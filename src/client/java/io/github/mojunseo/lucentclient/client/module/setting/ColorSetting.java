package io.github.mojunseo.lucentclient.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** An opaque color, picked from the sixteen Minecraft dye colors. */
public class ColorSetting extends Setting<Integer> {
	/** Dye colors in the order of the creative inventory. */
	public static final int[] DYES = {
			0xFFF9FFFE, 0xFF9D9D97, 0xFF474F52, 0xFF1D1D21,
			0xFF835432, 0xFFB02E26, 0xFFF9801D, 0xFFFED83D,
			0xFF80C71F, 0xFF5E7C16, 0xFF169C9C, 0xFF3AB3DA,
			0xFF3C44AA, 0xFF8932B8, 0xFFC74EBD, 0xFFF38BAA
	};

	public ColorSetting(String id, int defaultValue) {
		super(id, defaultValue);
	}

	public int argb() {
		return value;
	}

	@Override
	public void read(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) value = 0xFF000000 | json.getAsInt();
	}

	@Override
	public JsonElement write() {
		return new JsonPrimitive(value & 0xFFFFFF);
	}
}
