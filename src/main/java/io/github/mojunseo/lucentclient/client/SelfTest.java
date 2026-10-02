package io.github.mojunseo.lucentclient.client;

import io.github.mojunseo.lucentclient.LucentClient;
import io.github.mojunseo.lucentclient.client.compat.Mc;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticType;
import io.github.mojunseo.lucentclient.client.cosmetic.CosmeticsManager;
import io.github.mojunseo.lucentclient.client.cosmetic.PlayerCosmetics;
import io.github.mojunseo.lucentclient.client.gui.HudEditScreen;
import io.github.mojunseo.lucentclient.client.gui.LucentMenuScreen;
import io.github.mojunseo.lucentclient.client.module.Module;
import io.github.mojunseo.lucentclient.client.module.ModuleManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Development self-test, enabled with -Dlucentclient.selfTest=<name>: creates a fresh world, turns
 * every module and a cosmetic of each kind on, takes screenshots of the HUD, the cosmetics from the
 * front and back, and every menu screen, then closes the game. Screenshots go to
 * run/screenshots/selftest-<name>-<step>.png.
 */
final class SelfTest {
	private static final String NAME = System.getProperty("lucentclient.selfTest");
	private static final String WORLD = "lucent-selftest";

	private record Step(int at, String shot, Runnable action) {
	}

	private static boolean worldRequested;
	private static int ticksInWorld = -1;
	private static int next;
	private static List<Step> steps;

	private SelfTest() {
	}

	static boolean enabled() {
		return NAME != null && !NAME.isBlank();
	}

	static void tick(Minecraft minecraft) {
		if (!worldRequested) {
			if (Mc.screen(minecraft) instanceof TitleScreen) {
				worldRequested = true;
				createWorld(minecraft);
			}
			return;
		}
		if (minecraft.player == null || minecraft.level == null) return;
		if (ticksInWorld < 0) setUp(minecraft);
		ticksInWorld++;
		while (next < steps.size() && steps.get(next).at() <= ticksInWorld) {
			Step step = steps.get(next++);
			step.action().run();
			if (step.shot() != null) shot(minecraft, step.shot());
		}
	}

	private static void createWorld(Minecraft minecraft) {
		minecraft.options.pauseOnLostFocus = false;
		Path saves = minecraft.getLevelSource().getBaseDir();
		Path old = saves.resolve(WORLD);
		if (Files.exists(old)) {
			try (Stream<Path> files = Files.walk(old)) {
				files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
			} catch (IOException e) {
				LucentClient.LOGGER.error("[selftest] could not delete the old world", e);
			}
		}
		LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
				false, WorldDataConfiguration.DEFAULT);
		minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions("lucent".hashCode(), false, false),
				WorldPresets::createNormalWorldDimensions, Mc.screen(minecraft));
	}

	private static void setUp(Minecraft minecraft) {
		ticksInWorld = 0;
		// Keep running when the test window isn't focused, instead of opening the pause menu.
		minecraft.options.pauseOnLostFocus = false;
		for (Module module : ModuleManager.modules()) {
			if (module != ModuleManager.FULLBRIGHT && module != ModuleManager.WEATHER) module.setEnabled(true);
		}
		CosmeticsManager.setLocal(PlayerCosmetics.NONE
				.with(CosmeticType.CAPE, "galaxy").with(CosmeticType.WINGS, "phoenix")
				.with(CosmeticType.HAT, "crown").with(CosmeticType.HALO, "gold"));
		LucentMenuScreen menu = new LucentMenuScreen();
		steps = List.of(
				new Step(100, "hud", () -> minecraft.options.setCameraType(CameraType.FIRST_PERSON)),
				new Step(110, null, () -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT)),
				new Step(140, "cosmetics-front", () -> {}),
				new Step(150, null, () -> minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK)),
				new Step(180, "cosmetics-back", () -> {}),
				new Step(190, null, () -> {
					minecraft.options.setCameraType(CameraType.FIRST_PERSON);
					Mc.setScreen(minecraft, menu);
				}),
				new Step(220, "menu-modules", () -> {}),
				new Step(230, null, () -> menu.showSettings(ModuleManager.KEYSTROKES)),
				new Step(250, "menu-settings", () -> {}),
				new Step(260, null, menu::showCosmetics),
				new Step(290, "menu-cosmetics", () -> {}),
				new Step(300, null, () -> Mc.setScreen(minecraft, new HudEditScreen(menu))),
				new Step(320, "hud-edit", () -> {}),
				new Step(330, null, () -> {
					LucentClient.LOGGER.info("[selftest] finished");
					Mc.setScreen(minecraft, null);
					minecraft.stop();
				}));
	}

	private static void shot(Minecraft minecraft, String step) {
		String file = "selftest-" + NAME + "-" + step + ".png";
		Screenshot.grab(minecraft.gameDirectory, file, Mc.mainRenderTarget(minecraft), 1,
				message -> LucentClient.LOGGER.info("[selftest] saved {}", file));
	}
}
