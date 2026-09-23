package io.github.mojunseo.lucentclient.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mojunseo.lucentclient.LucentClient;
import io.github.mojunseo.lucentclient.client.cosmetic.Cosmetic;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticType;
import io.github.mojunseo.lucentclient.client.cosmetic.Cosmetics;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticsManager;
import io.github.mojunseo.lucentclient.client.gui.ui.Animations;
import io.github.mojunseo.lucentclient.client.gui.ui.PlayerPreview;
import io.github.mojunseo.lucentclient.client.gui.ui.Theme;
import io.github.mojunseo.lucentclient.client.gui.ui.Ui;
import io.github.mojunseo.lucentclient.client.module.Module;
import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Lucent Client menu: a sidebar with tabs and custom-drawn cards, toggles and a cosmetics
 * preview. Nothing here uses vanilla widgets; clickable areas are collected while drawing and
 * checked on click.
 */
public class LucentMenuScreen extends Screen {
	private static final int SIDEBAR_WIDTH = 112;
	private static final int RADIUS = 8;
	private static final int HEADER_HEIGHT = 46;
	private static final long OPEN_MS = 220;

	private enum Tab {
		MODULES,
		COSMETICS
	}

	private record Hit(int x, int y, int w, int h, Runnable action) {
	}

	private final Animations animations = new Animations();
	private final List<Hit> hits = new ArrayList<>();
	private final long openedAt = Util.getMillis();
	private final String version = FabricLoader.getInstance().getModContainer(LucentClient.MOD_ID)
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");

	private Tab tab = Tab.MODULES;
	private CosmeticType cosmeticType = CosmeticType.CAPE;
	private float scroll;
	private float scrollTarget;
	private float maxScroll;
	private int scrollX, scrollY, scrollW, scrollH;

	private float previewYaw = 200.0F;
	private boolean draggingPreview;
	private int previewX, previewY, previewW, previewH;
	private long lastFrame = Util.getMillis();

	private float alpha = 1.0F;

	public LucentMenuScreen() {
		super(Component.translatable("screen.lucentclient.menu"));
	}

	private int color(int argb) {
		return Ui.withAlpha(argb, alpha);
	}

	private void hit(int x, int y, int w, int h, Runnable action) {
		hits.add(new Hit(x, y, w, h, action));
	}

	/** Adds a hit area inside the scrolled content, clipped to the visible part. */
	private void scrolledHit(int x, int y, int w, int h, Runnable action) {
		int top = Math.max(y, scrollY);
		int bottom = Math.min(y + h, scrollY + scrollH);
		if (bottom > top) hit(x, top, w, bottom - top, action);
	}

	private void setTab(Tab tab) {
		if (this.tab == tab) return;
		this.tab = tab;
		scroll = scrollTarget = 0;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		animations.frame();
		hits.clear();
		long now = Util.getMillis();
		float open = Ui.easeOutCubic((now - openedAt) / (float) OPEN_MS);
		alpha = open;
		scroll += (scrollTarget - scroll) * 0.35F;
		if (!draggingPreview) previewYaw += (now - lastFrame) * 0.03F;
		lastFrame = now;

		int pw = Math.min(width - 32, 500);
		int ph = Math.min(height - 32, 300);
		int px = (width - pw) / 2;
		int py = (height - ph) / 2;

		graphics.fill(0, 0, width, height, color(Theme.BACKDROP));
		graphics.pose().pushMatrix();
		float scale = 0.94F + 0.06F * open;
		graphics.pose().translate(width / 2.0F, height / 2.0F);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-width / 2.0F, -height / 2.0F);

		// Panel with a darker sidebar on the left.
		Ui.roundedRect(graphics, px, py, pw, ph, RADIUS, color(Theme.PANEL));
		Ui.roundedRect(graphics, px, py, SIDEBAR_WIDTH + RADIUS, ph, RADIUS, color(Theme.SIDEBAR));
		graphics.fill(px + SIDEBAR_WIDTH, py, px + SIDEBAR_WIDTH + RADIUS, py + ph, color(Theme.PANEL));
		graphics.fill(px + SIDEBAR_WIDTH, py + 10, px + SIDEBAR_WIDTH + 1, py + ph - 10, color(Theme.OUTLINE));

		extractSidebar(graphics, mouseX, mouseY, px, py, ph);

