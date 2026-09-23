package io.github.mojunseo.lucentclient.client.module;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * A module that draws a box on the HUD. Its position is stored as a fraction of the free space
 * on each axis, so it stays anchored to the same edge when the window is resized.
 */
public abstract class HudModule extends Module {
	private static final int MARGIN = 2;

	private float x;
	private float y;

	protected HudModule(String id, boolean enabledByDefault, float defaultX, float defaultY) {
		super(id, enabledByDefault);
		this.x = defaultX;
		this.y = defaultY;
	}

	public abstract int width(Minecraft minecraft);

	public abstract int height(Minecraft minecraft);

	protected abstract void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics, int x, int y);

	public int screenX(Minecraft minecraft, int guiWidth) {
		return MARGIN + Math.round(x * Math.max(0, guiWidth - width(minecraft) - MARGIN * 2));
	}

	public int screenY(Minecraft minecraft, int guiHeight) {
		return MARGIN + Math.round(y * Math.max(0, guiHeight - height(minecraft) - MARGIN * 2));
	}

	public void moveTo(Minecraft minecraft, int screenX, int screenY, int guiWidth, int guiHeight) {
		int freeX = guiWidth - width(minecraft) - MARGIN * 2;
		int freeY = guiHeight - height(minecraft) - MARGIN * 2;
		x = freeX > 0 ? Mth.clamp((screenX - MARGIN) / (float) freeX, 0.0F, 1.0F) : 0.0F;
		y = freeY > 0 ? Mth.clamp((screenY - MARGIN) / (float) freeY, 0.0F, 1.0F) : 0.0F;
	}

	public void extract(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		extractAt(minecraft, graphics, screenX(minecraft, graphics.guiWidth()), screenY(minecraft, graphics.guiHeight()));
	}

	@Override
	public void read(JsonObject json) {
		super.read(json);
		if (json.has("x")) x = Mth.clamp(json.get("x").getAsFloat(), 0.0F, 1.0F);
		if (json.has("y")) y = Mth.clamp(json.get("y").getAsFloat(), 0.0F, 1.0F);
	}

	@Override
	public void write(JsonObject json) {
		super.write(json);
		json.addProperty("x", x);
		json.addProperty("y", y);
	}
}
