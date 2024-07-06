package org.nguh.nguhcraft.mixin.client;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import org.nguh.nguhcraft.item.KeyItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.nguh.nguhcraft.Nguhcraft.Id;

@Mixin(TexturedRenderLayers.class)
public abstract class TexturedRenderLayersMixin {
    @Shadow @Final public static Identifier CHEST_ATLAS_TEXTURE;

    @Shadow private static SpriteIdentifier getChestTextureId(
        ChestType Ty,
        SpriteIdentifier Single,
        SpriteIdentifier Left,
        SpriteIdentifier Right
    ) { return null; }

    @Unique private static final SpriteIdentifier LOCKED =
        new SpriteIdentifier(CHEST_ATLAS_TEXTURE, Id("entity/chest/locked"));

    @Unique private static final SpriteIdentifier LOCKED_LEFT =
        new SpriteIdentifier(CHEST_ATLAS_TEXTURE, Id("entity/chest/locked_left"));

    @Unique private static final SpriteIdentifier LOCKED_RIGHT =
        new SpriteIdentifier(CHEST_ATLAS_TEXTURE, Id("entity/chest/locked_right"));

    /** Render a lock on locked chests. */
    @Inject(
        method = "getChestTextureId(Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/block/enums/ChestType;Z)Lnet/minecraft/client/util/SpriteIdentifier;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$getChestTextureId(
        BlockEntity BE,
        ChestType Ty,
        boolean Christmas,
        CallbackInfoReturnable<SpriteIdentifier> CIR
    ) {
        // TODO: Locked christmas and trapped chests.
        if (BE instanceof ChestBlockEntity && KeyItem.IsChestLocked(BE))
            CIR.setReturnValue(getChestTextureId(Ty, LOCKED, LOCKED_LEFT, LOCKED_RIGHT));
    }
}
