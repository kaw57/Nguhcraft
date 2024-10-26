package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.block.BlockState;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WitherSkullEntity.class)
public abstract class WitherSkullEntityMixin {
    /**
    * @author Sirraide
    * @reason Disabled entirely to prevent them from destroying protected blocks.
    */
    @Overwrite
    public float getEffectiveExplosionResistance(
        Explosion explosion,
        BlockView world,
        BlockPos pos,
        BlockState blockState,
        FluidState
        fluidState,
        float max
    ) { return max; }
}
