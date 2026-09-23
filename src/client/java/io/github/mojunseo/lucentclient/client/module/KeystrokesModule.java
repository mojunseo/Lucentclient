package io.github.mojunseo.lucentclient.client.module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class KeystrokesModule extends Module {
	private static final int KEY = 22;
	private static final int GAP = 2;
	private static final int PRESSED_BG = 0xC0FFFFFF;
	private static final int RELEASED_BG = 0x80000000;
	private static final int PRESSED_TEXT = 0xFF000000;
	private static final int RELEASED_TEXT = 0xFFFFFFFF;

	public KeystrokesModule() {
		super("keystrokes", true);
	}

	@Override
	public void extractHud(Minecraft minecraft, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Options options = minecraft.options;
		int width = KEY * 3 + GAP * 2;
		int x = graphics.guiWidth() - width - 4;
		int y = 4;

		key(minecraft, graphics, options.keyUp, x + KEY + GAP, y, KEY, KEY);
		y += KEY + GAP;
		key(minecraft, graphics, options.keyLeft, x, y, KEY, KEY);
		key(minecraft, graphics, options.keyDown, x + KEY + GAP, y, KEY, KEY);
		key(minecraft, graphics, options.keyRight, x + (KEY + GAP) * 2, y, KEY, KEY);
		y += KEY + GAP;
		int half = (width - GAP) / 2;
		box(minecraft, graphics, "LMB", options.keyAttack.isDown(), x, y, half, KEY);
		box(minecraft, graphics, "RMB", options.keyUse.isDown(), x + half + GAP, y, width - half - GAP, KEY);
		y += KEY + GAP;
		box(minecraft, graphics, "———", options.keyJump.isDown(), x, y, width, 12);
	}

	private static void key(Minecraft minecraft, GuiGraphicsExtractor graphics, KeyMapping mapping, int x, int y, int w, int h) {
		box(minecraft, graphics, mapping.getTranslatedKeyMessage().getString(), mapping.isDown(), x, y, w, h);
	}

	private static void box(Minecraft minecraft, GuiGraphicsExtractor graphics, String label, boolean pressed, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, pressed ? PRESSED_BG : RELEASED_BG);
		int textY = y + (h - minecraft.font.lineHeight) / 2 + 1;
		graphics.centeredText(minecraft.font, label, x + w / 2, textY, pressed ? PRESSED_TEXT : RELEASED_TEXT);
	}
}
