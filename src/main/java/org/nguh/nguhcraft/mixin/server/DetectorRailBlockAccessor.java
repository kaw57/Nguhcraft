package org.nguh.nguhcraft.mixin.server;

import net.minecraft.block.BlockState;
import net.minecraft.block.DetectorRailBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DetectorRailBlock.class)
public interface DetectorRailBlockAccessor {
    @Invoker("updatePoweredStatus")
    void Nguhcraft$UpdatePoweredStatus(World world, BlockPos pos, BlockState state);
}
