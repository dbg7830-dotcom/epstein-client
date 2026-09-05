package com.mayheemtest;

import com.mayheemtest.gui.ClickGUI;
import com.mayheemtest.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * MayheemTestMod – Fabric client entry point.
 *
 * Responsibilities
 * ────────────────
 * 1. Instantiate the ModuleManager (which registers all modules).
 * 2. Register the GUI keybind (Right Shift by default).
 * 3. Open the ClickGUI when the keybind is pressed.
 */
public class MayheemTestMod implements ClientModInitializer {

    public static ModuleManager moduleManager;

    private static KeyBinding guiKeybind;

    @Override
    public void onInitializeClient() {
        // 1. Boot the module system
        moduleManager = new ModuleManager();

        // 2. Register GUI keybind – Right Shift
        guiKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mayheemtest.gui",          // translation key
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.mayheemtest"          // category shown in controls menu
        ));

        // 3. Open GUI on keybind press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKeybind.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGUI());
                } else if (client.currentScreen instanceof ClickGUI) {
                    client.setScreen(null);
                }
            }
        });
    }
}
