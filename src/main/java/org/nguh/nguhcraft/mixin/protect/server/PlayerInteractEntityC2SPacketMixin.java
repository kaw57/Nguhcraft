package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.nguh.nguhcraft.server.accessors.PlayerInteractEntityC2SPacketAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerInteractEntityC2SPacket.class)
public abstract class PlayerInteractEntityC2SPacketMixin implements PlayerInteractEntityC2SPacketAccessor {
    @Shadow @Final private PlayerInteractEntityC2SPacket.InteractTypeHandler type;

    /**
    * Fuck you whoever named this packet for calling this an ‘interact’
    * packet EVEN THOUGH IT ALSO HANDLES ATTACKING YOU MORON, and fuck
    * you Minecraft for making this damnable information so hard to access
    * that it needs 4 access wideners and for making the handler an
    * anonymous local class.
    */
    public boolean IsAttack() {
        return type.getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK;
    }
}
