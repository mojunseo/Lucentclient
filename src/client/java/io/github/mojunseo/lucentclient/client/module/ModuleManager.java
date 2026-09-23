package io.github.mojunseo.lucentclient.client.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.mojunseo.lucentclient.LucentClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("lucentclient.json");

	public static final FpsModule FPS = new FpsModule();
	public static final KeystrokesModule KEYSTROKES = new KeystrokesModule();
	public static final ZoomModule ZOOM = new ZoomModule();

	private static final List<Module> MODULES = List.of(FPS, KEYSTROKES, ZOOM);

	private ModuleManager() {
	}

	public static List<Module> modules() {
		return MODULES;
	}

	public static void load() {
		if (!Files.exists(CONFIG)) return;
		try (Reader reader = Files.newBufferedReader(CONFIG)) {
			Map<String, Boolean> enabled = GSON.fromJson(reader, new TypeToken<Map<String, Boolean>>() {}.getType());
			if (enabled == null) return;
			for (Module module : MODULES) {
				Boolean value = enabled.get(module.id());
				if (value != null) module.setEnabled(value);
			}
		} catch (IOException | RuntimeException e) {
			LucentClient.LOGGER.error("Failed to load {}", CONFIG, e);
		}
	}

	public static void save() {
		Map<String, Boolean> enabled = new LinkedHashMap<>();
		for (Module module : MODULES) enabled.put(module.id(), module.isEnabled());
		try (Writer writer = Files.newBufferedWriter(CONFIG)) {
			GSON.toJson(enabled, writer);
		} catch (IOException e) {
			LucentClient.LOGGER.error("Failed to save {}", CONFIG, e);
		}
	}
}
