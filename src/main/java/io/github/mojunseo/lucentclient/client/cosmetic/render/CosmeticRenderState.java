package io.github.mojunseo.lucentclient.client.cosmetic.render;

import io.github.mojunseo.lucentclient.client.cosmetic.PlayerCosmetics;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
//?}

// Only used from 26.1 onward; older versions attach cosmetic data via CosmeticDataHolder instead.
public final class CosmeticRenderState {
	//? if >=26.1 {
	/** Set on every {@code AvatarRenderState} for real players; absent for mannequins. */
	public static final RenderStateDataKey<PlayerCosmetics> COSMETICS = RenderStateDataKey.create(() -> "lucentclient:cosmetics");
	/** Set alongside {@link #COSMETICS} when the player wears anything. */
	public static final RenderStateDataKey<CosmeticMotion> MOTION = RenderStateDataKey.create(() -> "lucentclient:cosmetic_motion");
	//?}

	private CosmeticRenderState() {
	}
}
