package com.mayheemtest.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * ChatUtil
 *
 * Sends a message to the local chat HUD only — never to the server.
 * Uses client.player.addMessage() which is the 1.21.11 client-side only path.
 */
public class ChatUtil {

    // Prefix: [Epstein Client] in dark gray brackets, gray text
    private static final Text PREFIX = Text.empty()
            .append(Text.literal("[").formatted(Formatting.DARK_GRAY))
            .append(Text.literal("Epstein Client").formatted(Formatting.GRAY))
            .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));

    /**
     * Send a plain client-side message with the mod prefix.
     */
    public static void send(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        MutableText full = Text.empty().append(PREFIX).append(message);
        mc.player.addMessage(full, false);
    }

    /**
     * Send a module toggle message.
     * Format: [Epstein Client] ModuleName ON   (ON = bold green)
     *         [Epstein Client] ModuleName OFF  (OFF = bold red)
     */
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
