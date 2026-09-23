package io.github.mojunseo.lucentclient.client.cosmetic.render;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Smoothed per-player animation inputs for cosmetics.
 *
 * <p>Oscillations are driven by phases that advance a little every frame, instead of
 * {@code sin(ageInTicks * speed)}: with a large age, any change in speed would jump the phase and
 * make wings twitch. Movement speed and crouching are eased so bumping into a block or turning
 * doesn't snap the pose either.
 */
public record CosmeticMotion(float moving, float crouch, float wingPhase, float clothPhase, float flutterPhase) {
	private static final long FORGET_AFTER_MS = 10_000;
	private static final Map<Integer, Tracker> TRACKERS = new HashMap<>();
	private static long lastCleanup;

	private static final class Tracker {
		float lastAge = Float.NaN;
		float moving;
		float crouch;
		float wingPhase;
		float clothPhase;
		float flutterPhase;
		long lastSeen;
	}

	/** Advances the player's tracker to this frame. Calling it again in the same frame changes nothing. */
	public static CosmeticMotion update(AvatarRenderState state) {
		long now = Util.getMillis();
		Tracker tracker = TRACKERS.computeIfAbsent(state.id, id -> new Tracker());
		tracker.lastSeen = now;
		float targetMoving = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
		float targetCrouch = state.isCrouching ? 1.0F : 0.0F;
		if (Float.isNaN(tracker.lastAge)) {
			tracker.moving = targetMoving;
			tracker.crouch = targetCrouch;
		} else {
			// Clamp so a long pause (e.g. the player out of view) doesn't cause a big jump.
			float dt = Mth.clamp(state.ageInTicks - tracker.lastAge, 0.0F, 5.0F);
			if (dt > 0.0F) {
				tracker.moving += (targetMoving - tracker.moving) * (1.0F - (float) Math.exp(-dt * 0.12F));
				tracker.crouch += (targetCrouch - tracker.crouch) * (1.0F - (float) Math.exp(-dt * 0.35F));
				tracker.wingPhase += dt * (0.07F + tracker.moving * 0.08F);
				tracker.clothPhase += dt * (0.15F + tracker.moving * 0.15F);
				tracker.flutterPhase += dt * (0.25F + tracker.moving * 0.25F);
			}
		}
		tracker.lastAge = state.ageInTicks;
		cleanup(now);
		return new CosmeticMotion(tracker.moving, tracker.crouch, tracker.wingPhase, tracker.clothPhase, tracker.flutterPhase);
	}

	private static void cleanup(long now) {
		if (now - lastCleanup < FORGET_AFTER_MS) return;
		lastCleanup = now;
		for (Iterator<Tracker> it = TRACKERS.values().iterator(); it.hasNext(); ) {
			if (now - it.next().lastSeen > FORGET_AFTER_MS) it.remove();
		}
	}
}
