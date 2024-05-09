package org.nguh.nguhcraft.mixin.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TriggerCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TriggerCommand.class)
public abstract class TriggerCommandMixin {
    /**
    * @author Sirraide
    * @reason It’s unnecessary, and removing it cleans up the command list a bit.
    */
    @Overwrite
    public static void register(CommandDispatcher<ServerCommandSource> D) {}
}
