package io.github.mojunseo.lucentclient.client.cosmetic.render;

import io.github.mojunseo.lucentclient.client.cosmetic.PlayerCosmetics;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

public final class CosmeticRenderState {
	/** Set on every {@code AvatarRenderState} for real players; absent for mannequins. */
	public static final RenderStateDataKey<PlayerCosmetics> COSMETICS = RenderStateDataKey.create(() -> "lucentclient:cosmetics");

	private CosmeticRenderState() {
	}
}
