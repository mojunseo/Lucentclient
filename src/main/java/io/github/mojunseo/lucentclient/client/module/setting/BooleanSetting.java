package io.github.mojunseo.lucentclient.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String id, boolean defaultValue) {
		super(id, defaultValue);
	}

	public boolean enabled() {
		return value;
	}

	public void toggle() {
		value = !value;
	}

	@Override
	public void read(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) value = json.getAsBoolean();
	}

	@Override
	public JsonElement write() {
		return new JsonPrimitive(value);
	}
}
