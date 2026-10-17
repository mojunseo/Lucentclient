package io.github.mojunseo.lucentclient.client.compat;

/**
 * Only meaningful before 26.1: there's no {@code RenderStateDataKey} yet to stash extra data on a
 * render state, so a mixin on {@code PlayerRenderState} implements this instead, and the cosmetics
 * renderer casts to it. Generic key/value storage mirrors what RenderStateDataKey does, just without
 * the type-safe key objects.
 */
public interface CosmeticDataHolder {
	Object lucentclient$getCosmeticData(String key);

	void lucentclient$setCosmeticData(String key, Object value);
}
