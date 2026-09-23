package io.github.mojunseo.lucentclient.client.gui.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** Drawing helpers for the custom menus. */
public final class Ui {
	private Ui() {
	}

	/** A filled rectangle with rounded corners, drawn one scanline at a time in the corners. */
	public static void roundedRect(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int color) {
		if (w <= 0 || h <= 0) return;
		int r = Math.min(radius, Math.min(w, h) / 2);
		if (r <= 0) {
			graphics.fill(x, y, x + w, y + h, color);
			return;
		}
		for (int i = 0; i < r; i++) {
			float dy = r - i - 0.5F;
			int inset = Math.round(r - (float) Math.sqrt(r * r - dy * dy));
			graphics.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
			graphics.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
		}
		graphics.fill(x, y + r, x + w, y + h - r, color);
	}

	/** A rounded rectangle with a 1px border. */
	public static void roundedBox(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int fill, int border) {
		roundedRect(graphics, x, y, w, h, radius, border);
		roundedRect(graphics, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
	}

	/** A pill-shaped on/off switch; {@code on} animates from 0 to 1. */
	public static void toggle(GuiGraphicsExtractor graphics, int x, int y, float on, float alpha) {
		int w = 22;
		int h = 12;
		roundedRect(graphics, x, y, w, h, h / 2, withAlpha(lerpColor(Theme.SWITCH_OFF, Theme.ACCENT, on), alpha));
		int knob = h - 4;
		int knobX = Math.round(Mth.lerp(on, x + 2, x + w - 2 - knob));
		roundedRect(graphics, knobX, y + 2, knob, knob, knob / 2, withAlpha(Theme.KNOB, alpha));
	}

	public static void text(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color, float scale) {
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

	public static float easeOutCubic(float t) {
		float inv = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
		return 1.0F - inv * inv * inv;
	}
}
