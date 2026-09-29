package net.samue.autoclicker;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;

/**
 * Client game test (dev only, runs with {@code gradle runClientGametest}).
 *
 * <p>It checks the two things that are easy to break and hard to see: the settings screen
 * actually renders, and clicks keep firing while the window is NOT focused (the whole point
 * of the mod). Not part of normal gameplay - the entrypoint only runs when the
 * {@code fabric.client.gametest} system property is set.
 *
 * <p>Everything touching the client goes through {@code computeOnClient} / {@code runOnClient},
 * because the test body runs on its own thread.
 */
public class AutoClickerGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        MinecraftClient client = context.computeOnClient(c -> MinecraftClient.getInstance());
        ClickerConfig cfg = AutoClickerClient.config;
        AutoClicker clicker = AutoClickerClient.clicker;

        // ---- 1) settings screen opens and renders ----
        context.setScreen(AutoClickerScreen::new);
        context.waitTicks(5);
        check(context.computeOnClient(c -> c.currentScreen instanceof AutoClickerScreen), "settings screen did not open");
        context.takeScreenshot("autoclicker_settings");
        context.setScreen(() -> null);
        context.waitTicks(2);

        // ---- 2) in a world, unfocused: do clicks keep coming? ----
        cfg.seconds = 0;
        cfg.millis = 100;
        cfg.randomOffset = false;
        cfg.mouseButton = ClickerConfig.MouseButton.LEFT;
        cfg.clickType = ClickerConfig.ClickType.SINGLE;
        cfg.repeatMode = ClickerConfig.RepeatMode.UNTIL_STOPPED;
        cfg.keepGameRunning = true;

        try (TestSingleplayerContext singleplayer = context.worldBuilder().setUseConsistentSettings(true).create()) {
            context.waitTicks(40);

            context.runOnClient(c -> c.onWindowFocusChanged(false)); // simulate alt-tab out of Minecraft
            context.waitTicks(5);
            check(context.computeOnClient(c -> !c.isWindowFocused()), "window should be simulated as unfocused");

            long before = clicker.getClicksSent();
            context.runOnClient(c -> clicker.setEnabled(c, true));

            int swingTicks = 0;
            for (int i = 0; i < 60; i++) {
                context.waitTick();
                if (context.computeOnClient(c -> c.player != null && (c.player.handSwinging || c.player.handSwingTicks > 0))) {
                    swingTicks++;
                }
            }
            long clicks = clicker.getClicksSent() - before;

            log("clicks=" + clicks + " swingTicks=" + swingTicks
                    + " focused=" + context.computeOnClient(c -> c.isWindowFocused())
                    + " screen=" + context.computeOnClient(c -> String.valueOf(c.currentScreen))
                    + " pauseOnLostFocus=" + context.computeOnClient(c -> c.options.pauseOnLostFocus));

            context.takeScreenshot("autoclicker_ingame_unfocused");

            check(clicks >= 15, "too few clicks while unfocused: " + clicks);
            check(context.computeOnClient(c -> c.currentScreen == null), "a screen opened while unfocused");

            context.runOnClient(c -> clicker.setEnabled(c, false));
            context.waitTicks(5);
            check(!clicker.isEnabled(), "clicker did not switch off");
        }

        // the harness requires the test to finish on the title screen with no world open
        context.setScreen(TitleScreen::new);
        context.waitTicks(5);
        log("ALL CHECKS PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("[AutoClicker gametest] " + message);
        }
    }

    private static void log(String message) {
        System.out.println("[AutoClicker gametest] " + message);
        AutoClickerClient.LOGGER.info("[gametest] {}", message);
    }
}
