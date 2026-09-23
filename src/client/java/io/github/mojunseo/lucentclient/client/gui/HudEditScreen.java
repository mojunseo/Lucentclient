package io.github.mojunseo.lucentclient.client.gui;

import io.github.mojunseo.lucentclient.client.module.HudModule;
import io.github.mojunseo.lucentclient.client.module.Module;
import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Lets the player drag enabled HUD modules around. The regular HUD skips drawing while this is open. */
public class HudEditScreen extends Screen {
	private final Screen parent;
	private HudModule dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	public HudEditScreen(Screen parent) {
		super(Component.translatable("screen.lucentclient.hud_edit"));
		this.parent = parent;
	}

	private static List<HudModule> hudModules() {
		List<HudModule> hud = new ArrayList<>();
		for (Module module : ModuleManager.modules()) {
			if (module instanceof HudModule hudModule && module.isEnabled()) hud.add(hudModule);
		}
		return hud;
	}

	private boolean contains(HudModule module, double mouseX, double mouseY) {
		int x = module.screenX(minecraft, width);
		int y = module.screenY(minecraft, height);
		return mouseX >= x && mouseX < x + module.width(minecraft) && mouseY >= y && mouseY < y + module.height(minecraft);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// Keep the world visible so the HUD can be placed against it.
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		for (HudModule module : hudModules()) {
			module.extract(minecraft, graphics);
			int x = module.screenX(minecraft, width);
			int y = module.screenY(minecraft, height);
			boolean active = module == dragging || (dragging == null && contains(module, mouseX, mouseY));
			graphics.outline(x - 1, y - 1, module.width(minecraft) + 2, module.height(minecraft) + 2,
					active ? 0xFFFFFF55 : 0x80FFFFFF);
		}
		graphics.centeredText(font, Component.translatable("screen.lucentclient.hud_edit.hint"),
				width / 2, height / 2, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			List<HudModule> hud = hudModules();
			for (int i = hud.size() - 1; i >= 0; i--) {
				HudModule module = hud.get(i);
				if (contains(module, event.x(), event.y())) {
					dragging = module;
					dragOffsetX = (int) event.x() - module.screenX(minecraft, width);
					dragOffsetY = (int) event.y() - module.screenY(minecraft, height);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging != null) {
			dragging.moveTo(minecraft, (int) event.x() - dragOffsetX, (int) event.y() - dragOffsetY, width, height);
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public void removed() {
		ModuleManager.save();
	}
}
