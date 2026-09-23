package io.github.mojunseo.lucentclient.client.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.mojunseo.lucentclient.LucentClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ModuleManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("lucentclient.json");

	public static final FpsModule FPS = new FpsModule();
	public static final CoordinatesModule COORDINATES = new CoordinatesModule();
	public static final KeystrokesModule KEYSTROKES = new KeystrokesModule();
	public static final ZoomModule ZOOM = new ZoomModule();
	public static final FullbrightModule FULLBRIGHT = new FullbrightModule();

	private static final List<Module> MODULES = List.of(FPS, COORDINATES, KEYSTROKES, ZOOM, FULLBRIGHT);

	private ModuleManager() {
	}

	public static List<Module> modules() {
		return MODULES;
	}

	public static void load() {
		if (!Files.exists(CONFIG)) return;
		try (Reader reader = Files.newBufferedReader(CONFIG)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (Module module : MODULES) {
				JsonElement element = root.get(module.id());
				if (element == null) continue;
				if (element.isJsonObject()) {
					module.read(element.getAsJsonObject());
				} else {
					// Configs from 0.1.0 stored only a boolean per module.
					module.setEnabled(element.getAsBoolean());
				}
			}
		} catch (IOException | RuntimeException e) {
			LucentClient.LOGGER.error("Failed to load {}", CONFIG, e);
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		for (Module module : MODULES) {
			JsonObject json = new JsonObject();
			module.write(json);
			root.add(module.id(), json);
		}
		try (Writer writer = Files.newBufferedWriter(CONFIG)) {
			GSON.toJson(root, writer);
		} catch (IOException e) {
			LucentClient.LOGGER.error("Failed to save {}", CONFIG, e);
		}
	}
}
