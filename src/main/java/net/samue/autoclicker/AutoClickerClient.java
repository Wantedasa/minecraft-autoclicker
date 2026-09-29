package net.samue.autoclicker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoClickerClient implements ClientModInitializer {
    public static final String MOD_ID = "autoclicker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ClickerConfig config;
    public static AutoClicker clicker;
    public static Keybinds keybinds;

    @Override
    public void onInitializeClient() {
        config = ClickerConfig.load();
        clicker = new AutoClicker(config);
        keybinds = new Keybinds();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                keybinds.handle(client);
                clicker.tick(client);
            } catch (Throwable t) {
                LOGGER.error("[AutoClicker] tick failed", t);
                throw t;
            }
        });

        LOGGER.info("[AutoClicker] loaded - {} (always starts disabled)", config.describe());
    }
}
