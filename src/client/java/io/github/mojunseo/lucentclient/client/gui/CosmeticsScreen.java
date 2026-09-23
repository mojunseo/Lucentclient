package io.github.mojunseo.lucentclient.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mojunseo.lucentclient.client.cosmetic.Cosmetic;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticType;
import io.github.mojunseo.lucentclient.client.cosmetic.Cosmetics;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticsManager;
import io.github.mojunseo.lucentclient.client.cosmetic.PlayerCosmetics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pick cosmetics per slot, with a rotatable preview of the local player on the right. */
public class CosmeticsScreen extends Screen {
	private static final int TAB_WIDTH = 60;
	private static final int OPTION_WIDTH = 150;
	private static final int ROW_HEIGHT = 22;
	private static final float AUTO_ROTATE_DEGREES_PER_MS = 0.03F;

	private final Screen parent;
	private CosmeticType tab = CosmeticType.CAPE;
	private float yaw = 200.0F;
	private boolean draggingPreview;
	private long lastFrame = Util.getMillis();

	public CosmeticsScreen(Screen parent) {
		super(Component.translatable("screen.lucentclient.cosmetics"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = 20;
		int y = 32;
		for (CosmeticType type : CosmeticType.values()) {
			Component label = type == tab ? Component.literal("[").append(type.displayName()).append("]") : type.displayName();
			addRenderableWidget(Button.builder(label, button -> {
				tab = type;
				rebuildWidgets();
			}).bounds(left + type.ordinal() * (TAB_WIDTH + 2), y, TAB_WIDTH, 20).build());
		}
		y += ROW_HEIGHT + 8;

		addRenderableWidget(optionButton(null, Component.translatable("cosmetic.lucentclient.none"), left, y));
		for (Cosmetic cosmetic : Cosmetics.ofType(tab)) {
			y += ROW_HEIGHT;
			addRenderableWidget(optionButton(cosmetic.id(), cosmetic.displayName(), left, y));
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(left, height - 30, OPTION_WIDTH, 20).build());
	}

	private Button optionButton(String id, Component name, int x, int y) {
		boolean selected = java.util.Objects.equals(CosmeticsManager.local().id(tab), id);
		Component label = selected ? Component.literal("✔ ").append(name) : name;
		return Button.builder(label, button -> {
			PlayerCosmetics current = CosmeticsManager.local();
			CosmeticsManager.setLocal(current.with(tab, id));
			rebuildWidgets();
		}).bounds(x, y, OPTION_WIDTH, 20).build();
	}

	private int previewLeft() {
		return Math.max(20 + (TAB_WIDTH + 2) * CosmeticType.values().length + 10, width / 2);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);

		long now = Util.getMillis();
		if (!draggingPreview) yaw += (now - lastFrame) * AUTO_ROTATE_DEGREES_PER_MS;
		lastFrame = now;

		int x0 = previewLeft();
		int x1 = width - 20;
		int y0 = 32;
		int y1 = height - 40;
		graphics.fill(x0, y0, x1, y1, 0x40000000);
		LocalPlayer player = minecraft.player;
		if (player == null) {
			graphics.centeredText(font, Component.translatable("screen.lucentclient.cosmetics.no_preview"),
					(x0 + x1) / 2, (y0 + y1) / 2, 0xFFAAAAAA);
			return;
		}
		int size = Math.min(x1 - x0, y1 - y0) / 3;
		extractPlayer(graphics, player, x0, y0, x1, y1, size, yaw);
		graphics.centeredText(font, Component.translatable("screen.lucentclient.cosmetics.drag_hint"),
				(x0 + x1) / 2, y1 + 6, 0xFFAAAAAA);
	}

	/** Like InventoryScreen's preview, but turned by an arbitrary yaw so the back is visible too. */
	private void extractPlayer(GuiGraphicsExtractor graphics, LivingEntity entity, int x0, int y0, int x1, int y1, int size, float yaw) {
		EntityRenderer<? super LivingEntity, ?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
		EntityRenderState state = renderer.createRenderState(entity, 1.0F);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180.0F + yaw;
			living.yRot = 0.0F;
			living.xRot = 0.0F;
			living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
			living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
			living.scale = 1.0F;
		}
		Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
		graphics.entity(state, size, translation, new Quaternionf().rotateZ((float) Math.PI), new Quaternionf(), x0, y0, x1, y1);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.x() >= previewLeft()) {
			draggingPreview = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingPreview) {
			yaw += (float) dragX * 2.0F;
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingPreview = false;
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
