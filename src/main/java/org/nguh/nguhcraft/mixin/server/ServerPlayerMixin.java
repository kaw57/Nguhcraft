package org.nguh.nguhcraft.mixin.server;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin extends PlayerEntity implements ServerPlayerAccessor {
    public ServerPlayerMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique private boolean Vanished = false;
    @Unique private boolean BypassesRegionProtection = false;

    @Unique static private final Logger LOGGER = LogUtils.getLogger();

    @Shadow public abstract void sendMessage(Text message);

    @Override public boolean getVanished() { return Vanished; }
    @Override public void setVanished(boolean vanished) { Vanished = vanished; }

    @Override public boolean getBypassesRegionProtection() { return BypassesRegionProtection; }
    @Override public void setBypassesRegionProtection(boolean bypassesProtection) {
        BypassesRegionProtection = bypassesProtection;
    }

    /** Load custom data from Nbt. */
    @Override
    public void LoadGeneralNguhcraftNbt(@NotNull NbtCompound nbt) {
        if (nbt.contains(TAG_ROOT)) {
            var nguh = nbt.getCompound(TAG_ROOT);
            Vanished = nguh.getBoolean(TAG_VANISHED);
            BypassesRegionProtection = nguh.getBoolean(TAG_BYPASSES_REGION_PROTECTION);
        }
    }

    /**
    * Copy over player data from a to-be-deleted instance.
    * <p>
    * For SOME UNGODLY REASON, Minecraft creates a NEW PLAYER ENTITY when the player
    * dies and basically does the equivalent of reconnecting the player, but on the
    * same network connexion etc.
    * <p>
    * This function copies our custom data over to the new player entity.
    */
    @Inject(
        method = "copyFrom(Lnet/minecraft/server/network/ServerPlayerEntity;Z)V",
        at = @At("TAIL")
    )
    private void inject$copyFrom(ServerPlayerEntity Old, boolean Alive, CallbackInfo CI) {
        var OldNSP = (ServerPlayerAccessor) Old;
        Vanished = OldNSP.getVanished();
        BypassesRegionProtection = OldNSP.getBypassesRegionProtection();
    }

    /** Save Nbt data to the player file. */
    @Inject(method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V", at = @At("TAIL"))
    private void inject$saveData(@NotNull NbtCompound nbt, CallbackInfo ci) {
        var tag = nbt.getCompound(TAG_ROOT);
        tag.putBoolean(TAG_VANISHED, Vanished);
        tag.putBoolean(TAG_BYPASSES_REGION_PROTECTION, BypassesRegionProtection);
        nbt.put(TAG_ROOT, tag);
    }

    /**
     * Make sure we *never* send signed messages under any circumstances.
     * <p>
     * @author Sirraide
     * @reason Last line of defence. This should *never* be called in the first place. If
     *         it ever is, that means we forgot to override something else.
     */
     @Overwrite
     public void sendChatMessage(@NotNull SentMessage message, boolean filterMaskEnabled, MessageType.Parameters params) {
         LOGGER.error("Refusing to send signed message to '{}': {}", getNameForScoreboard(), message.content());
     }
}
