package io.github.mojunseo.lucentclient.client.module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FpsModule extends Module {
	public FpsModule() {
		super("fps", true);
	}

	@Override
	public void extractHud(Minecraft minecraft, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		String text = minecraft.getFps() + " FPS";
		int width = minecraft.font.width(text);
		graphics.fill(2, 2, 2 + width + 6, 2 + 13, 0x80000000);
		graphics.text(minecraft.font, text, 5, 5, 0xFFFFFFFF, true);
	}
}
