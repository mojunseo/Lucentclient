package io.github.mojunseo.lucentclient.client.cosmetic;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** Every cosmetic the client knows how to draw. Players only ever reference these by id. */
public final class Cosmetics {
	private static final int WHITE = 0xFFFFFFFF;

	public static final List<Cosmetic> ALL = List.of(
			Cosmetic.of(CosmeticType.CAPE, "lucent", "lucent", WHITE),
			Cosmetic.of(CosmeticType.CAPE, "flame", "flame", WHITE),
			Cosmetic.of(CosmeticType.CAPE, "ocean", "ocean", WHITE),
			Cosmetic.of(CosmeticType.CAPE, "galaxy", "galaxy", WHITE),
			Cosmetic.of(CosmeticType.WINGS, "angel", "angel", WHITE),
			Cosmetic.of(CosmeticType.WINGS, "demon", "demon", WHITE),
			Cosmetic.of(CosmeticType.WINGS, "butterfly", "butterfly", WHITE),
			Cosmetic.of(CosmeticType.HAT, "top_hat", "top_hat", WHITE),
			Cosmetic.of(CosmeticType.HAT, "crown", "crown", WHITE),
			Cosmetic.of(CosmeticType.HALO, "gold", "halo", 0xFFFFD86B),
			Cosmetic.of(CosmeticType.HALO, "ice", "halo", 0xFF8FE3FF));

	private Cosmetics() {
	}

	public static List<Cosmetic> ofType(CosmeticType type) {
		return ALL.stream().filter(cosmetic -> cosmetic.type() == type).toList();
	}

	public static @Nullable Cosmetic get(CosmeticType type, @Nullable String id) {
		if (id == null) return null;
		for (Cosmetic cosmetic : ALL) {
			if (cosmetic.type() == type && cosmetic.id().equals(id)) return cosmetic;
		}
		return null;
	}
}
