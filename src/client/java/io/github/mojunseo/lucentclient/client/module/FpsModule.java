package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class FpsModule extends HudModule {
	private final ChoiceSetting format = setting(new ChoiceSetting("format", "suffix", List.of("suffix", "prefix", "number")));

	public FpsModule() {
		super("fps", true, 0.0F, 0.0F);
	}

	private String text(Minecraft minecraft) {
		int fps = minecraft.getFps();
		return switch (format.get()) {
			case "prefix" -> "FPS: " + fps;
			case "number" -> String.valueOf(fps);
			default -> fps + " FPS";
		};
	}

	@Override
	public int width(Minecraft minecraft) {
		return minecraft.font.width(text(minecraft)) + 6;
	}

	@Override
	public int height(Minecraft minecraft) {
		return 13;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		fillBackground(graphics, 0, 0, width(minecraft), height(minecraft));
		text(minecraft, graphics, text(minecraft), 3, 3);
	}
}
