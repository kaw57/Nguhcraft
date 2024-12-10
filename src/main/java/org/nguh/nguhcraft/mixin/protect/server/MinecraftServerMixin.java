package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.server.MinecraftServer;
import org.nguh.nguhcraft.protect.ProtectionManagerAccessor;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.server.ServerProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ProtectionManagerAccessor {
    @Unique private ProtectionManager Manager = new ServerProtectionManager();

    @Override public ProtectionManager Nguhcraft$GetProtectionManager() { return Manager; }
    @Override public void Nguhcraft$SetProtectionManager(ProtectionManager manager) { Manager = manager; }
}
