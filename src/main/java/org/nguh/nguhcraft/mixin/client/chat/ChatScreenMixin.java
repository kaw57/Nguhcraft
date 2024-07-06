package org.nguh.nguhcraft.mixin.client.chat;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nguh.nguhcraft.client.ClientUtils.MAX_CHAT_LENGTH;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow protected TextFieldWidget chatField;

    /** Override the max length of the text box. */
    @Inject(method = "init()V", at = @At("TAIL"))
    private void inject$init(CallbackInfo CI) {
        chatField.setMaxLength(MAX_CHAT_LENGTH);
    }

    /**
    * Also override the max length here.
    *
    * @author Sirraide
    * @reason This is so short an injection isn’t worth it.
    */
    @Overwrite
    public String normalize(@NotNull String Message) {
        return StringHelper.truncate(
            StringUtils.normalizeSpace(Message.trim()),
            MAX_CHAT_LENGTH,
            false
        );
    }
}
