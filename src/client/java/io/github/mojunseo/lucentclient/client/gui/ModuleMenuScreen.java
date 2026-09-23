package io.github.mojunseo.lucentclient.client.gui;

import io.github.mojunseo.lucentclient.client.module.Module;
import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ModuleMenuScreen extends Screen {
	private static final int BUTTON_WIDTH = 200;
	private static final int ROW_HEIGHT = 24;

	public ModuleMenuScreen() {
		super(Component.translatable("screen.lucentclient.modules"));
	}

	@Override
	protected void init() {
		int x = (width - BUTTON_WIDTH) / 2;
		int y = height / 4;
		for (Module module : ModuleManager.modules()) {
			addRenderableWidget(Button.builder(label(module), button -> {
				module.setEnabled(!module.isEnabled());
				button.setMessage(label(module));
			}).bounds(x, y, BUTTON_WIDTH, 20).build());
			y += ROW_HEIGHT;
		}
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(x, y + ROW_HEIGHT / 2, BUTTON_WIDTH, 20).build());
	}

	private static Component label(Module module) {
		return CommonComponents.optionStatus(module.name(), module.isEnabled());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, height / 4 - 20, 0xFFFFFFFF);
	}

	@Override
	public void removed() {
		ModuleManager.save();
	}
}
