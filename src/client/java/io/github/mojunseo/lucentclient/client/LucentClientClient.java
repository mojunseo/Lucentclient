package io.github.mojunseo.lucentclient.client;

import io.github.mojunseo.lucentclient.LucentClient;
import net.fabricmc.api.ClientModInitializer;

public class LucentClientClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LucentClient.LOGGER.info("Lucent Client initialized");
	}
}
