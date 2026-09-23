package io.github.mojunseo.lucentclient.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.chat.Component;

import java.util.List;

/** One of a fixed list of options, shown with the translation {@code setting.lucentclient.<id>.<option>}. */
public class ChoiceSetting extends Setting<String> {
	private final List<String> options;

	public ChoiceSetting(String id, String defaultValue, List<String> options) {
		super(id, defaultValue);
		this.options = options;
	}

	public Component display() {
		return Component.translatable("setting.lucentclient." + id() + "." + value);
	}

	public void cycle(int direction) {
		int index = options.indexOf(value);
		value = options.get(Math.floorMod(index + direction, options.size()));
	}

	@Override
	public void read(JsonElement json) {
		if (json.isJsonPrimitive() && options.contains(json.getAsString())) value = json.getAsString();
	}

	@Override
	public JsonElement write() {
		return new JsonPrimitive(value);
	}
}
