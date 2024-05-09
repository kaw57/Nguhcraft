package org.nguh.nguhcraft.mixin.server;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nguh.nguhcraft.Utils.EnchantLvl;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(
        method = "dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/block/Block.getDroppedStacks (Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
            ordinal = 0
        )
    )
    private static void inject$dropStacks(
        BlockState State,
        World World,
        BlockPos Pos,
        BlockEntity BE,
        Entity E,
        ItemStack T,
        CallbackInfo CI
    ) {
        if (!(World instanceof ServerWorld SW)) return;

        // Try to smelt the block if the tool has smelting.
        var Smelting = EnchantLvl(T, NguhcraftEnchantments.SMELTING);
        ServerUtils.SmeltingResult SR = null;
        if (Smelting != 0) SR = ServerUtils.TrySmeltBlock(SW, State);
        if (SR != null) {
            Block.dropStack(SW, Pos, SR.getStack());
            ExperienceOrbEntity.spawn(SW, Vec3d.ofCenter(Pos), SR.getExperience());
            CI.cancel();
        }
    }
}
