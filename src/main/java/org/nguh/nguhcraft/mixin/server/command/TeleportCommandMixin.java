package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.LookTarget;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

import static org.nguh.nguhcraft.server.ExtensionsKt.SavePositionBeforeTeleport;

@Mixin(TeleportCommand.class)
public abstract class TeleportCommandMixin {
    /** Save last position before teleporting. */
    @Inject(
        method = "teleport",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FFZ)Z"
        )
    )
    private static void inject$teleport(ServerCommandSource source, Entity target, ServerWorld world, double x, double y, double z, Set<PositionFlag> movementFlags, float yaw, float pitch, @Nullable LookTarget facingLocation, CallbackInfo ci) {
        if (target instanceof ServerPlayerEntity SP)
            SavePositionBeforeTeleport(SP);
    }
}
