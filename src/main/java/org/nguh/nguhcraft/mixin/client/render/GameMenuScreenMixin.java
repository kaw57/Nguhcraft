package org.nguh.nguhcraft.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {
    @Unique static private final Tooltip TT = Tooltip.of(Text.of("LAN multiplayer is not supported by Nguhcraft!"));

    /** We don’t support LAN multiplayer, so disable the button. */
    @WrapOperation(
        method = "initWidgets",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/GridWidget$Adder;add(Lnet/minecraft/client/gui/widget/Widget;)Lnet/minecraft/client/gui/widget/Widget;",
            ordinal = 5
        )
    )
    private Widget inject$initWidgets(
        GridWidget.Adder Instance,
        Widget W,
        Operation<Widget> Orig
    ) {
        Orig.call(Instance, W);
        var BW = ((ButtonWidget)W);
        BW.active = false;
        BW.setTooltip(TT);
        return W;
    }
}
