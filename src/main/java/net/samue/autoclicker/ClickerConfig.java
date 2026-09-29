package net.samue.autoclicker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistent configuration for the Auto Clicker.
 *
 * <p>Every tunable the GUI exposes lives in {@code config/autoclicker.json} and can be
 * edited by hand as well - the mod never hardcodes a value the user may want to change.
 */
public class ClickerConfig {
    public enum MouseButton {
        LEFT("Left"), RIGHT("Right");

        private final String label;

        MouseButton(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum ClickType {
        SINGLE("Single", 1), DOUBLE("Double", 2), TRIPLE("Triple", 3), HOLD("Hold", 1);

        private final String label;
        private final int presses;

        ClickType(String label, int presses) {
            this.label = label;
            this.presses = presses;
        }

        public String label() {
            return label;
        }

        /** How many mouse presses are sent per interval firing. */
        public int presses() {
            return presses;
        }
    }

    public enum RepeatMode {
        UNTIL_STOPPED("Until stopped"), TIMES("N times");

        private final String label;

        RepeatMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ---- click interval (same four boxes as every auto clicker) ----
    public int hours = 0;
    public int minutes = 0;
    public int seconds = 1;
    public int millis = 100;

    // ---- random offset (interval + random(0..randomOffsetMs)) ----
    public boolean randomOffset = true;
    public int randomOffsetMs = 40;

    // ---- what to click ----
    public MouseButton mouseButton = MouseButton.LEFT;
    public ClickType clickType = ClickType.SINGLE;

    // ---- how often / how long ----
    public RepeatMode repeatMode = RepeatMode.UNTIL_STOPPED;
    public int repeatTimes = 1;

    // ---- quality of life ----
    /** Force "Pause on lost focus" off while running so clicks continue after tabbing out. */
    public boolean keepGameRunning = true;

    // ------------------------------------------------------------------

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("autoclicker.json");
    }

    public static ClickerConfig load() {
        Path p = path();
        ClickerConfig config = null;
        if (Files.exists(p)) {
            try (Reader reader = Files.newBufferedReader(p)) {
                config = GSON.fromJson(reader, ClickerConfig.class);
            } catch (Exception e) {
                AutoClickerClient.LOGGER.warn("[AutoClicker] Could not read autoclicker.json, using defaults", e);
            }
        }
        if (config == null) {
            config = new ClickerConfig();
        }
        config.sanitize();
        config.save();
        return config;
    }

    public void save() {
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            try (Writer writer = Files.newBufferedWriter(p)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            AutoClickerClient.LOGGER.warn("[AutoClicker] Could not save autoclicker.json", e);
        }
    }

    /** Repairs hand-edited / legacy files so nothing can go negative or null. */
    public void sanitize() {
        hours = clamp(hours, 0, 999);
        minutes = clamp(minutes, 0, 999);
        seconds = clamp(seconds, 0, 9999);
        millis = clamp(millis, 0, 999_999);
        randomOffsetMs = clamp(randomOffsetMs, 0, 60_000);
        repeatTimes = clamp(repeatTimes, 1, 1_000_000);
        if (mouseButton == null) mouseButton = MouseButton.LEFT;
        if (clickType == null) clickType = ClickType.SINGLE;
        if (repeatMode == null) repeatMode = RepeatMode.UNTIL_STOPPED;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Base interval in milliseconds. 0 means "as fast as one client tick allows". */
    public long intervalMillis() {
        long total = ((long) hours * 3600L + (long) minutes * 60L + (long) seconds) * 1000L + (long) millis;
        return Math.max(0L, total);
    }

    public long intervalNanos() {
        return Math.max(1_000_000L, intervalMillis() * 1_000_000L);
    }

    /** Interval of the next firing, including the random offset. */
    public long nextIntervalNanos() {
        long base = intervalNanos();
        if (!randomOffset || randomOffsetMs <= 0) {
            return base;
        }
        long extra = (long) (Math.random() * (double) randomOffsetMs);
        return base + extra * 1_000_000L;
    }

    public int pressCount() {
        return clickType.presses();
    }

    /** Short human readable summary, used in chat messages. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(mouseButton.label()).append(" · ");
        sb.append(clickType == ClickType.HOLD ? "Hold" : clickType.label()).append(" · ");
        if (clickType == ClickType.HOLD) {
            sb.append("every tick");
        } else {
            sb.append(intervalMillis()).append(" ms");
            if (randomOffset && randomOffsetMs > 0) {
                sb.append(" (+0-").append(randomOffsetMs).append(")");
            }
        }
        if (repeatMode == RepeatMode.TIMES) {
            sb.append(" · ").append(repeatTimes).append("x");
        }
        return sb.toString();
    }
}
