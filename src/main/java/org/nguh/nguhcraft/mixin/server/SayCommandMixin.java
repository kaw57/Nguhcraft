package org.nguh.nguhcraft.mixin.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.SayCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SayCommand.class)
public abstract class SayCommandMixin {
    /**
     * @author Sirraide
     * @reason This command is pointless and uses chat signing.
     */
    @Overwrite
    public static void register(CommandDispatcher<ServerCommandSource> S) { }
}
