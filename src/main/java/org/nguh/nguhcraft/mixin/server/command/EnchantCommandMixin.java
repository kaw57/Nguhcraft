package org.nguh.nguhcraft.mixin.server.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.EnchantCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EnchantCommand.class)
public class EnchantCommandMixin {
    /**
    * @author Sirraide
    * @reason We provide our own version of this.
    */
    @Overwrite
    public static void register(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess
    ) {}
}
