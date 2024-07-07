package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecartEntity.class)
public interface AbstractMinecartEntityAccessor {
    @Invoker("moveOffRail")
    void Nguhcraft$MoveOffRail();
}
