package io.github.mojunseo.lucentclient.client.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.mojunseo.lucentclient.LucentClient;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A minimal client for the Discord desktop app's local RPC socket, enough to set Rich Presence.
 *
 * <p>Frames are an opcode and a length (both little-endian int32) followed by UTF-8 JSON. After a
 * handshake, SET_ACTIVITY commands update the presence; Discord clears it when the connection closes.
 */
final class DiscordIpc implements Closeable {
	private static final int OP_HANDSHAKE = 0;
	private static final int OP_FRAME = 1;
	private static final int OP_CLOSE = 2;
	private static final int OP_PING = 3;
	private static final int OP_PONG = 4;

	private final Transport transport;
	private final Thread reader;
	private volatile boolean open = true;
	/** Discord ignores commands sent before it answers the handshake with READY. */
	private volatile boolean ready;

	private DiscordIpc(Transport transport, String clientId) throws IOException {
		this.transport = transport;
		JsonObject handshake = new JsonObject();
		handshake.addProperty("v", 1);
		handshake.addProperty("client_id", clientId);
		send(OP_HANDSHAKE, handshake);
		this.reader = new Thread(this::readLoop, "Lucent Discord IPC reader");
		reader.setDaemon(true);
		reader.start();
	}

	/** Connects to the first Discord socket that answers, or returns null if Discord isn't running. */
	static @Nullable DiscordIpc connect(String clientId) {
		for (int i = 0; i < 10; i++) {
			for (Transport.Opener opener : candidates(i)) {
				try {
					return new DiscordIpc(opener.open(), clientId);
				} catch (IOException ignored) {
					// Not this one; try the next candidate.
				}
			}
		}
		return null;
	}

	private static List<Transport.Opener> candidates(int index) {
		String name = "discord-ipc-" + index;
		List<Transport.Opener> openers = new ArrayList<>();
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
			openers.add(() -> new PipeTransport("\\\\.\\pipe\\" + name));
			return openers;
		}
		List<String> dirs = new ArrayList<>();
		for (String variable : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
			String value = System.getenv(variable);
			if (value != null) dirs.add(value);
		}
		dirs.add("/tmp");
		for (String dir : dirs) {
			// Plain installs, then Flatpak and Snap sandboxes.
			for (String sub : new String[]{"", "app/com.discordapp.Discord", "snap.discord", ".flatpak/dev.vencord.Vesktop/xdg-run"}) {
				Path path = Path.of(dir, sub, name);
				if (Files.exists(path)) openers.add(() -> new SocketTransport(path));
			}
		}
		return openers;
	}

	boolean isOpen() {
		return open;
	}

	boolean isReady() {
		return open && ready;
	}

	/** Sets the presence; a null activity clears it. */
	void setActivity(@Nullable JsonObject activity) throws IOException {
		JsonObject args = new JsonObject();
		args.addProperty("pid", ProcessHandle.current().pid());
		if (activity != null) args.add("activity", activity);
		JsonObject frame = new JsonObject();
		frame.addProperty("cmd", "SET_ACTIVITY");
		frame.add("args", args);
		frame.addProperty("nonce", UUID.randomUUID().toString());
		send(OP_FRAME, frame);
	}

	private synchronized void send(int opcode, JsonObject payload) throws IOException {
		byte[] json = payload.toString().getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(8 + json.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(opcode).putInt(json.length).put(json);
		try {
			transport.write(buffer.array());
		} catch (IOException e) {
			open = false;
			throw e;
		}
	}

	private void readLoop() {
		try {
			byte[] header = new byte[8];
			while (open) {
				transport.readFully(header);
				ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
				int opcode = buffer.getInt();
				int length = buffer.getInt();
				byte[] body = new byte[length];
				transport.readFully(body);
				String json = new String(body, StandardCharsets.UTF_8);
				switch (opcode) {
					case OP_PING -> send(OP_PONG, JsonParser.parseString(json).getAsJsonObject());
					case OP_CLOSE -> {
						LucentClient.LOGGER.warn("Discord closed the connection: {}", json);
						open = false;
					}
					default -> {
						JsonObject message = JsonParser.parseString(json).getAsJsonObject();
						String event = message.has("evt") && !message.get("evt").isJsonNull() ? message.get("evt").getAsString() : null;
						if ("READY".equals(event)) {
							ready = true;
						} else if ("ERROR".equals(event)) {
							LucentClient.LOGGER.warn("Discord RPC error: {}", json);
						}
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			if (open) LucentClient.LOGGER.debug("Discord connection lost", e);
		} finally {
			open = false;
		}
	}

	@Override
	public void close() {
		if (!open) return;
		open = false;
		try {
			send(OP_CLOSE, new JsonObject());
		} catch (IOException ignored) {
			// Closing anyway.
		}
		try {
			transport.close();
		} catch (IOException ignored) {
			// Nothing left to do.
		}
	}

	private interface Transport extends Closeable {
		void write(byte[] bytes) throws IOException;

		void readFully(byte[] bytes) throws IOException;

		@FunctionalInterface
		interface Opener {
			Transport open() throws IOException;
		}
	}

	/** Linux and macOS: a Unix domain socket. */
	private static final class SocketTransport implements Transport {
		private final SocketChannel channel;

		SocketTransport(Path path) throws IOException {
			channel = SocketChannel.open(StandardProtocolFamily.UNIX);
			try {
				channel.connect(UnixDomainSocketAddress.of(path));
			} catch (IOException e) {
				channel.close();
				throw e;
			}
		}

		@Override
		public void write(byte[] bytes) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			while (buffer.hasRemaining()) channel.write(buffer);
		}

		@Override
		public void readFully(byte[] bytes) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			while (buffer.hasRemaining()) {
				if (channel.read(buffer) < 0) throw new EOFException();
			}
		}

		@Override
		public void close() throws IOException {
			channel.close();
		}
	}

	/** Windows: a named pipe, which Java can open as a file. */
	private static final class PipeTransport implements Transport {
		private final RandomAccessFile pipe;

		PipeTransport(String path) throws IOException {
			pipe = new RandomAccessFile(path, "rw");
		}

		@Override
		public void write(byte[] bytes) throws IOException {
			pipe.write(bytes);
		}

		@Override
		public void readFully(byte[] bytes) throws IOException {
			pipe.readFully(bytes);
		}

		@Override
		public void close() throws IOException {
			pipe.close();
		}
	}
}
