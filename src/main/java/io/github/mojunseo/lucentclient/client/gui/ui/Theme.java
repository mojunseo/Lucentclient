package io.github.mojunseo.lucentclient.client.gui.ui;

/**
 * Colors for the Lucent menus (ARGB), taken from Minecraft materials: deepslate and tuff for the
 * surfaces, calcite for text, and a redstone lamp for anything that is switched on.
 */
public final class Theme {
	public static final int BACKDROP = 0x80000000;
	/** Main panel. */
	public static final int SLATE = 0xFF2B2D33;
	/** Sidebar and preview stage. */
	public static final int DEEPSLATE = 0xFF1F2025;
	/** Borders, dividers and hovered rows. */
	public static final int SEAM = 0xFF3A3D45;
	public static final int CALCITE = 0xFFE4E2DA;
	public static final int TUFF = 0xFF8E9089;
	public static final int LAMP_ON = 0xFFF6C768;
	public static final int LAMP_ON_CORE = 0xFFFFEBB0;
	public static final int LAMP_OFF = 0xFF4A3326;
	public static final int LAMP_OFF_GRID = 0xFF2E2019;

	private Theme() {
	}
}
