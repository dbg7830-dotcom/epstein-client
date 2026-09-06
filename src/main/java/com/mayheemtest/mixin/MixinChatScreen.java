package com.mayheemtest.mixin;

import com.mayheemtest.command.CommandHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinChatScreen
 *
 * In 1.21.11 ChatScreen#sendMessage returns void, not boolean.
 * We use CallbackInfo (not CallbackInfoReturnable) and cancel() to
 * prevent the message reaching the network stack entirely.
 */
@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Inject(
        method = "sendMessage",
        at = @At("HEAD"),
        cancellable = true
    )
    private void interceptChat(String message, boolean addToHistory, CallbackInfo ci) {
        if (CommandHandler.handle(message)) {
            ci.cancel(); // stops Minecraft sending anything to the server
        }
    }
}
