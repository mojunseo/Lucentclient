package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/** Shows how far away the last entity you hit was, measured from your eyes to where you hit it. */
public class ReachModule extends HudModule {
	private final NumberSetting decimals = setting(new NumberSetting("decimals", 2, 1, 3, 1, ""));
	private final NumberSetting showFor = setting(new NumberSetting("show_for", 3, 1, 10, 1, "s"));
	private final BooleanSetting playersOnly = setting(new BooleanSetting("players_only", true));
	private final BooleanSetting colorByReach = setting(new BooleanSetting("color_by_reach", true));

	private double lastReach = -1;
	private long lastHitAt;

	public ReachModule() {
		super("reach", true, 0.5F, 0.58F);
	}

	/** Called when the local player attacks an entity. */
	public void onAttack(Player player, Entity target, @Nullable EntityHitResult hit) {
		if (!isEnabled() || (playersOnly.enabled() && !(target instanceof Player))) return;
		Vec3 eyes = player.getEyePosition();
		Vec3 point;
		if (hit != null && hit.getEntity() == target) {
			point = hit.getLocation();
		} else {
			// Fall back to the closest point of the target's hitbox.
			AABB box = target.getBoundingBox();
			point = new Vec3(Mth.clamp(eyes.x, box.minX, box.maxX), Mth.clamp(eyes.y, box.minY, box.maxY), Mth.clamp(eyes.z, box.minZ, box.maxZ));
		}
		lastReach = eyes.distanceTo(point);
		lastHitAt = Util.getMillis();
	}

	private @Nullable String text(Minecraft minecraft) {
		boolean recent = lastReach >= 0 && Util.getMillis() - lastHitAt < showFor.intValue() * 1000L;
		double reach = recent ? lastReach : previewing(minecraft) ? 3.0 : -1;
		if (reach < 0) return null;
		String number = String.format(Locale.ROOT, "%." + decimals.intValue() + "f", reach);
		return Component.translatable("hud.lucentclient.reach", number).getString();
	}

	@Override
	public int width(Minecraft minecraft) {
		String text = text(minecraft);
		return text == null ? 0 : minecraft.font.width(text) + 6;
	}

	@Override
	public int height(Minecraft minecraft) {
		return text(minecraft) == null ? 0 : 13;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		String text = text(minecraft);
		if (text == null) return;
		fillBackground(graphics, 0, 0, width(minecraft), height(minecraft));
		int color = textColor.argb();
		if (colorByReach.enabled() && lastReach >= 0) {
			color = lastReach < 2.5 ? 0xFF80C71F : lastReach < 3.0 ? 0xFFFED83D : 0xFFB02E26;
		}
		graphics.text(minecraft.font, text, 3, 3, color, shadow.enabled());
	}
}