		int cx = px + SIDEBAR_WIDTH + 16;
		int cw = pw - SIDEBAR_WIDTH - 32;
		switch (tab) {
			case MODULES -> extractModules(graphics, mouseX, mouseY, cx, py, cw, ph);
			case COSMETICS -> extractCosmetics(graphics, mouseX, mouseY, cx, py, cw, ph);
		}
		graphics.pose().popMatrix();
	}

	private void extractSidebar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int px, int py, int ph) {
		Ui.text(graphics, font, "LUCENT", px + 14, py + 14, color(Theme.ACCENT), 1.8F);
		Ui.text(graphics, font, "CLIENT", px + 15, py + 32, color(Theme.SUBTEXT), 0.8F);
		graphics.fillGradient(px + 14, py + 42, px + SIDEBAR_WIDTH - 14, py + 43, color(Theme.ACCENT), color(Theme.ACCENT_2));

		int y = py + 56;
		y = sidebarTab(graphics, mouseX, mouseY, px, y, "tab_modules", Component.translatable("screen.lucentclient.tab.modules"),
				tab == Tab.MODULES, () -> setTab(Tab.MODULES));
		y = sidebarTab(graphics, mouseX, mouseY, px, y, "tab_cosmetics", Component.translatable("screen.lucentclient.tab.cosmetics"),
				tab == Tab.COSMETICS, () -> setTab(Tab.COSMETICS));
		sidebarTab(graphics, mouseX, mouseY, px, y, "tab_hud", Component.translatable("screen.lucentclient.hud_edit"),
				false, () -> minecraft.gui.setScreen(new HudEditScreen(this)));

		Ui.text(graphics, font, "v" + version, px + 14, py + ph - 16, color(Theme.SUBTEXT), 0.75F);
	}

	private int sidebarTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int px, int y, String key, Component label,
			boolean active, Runnable action) {
		int x = px + 8;
		int w = SIDEBAR_WIDTH - 16;
		int h = 22;
		boolean hovered = Ui.inside(mouseX, mouseY, x, y, w, h);
		float hover = animations.get(key + "_hover", hovered);
		float selected = animations.get(key + "_active", active);
		float highlight = Math.max(hover * 0.6F, selected);
		Ui.roundedRect(graphics, x, y, w, h, 5, color(Ui.lerpColor(0x00000000 | (Theme.CARD & 0xFFFFFF), Theme.CARD, highlight)));
		if (selected > 0.01F) {
			int barH = Math.round(12 * selected);
			Ui.roundedRect(graphics, x + 3, y + (h - barH) / 2, 2, barH, 1, color(Theme.ACCENT));
		}
		graphics.text(font, label, x + 12, y + 7, color(Ui.lerpColor(Theme.SUBTEXT, Theme.TEXT, highlight)), false);
		hit(x, y, w, h, action);
		return y + h + 4;
	}

	private void extractHeader(GuiGraphicsExtractor graphics, int cx, int py, Component title, Component subtitle) {
		Ui.text(graphics, font, title.getString(), cx, py + 14, color(Theme.TEXT), 1.5F);
		graphics.text(font, subtitle, cx, py + 31, color(Theme.SUBTEXT), false);
	}

	/** Starts the scrollable content area; returns the y to draw at, already offset by the scroll. */
	private int beginScroll(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		scrollX = x;
		scrollY = y;
		scrollW = w;
		scrollH = h;
		graphics.enableScissor(x, y, x + w, y + h);
		return y - Math.round(scroll);
	}

	private void endScroll(GuiGraphicsExtractor graphics, int contentHeight) {
		graphics.disableScissor();
		maxScroll = Math.max(0, contentHeight - scrollH);
		scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll);
		if (maxScroll > 0) {
			int barH = Math.max(16, scrollH * scrollH / contentHeight);
			int barY = scrollY + Math.round((scrollH - barH) * (scroll / maxScroll));
			Ui.roundedRect(graphics, scrollX + scrollW + 4, barY, 3, barH, 1, color(Theme.OUTLINE));
		}
	}

	// --- Modules ------------------------------------------------------------------------------

	private void extractModules(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int cx, int py, int cw, int ph) {
		List<Module> modules = ModuleManager.modules();
		long enabled = modules.stream().filter(Module::isEnabled).count();
		extractHeader(graphics, cx, py, Component.translatable("screen.lucentclient.tab.modules"),
				Component.translatable("screen.lucentclient.modules.count", modules.size(), enabled));

		int columns = cw >= 330 ? 3 : 2;
		int gap = 8;
		int cardW = (cw - gap * (columns - 1)) / columns;
		int cardH = 58;
		int top = py + HEADER_HEIGHT + 6;
		int areaH = py + ph - 14 - top;
		int y0 = beginScroll(graphics, cx, top, cw, areaH);
		for (int i = 0; i < modules.size(); i++) {
			Module module = modules.get(i);
			int x = cx + (i % columns) * (cardW + gap);
			int y = y0 + (i / columns) * (cardH + gap);
			extractModuleCard(graphics, mouseX, mouseY, module, x, y, cardW, cardH);
		}
		int rows = (modules.size() + columns - 1) / columns;
		endScroll(graphics, rows * (cardH + gap) - gap);
	}

	private void extractModuleCard(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Module module, int x, int y, int w, int h) {
		boolean hovered = Ui.inside(mouseX, mouseY, x, y, w, h) && Ui.inside(mouseX, mouseY, scrollX, scrollY, scrollW, scrollH);
		float hover = animations.get("module_hover_" + module.id(), hovered);
		float on = animations.get("module_on_" + module.id(), module.isEnabled());

		Ui.roundedBox(graphics, x, y, w, h, 6, color(Ui.lerpColor(Theme.CARD, Theme.CARD_HOVER, hover)),
				color(Ui.lerpColor(Theme.OUTLINE, Theme.ACCENT, on * 0.7F)));
		int badge = 18;
		Ui.roundedRect(graphics, x + 10, y + 10, badge, badge, 5, color(Ui.lerpColor(Theme.SWITCH_OFF, Theme.ACCENT, on)));
		String name = module.name().getString();
		graphics.centeredText(font, name.substring(0, 1), x + 10 + badge / 2, y + 15, color(Theme.TEXT));
		graphics.text(font, Ui.ellipsize(font, name, w - 70), x + 34, y + 15, color(Theme.TEXT), false);
		Ui.toggle(graphics, x + w - 32, y + 13, on, alpha);
		String description = Component.translatable("module.lucentclient." + module.id() + ".desc").getString();
		Ui.text(graphics, font, Ui.ellipsize(font, description, Math.round((w - 20) / 0.8F)), x + 10, y + 38, color(Theme.SUBTEXT), 0.8F);

		scrolledHit(x, y, w, h, () -> {
			module.setEnabled(!module.isEnabled());
			ModuleManager.save();
		});
	}

	// --- Cosmetics ----------------------------------------------------------------------------

	private void extractCosmetics(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int cx, int py, int cw, int ph) {
		extractHeader(graphics, cx, py, Component.translatable("screen.lucentclient.tab.cosmetics"),
				Component.translatable("screen.lucentclient.cosmetics.subtitle"));

		int gridW = Math.round(cw * 0.56F);
		int top = py + HEADER_HEIGHT + 4;

		// Category chips.
		int chipX = cx;
		for (CosmeticType type : CosmeticType.values()) {
			String label = type.displayName().getString();
			int w = font.width(label) + 16;
			boolean active = type == cosmeticType;
			boolean hovered = Ui.inside(mouseX, mouseY, chipX, top, w, 16);
			float t = animations.get("chip_" + type.id(), active);
			float hover = animations.get("chip_hover_" + type.id(), hovered);
			int fill = Ui.lerpColor(Ui.lerpColor(Theme.CARD, Theme.CARD_HOVER, hover), Theme.ACCENT, t);
			Ui.roundedRect(graphics, chipX, top, w, 16, 8, color(fill));
			graphics.text(font, label, chipX + 8, top + 4, color(Ui.lerpColor(Theme.SUBTEXT, Theme.TEXT, Math.max(t, hover))), false);
			hit(chipX, top, w, 16, () -> {
				if (cosmeticType != type) {
					cosmeticType = type;
					scroll = scrollTarget = 0;
				}
			});
			chipX += w + 6;
		}

		// Options grid.
		int gridTop = top + 24;
		int areaH = py + ph - 14 - gridTop;
		int columns = 2;
		int gap = 6;
		int cardW = (gridW - gap) / columns;
		int cardH = 30;
		List<@Nullable Cosmetic> options = new ArrayList<>();
		options.add(null);
		options.addAll(Cosmetics.ofType(cosmeticType));
		int y0 = beginScroll(graphics, cx, gridTop, gridW, areaH);
		for (int i = 0; i < options.size(); i++) {
			int x = cx + (i % columns) * (cardW + gap);
			int y = y0 + (i / columns) * (cardH + gap);
			extractCosmeticCard(graphics, mouseX, mouseY, options.get(i), x, y, cardW, cardH);
		}
		int rows = (options.size() + columns - 1) / columns;
		endScroll(graphics, rows * (cardH + gap) - gap);

		// Preview.
		previewX = cx + gridW + 14;
		previewY = top;
		previewW = cx + cw - previewX;
		previewH = py + ph - 14 - top;
		Ui.roundedBox(graphics, previewX, previewY, previewW, previewH, 6, color(0xFF0E1015), color(Theme.OUTLINE));
		graphics.fillGradient(previewX + 1, previewY + previewH / 2, previewX + previewW - 1, previewY + previewH - 6,
				color(0x00000000), color(0x308B6CFF));
		LocalPlayer player = minecraft.player;
		if (player == null) {
			graphics.centeredText(font, Component.translatable("screen.lucentclient.cosmetics.no_preview"),
					previewX + previewW / 2, previewY + previewH / 2, color(Theme.SUBTEXT));
		} else if (alpha > 0.99F) {
			int size = Math.min(previewW, previewH) * 2 / 5;
			PlayerPreview.extract(graphics, player, previewX, previewY + 4, previewX + previewW, previewY + previewH - 14, size, previewYaw);
			Ui.text(graphics, font, Component.translatable("screen.lucentclient.cosmetics.drag_hint").getString(),
					previewX + 6, previewY + previewH - 11, color(Theme.SUBTEXT), 0.75F);
		}
	}

	private void extractCosmeticCard(GuiGraphicsExtractor graphics, int mouseX, int mouseY, @Nullable Cosmetic cosmetic, int x, int y, int w, int h) {
		String id = cosmetic == null ? null : cosmetic.id();
		String key = cosmeticType.id() + "_" + (id == null ? "none" : id);
		boolean equipped = Objects.equals(CosmeticsManager.local().id(cosmeticType), id);
		boolean hovered = Ui.inside(mouseX, mouseY, x, y, w, h) && Ui.inside(mouseX, mouseY, scrollX, scrollY, scrollW, scrollH);
		float hover = animations.get("cosmetic_hover_" + key, hovered);
		float on = animations.get("cosmetic_on_" + key, equipped);

		int fill = Ui.lerpColor(Ui.lerpColor(Theme.CARD, Theme.CARD_HOVER, hover), Theme.CARD_ACTIVE, on);
		Ui.roundedBox(graphics, x, y, w, h, 6, color(fill), color(Ui.lerpColor(Theme.OUTLINE, Theme.ACCENT, on)));
		Component name = cosmetic == null ? Component.translatable("cosmetic.lucentclient.none") : cosmetic.displayName();
		graphics.text(font, Ui.ellipsize(font, name.getString(), w - 26), x + 9, y + (h - 8) / 2, color(Theme.TEXT), false);
		if (on > 0.01F) {
			int dot = 8;
			Ui.roundedRect(graphics, x + w - 9 - dot, y + (h - dot) / 2, dot, dot, dot / 2, color(Ui.withAlpha(Theme.ACCENT_2, on)));
		}
		scrolledHit(x, y, w, h, () -> CosmeticsManager.setLocal(CosmeticsManager.local().with(cosmeticType, id)));
	}

	// --- Input --------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
		for (Hit hit : List.copyOf(hits)) {
			if (Ui.inside(event.x(), event.y(), hit.x, hit.y, hit.w, hit.h)) {
				hit.action.run();
				return true;
			}
		}
		if (tab == Tab.COSMETICS && Ui.inside(event.x(), event.y(), previewX, previewY, previewW, previewH)) {
			draggingPreview = true;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingPreview) {
			previewYaw += (float) dragX * 2.0F;
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingPreview = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scrollTarget = Mth.clamp(scrollTarget - (float) scrollY * 20.0F, 0, maxScroll);
		return true;
	}

	@Override
	public void removed() {
		ModuleManager.save();
	}
}
