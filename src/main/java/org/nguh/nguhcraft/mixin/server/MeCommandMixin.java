package org.nguh.nguhcraft.mixin.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.MeCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MeCommand.class)
public abstract class MeCommandMixin {
    /**
    * @author Sirraide
    * @reason This command is pointless and uses chat signing.
    */
    @Overwrite
    public static void register(CommandDispatcher<ServerCommandSource> S) { }
}
