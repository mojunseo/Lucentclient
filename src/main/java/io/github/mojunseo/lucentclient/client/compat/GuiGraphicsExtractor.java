package io.github.mojunseo.lucentclient.client.compat;

// Only has a body before 26.1, when net.minecraft.client.gui.GuiGraphicsExtractor didn't exist yet
// and every GUI-drawing module/screen in this mod called the classic GuiGraphics directly. Giving
// this class the exact same name and method set lets every one of those call sites compile
// unchanged on both sides of that rename — only their import line needs to pick a branch.
//? if <26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
*///?}
public final class GuiGraphicsExtractor {
	//? if <26.1 {
	/*private final GuiGraphics graphics;
	private final Pose pose = new Pose();

	public GuiGraphicsExtractor(GuiGraphics graphics) {
		this.graphics = graphics;
	}

	public GuiGraphics delegate() {
		return graphics;
	}

	public int guiWidth() {
		return graphics.guiWidth();
	}

	public int guiHeight() {
		return graphics.guiHeight();
	}

	public void fill(int x0, int y0, int x1, int y1, int color) {
		graphics.fill(x0, y0, x1, y1, color);
	}

	public void item(ItemStack stack, int x, int y) {
		graphics.renderItem(stack, x, y);
	}

	public void text(Font font, String text, int x, int y, int color, boolean shadow) {
		graphics.drawString(font, text, x, y, color, shadow);
	}

	public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
		graphics.drawString(font, text, x, y, color, shadow);
	}

	public void centeredText(Font font, Component text, int x, int y, int color) {
		graphics.drawCenteredString(font, text, x, y, color);
	}

	// No single vanilla method for a hollow rectangle in this era: four thin fills.
	public void outline(int x, int y, int width, int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y + 1, x + 1, y + height - 1, color);
		graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
	}

	public void enableScissor(int x0, int y0, int x1, int y1) {
		graphics.enableScissor(x0, y0, x1, y1);
	}

	public void disableScissor() {
		graphics.disableScissor();
	}

	public Pose pose() {
		return pose;
	}

	public final class Pose {
		public void pushMatrix() {
			graphics.pose().pushPose();
		}

		public void popMatrix() {
			graphics.pose().popPose();
		}

		public void translate(float x, float y) {
			graphics.pose().translate(x, y, 0.0F);
		}

		public void scale(float x, float y) {
			graphics.pose().scale(x, y, 1.0F);
		}
	}
	*///?}
}
