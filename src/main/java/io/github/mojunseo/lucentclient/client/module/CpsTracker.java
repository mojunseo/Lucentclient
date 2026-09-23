package io.github.mojunseo.lucentclient.client.module;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Util;

import java.util.ArrayDeque;

/** Counts mouse clicks over the last second. */
public final class CpsTracker {
	public static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	public static final int RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;

	private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();

	private CpsTracker() {
	}

	public static void click(int button) {
		ArrayDeque<Long> clicks = clicks(button);
		if (clicks != null) clicks.addLast(Util.getMillis());
	}

	public static int cps(int button) {
		ArrayDeque<Long> clicks = clicks(button);
		if (clicks == null) return 0;
		long cutoff = Util.getMillis() - 1000;
		while (!clicks.isEmpty() && clicks.peekFirst() < cutoff) clicks.removeFirst();
		return clicks.size();
	}

	private static ArrayDeque<Long> clicks(int button) {
		return switch (button) {
			case LEFT -> LEFT_CLICKS;
			case RIGHT -> RIGHT_CLICKS;
			default -> null;
		};
	}
}
