package io.github.mojunseo.lucentclient.client.mixin;

import io.github.mojunseo.lucentclient.client.compat.CosmeticDataHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

/**
 * Before 26.1, there's no {@code RenderStateDataKey} to stash cosmetics data on a render state
 * (see {@link io.github.mojunseo.lucentclient.client.cosmetic.render.CosmeticRenderState} for the
 * 26.1+ mechanism), so this mixin adds the generic storage itself. On 26.1+ it targets
 * {@code AvatarRenderState} but does nothing — that version already has what it needs.
 */
//? if >=26.1 {
@Mixin(net.minecraft.client.renderer.entity.state.AvatarRenderState.class)
public class PlayerRenderStateMixin {
}
//?} else {
/*@Mixin(net.minecraft.client.renderer.entity.state.PlayerRenderState.class)
public class PlayerRenderStateMixin implements CosmeticDataHolder {
	@Unique
	private final Map<String, Object> lucentclient$data = new HashMap<>();

	@Override
	public Object lucentclient$getCosmeticData(String key) {
		return lucentclient$data.get(key);
	}

	@Override
	public void lucentclient$setCosmeticData(String key, Object value) {
		if (value == null) lucentclient$data.remove(key);
		else lucentclient$data.put(key, value);
	}
}
*///?}
