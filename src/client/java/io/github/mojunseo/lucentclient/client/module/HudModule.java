package io.github.mojunseo.lucentclient.client.module;

import com.google.gson.JsonObject;
import io.github.mojunseo.lucentclient.client.gui.HudPreviewScreen;
import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.ColorSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A module that draws a box on the HUD. Its position is stored as a fraction of the free space
 * on each axis, so it stays anchored to the same edge when the window is resized.
 *
 * <p>Every HUD module shares the style settings below. Subclasses draw at (0, 0) in unscaled
 * units; {@link #extract} applies the position and scale.
 */
public abstract class HudModule extends Module {
	private static final int MARGIN = 2;

	protected final ColorSetting textColor = setting(new ColorSetting("text_color", ColorSetting.DYES[0]));
	protected final BooleanSetting shadow = setting(new BooleanSetting("shadow", true));
	protected final BooleanSetting background = setting(new BooleanSetting("background", true));
	protected final NumberSetting backgroundOpacity = setting(new NumberSetting("background_opacity", 50, 0, 100, 5, "%"));
	protected final NumberSetting scale = setting(new NumberSetting("scale", 1.0, 0.5, 2.0, 0.1, "x"));

	private float x;
	private float y;

	protected HudModule(String id, boolean enabledByDefault, float defaultX, float defaultY) {
		super(id, enabledByDefault);
		this.x = defaultX;
		this.y = defaultY;
	}

	/** Unscaled size of what {@link #extractAt} draws. */
	public abstract int width(Minecraft minecraft);

	public abstract int height(Minecraft minecraft);

	protected abstract void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics);

	/** Size on screen, after scaling. */
	public int scaledWidth(Minecraft minecraft) {
		return Mth.ceil(width(minecraft) * scale.floatValue());
	}

	public int scaledHeight(Minecraft minecraft) {
		return Mth.ceil(height(minecraft) * scale.floatValue());
	}

	public int screenX(Minecraft minecraft, int guiWidth) {
		return MARGIN + Math.round(x * Math.max(0, guiWidth - scaledWidth(minecraft) - MARGIN * 2));
	}

	public int screenY(Minecraft minecraft, int guiHeight) {
		return MARGIN + Math.round(y * Math.max(0, guiHeight - scaledHeight(minecraft) - MARGIN * 2));
	}

	public void moveTo(Minecraft minecraft, int screenX, int screenY, int guiWidth, int guiHeight) {
		int freeX = guiWidth - scaledWidth(minecraft) - MARGIN * 2;
		int freeY = guiHeight - scaledHeight(minecraft) - MARGIN * 2;
		x = freeX > 0 ? Mth.clamp((screenX - MARGIN) / (float) freeX, 0.0F, 1.0F) : 0.0F;
		y = freeY > 0 ? Mth.clamp((screenY - MARGIN) / (float) freeY, 0.0F, 1.0F) : 0.0F;
	}

	public void extract(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		extractAt(minecraft, graphics, screenX(minecraft, graphics.guiWidth()), screenY(minecraft, graphics.guiHeight()));
	}

	/** Draws the module with its top-left corner at (x, y), e.g. for a settings preview. */
	public void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics, int x, int y) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale.floatValue(), scale.floatValue());
		extractAt(minecraft, graphics);
		graphics.pose().popMatrix();
	}

	/** True while a Lucent editor or menu is open, when empty modules should draw sample content. */
	protected static boolean previewing(Minecraft minecraft) {
		return minecraft.gui.screen() instanceof HudPreviewScreen;
	}

	/** Background fill for boxes, or fully transparent when the background is off. */
	protected int backgroundColor() {
		if (!background.enabled()) return 0;
		int alpha = Math.round(backgroundOpacity.floatValue() * 2.55F);
		return alpha << 24;
	}

	protected void fillBackground(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
		int color = backgroundColor();
		if (color != 0) graphics.fill(x0, y0, x1, y1, color);
	}

	protected void text(Minecraft minecraft, GuiGraphicsExtractor graphics, Component text, int x, int y) {
		graphics.text(minecraft.font, text, x, y, textColor.argb(), shadow.enabled());
	}

	protected void text(Minecraft minecraft, GuiGraphicsExtractor graphics, String text, int x, int y) {
		graphics.text(minecraft.font, text, x, y, textColor.argb(), shadow.enabled());
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
