package io.github.mojunseo.lucentclient.client;

import io.github.mojunseo.lucentclient.client.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.mojunseo.lucentclient.LucentClient;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticType;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticsManager;
import io.github.mojunseo.lucentclient.client.cosmetic.PlayerCosmetics;
import io.github.mojunseo.lucentclient.client.cosmetic.render.CosmeticRenderState;
import io.github.mojunseo.lucentclient.client.cosmetic.render.CosmeticLayer;
import io.github.mojunseo.lucentclient.client.cosmetic.render.CosmeticModels;
import io.github.mojunseo.lucentclient.client.gui.HudEditScreen;
import io.github.mojunseo.lucentclient.client.gui.LucentMenuScreen;
import io.github.mojunseo.lucentclient.client.module.HudModule;
import io.github.mojunseo.lucentclient.client.module.Module;
import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
//?} else {
/*import io.github.mojunseo.lucentclient.client.compat.CosmeticDataHolder;
import io.github.mojunseo.lucentclient.client.compat.GuiGraphicsExtractor;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
*///?}

public class LucentClientClient implements ClientModInitializer {
	// Before 26.1, a key category is just the translation key string, not a registered object.
	//? if >=26.1 {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(LucentClient.id("main"));
	//?} else {
	/*private static final String CATEGORY = "key.category.lucentclient.main";
	*///?}

	@Override
	public void onInitializeClient() {
		ModuleManager.load();
		CosmeticsManager.load();

		CosmeticModels.register();
		//? if >=26.1 {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatarRenderer) {
				helper.register(new CosmeticLayer(avatarRenderer, context.getModelSet()));
			}
		});
		// CosmeticLayer draws Lucent capes as cloth, so hide the vanilla box cape under them.
		LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER.register(state -> {
			PlayerCosmetics cosmetics = state.getData(CosmeticRenderState.COSMETICS);
			return cosmetics == null || cosmetics.get(CosmeticType.CAPE) == null;
		});
		//?} else {
		/*LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof PlayerRenderer playerRenderer) {
				helper.register(new CosmeticLayer(playerRenderer, context.getModelSet()));
			}
		});
		// CosmeticLayer draws Lucent capes as cloth, so hide the vanilla box cape under them.
		LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER.register(state -> {
			if (!(state instanceof CosmeticDataHolder holder)) return true;
			PlayerCosmetics cosmetics = (PlayerCosmetics) holder.lucentclient$getCosmeticData("cosmetics");
			return cosmetics == null || cosmetics.get(CosmeticType.CAPE) == null;
		});
		*///?}

		//? if >=26.1 {
		KeyMapping menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.lucentclient.menu", InputConstants.KEY_RSHIFT, CATEGORY));
		ModuleManager.ZOOM.setKey(KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.lucentclient.zoom", InputConstants.KEY_C, CATEGORY)));
		//?} else {
		/*KeyMapping menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.lucentclient.menu", InputConstants.KEY_RSHIFT, CATEGORY));
		ModuleManager.ZOOM.setKey(KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.lucentclient.zoom", InputConstants.KEY_C, CATEGORY)));
		*///?}

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (menuKey.consumeClick()) {
				if (Mc.screen(minecraft) == null) Mc.setScreen(minecraft, new LucentMenuScreen());
			}
			for (Module module : ModuleManager.modules()) {
				if (module.isEnabled()) module.tick(minecraft);
			}
			if (SelfTest.enabled()) SelfTest.tick(minecraft);
		});

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (level.isClientSide() && player == Minecraft.getInstance().player) ModuleManager.REACH.onAttack(player, entity, hit);
			return InteractionResult.PASS;
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> ModuleManager.DISCORD.shutdown());

		//? if >=26.1 {
		HudElementRegistry.addLast(LucentClient.id("hud"), (graphics, deltaTracker) -> {
			Minecraft minecraft = Minecraft.getInstance();
			// The edit screen draws the HUD modules itself.
			if (Mc.screen(minecraft) instanceof HudEditScreen) return;
			for (Module module : ModuleManager.modules()) {
				if (module instanceof HudModule hudModule && module.isEnabled()) hudModule.extract(minecraft, graphics);
			}
		});
		//?} else {
		/*// There is no addLast ordering before 26.1; HudRenderCallback just runs after the vanilla HUD.
		HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (Mc.screen(minecraft) instanceof HudEditScreen) return;
			GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(guiGraphics);
			for (Module module : ModuleManager.modules()) {
				if (module instanceof HudModule hudModule && module.isEnabled()) hudModule.extract(minecraft, graphics);
			}
		});
		*///?}

		if (Boolean.getBoolean("lucentclient.auditMixins")) {
			// Development check: apply every mixin now, so a wrong target fails at startup, not later in play.
			org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
			LucentClient.LOGGER.info("Mixin audit finished");
		}
		LucentClient.LOGGER.info("Lucent Client initialized");
	}
}
