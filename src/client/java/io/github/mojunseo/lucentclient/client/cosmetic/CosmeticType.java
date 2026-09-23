package io.github.mojunseo.lucentclient.client.cosmetic;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum CosmeticType {
	CAPE,
	WINGS,
	HAT,
	HALO;

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Component displayName() {
		return Component.translatable("cosmetic.lucentclient.type." + id());
	}
}
