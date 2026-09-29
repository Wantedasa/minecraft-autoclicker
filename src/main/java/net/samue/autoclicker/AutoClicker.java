package net.samue.autoclicker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

/**
 * The click engine.
 *
 * <p>Instead of moving the real cursor, clicks are injected into the vanilla attack/use
 * bindings exactly the way the mouse does it ({@link KeyBinding#setKeyPressed} plus
 * {@link KeyBinding#onKeyPressed}), so the client itself sends the interaction packets -
 * no packet spoofing, no server desync.
 *
 * <p>Why it keeps clicking while Minecraft is not focused: the client keeps ticking and
 * keeps processing input when the window is unfocused (unlike a real mouse, which the OS
 * routes to the window you are looking at). The only thing that stops it in vanilla is the
 * "Pause on lost focus" behaviour, which opens the game menu after 500 ms - the engine turns
 * that option off while it runs (see {@link #keepGameRunning}).
 */
public class AutoClicker {
    /** One client tick (20/s). Sub-tick intervals are handled with several presses per tick. */
    private static final long TICK_NANOS = 50_000_000L;
    /** Safety cap so a 1 ms interval can never flood the server. */
    private static final int MAX_CLICKS_PER_TICK = 20;

    private final ClickerConfig config;

    private boolean enabled = false;
    private long nextClickAtNanos = 0L;
    private long sequences = 0L;
    private long clicksSent = 0L;

    /** The key we pressed ourselves, so it can be released again. */
    private InputUtil.Key heldKey = null;
    private int releaseAtTick = -1;
    private int tickCounter = 0;

    private boolean pauseOptionChanged = false;
    private boolean pauseOptionOriginal = true;

    public AutoClicker(ClickerConfig config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getClicksSent() {
        return clicksSent;
    }

    public long getSequences() {
        return sequences;
    }

    public void toggle(MinecraftClient client) {
        setEnabled(client, !this.enabled);
    }

    public void setEnabled(MinecraftClient client, boolean value) {
        if (this.enabled == value) {
            return;
        }
        this.enabled = value;
        this.nextClickAtNanos = 0L;
        this.sequences = 0L;
        this.clicksSent = 0L;

        if (value) {
            keepGameRunning(client);
            message(client, Text.literal("§a[AutoClicker] ON §7· " + config.describe()));
        } else {
            releaseHeldKey(client);
            restorePauseOption(client);
            message(client, Text.literal("§7[AutoClicker] OFF §7· " + clicksSent + " clicks"));
        }
    }

    /** Called once per client tick. */
    public void tick(MinecraftClient client) {
        this.tickCounter++;

        // Release the key we held: a click is a press + release, never a stuck button.
        if (heldKey != null && releaseAtTick >= 0 && tickCounter >= releaseAtTick) {
            KeyBinding.setKeyPressed(heldKey, false);
            releaseAtTick = -1;
        }

        if (!this.enabled) {
            return;
        }
        // Only in a world, and never while a GUI is open - that is also how vanilla behaves.
        if (client.player == null || client.world == null || client.currentScreen != null) {
            nextClickAtNanos = 0L;
            return;
        }

        if (config.clickType == ClickerConfig.ClickType.HOLD) {
            // Keep the button down: repeats attacks/item use every tick, and keeps the
            // "still holding" branch alive for eating / fishing / bows.
            press(client);
            sequences++;
            return;
        }

        long now = System.nanoTime();
        if (nextClickAtNanos == 0L) {
            nextClickAtNanos = now;
        }

        long interval = config.intervalNanos();
        int budget = (int) Math.max(1L, Math.min((long) MAX_CLICKS_PER_TICK, TICK_NANOS / interval));
        int fired = 0;
        while (now >= nextClickAtNanos && fired < budget) {
            if (config.repeatMode == ClickerConfig.RepeatMode.TIMES && sequences >= config.repeatTimes) {
                setEnabled(client, false);
                message(client, Text.literal("§e[AutoClicker] Done: " + config.repeatTimes + " repeat(s)."));
                return;
            }
            press(client);
            sequences++;
            fired++;
            nextClickAtNanos += config.nextIntervalNanos();
        }
        if (fired >= budget) {
            // Could not keep up (interval below one tick): resync instead of building a backlog.
            nextClickAtNanos = now + interval;
        }
    }

    private void press(MinecraftClient client) {
        int count = config.pressCount();
        for (int i = 0; i < count; i++) {
            pressOnce(client);
        }
    }

    private void pressOnce(MinecraftClient client) {
        KeyBinding binding = config.mouseButton == ClickerConfig.MouseButton.LEFT
                ? client.options.attackKey
                : client.options.useKey;
        InputUtil.Key key = boundKey(binding);
        if (key == null) {
            return;
        }
        KeyBinding.setKeyPressed(key, true);
        KeyBinding.onKeyPressed(key);
        clicksSent++;
        heldKey = key;
        releaseAtTick = tickCounter + 1;
    }

    private void releaseHeldKey(MinecraftClient client) {
        if (heldKey != null) {
            KeyBinding.setKeyPressed(heldKey, false);
        }
        heldKey = null;
        releaseAtTick = -1;
    }

    /**
     * Resolves the key the player currently has bound to attack/use. Uses the translation key
     * so a rebound attack key (e.g. another mouse button) still works.
     */
    private static InputUtil.Key boundKey(KeyBinding binding) {
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
            if (key == null || key == InputUtil.UNKNOWN_KEY) {
                return null;
            }
            return key;
        } catch (Throwable t) {
            return null;
        }
    }

    // ---- "keep running while tabbed out" ------------------------------------------------

    private void keepGameRunning(MinecraftClient client) {
        if (!config.keepGameRunning) {
            return;
        }
        if (client.options.pauseOnLostFocus) {
            pauseOptionOriginal = true;
            pauseOptionChanged = true;
            client.options.pauseOnLostFocus = false;
            message(client, Text.literal("§e[AutoClicker] 'Pause on lost focus' disabled for this session."));
        }
    }

    private void restorePauseOption(MinecraftClient client) {
        if (pauseOptionChanged) {
            client.options.pauseOnLostFocus = pauseOptionOriginal;
            pauseOptionChanged = false;
        }
    }

    private static void message(MinecraftClient client, Text text) {
        if (client.player != null) {
            client.player.sendMessage(text, false);
        }
    }
}
