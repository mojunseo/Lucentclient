package io.github.mojunseo.lucentclient.client.module.setting;

import com.google.gson.JsonElement;
import net.minecraft.network.chat.Component;

/** One adjustable option of a module, saved with the module's config. */
public abstract class Setting<T> {
	private final String id;
	private final T defaultValue;
	protected T value;

	protected Setting(String id, T defaultValue) {
		this.id = id;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String id() {
		return id;
	}

	public Component name() {
		return Component.translatable("setting.lucentclient." + id);
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public void reset() {
		value = defaultValue;
	}

	/** Reads a saved value; anything malformed leaves the current value in place. */
	public abstract void read(JsonElement json);

	public abstract JsonElement write();
}
