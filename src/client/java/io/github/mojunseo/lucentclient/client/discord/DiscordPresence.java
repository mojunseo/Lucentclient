package io.github.mojunseo.lucentclient.client.discord;

import com.google.gson.JsonObject;
import io.github.mojunseo.lucentclient.LucentClient;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * Keeps Discord's Rich Presence in sync with the latest wanted activity. Runs on its own daemon
 * thread so the game never waits on Discord: it reconnects when Discord starts later, and sends
 * an update only when the activity changed, no more than once every few seconds (Discord allows
 * 5 updates per 20 seconds).
 */
public final class DiscordPresence {
	private static final long MIN_UPDATE_INTERVAL_MS = 4_000;
	private static final long RECONNECT_INTERVAL_MS = 20_000;

	private final String clientId;
	private volatile @Nullable JsonObject wanted;
	private volatile boolean running = true;
	private final Thread worker;

	public DiscordPresence(String clientId) {
		this.clientId = clientId;
		this.worker = new Thread(this::run, "Lucent Discord presence");
		worker.setDaemon(true);
		worker.start();
	}

	/** Sets what the presence should show; null clears it. Safe to call every tick. */
	public void set(@Nullable JsonObject activity) {
		wanted = activity;
	}

	public void stop() {
		running = false;
		worker.interrupt();
	}

	private void run() {
		DiscordIpc ipc = null;
		JsonObject sent = null;
		long lastAttempt = 0;
		try {
			while (running) {
				long now = System.currentTimeMillis();
				if (ipc == null || !ipc.isOpen()) {
					ipc = null;
					sent = null;
					if (now - lastAttempt >= RECONNECT_INTERVAL_MS) {
						lastAttempt = now;
						ipc = DiscordIpc.connect(clientId);
						if (ipc != null) LucentClient.LOGGER.info("Connected to Discord");
					}
				}
				JsonObject target = wanted;
				if (ipc != null && ipc.isOpen() && !Objects.equals(target, sent)) {
					try {
						ipc.setActivity(target);
						sent = target;
					} catch (IOException e) {
						LucentClient.LOGGER.debug("Failed to update Discord presence", e);
					}
				}
				Thread.sleep(MIN_UPDATE_INTERVAL_MS);
			}
		} catch (InterruptedException ignored) {
			// Stopping.
		} finally {
			if (ipc != null) ipc.close();
		}
	}
}
