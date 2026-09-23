package io.github.mojunseo.lucentclient.client.cosmetic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.github.mojunseo.lucentclient.LucentClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows what each player is wearing. The local player's choice is saved to disk; other players'
 * cosmetics are filled in by {@link #put} once they are fetched from the cosmetics server.
 */
public final class CosmeticsManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("lucentclient-cosmetics.json");
	private static final Map<UUID, PlayerCosmetics> OTHERS = new ConcurrentHashMap<>();

	private static PlayerCosmetics local = PlayerCosmetics.NONE;

	private CosmeticsManager() {
	}

	public static PlayerCosmetics local() {
		return local;
	}

	public static void setLocal(PlayerCosmetics cosmetics) {
		local = cosmetics;
		save();
	}

	public static PlayerCosmetics get(UUID player) {
		if (player.equals(Minecraft.getInstance().getUser().getProfileId())) return local;
		return OTHERS.getOrDefault(player, PlayerCosmetics.NONE);
	}

	public static void put(UUID player, PlayerCosmetics cosmetics) {
		OTHERS.put(player, cosmetics);
	}

	public static void load() {
		if (!Files.exists(CONFIG)) return;
		try (Reader reader = Files.newBufferedReader(CONFIG)) {
			local = PlayerCosmetics.fromJson(JsonParser.parseReader(reader).getAsJsonObject());
		} catch (IOException | RuntimeException e) {
			LucentClient.LOGGER.error("Failed to load {}", CONFIG, e);
		}
	}

	private static void save() {
		try (Writer writer = Files.newBufferedWriter(CONFIG)) {
			GSON.toJson(local.toJson(), writer);
		} catch (IOException e) {
			LucentClient.LOGGER.error("Failed to save {}", CONFIG, e);
		}
	}
}
