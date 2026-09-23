package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Makes the sprint and sneak keys toggle instead of hold, by switching on vanilla's toggle behavior
 * for those keys (see ToggleKeyMappingMixin). Shows what is toggled on the HUD.
 */
public class ToggleSprintModule extends HudModule {
	private final BooleanSetting sprint = setting(new BooleanSetting("toggle_sprint", true));
	private final BooleanSetting sneak = setting(new BooleanSetting("toggle_sneak", false));
	private final BooleanSetting showStatus = setting(new BooleanSetting("show_status", true));

	public ToggleSprintModule() {
		super("toggle_sprint", Category.PVP, false, 0.0F, 1.0F);
	}

	/** Whether this key should toggle, on top of the vanilla toggle options. */
	public boolean forcesToggle(KeyMapping mapping) {
		if (!isEnabled()) return false;
		Options options = Minecraft.getInstance().options;
		if (options == null) return false;
		return (sprint.enabled() && mapping == options.keySprint) || (sneak.enabled() && mapping == options.keyShift);
	}

	@Override
	protected void onDisable() {
		Options options = Minecraft.getInstance().options;
		// Release anything left toggled on.
		options.keySprint.setDown(false);
		options.keyShift.setDown(false);
	}

	private Component status(Minecraft minecraft) {
		if (!showStatus.enabled()) return null;
		Options options = minecraft.options;
		if (sneak.enabled() && options.keyShift.isDown()) return Component.translatable("hud.lucentclient.sneaking_toggled");
		if (sprint.enabled() && options.keySprint.isDown()) return Component.translatable("hud.lucentclient.sprinting_toggled");
		if (previewing(minecraft)) return Component.translatable("hud.lucentclient.sprinting_toggled");
		return null;
	}

	@Override
	public int width(Minecraft minecraft) {
		Component status = status(minecraft);
		return status == null ? 0 : minecraft.font.width(status) + 6;
	}

	@Override
	public int height(Minecraft minecraft) {
		return status(minecraft) == null ? 0 : 13;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		Component status = status(minecraft);
		if (status == null) return;
		fillBackground(graphics, 0, 0, width(minecraft), height(minecraft));
		text(minecraft, graphics, status, 3, 3);
	}
}
