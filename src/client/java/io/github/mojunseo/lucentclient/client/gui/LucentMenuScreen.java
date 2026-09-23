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
 * The Lucent Client menu. Modules and cosmetics are plain rows, each with a redstone lamp that is
 * lit when the row is on. Everything is drawn by hand; clickable areas are collected while drawing
 * and checked on click.
 */
public class LucentMenuScreen extends Screen {
	private static final int SIDEBAR_WIDTH = 96;
	private static final int ROW_HEIGHT = 22;
	private static final int PAD = 14;
	private static final long FADE_MS = 120;
	/** On open, lit lamps switch on one after another, this far apart. */
	private static final long LAMP_STAGGER_MS = 45;

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
	private long now;
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

	/** Adds a hit area inside the scrolled list, clipped to the visible part. */
	private void scrolledHit(int x, int y, int w, int h, Runnable action) {
		int top = Math.max(y, scrollY);
		int bottom = Math.min(y + h, scrollY + scrollH);
		if (bottom > top) hit(x, top, w, bottom - top, action);
	}

	private boolean hovered(int mouseX, int mouseY, int x, int y, int w, int h) {
		return Ui.inside(mouseX, mouseY, x, y, w, h) && Ui.inside(mouseX, mouseY, scrollX, scrollY, scrollW, scrollH);
	}

	/** Lamp brightness for a row, including the staggered switch-on when the menu opens. */
	private float lamp(String key, boolean on, int order) {
		boolean lit = on && now - openedAt > 80 + order * LAMP_STAGGER_MS;
		return animations.get("lamp_" + key, lit);
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
		now = Util.getMillis();
		alpha = Mth.clamp((now - openedAt) / (float) FADE_MS, 0.0F, 1.0F);
		scroll += (scrollTarget - scroll) * 0.35F;
		if (!draggingPreview) previewYaw += (now - lastFrame) * 0.03F;
		lastFrame = now;

		int pw = Math.min(width - 24, 460);
		int ph = Math.min(height - 24, 280);
		int px = (width - pw) / 2;
		int py = (height - ph) / 2;

		graphics.fill(0, 0, width, height, color(Theme.BACKDROP));
		Ui.box(graphics, px - 1, py - 1, pw + 2, ph + 2, color(Theme.SLATE), color(Theme.SEAM));
		graphics.fill(px, py, px + SIDEBAR_WIDTH, py + ph, color(Theme.DEEPSLATE));

		extractSidebar(graphics, mouseX, mouseY, px, py, ph);

		int cx = px + SIDEBAR_WIDTH + PAD;
		int cw = pw - SIDEBAR_WIDTH - PAD * 2;
		switch (tab) {
			case MODULES -> extractModules(graphics, mouseX, mouseY, cx, py, cw, ph);
			case COSMETICS -> extractCosmetics(graphics, mouseX, mouseY, cx, py, cw, ph);
		}
	}

