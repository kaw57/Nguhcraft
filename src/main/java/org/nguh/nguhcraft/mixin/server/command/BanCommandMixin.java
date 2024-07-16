package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.BanCommand;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BanCommand.class)
public abstract class BanCommandMixin {
    @Redirect(
        method = "method_13024",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/command/ServerCommandSource;hasPermissionLevel(I)Z"
        )
    )
    private static boolean inject$register(ServerCommandSource S, int Lvl) {
        return ServerUtils.IsModerator(S);
    }
}
