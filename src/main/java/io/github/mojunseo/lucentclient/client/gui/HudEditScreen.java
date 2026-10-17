package io.github.mojunseo.lucentclient.client.gui;

import io.github.mojunseo.lucentclient.client.compat.Mc;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.mojunseo.lucentclient.client.module.HudModule;
import io.github.mojunseo.lucentclient.client.module.Module;
import io.github.mojunseo.lucentclient.client.module.ModuleManager;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
//?} else {
/*import io.github.mojunseo.lucentclient.client.compat.GuiGraphicsExtractor;
*///?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Lets the player drag enabled HUD modules around. The regular HUD skips drawing while this is open. */
public class HudEditScreen extends Screen implements HudPreviewScreen {
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
		return mouseX >= x && mouseX < x + module.scaledWidth(minecraft) && mouseY >= y && mouseY < y + module.scaledHeight(minecraft);
	}

	//? if >=26.1 {
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// Keep the world visible so the HUD can be placed against it.
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		drawModules(graphics, mouseX, mouseY);
	}
	//?} else {
	/*// There is no extractBackground/extractRenderState split before 26.1: render() does both, and
	// skipping the renderBackground() call here is what keeps the world visible behind the HUD.
	@Override
	public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		drawModules(new GuiGraphicsExtractor(guiGraphics), mouseX, mouseY);
	}
	*///?}

	private void drawModules(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		for (HudModule module : hudModules()) {
			module.extract(minecraft, graphics);
			int x = module.screenX(minecraft, width);
			int y = module.screenY(minecraft, height);
			boolean active = module == dragging || (dragging == null && contains(module, mouseX, mouseY));
			graphics.outline(x - 1, y - 1, module.scaledWidth(minecraft) + 2, module.scaledHeight(minecraft) + 2,
					active ? 0xFFFFFF55 : 0x80FFFFFF);
		}
		graphics.centeredText(font, Component.translatable("screen.lucentclient.hud_edit.hint"),
				width / 2, height / 2, 0xFFFFFFFF);
	}

	//? if >=26.1 {
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && startDrag(event.x(), event.y())) return true;
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (continueDrag(event.x(), event.y())) return true;
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		return super.mouseReleased(event);
	}
	//?} else {
	/*@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == InputConstants.MOUSE_BUTTON_LEFT && startDrag(mouseX, mouseY)) return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (continueDrag(mouseX, mouseY)) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragging = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}
	*///?}

	private boolean startDrag(double mouseX, double mouseY) {
		List<HudModule> hud = hudModules();
		for (int i = hud.size() - 1; i >= 0; i--) {
			HudModule module = hud.get(i);
			if (contains(module, mouseX, mouseY)) {
				dragging = module;
				dragOffsetX = (int) mouseX - module.screenX(minecraft, width);
				dragOffsetY = (int) mouseY - module.screenY(minecraft, height);
				return true;
			}
		}
		return false;
	}

	private boolean continueDrag(double mouseX, double mouseY) {
		if (dragging == null) return false;
		dragging.moveTo(minecraft, (int) mouseX - dragOffsetX, (int) mouseY - dragOffsetY, width, height);
		return true;
	}

	@Override
	public void onClose() {
		Mc.setScreen(minecraft, parent);
	}

	@Override
	public void removed() {
		ModuleManager.save();
	}
}
