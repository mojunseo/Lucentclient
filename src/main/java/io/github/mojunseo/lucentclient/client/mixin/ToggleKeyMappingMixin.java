package io.github.mojunseo.lucentclient.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ToggleKeyMapping.class)
public class ToggleKeyMappingMixin {
	// Everywhere vanilla asks "should this key toggle?", also say yes when Toggle Sprint wants it to.
	@ModifyExpressionValue(method = "*", at = @At(value = "INVOKE", target = "Ljava/util/function/BooleanSupplier;getAsBoolean()Z"))
	private boolean lucentclient$toggle(boolean original) {
		return original || ModuleManager.TOGGLE_SPRINT.forcesToggle((KeyMapping) (Object) this);
	}
}
