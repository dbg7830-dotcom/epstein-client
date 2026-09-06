package com.mayheemtest.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * ChatUtil — client-side only chat messages, never sent to server.
 */
public class ChatUtil {

    private static final Text PREFIX = Text.empty()
            .append(Text.literal("[").formatted(Formatting.DARK_GRAY))
            .append(Text.literal("Epstein Client").formatted(Formatting.GRAY))
            .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));

    public static void send(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        MutableText full = Text.empty().append(PREFIX).append(message);
        // sendMessage(text, overlay=false) — overlay=false means chat HUD, not action bar
        // This is purely client-side and never touches the network
        mc.player.sendMessage(full, false);
    }

    public static void sendToggle(String moduleName, boolean enabled) {
        MutableText state = enabled
                ? Text.literal("ON").formatted(Formatting.BOLD, Formatting.GREEN)
                : Text.literal("OFF").formatted(Formatting.BOLD, Formatting.RED);

        MutableText message = Text.empty()
                .append(Text.literal(moduleName + " ").formatted(Formatting.WHITE))
                .append(state);

        send(message);
    }
}
