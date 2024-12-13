package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.nguh.nguhcraft.block.LockedDoorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin {
    @Shadow public abstract Map<BlockPos, BlockEntity> getBlockEntities();

    /**
     * Repair the missing LOCKED block state.
     * <p>
     * The LOCKED state was introduced after the fact, so there are a bunch
     * of locked door blocks that don’t have this state set properly; this
     * fixes that during the chunk post-processing step.
     */
    @Inject(method = "runPostProcessing", at = @At("HEAD"))
    private void inject$runPostProcessing(ServerWorld SW, CallbackInfo CI) {
        for (var BE : getBlockEntities().values())
            if (BE instanceof LockedDoorBlockEntity LBE)
                LBE.UpdateBlockState(SW);
    }
}
