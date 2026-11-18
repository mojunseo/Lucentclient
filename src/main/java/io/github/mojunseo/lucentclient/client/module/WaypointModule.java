package io.github.mojunseo.lucentclient.client.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.mojunseo.lucentclient.client.compat.Mc;
import io.github.mojunseo.lucentclient.client.module.setting.BooleanSetting;
import io.github.mojunseo.lucentclient.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import io.github.mojunseo.lucentclient.client.compat.GuiGraphicsExtractor;
*///?}
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Named markers the player drops in the world. Shown as a colored beam and name in the world
 * (see the world-render registration in {@code LucentClientClient}) and as a HUD list of the
 * nearest ones in the current dimension.
 */
public class WaypointModule extends HudModule {
	private static final int ROW_HEIGHT = 11;
	private static final int SWATCH = 6;
	/** Only the nearest few are listed, so the HUD box doesn't grow without bound. */
	private static final int MAX_HUD_ENTRIES = 5;

	private final BooleanSetting showBeam = setting(new BooleanSetting("waypoint_show_beam", true));
	private final NumberSetting renderDistance = setting(new NumberSetting("waypoint_render_distance", 256, 32, 1024, 32, "m"));

	private final List<Waypoint> waypoints = new ArrayList<>();

	public WaypointModule() {
		super("waypoints", Category.WAYPOINTS, true, 1.0F, 0.0F);
	}

	public List<Waypoint> waypoints() {
		return Collections.unmodifiableList(waypoints);
	}

	public boolean showBeam() {
		return showBeam.enabled();
	}

	public double renderDistance() {
		return renderDistance.get();
	}

	/** Adds a waypoint at the player's current position, named "Waypoint N". */
	public void addAtPlayer(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) return;
		int color = ColorPalette.next(waypoints.size());
		waypoints.add(new Waypoint("Waypoint " + (waypoints.size() + 1), player.getX(), player.getY(), player.getZ(),
				Mc.dimensionId(minecraft.level), color));
	}

	public void remove(int index) {
		if (index >= 0 && index < waypoints.size()) waypoints.remove(index);
	}

	public void cycleColor(int index) {
		if (index < 0 || index >= waypoints.size()) return;
		Waypoint w = waypoints.get(index);
		waypoints.set(index, new Waypoint(w.name(), w.x(), w.y(), w.z(), w.dimension(), ColorPalette.next(ColorPalette.indexOf(w.color()) + 1)));
	}

	/** Waypoints in the player's current dimension, nearest first. */
	private List<Waypoint> nearby(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null) return List.of();
		String dimension = Mc.dimensionId(minecraft.level);
		double max = renderDistance.get();
		List<Waypoint> list = new ArrayList<>();
		for (Waypoint w : waypoints) {
			if (!w.dimension().equals(dimension)) continue;
			if (w.distanceTo(player.getX(), player.getY(), player.getZ()) > max) continue;
			list.add(w);
		}
		list.sort(Comparator.comparingDouble(w -> w.distanceTo(player.getX(), player.getY(), player.getZ())));
		if (list.size() > MAX_HUD_ENTRIES) list = list.subList(0, MAX_HUD_ENTRIES);
		return list;
	}

	private String line(Minecraft minecraft, Waypoint w) {
		LocalPlayer player = minecraft.player;
		double distance = player == null ? 0 : w.distanceTo(player.getX(), player.getY(), player.getZ());
		return String.format(Locale.ROOT, "%s - %dm", w.name(), Math.round(distance));
	}

	@Override
	public int width(Minecraft minecraft) {
		List<Waypoint> list = nearby(minecraft);
		if (list.isEmpty()) {
			if (!previewing(minecraft)) return 0;
			return SWATCH + 4 + minecraft.font.width("Waypoint 1 - 128m");
		}
		int width = 0;
		for (Waypoint w : list) width = Math.max(width, minecraft.font.width(line(minecraft, w)));
		return SWATCH + 4 + width + 4;
	}

	@Override
	public int height(Minecraft minecraft) {
		List<Waypoint> list = nearby(minecraft);
		if (list.isEmpty() && !previewing(minecraft)) return 0;
		return Math.max(1, list.size()) * ROW_HEIGHT + 2;
	}

	@Override
	protected void extractAt(Minecraft minecraft, GuiGraphicsExtractor graphics) {
		List<Waypoint> list = nearby(minecraft);
		if (list.isEmpty() && !previewing(minecraft)) return;
		fillBackground(graphics, 0, 0, width(minecraft), height(minecraft));
		if (list.isEmpty()) {
			graphics.fill(2, 3, 2 + SWATCH, 3 + SWATCH, ColorPalette.DYES[0]);
			text(minecraft, graphics, "Waypoint 1 - 128m", SWATCH + 4, 2);
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			Waypoint w = list.get(i);
			int y = 2 + i * ROW_HEIGHT;
			graphics.fill(2, y + 1, 2 + SWATCH, y + 1 + SWATCH, w.color());
			text(minecraft, graphics, line(minecraft, w), SWATCH + 4, y);
		}
	}

	@Override
	public void read(JsonObject json) {
		super.read(json);
		waypoints.clear();
		if (json.get("waypoints") instanceof JsonArray saved) {
			for (var element : saved) {
				if (!(element instanceof JsonObject entry)) continue;
				try {
					waypoints.add(new Waypoint(entry.get("name").getAsString(), entry.get("x").getAsDouble(),
							entry.get("y").getAsDouble(), entry.get("z").getAsDouble(), entry.get("dimension").getAsString(),
							0xFF000000 | entry.get("color").getAsInt()));
				} catch (RuntimeException ignored) {
					// Malformed entry: skip it rather than fail loading the whole config.
				}
			}
		}
	}

	@Override
	public void write(JsonObject json) {
		super.write(json);
		JsonArray saved = new JsonArray();
		for (Waypoint w : waypoints) {
			JsonObject entry = new JsonObject();
			entry.addProperty("name", w.name());
			entry.addProperty("x", w.x());
			entry.addProperty("y", w.y());
			entry.addProperty("z", w.z());
			entry.addProperty("dimension", w.dimension());
			entry.addProperty("color", w.color() & 0xFFFFFF);
			saved.add(entry);
		}
		json.add("waypoints", saved);
	}

	/** The sixteen dye colors, shared so waypoints cycle through the same palette as {@code ColorSetting}. */
	private static final class ColorPalette {
		private static final int[] DYES = io.github.mojunseo.lucentclient.client.module.setting.ColorSetting.DYES;

		static int next(int index) {
			return 0xFF000000 | (DYES[Math.floorMod(index, DYES.length)] & 0xFFFFFF);
		}

		static int indexOf(int argb) {
			int rgb = argb & 0xFFFFFF;
			for (int i = 0; i < DYES.length; i++) {
				if ((DYES[i] & 0xFFFFFF) == rgb) return i;
			}
			return 0;
		}
	}
}
