package org.nguh.nguhcraft.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @SuppressWarnings("deprecation") @Unique private static final SpriteIdentifier SOUL_FIRE_0 =
        new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/soul_fire_0"));

    @SuppressWarnings("deprecation") @Unique private static final SpriteIdentifier SOUL_FIRE_1 =
        new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, Identifier.ofVanilla("block/soul_fire_1"));

    /** Hack to check if we’re rendering a trident. */
    @Unique private boolean RenderingTrident = false;

    /** Remember that we’re rendering a trident. */
    @Inject(
        method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
        at = @At("HEAD")
    )
    private  <E extends Entity, S extends EntityRenderState> void inject$render$0(
        E Entity,
        double x,
        double y,
        double z,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        EntityRenderer<? super E, S> renderer,
        CallbackInfo ci
    ) { RenderingTrident = Entity instanceof TridentEntity; }

    /** And clear the flag when we’re done. */
    @Inject(
        method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
        at = @At("TAIL")
    )
    private <E extends Entity, S extends EntityRenderState> void inject$render$1(
        E Entity,
        double x,
        double y,
        double z,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        EntityRenderer<? super E, S> renderer,
        CallbackInfo ci
    ) { RenderingTrident = false; }

    /** Render blue fire for tridents instead. */
    @Inject(
        method = "renderFire",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
            ordinal = 0
        )
    )
    private void inject$renderFire(
        MatrixStack MS,
        VertexConsumerProvider VC,
        EntityRenderState E,
        Quaternionf Rot,
        CallbackInfo CI,
        @Local(ordinal = 0) LocalRef<Sprite> S1,
        @Local(ordinal = 1) LocalRef<Sprite> S2
    ) {
        if (RenderingTrident) {
            S1.set(SOUL_FIRE_0.getSprite());
            S2.set(SOUL_FIRE_1.getSprite());
        }
    }
}
