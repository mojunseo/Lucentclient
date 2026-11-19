package io.github.mojunseo.lucentclient.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.mojunseo.lucentclient.client.compat.Mc;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Under some Windows input methods (reported for Microsoft's Japanese and Chinese IME; see Mojira
 * MC-311424), the right Shift key sometimes arrives as {@code InputConstants.UNKNOWN} instead of
 * its real key. Vanilla's {@code click}/{@code set} look that key up directly, which matches every
 * unbound {@code KeyMapping} too, since they all equal {@code UNKNOWN} — so the bugged event clicks
 * or holds down every one of them, e.g. toggling Cinematic Camera out of nowhere (Mojira MC-312123),
 * while a key genuinely bound to right Shift never sees a matching event at all. This stops the
 * spurious clicks, the same fix already present in {@code KeyMapping#matches}, and tells the player
 * once in a while so a silently unresponsive key doesn't look broken for no reason.
 */
@Mixin(KeyMapping.class)
public class KeyMappingMixin {
	private static long lucentclient$lastWarningAt;

	@Inject(method = "click", at = @At("HEAD"), cancellable = true)
	private static void lucentclient$click(InputConstants.Key key, CallbackInfo ci) {
		if (key.equals(InputConstants.UNKNOWN)) {
			lucentclient$warn();
			ci.cancel();
		}
	}

	@Inject(method = "set", at = @At("HEAD"), cancellable = true)
	private static void lucentclient$set(InputConstants.Key key, boolean state, CallbackInfo ci) {
		if (key.equals(InputConstants.UNKNOWN)) ci.cancel();
	}

	private static void lucentclient$warn() {
		long now = System.currentTimeMillis();
		if (now - lucentclient$lastWarningAt < 5000) return;
		lucentclient$lastWarningAt = now;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) return;
		Mc.systemMessage(minecraft, Component.translatable("message.lucentclient.unbound_key_event"));
	}
}
