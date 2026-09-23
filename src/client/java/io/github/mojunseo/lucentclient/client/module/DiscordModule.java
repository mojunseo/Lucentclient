package io.github.mojunseo.lucentclient.client.module;

import com.google.gson.JsonObject;
import io.github.mojunseo.lucentclient.client.discord.DiscordPresence;
import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Shows what you're playing on your Discord profile (Rich Presence). */
public class DiscordModule extends Module {
	/**
	 * The Discord application the presence is shown as. Create one at
	 * https://discord.com/developers/applications; its name is what Discord shows as the game, and
	 * the image uploaded as the Rich Presence art asset "logo" is used as the large picture.
	 */
	private static final String CLIENT_ID = "";
	private static final String LOGO_ASSET = "logo";

	private final BooleanSetting showServer = setting(new BooleanSetting("show_server", false));
	private final BooleanSetting showWorld = setting(new BooleanSetting("show_world", true));
	private final BooleanSetting showDimension = setting(new BooleanSetting("show_dimension", true));
	private final BooleanSetting showTime = setting(new BooleanSetting("show_elapsed", true));

	private @Nullable DiscordPresence presence;
	/** When the current game session (menu, world or server) started, in epoch seconds. */
	private long sessionStart = System.currentTimeMillis() / 1000;
	private String sessionKey = "";

	public DiscordModule() {
		super("discord", true);
	}

	public static boolean configured() {
		return !CLIENT_ID.isEmpty();
	}

	@Override
	public void tick(Minecraft minecraft) {
		if (!configured()) return;
		if (presence == null) presence = new DiscordPresence(CLIENT_ID);
		presence.set(activity(minecraft));
	}

	@Override
	protected void onDisable() {
		if (presence != null) {
			presence.stop();
			presence = null;
		}
	}

	public void shutdown() {
		onDisable();
	}

	private JsonObject activity(Minecraft minecraft) {
		String details;
		String state = null;
		String key;
		IntegratedServer singleplayer = minecraft.getSingleplayerServer();
		ServerData server = minecraft.getCurrentServer();
		if (minecraft.level == null) {
			details = Component.translatable("discord.lucentclient.menu").getString();
			key = "menu";
		} else if (singleplayer != null) {
			details = Component.translatable("discord.lucentclient.singleplayer").getString();
			String world = singleplayer.getWorldData().getLevelName();
			if (showWorld.enabled()) state = world;
			key = "world:" + world;
		} else {
			details = Component.translatable("discord.lucentclient.multiplayer").getString();
			String address = server == null ? "" : server.ip;
			if (showServer.enabled() && !address.isEmpty()) state = address;
			key = "server:" + address;
		}
		// Restart the timer whenever you move between the menu, worlds and servers.
		if (!key.equals(sessionKey)) {
			sessionKey = key;
			sessionStart = System.currentTimeMillis() / 1000;
		}

		JsonObject activity = new JsonObject();
		activity.addProperty("details", details);
		if (state != null) activity.addProperty("state", state);
		if (showTime.enabled()) {
			JsonObject timestamps = new JsonObject();
			timestamps.addProperty("start", sessionStart);
			activity.add("timestamps", timestamps);
		}
		JsonObject assets = new JsonObject();
		assets.addProperty("large_image", LOGO_ASSET);
		String version = FabricLoader.getInstance().getModContainer("lucentclient")
				.map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
		assets.addProperty("large_text", "Lucent Client " + version);
		if (showDimension.enabled() && minecraft.level != null) {
			Identifier dimension = minecraft.level.dimension().identifier();
			assets.addProperty("small_text", Component.translatable("discord.lucentclient.dimension."
					+ dimension.getNamespace() + "." + dimension.getPath()).getString());
		}
		activity.add("assets", assets);
		return activity;
	}
}
