package org.nguh.nguhcraft.mixin.server;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow protected abstract void onBlockBreakingAction(BlockPos pos, boolean success, int sequence, String reason);

    @Shadow @Final protected ServerPlayerEntity player;

    @Shadow protected ServerWorld world;

    /** Prevent item use. */
    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void inject$interactItem(
        ServerPlayerEntity SP,
        World W,
        ItemStack St,
        Hand H,
        CallbackInfoReturnable<ActionResult> CIR
    ) {
        if (!ProtectionManager.AllowItemUse(SP, W, St))
            CIR.setReturnValue(ActionResult.PASS);
    }

    /** Prevent block modification. */
    @Inject(method = "processBlockBreakingAction", at = @At("HEAD"), cancellable = true)
    private void inject$processBlockBreakingAction(
        BlockPos Pos,
        PlayerActionC2SPacket.Action A,
        Direction Dir,
        int WH,
        int Seq,
        CallbackInfo CI
    ) {
        if (!ProtectionManager.AllowBlockModify(player, this.world, Pos)) {
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(Pos, this.world.getBlockState(Pos)));
            onBlockBreakingAction(Pos, false, Seq, "disallowed");
            CI.cancel();
        }
    }
}
