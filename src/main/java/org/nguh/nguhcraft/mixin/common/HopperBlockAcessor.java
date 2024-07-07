package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HopperBlock.class)
public interface HopperBlockAcessor {
    @Invoker("getOutlineShape")
    VoxelShape Nguhcraft$GetOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context);

    @Invoker("getRaycastShape")
    VoxelShape Nguhcraft$GetRaycastShape(BlockState state, BlockView world, BlockPos pos);

    @Invoker("rotate")
    BlockState Nguhcraft$Rotate(BlockState state, BlockRotation rotation);

    @Invoker("mirror")
    BlockState Nguhcraft$Mirror(BlockState state, BlockMirror mirror);
}
