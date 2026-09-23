package io.github.mojunseo.lucentclient.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mojunseo.lucentclient.LucentClient;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticsManager;
import io.github.mojunseo.lucentclient.client.cosmetic.render.CosmeticLayer;
import io.github.mojunseo.lucentclient.client.cosmetic.render.CosmeticModels;
import io.github.mojunseo.lucentclient.client.gui.HudEditScreen;
import io.github.mojunseo.lucentclient.client.gui.ModuleMenuScreen;
import io.github.mojunseo.lucentclient.client.module.HudModule;
import io.github.mojunseo.lucentclient.client.module.Module;
import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public class LucentClientClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(LucentClient.id("main"));

	@Override
	public void onInitializeClient() {
		ModuleManager.load();
		CosmeticsManager.load();

		CosmeticModels.register();
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatarRenderer) {
				helper.register(new CosmeticLayer(avatarRenderer, context.getModelSet()));
			}
		});

		KeyMapping menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.lucentclient.menu", InputConstants.KEY_RSHIFT, CATEGORY));
		ModuleManager.ZOOM.setKey(KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.lucentclient.zoom", InputConstants.KEY_C, CATEGORY)));

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (menuKey.consumeClick()) {
				if (minecraft.gui.screen() == null) minecraft.gui.setScreen(new ModuleMenuScreen());
			}
			for (Module module : ModuleManager.modules()) {
				if (module.isEnabled()) module.tick(minecraft);
			}
		});

		HudElementRegistry.addLast(LucentClient.id("hud"), (graphics, deltaTracker) -> {
			Minecraft minecraft = Minecraft.getInstance();
			// The edit screen draws the HUD modules itself.
			if (minecraft.gui.screen() instanceof HudEditScreen) return;
			for (Module module : ModuleManager.modules()) {
				if (module instanceof HudModule hudModule && module.isEnabled()) hudModule.extract(minecraft, graphics);
			}
		});

		LucentClient.LOGGER.info("Lucent Client initialized");
	}
}