	private void extractSidebar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int px, int py, int ph) {
		Ui.text(graphics, font, "Lucent", px + PAD, py + PAD, color(Theme.CALCITE), 2);

		int y = py + 52;
		y = sidebarItem(graphics, mouseX, mouseY, px, y, Component.translatable("screen.lucentclient.tab.modules"),
				tab == Tab.MODULES, () -> setTab(Tab.MODULES));
		y = sidebarItem(graphics, mouseX, mouseY, px, y, Component.translatable("screen.lucentclient.tab.cosmetics"),
				tab == Tab.COSMETICS, () -> setTab(Tab.COSMETICS));
		sidebarItem(graphics, mouseX, mouseY, px, y, Component.translatable("screen.lucentclient.hud_edit"),
				false, () -> minecraft.gui.setScreen(new HudEditScreen(this)));

		graphics.text(font, version, px + PAD, py + ph - PAD - 8, color(Theme.SEAM), false);
	}

	private int sidebarItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int px, int y, Component label,
			boolean active, Runnable action) {
		int h = 18;
		boolean hovered = Ui.inside(mouseX, mouseY, px, y, SIDEBAR_WIDTH, h);
		if (active) {
			graphics.fill(px, y, px + SIDEBAR_WIDTH, y + h, color(Theme.SLATE));
		} else if (hovered) {
			graphics.fill(px, y, px + SIDEBAR_WIDTH, y + h, color(0xFF26272D));
		}
		int textColor = active ? Theme.CALCITE : hovered ? Ui.lerpColor(Theme.TUFF, Theme.CALCITE, 0.5F) : Theme.TUFF;
		graphics.text(font, label, px + PAD, y + 5, color(textColor), false);
		hit(px, y, SIDEBAR_WIDTH, h, action);
		return y + h;
	}

	private void extractTitle(GuiGraphicsExtractor graphics, int cx, int py, Component title, @Nullable Component note) {
		Ui.text(graphics, font, title.getString(), cx, py + PAD, color(Theme.CALCITE), 2);
		if (note != null) {
			graphics.text(font, note, cx, py + PAD + 22, color(Theme.TUFF), false);
		}
	}

	/** Starts the scrollable list; returns the y to draw at, already offset by the scroll. */
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
			int barH = Math.max(12, scrollH * scrollH / contentHeight);
			int barY = scrollY + Math.round((scrollH - barH) * (scroll / maxScroll));
			graphics.fill(scrollX + scrollW + 3, barY, scrollX + scrollW + 5, barY + barH, color(Theme.SEAM));
		}
	}

	// --- Modules ------------------------------------------------------------------------------

	private void extractModules(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int cx, int py, int cw, int ph) {
		List<Module> modules = ModuleManager.modules();
		long enabled = modules.stream().filter(Module::isEnabled).count();
		extractTitle(graphics, cx, py, Component.translatable("screen.lucentclient.tab.modules"),
				Component.translatable("screen.lucentclient.modules.enabled", enabled));

		int nameColumn = 0;
		for (Module module : modules) nameColumn = Math.max(nameColumn, font.width(module.name()));
		nameColumn += 16;

		int top = py + 56;
		graphics.fill(cx, top - 1, cx + cw, top, color(Theme.SEAM));
		int y0 = beginScroll(graphics, cx - 6, top, cw + 12, py + ph - PAD - top);
		for (int i = 0; i < modules.size(); i++) {
			Module module = modules.get(i);
			int y = y0 + i * ROW_HEIGHT;
			boolean hovered = hovered(mouseX, mouseY, cx - 6, y, cw + 12, ROW_HEIGHT);
			if (hovered) graphics.fill(cx - 6, y, cx + cw + 6, y + ROW_HEIGHT, color(Theme.SEAM));

			int textY = y + (ROW_HEIGHT - 8) / 2;
			graphics.text(font, module.name(), cx, textY, color(Theme.CALCITE), false);
			String description = Component.translatable("module.lucentclient." + module.id() + ".desc").getString();
			int descriptionWidth = cw - nameColumn - Ui.LAMP_SIZE - 12;
			graphics.text(font, Ui.ellipsize(font, description, descriptionWidth), cx + nameColumn, textY, color(Theme.TUFF), false);
			Ui.lamp(graphics, cx + cw - Ui.LAMP_SIZE, y + (ROW_HEIGHT - Ui.LAMP_SIZE) / 2, lamp(module.id(), module.isEnabled(), i), alpha);

			scrolledHit(cx - 6, y, cw + 12, ROW_HEIGHT, () -> {
				module.setEnabled(!module.isEnabled());
				ModuleManager.save();
			});
		}
		endScroll(graphics, modules.size() * ROW_HEIGHT);
	}

	// --- Cosmetics ----------------------------------------------------------------------------

	private void extractCosmetics(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int cx, int py, int cw, int ph) {
		extractTitle(graphics, cx, py, Component.translatable("screen.lucentclient.tab.cosmetics"), null);

		// The preview stage takes the right part of the panel, edge to edge.
		int listW = Math.min(150, cw / 2);
		previewX = cx + listW + PAD;
		previewY = py;
		previewW = cx + cw + PAD - previewX;
		previewH = ph;
		graphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, color(Theme.DEEPSLATE));

		// Slot switcher: plain words, the current one underlined.
		int tabY = py + 40;
		int tabX = cx;
		for (CosmeticType type : CosmeticType.values()) {
			Component label = type.displayName();
			int w = font.width(label);
			boolean active = type == cosmeticType;
			boolean hovered = Ui.inside(mouseX, mouseY, tabX - 2, tabY - 3, w + 4, 14);
			int textColor = active ? Theme.CALCITE : hovered ? Ui.lerpColor(Theme.TUFF, Theme.CALCITE, 0.5F) : Theme.TUFF;
			graphics.text(font, label, tabX, tabY, color(textColor), false);
			if (active) graphics.fill(tabX, tabY + 10, tabX + w, tabY + 11, color(Theme.CALCITE));
			hit(tabX - 2, tabY - 3, w + 4, 14, () -> {
				if (cosmeticType != type) {
					cosmeticType = type;
					scroll = scrollTarget = 0;
				}
			});
			tabX += w + 10;
		}

		List<@Nullable Cosmetic> options = new ArrayList<>();
		options.add(null);
		options.addAll(Cosmetics.ofType(cosmeticType));
		int top = py + 58;
		graphics.fill(cx, top - 1, cx + listW, top, color(Theme.SEAM));
		int y0 = beginScroll(graphics, cx - 6, top, listW + 6, py + ph - PAD - top);
		String equipped = CosmeticsManager.local().id(cosmeticType);
		for (int i = 0; i < options.size(); i++) {
			Cosmetic cosmetic = options.get(i);
			String id = cosmetic == null ? null : cosmetic.id();
			int y = y0 + i * ROW_HEIGHT;
			if (hovered(mouseX, mouseY, cx - 6, y, listW + 6, ROW_HEIGHT)) {
				graphics.fill(cx - 6, y, cx + listW, y + ROW_HEIGHT, color(Theme.SEAM));
			}
			Component name = cosmetic == null ? Component.translatable("cosmetic.lucentclient.none") : cosmetic.displayName();
			graphics.text(font, Ui.ellipsize(font, name.getString(), listW - Ui.LAMP_SIZE - 8), cx, y + (ROW_HEIGHT - 8) / 2,
					color(Theme.CALCITE), false);
			float lit = lamp(cosmeticType.id() + "_" + id, Objects.equals(equipped, id), i);
			Ui.lamp(graphics, cx + listW - Ui.LAMP_SIZE - 4, y + (ROW_HEIGHT - Ui.LAMP_SIZE) / 2, lit, alpha);
			scrolledHit(cx - 6, y, listW + 6, ROW_HEIGHT,
					() -> CosmeticsManager.setLocal(CosmeticsManager.local().with(cosmeticType, id)));
		}
		endScroll(graphics, options.size() * ROW_HEIGHT);

		extractPreview(graphics);
	}

	private void extractPreview(GuiGraphicsExtractor graphics) {
		int centerX = previewX + previewW / 2;
		LocalPlayer player = minecraft.player;
		if (player == null) {
			graphics.centeredText(font, Component.translatable("screen.lucentclient.cosmetics.no_preview"),
					centerX, previewY + previewH / 2, color(Theme.TUFF));
			return;
		}
		int ground = previewY + previewH - 34;
		int size = Math.min(previewW, previewH) * 2 / 5;
		// A floor for the player to stand on.
		graphics.fill(previewX + 18, ground, previewX + previewW - 18, ground + 1, color(Theme.SEAM));
		if (alpha >= 1.0F) {
			PlayerPreview.extract(graphics, player, previewX, previewY + 8, previewX + previewW, ground + 4, size, previewYaw);
		}
		graphics.centeredText(font, Component.translatable("screen.lucentclient.cosmetics.drag_hint"),
				centerX, previewY + previewH - 20, color(Theme.TUFF));
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
