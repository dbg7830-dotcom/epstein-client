package com.mayheemtest.mixin;

import com.mayheemtest.command.CommandHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinChatScreen
 *
 * Intercepts ChatScreen#sendMessage before it reaches the network stack.
 * If CommandHandler recognises the message as a dot-command, we cancel
 * the return (returning false) so Minecraft never sends it to the server.
 *
 * Nothing from a dot-command ever leaves the client.
 */
@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Inject(
        method = "sendMessage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void interceptChat(String message, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (CommandHandler.handle(message)) {
            // It was a dot-command — cancel network send, return true so the
            // chat screen closes normally (same UX as sending a real message).
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
