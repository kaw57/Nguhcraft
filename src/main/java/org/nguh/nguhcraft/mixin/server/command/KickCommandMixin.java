package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.server.command.KickCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KickCommand.class)
public abstract class KickCommandMixin {
    @Redirect(
        method = "method_13413",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/command/ServerCommandSource;hasPermissionLevel(I)Z"
        )
    )
    private static boolean inject$register(ServerCommandSource S, int Lvl) {
        return ServerUtils.IsModerator(S);
    }
}
