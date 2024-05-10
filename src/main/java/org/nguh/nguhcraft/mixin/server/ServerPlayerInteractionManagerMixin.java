package org.nguh.nguhcraft.mixin.server;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow protected abstract void onBlockBreakingAction(BlockPos pos, boolean success, int sequence, String reason);

    @Shadow @Final protected ServerPlayerEntity player;

    @Shadow protected ServerWorld world;

    @Inject(method = "processBlockBreakingAction", at = @At("HEAD"), cancellable = true)
    private void inject$processBlockBreakingAction(
        BlockPos Pos,
        PlayerActionC2SPacket.Action A,
        Direction Dir,
        int WH,
        int Seq,
        CallbackInfo CI
    ) {
        if (!ProtectionManager.AllowBlockBreak(player, this.world, Pos)) {
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(Pos, this.world.getBlockState(Pos)));
            onBlockBreakingAction(Pos, false, Seq, "disallowed");
            CI.cancel();
        }
    }
}
