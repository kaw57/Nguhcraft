package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.server.Discord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow private ServerPlayerEntity owner;

    /**
    * Grab advancement message.
    * <p>
    * The advancement message is computed in a method call to PlayerList::broadcast;
    * save it for forwarding to Discord. The call to broadcast also happens in a lambda,
    * which makes this even more annoying to intercept.
    */
    @ModifyArg(
        method = "method_53637(Lnet/minecraft/advancement/AdvancementEntry;Lnet/minecraft/advancement/AdvancementDisplay;)V",
        index = 0,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/server/PlayerManager.broadcast(Lnet/minecraft/text/Text;Z)V",
            ordinal = 0
        )
    )
    private Text inject$grantCriterion$lambda0$0(Text AdvancementMessage, @Share("AdvancementMessage") LocalRef<Text> Message) {
        Message.set(AdvancementMessage);
        return AdvancementMessage;
    }

    /** And forward it to Discord. */
    @Inject(
        method = "method_53637(Lnet/minecraft/advancement/AdvancementEntry;Lnet/minecraft/advancement/AdvancementDisplay;)V",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/server/PlayerManager.broadcast(Lnet/minecraft/text/Text;Z)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void inject$grantCriterion$lambda0$1(CallbackInfo CI, @Share("AdvancementMessage") LocalRef<Text> Message) {
        Discord.BroadcastAdvancement(owner, Message.get());
    }
}
