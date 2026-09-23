package io.github.mojunseo.lucentclient.client.module;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Groups modules in the menu. */
public enum Category {
	HUD,
	PVP,
	PERFORMANCE,
	OTHER;

	public Component displayName() {
		return Component.translatable("category.lucentclient." + name().toLowerCase(Locale.ROOT));
	}
}
