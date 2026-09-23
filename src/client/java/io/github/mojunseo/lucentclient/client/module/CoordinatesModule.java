package io.github.mojunseo.lucentclient.client.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

public class CoordinatesModule extends HudModule {
	private static final int LINE_HEIGHT = 10;

	public CoordinatesModule() {
		super("coordinates", true, 0.0F, 0.1F);
	}

	private static List<Component> lines(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) {
			return List.of(Component.literal("XYZ: - / - / -"));
		}
		Component xyz = Component.literal(String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f",
				player.getX(), player.getY(), player.getZ()));

		Direction facing = player.getDirection();
		String axis = (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? "+" : "-")
				+ facing.getAxis().getName().toUpperCase(Locale.ROOT);
		Component direction = Component.translatable("hud.lucentclient.facing",
				Component.translatable("hud.lucentclient.direction." + facing.getName()), axis);

		Component biome = minecraft.level.getBiome(player.blockPosition()).unwrapKey()
				.map(key -> {
					Identifier id = key.identifier();
					return (Component) Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
				})
				.orElse(Component.literal("?"));

		return List.of(xyz, direction, Component.translatable("hud.lucentclient.biome", biome));
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
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics, int x, int y) {
		List<Component> lines = lines(minecraft);
		graphics.fill(x, y, x + width(minecraft), y + lines.size() * LINE_HEIGHT + 3, 0x80000000);
		for (int i = 0; i < lines.size(); i++) {
			graphics.text(minecraft.font, lines.get(i), x + 3, y + 3 + i * LINE_HEIGHT, 0xFFFFFFFF, true);
		}
	}
}
