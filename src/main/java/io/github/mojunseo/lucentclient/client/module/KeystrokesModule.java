package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.ColorSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class KeystrokesModule extends HudModule {
	private static final int GAP = 2;
	private static final int SPACE_HEIGHT = 12;

	private final NumberSetting keySize = setting(new NumberSetting("key_size", 22, 16, 30, 1, "px"));
	private final ColorSetting pressedColor = setting(new ColorSetting("pressed_color", ColorSetting.DYES[0]));
	private final BooleanSetting showMouse = setting(new BooleanSetting("show_mouse", true));
	private final BooleanSetting showCps = setting(new BooleanSetting("show_cps", true));
	private final BooleanSetting showSpace = setting(new BooleanSetting("show_space", true));

	public KeystrokesModule() {
		super("keystrokes", true, 1.0F, 0.0F);
	}

	private int key() {
		return keySize.intValue();
	}

	@Override
	public int width(Minecraft minecraft) {
		return key() * 3 + GAP * 2;
	}

	@Override
	public int height(Minecraft minecraft) {
		int height = key() * 2 + GAP;
		if (showMouse.enabled()) height += GAP + key();
		if (showSpace.enabled()) height += GAP + SPACE_HEIGHT;
		return height;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		Options options = minecraft.options;
		int key = key();
		int width = width(minecraft);
		int y = 0;
		key(minecraft, graphics, options.keyUp, key + GAP, y);
		y += key + GAP;
		key(minecraft, graphics, options.keyLeft, 0, y);
		key(minecraft, graphics, options.keyDown, key + GAP, y);
		key(minecraft, graphics, options.keyRight, (key + GAP) * 2, y);
		y += key + GAP;
		if (showMouse.enabled()) {
			int half = (width - GAP) / 2;
			mouse(minecraft, graphics, "LMB", CpsTracker.LEFT, options.keyAttack.isDown(), 0, y, half);
			mouse(minecraft, graphics, "RMB", CpsTracker.RIGHT, options.keyUse.isDown(), half + GAP, y, width - half - GAP);
			y += key + GAP;
		}
		if (showSpace.enabled()) {
			boolean pressed = options.keyJump.isDown();
			box(graphics, pressed, 0, y, width, SPACE_HEIGHT);
			label(minecraft, graphics, "———", pressed, width / 2, y + 2);
		}
	}

	private void key(Minecraft minecraft, GuiGraphicsExtractor graphics, KeyMapping mapping, int x, int y) {
		int key = key();
		boolean pressed = mapping.isDown();
		box(graphics, pressed, x, y, key, key);
		label(minecraft, graphics, mapping.getTranslatedKeyMessage().getString(), pressed, x + key / 2, y + (key - minecraft.font.lineHeight) / 2 + 1);
	}

	private void mouse(Minecraft minecraft, GuiGraphicsExtractor graphics, String name, int button, boolean pressed, int x, int y, int w) {
		int key = key();
		box(graphics, pressed, x, y, w, key);
		if (showCps.enabled()) {
			int top = y + (key - 19) / 2 + 1;
			label(minecraft, graphics, name, pressed, x + w / 2, top);
			label(minecraft, graphics, CpsTracker.cps(button) + " CPS", pressed, x + w / 2, top + 10);
		} else {
			label(minecraft, graphics, name, pressed, x + w / 2, y + (key - minecraft.font.lineHeight) / 2 + 1);
		}
	}

	private void box(GuiGraphicsExtractor graphics, boolean pressed, int x, int y, int w, int h) {
		int fill = pressed ? (pressedColor.argb() & 0xFFFFFF) | 0xC0000000 : backgroundColor();
		if (fill != 0) graphics.fill(x, y, x + w, y + h, fill);
	}

	private void label(Minecraft minecraft, GuiGraphicsExtractor graphics, String text, boolean pressed, int centerX, int y) {
		int color = pressed ? contrastingText(pressedColor.argb()) : textColor.argb();
		int x = centerX - minecraft.font.width(text) / 2;
		graphics.text(minecraft.font, text, x, y, color, shadow.enabled() && !pressed);
	}

	/** Black or white, whichever reads better on the pressed key color. */
	private static int contrastingText(int background) {
		int r = (background >> 16) & 0xFF;
		int g = (background >> 8) & 0xFF;
		int b = background & 0xFF;
		return (r * 299 + g * 587 + b * 114) / 1000 > 140 ? 0xFF000000 : 0xFFFFFFFF;
	}
}
