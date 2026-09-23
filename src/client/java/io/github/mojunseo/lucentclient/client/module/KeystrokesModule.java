package io.github.mojunseo.lucentclient.client.module;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class KeystrokesModule extends HudModule {
	private static final int KEY = 22;
	private static final int GAP = 2;
	private static final int SPACE_HEIGHT = 12;
	private static final int WIDTH = KEY * 3 + GAP * 2;
	private static final int HEIGHT = (KEY + GAP) * 3 + SPACE_HEIGHT;
	private static final int PRESSED_BG = 0xC0FFFFFF;
	private static final int RELEASED_BG = 0x80000000;
	private static final int PRESSED_TEXT = 0xFF000000;
	private static final int RELEASED_TEXT = 0xFFFFFFFF;

	public KeystrokesModule() {
		super("keystrokes", true, 1.0F, 0.0F);
	}

	@Override
	public int width(Minecraft minecraft) {
		return WIDTH;
	}

	@Override
	public int height(Minecraft minecraft) {
		return HEIGHT;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics, int x, int y) {
		Options options = minecraft.options;
		key(minecraft, graphics, options.keyUp, x + KEY + GAP, y);
		y += KEY + GAP;
		key(minecraft, graphics, options.keyLeft, x, y);
		key(minecraft, graphics, options.keyDown, x + KEY + GAP, y);
		key(minecraft, graphics, options.keyRight, x + (KEY + GAP) * 2, y);
		y += KEY + GAP;
		int half = (WIDTH - GAP) / 2;
		mouse(minecraft, graphics, "LMB", CpsTracker.LEFT, options.keyAttack.isDown(), x, y, half);
		mouse(minecraft, graphics, "RMB", CpsTracker.RIGHT, options.keyUse.isDown(), x + half + GAP, y, WIDTH - half - GAP);
		y += KEY + GAP;
		box(graphics, options.keyJump.isDown(), x, y, WIDTH, SPACE_HEIGHT);
		graphics.centeredText(minecraft.font, "———", x + WIDTH / 2, y + 2, textColor(options.keyJump.isDown()));
	}

	private static void key(Minecraft minecraft, GuiGraphicsExtractor graphics, KeyMapping mapping, int x, int y) {
		boolean pressed = mapping.isDown();
		box(graphics, pressed, x, y, KEY, KEY);
		String label = mapping.getTranslatedKeyMessage().getString();
		graphics.centeredText(minecraft.font, label, x + KEY / 2, y + (KEY - minecraft.font.lineHeight) / 2 + 1, textColor(pressed));
	}

	private static void mouse(Minecraft minecraft, GuiGraphicsExtractor graphics, String label, int button, boolean pressed, int x, int y, int w) {
		box(graphics, pressed, x, y, w, KEY);
		int color = textColor(pressed);
		graphics.centeredText(minecraft.font, label, x + w / 2, y + 2, color);
		graphics.centeredText(minecraft.font, CpsTracker.cps(button) + " CPS", x + w / 2, y + 12, color);
	}

	private static void box(GuiGraphicsExtractor graphics, boolean pressed, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, pressed ? PRESSED_BG : RELEASED_BG);
	}

	private static int textColor(boolean pressed) {
		return pressed ? PRESSED_TEXT : RELEASED_TEXT;
	}
}
