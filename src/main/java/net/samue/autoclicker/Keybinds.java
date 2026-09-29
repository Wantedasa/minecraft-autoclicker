package net.samue.autoclicker;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Two bindings only: toggle the clicker, open the settings screen. */
public class Keybinds {
    private static final Category CATEGORY = new Category(Identifier.of(AutoClickerClient.MOD_ID, "keycategory"));

    public final KeyBinding toggle;
    public final KeyBinding openGui;

    public Keybinds() {
        toggle = new KeyBinding("key.autoclicker.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, CATEGORY);
        openGui = new KeyBinding("key.autoclicker.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, CATEGORY);
        KeyBindingHelper.registerKeyBinding(toggle);
        KeyBindingHelper.registerKeyBinding(openGui);
    }

    public void handle(MinecraftClient client) {
        while (toggle.wasPressed()) {
            AutoClickerClient.clicker.toggle(client);
        }
        while (openGui.wasPressed()) {
            if (client.currentScreen instanceof AutoClickerScreen) {
                client.setScreen(null);
            } else {
                client.setScreen(new AutoClickerScreen());
            }
        }
    }
}
