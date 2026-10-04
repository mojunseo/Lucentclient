package io.github.mojunseo.lucentclient.client.mixin;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerList.class)
public class ServerListMixin {
	private static final String LUCENTCLIENT$PINNED_NAME = "PurityMC";
	private static final String LUCENTCLIENT$PINNED_IP = "puritymc.kr";

	@Shadow
	@Final
	private List<ServerData> serverList;

	// Re-pin PurityMC at the top of the list every time it's (re)loaded from servers.dat.
	@Inject(method = "load", at = @At("RETURN"))
	private void lucentclient$pinServer(CallbackInfo ci) {
		serverList.removeIf(server -> LUCENTCLIENT$PINNED_IP.equals(server.ip));
		serverList.add(0, new ServerData(LUCENTCLIENT$PINNED_NAME, LUCENTCLIENT$PINNED_IP, ServerData.Type.OTHER));
	}

	// The pinned entry can't be removed through the normal "delete server" flow.
	@Inject(method = "remove", at = @At("HEAD"), cancellable = true)
	private void lucentclient$keepPinned(ServerData thing, CallbackInfo ci) {
		if (LUCENTCLIENT$PINNED_IP.equals(thing.ip)) ci.cancel();
	}
}
