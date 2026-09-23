package io.github.mojunseo.lucentclient.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FpsModule extends HudModule {
	public FpsModule() {
		super("fps", true, 0.0F, 0.0F);
	}

	private static String text(Minecraft minecraft) {
		return minecraft.getFps() + " FPS";
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
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y, x + width(minecraft), y + height(minecraft), 0x80000000);
		graphics.text(minecraft.font, text(minecraft), x + 3, y + 3, 0xFFFFFFFF, true);
	}
}
