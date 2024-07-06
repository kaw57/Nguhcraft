package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ServerCommandSource.class)
public abstract class ServerCommandSourceMixin {
    /**
    * Do not broadcast command feedback to ops.
    *
    * @author Sirraide
    * @reason Annoying and pointless. Also horrible if we ever do adventure streams again.
    */
    @Overwrite
    private void sendToOps(Text T) {}
}
