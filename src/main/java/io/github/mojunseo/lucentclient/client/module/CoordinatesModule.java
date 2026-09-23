package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoordinatesModule extends HudModule {
	private static final int LINE_HEIGHT = 10;

	private final NumberSetting decimals = setting(new NumberSetting("decimals", 1, 0, 3, 1, ""));
	private final BooleanSetting showDirection = setting(new BooleanSetting("show_direction", true));
	private final BooleanSetting showBiome = setting(new BooleanSetting("show_biome", true));

	public CoordinatesModule() {
		super("coordinates", true, 0.0F, 0.1F);
	}

	private List<Component> lines(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		List<Component> lines = new ArrayList<>();
		if (player == null || minecraft.level == null) {
			lines.add(Component.literal("XYZ: - / - / -"));
			return lines;
		}
		String number = "%." + decimals.intValue() + "f";
		lines.add(Component.literal(String.format(Locale.ROOT, "XYZ: " + number + " / " + number + " / " + number,
				player.getX(), player.getY(), player.getZ())));

		if (showDirection.enabled()) {
			Direction facing = player.getDirection();
			String axis = (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? "+" : "-")
					+ facing.getAxis().getName().toUpperCase(Locale.ROOT);
			lines.add(Component.translatable("hud.lucentclient.facing",
					Component.translatable("hud.lucentclient.direction." + facing.getName()), axis));
		}

		if (showBiome.enabled()) {
			Component biome = minecraft.level.getBiome(player.blockPosition()).unwrapKey()
					.map(key -> {
						Identifier id = key.identifier();
						return (Component) Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
					})
					.orElse(Component.literal("?"));
			lines.add(Component.translatable("hud.lucentclient.biome", biome));
		}
		return lines;
	}

	@Override
	public int width(Minecraft minecraft) {
		int width = 0;
		for (Component line : lines(minecraft)) width = Math.max(width, minecraft.font.width(line));
		return width + 6;
	}

	@Override
	public int height(Minecraft minecraft) {
		return lines(minecraft).size() * LINE_HEIGHT + 3;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		List<Component> lines = lines(minecraft);
		fillBackground(graphics, 0, 0, width(minecraft), lines.size() * LINE_HEIGHT + 3);
		for (int i = 0; i < lines.size(); i++) {
			text(minecraft, graphics, lines.get(i), 3, 3 + i * LINE_HEIGHT);
		}
	}
}
