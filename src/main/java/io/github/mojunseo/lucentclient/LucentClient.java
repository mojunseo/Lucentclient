package io.github.mojunseo.lucentclient;

import net.fabricmc.api.ModInitializer;

//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LucentClient implements ModInitializer {
	public static final String MOD_ID = "lucentclient";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Lucent Client loading");
	}

	//? if >=26.1 {
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
	//?} else {
	/*public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
	*///?}
}
