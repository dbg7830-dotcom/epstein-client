package com.mayheemtest.command;

import com.mayheemtest.util.ChatUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;

/**
 * CommandHandler
 *
 * Processes client-only dot-commands (e.g. ".friends add Steve").
 * Called by the chat send event mixin BEFORE the message reaches the server.
 * The event is cancelled if the message starts with "." so nothing is ever
 * sent to the server or visible to other players.
 *
 * Supported commands
 * ──────────────────
 *  .friends add <name>     – add a player to the whitelist
 *  .friends remove <name>  – remove a player from the whitelist
 *  .friends list           – show all whitelisted players in local chat
 *  .friends clear          – remove everyone from the whitelist
 *  .help                   – list all available dot-commands
 */
public class CommandHandler {

    private static final String PREFIX = ".";

    /**
     * Entry point. Returns true if the message was a dot-command (caller
     * must cancel the chat event). Returns false if it's a normal message.
     */
    public static boolean handle(String message) {
        if (!message.startsWith(PREFIX)) return false;

        // Strip the leading dot and split on spaces
        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return true;

        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "friends" -> handleFriends(parts);
            case "help"    -> handleHelp();
            default        -> ChatUtil.send(
                    Text.literal("Unknown command. Type ")
                        .formatted(Formatting.RED)
                        .append(Text.literal(".help").formatted(Formatting.YELLOW))
                        .append(Text.literal(" for a list.").formatted(Formatting.RED))
            );
        }

        return true; // always cancel — dot prefix means client command
    }

    // ── .friends ──────────────────────────────────────────────────────────

    private static void handleFriends(String[] parts) {
        if (parts.length < 2) {
            sendFriendsUsage();
            return;
        }

        String sub = parts[1].toLowerCase();

        switch (sub) {
            case "add" -> {
                if (parts.length < 3) { sendFriendsUsage(); return; }
                String name = parts[2];
                if (FriendManager.get().add(name)) {
                    ChatUtil.send(
                        Text.literal(name).formatted(Formatting.GREEN, Formatting.BOLD)
                            .append(Text.literal(" added to friends whitelist.")
                                .formatted(Formatting.GREEN))
                    );
                } else {
                    ChatUtil.send(
                        Text.literal(name).formatted(Formatting.YELLOW)
                            .append(Text.literal(" is already on the whitelist.")
                                .formatted(Formatting.YELLOW))
                    );
                }
            }
            case "remove" -> {
                if (parts.length < 3) { sendFriendsUsage(); return; }
                String name = parts[2];
                if (FriendManager.get().remove(name)) {
                    ChatUtil.send(
                        Text.literal(name).formatted(Formatting.RED, Formatting.BOLD)
                            .append(Text.literal(" removed from friends whitelist.")
                                .formatted(Formatting.RED))
                    );
                } else {
                    ChatUtil.send(
                        Text.literal(name).formatted(Formatting.YELLOW)
                            .append(Text.literal(" was not on the whitelist.")
                                .formatted(Formatting.YELLOW))
                    );
                }
            }
            case "list" -> {
                Set<String> all = FriendManager.get().getAll();
                if (all.isEmpty()) {
                    ChatUtil.send(Text.literal("Whitelist is empty.").formatted(Formatting.GRAY));
                } else {
                    ChatUtil.send(Text.literal("Friends whitelist:").formatted(Formatting.GRAY));
                    for (String f : all) {
                        ChatUtil.send(
                            Text.literal("  • ").formatted(Formatting.DARK_GRAY)
                                .append(Text.literal(f).formatted(Formatting.WHITE))
                        );
                    }
                }
            }
            case "clear" -> {
                FriendManager.get().clear();
                ChatUtil.send(Text.literal("Whitelist cleared.").formatted(Formatting.GRAY));
            }
            default -> sendFriendsUsage();
        }
    }

    private static void sendFriendsUsage() {
        ChatUtil.send(Text.literal("Usage: ").formatted(Formatting.GRAY)
            .append(Text.literal(".friends add/remove/list/clear <name>")
                .formatted(Formatting.YELLOW)));
    }

    // ── .help ─────────────────────────────────────────────────────────────

    private static void handleHelp() {
        ChatUtil.send(Text.literal("Available commands:").formatted(Formatting.GRAY));
        helpLine(".friends add <name>",    "add player to attack whitelist");
        helpLine(".friends remove <name>", "remove player from whitelist");
        helpLine(".friends list",          "show all whitelisted players");
        helpLine(".friends clear",         "clear the entire whitelist");
        helpLine(".help",                  "show this message");
    }

    private static void helpLine(String cmd, String desc) {
        ChatUtil.send(
            Text.literal("  " + cmd).formatted(Formatting.YELLOW)
                .append(Text.literal(" – " + desc).formatted(Formatting.GRAY))
        );
    }
}
