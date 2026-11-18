package io.github.mojunseo.lucentclient.client.module;

/** A named point in the world, shown as an in-world marker and on the waypoints HUD list. */
public record Waypoint(String name, double x, double y, double z, String dimension, int color) {
	public double distanceTo(double px, double py, double pz) {
		double dx = x - px;
		double dy = y - py;
		double dz = z - pz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
