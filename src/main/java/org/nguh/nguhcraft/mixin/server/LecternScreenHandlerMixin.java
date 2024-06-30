package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.server.accessors.LecternScreenHandlerAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternScreenHandler.class)
public abstract class LecternScreenHandlerMixin implements LecternScreenHandlerAccessor {
    @Shadow @Final public static int TAKE_BOOK_BUTTON_ID;
    @Unique private BlockPos LecternPos;

    @Override public void Nguhcraft$SetLecternPos(BlockPos Pos) {
        LecternPos = Pos;
    }

    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void inject$onButtonClick(PlayerEntity PE, int Button, CallbackInfoReturnable<Boolean> CIR) {
        if (Button == TAKE_BOOK_BUTTON_ID && !ProtectionManager.AllowBlockModify(PE, PE.getWorld(), LecternPos))
            CIR.setReturnValue(false);
    }
}
