package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** The real-world time. */
public class ClockModule extends HudModule {
	private final ChoiceSetting format = setting(new ChoiceSetting("clock_format", "24h", List.of("24h", "12h")));
	private final BooleanSetting showSeconds = setting(new BooleanSetting("show_seconds", false));

	public ClockModule() {
		super("clock", false, 0.5F, 0.0F);
	}

	private String text() {
		String pattern = (format.get().equals("12h") ? "h:mm" : "HH:mm") + (showSeconds.enabled() ? ":ss" : "")
				+ (format.get().equals("12h") ? " a" : "");
		return LocalTime.now().format(DateTimeFormatter.ofPattern(pattern, Locale.ROOT));
	}

	@Override
	public int width(Minecraft minecraft) {
		return minecraft.font.width(text()) + 6;
	}

	@Override
	public int height(Minecraft minecraft) {
		return 13;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		fillBackground(graphics, 0, 0, width(minecraft), height(minecraft));
		text(minecraft, graphics, text(), 3, 3);
	}
}
