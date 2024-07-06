package org.nguh.nguhcraft.mixin.protect.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.nguh.nguhcraft.server.accessors.LecternScreenHandlerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin extends BlockEntity {
    public LecternBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Store the position of the lectern block in the screen handler. */
    @ModifyReturnValue(method = "createMenu", at = @At("RETURN"))
    private ScreenHandler inject$createMenu(ScreenHandler Original) {
        ((LecternScreenHandlerAccessor)Original).Nguhcraft$SetLecternPos(getPos());
        return Original;
    }
}
