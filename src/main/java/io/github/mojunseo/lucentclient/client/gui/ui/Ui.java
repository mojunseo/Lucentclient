package io.github.mojunseo.lucentclient.client.gui.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** Drawing helpers for the custom menus. */
public final class Ui {
	public static final int LAMP_SIZE = 10;

	private Ui() {
	}

	/** A 1px outline around a filled rectangle. */
	public static void box(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fill, int border) {
		graphics.fill(x, y, x + w, y + h, border);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
	}

	/**
	 * A redstone lamp used as the on/off indicator. {@code on} eases from 0 to 1; when lit, the lamp
	 * glows onto its surroundings.
	 */
	public static void lamp(GuiGraphicsExtractor graphics, int x, int y, float on, float alpha) {
		int s = LAMP_SIZE;
		if (on > 0.01F) {
			graphics.fill(x - 3, y - 3, x + s + 3, y + s + 3, withAlpha(Theme.LAMP_ON, 0.10F * on * alpha));
			graphics.fill(x - 1, y - 1, x + s + 1, y + s + 1, withAlpha(Theme.LAMP_ON, 0.25F * on * alpha));
		}
		graphics.fill(x, y, x + s, y + s, withAlpha(lerpColor(Theme.LAMP_OFF, Theme.LAMP_ON, on), alpha));
		// The lamp's grid: a cross and a border, dark when off and bright when lit.
		int grid = withAlpha(lerpColor(Theme.LAMP_OFF_GRID, Theme.LAMP_ON_CORE, on), alpha);
		graphics.fill(x + s / 2 - 1, y + 1, x + s / 2 + 1, y + s - 1, grid);
		graphics.fill(x + 1, y + s / 2 - 1, x + s - 1, y + s / 2 + 1, grid);
		graphics.outline(x, y, s, s, withAlpha(lerpColor(Theme.LAMP_OFF_GRID, 0xFFC98F2E, on), alpha));
	}

	/** Draws text at a whole-number scale so the pixel font stays crisp. */
	public static void text(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color, int scale) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, 0, 0, color, false);
		graphics.pose().popMatrix();
	}

	public static String ellipsize(Font font, String text, int width) {
		if (font.width(text) <= width) return text;
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
	}

	public static int lerpColor(int from, int to, float t) {
		t = Mth.clamp(t, 0.0F, 1.0F);
		int a = Math.round(Mth.lerp(t, from >>> 24, to >>> 24));
		int r = Math.round(Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
		int g = Math.round(Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
		int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
		return a << 24 | r << 16 | g << 8 | b;
	}

	public static int withAlpha(int color, float alpha) {
		int a = Math.round((color >>> 24) * Mth.clamp(alpha, 0.0F, 1.0F));
		return a << 24 | (color & 0xFFFFFF);
	}

	public static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}
}
