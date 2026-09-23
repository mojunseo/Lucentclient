package io.github.mojunseo.lucentclient.client.module;

import io.github.mojunseo.lucentclient.client.compat.Mc;
import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

/** Active status effects with their icon, level and time left. */
public class PotionsModule extends HudModule {
	private static final int ROW = 20;
	private static final int ICON = 18;

	private final BooleanSetting showIcons = setting(new BooleanSetting("show_icons", true));
	private final BooleanSetting showLevel = setting(new BooleanSetting("show_level", true));
	private final BooleanSetting hideAmbient = setting(new BooleanSetting("hide_ambient", true));

	public PotionsModule() {
		super("potions", true, 0.0F, 0.45F);
	}

	private List<MobEffectInstance> effects(Minecraft minecraft) {
		List<MobEffectInstance> effects = new ArrayList<>();
		LocalPlayer player = minecraft.player;
		if (player != null) {
			for (MobEffectInstance effect : player.getActiveEffects()) {
				if (hideAmbient.enabled() && effect.isAmbient()) continue;
				effects.add(effect);
			}
		}
		if (effects.isEmpty() && previewing(minecraft)) {
			effects.add(new MobEffectInstance(MobEffects.SPEED, 1800, 1));
			effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 4200, 0));
		}
		return effects;
	}

	private Component name(MobEffectInstance effect) {
		Component name = effect.getEffect().value().getDisplayName();
		if (!showLevel.enabled() || effect.getAmplifier() == 0) return name;
		return name.copy().append(" ").append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
	}

	private int textLeft() {
		return showIcons.enabled() ? ICON + 5 : 3;
	}

	@Override
	public int width(Minecraft minecraft) {
		int width = 0;
		for (MobEffectInstance effect : effects(minecraft)) {
			width = Math.max(width, Math.max(minecraft.font.width(name(effect)), minecraft.font.width(MobEffectUtil.formatDuration(effect, 1.0F, 20.0F))));
		}
		return textLeft() + width + 4;
	}

	@Override
	public int height(Minecraft minecraft) {
		return Math.max(1, effects(minecraft).size()) * ROW + 2;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		List<MobEffectInstance> effects = effects(minecraft);
		fillBackground(graphics, 0, 0, width(minecraft), height(minecraft));
		for (int i = 0; i < effects.size(); i++) {
			MobEffectInstance effect = effects.get(i);
			int y = 2 + i * ROW;
			if (showIcons.enabled()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Mc.mobEffectSprite(effect.getEffect()), 2, y, ICON, ICON);
			}
			text(minecraft, graphics, name(effect), textLeft(), y);
			// Time left in a dimmer shade of the text color.
			graphics.text(minecraft.font, MobEffectUtil.formatDuration(effect, 1.0F, 20.0F), textLeft(), y + 10,
					ARGB.scaleRGB(textColor.argb(), 0.7F), shadow.enabled());
		}
	}
}
