package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Latency to the server, as the server reports it in the player list. */
public class PingModule extends HudModule {
	private final BooleanSetting colorByPing = setting(new BooleanSetting("color_by_ping", true));

	public PingModule() {
		super("ping", false, 0.0F, 0.2F);
	}

	private int ping(Minecraft minecraft) {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection == null || minecraft.player == null) return 0;
		PlayerInfo info = connection.getPlayerInfo(minecraft.player.getUUID());
		return info == null ? 0 : info.getLatency();
	}

	private String text(Minecraft minecraft) {
		return ping(minecraft) + " ms";
	}

	@Override
	public int width(Minecraft minecraft) {
		return minecraft.font.width(text(minecraft)) + 6;
	}

	@Override
	public int height(Minecraft minecraft) {
		return 13;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		fillBackground(graphics, 0, 0, width(minecraft), height(minecraft));
		int ping = ping(minecraft);
		int color = textColor.argb();
		if (colorByPing.enabled()) {
			color = ping < 80 ? 0xFF80C71F : ping < 150 ? 0xFFFED83D : ping < 300 ? 0xFFF9801D : 0xFFB02E26;
		}
		graphics.text(minecraft.font, text(minecraft), 3, 3, color, shadow.enabled());
	}
}
