package com.mayheemtest;

import com.mayheemtest.command.FriendManager;
import com.mayheemtest.gui.ClickGUI;
import com.mayheemtest.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class MayheemTestMod implements ClientModInitializer {

    public static ModuleManager moduleManager;
    private static KeyBinding guiKeybind;

    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("mayheemtest", "general"));

    @Override
    public void onInitializeClient() {
        // Load persisted friends list before anything else
        FriendManager.get().load();

        // Boot module system
        moduleManager = new ModuleManager();

        // Register GUI keybind — Right Shift
        guiKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mayheemtest.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        // Open/close GUI on keybind press
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
