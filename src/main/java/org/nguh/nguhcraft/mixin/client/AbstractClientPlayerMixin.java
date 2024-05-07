package org.nguh.nguhcraft.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.client.NguhcraftClientPlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerMixin extends PlayerEntity {
    @Shadow private @Nullable PlayerListEntry playerListEntry;

    public AbstractClientPlayerMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Override
    public Text getDisplayName() {
        // Overridden for linked players.
        if (playerListEntry instanceof NguhcraftClientPlayerListEntry PLE) {
            Text name = PLE.getNameAboveHead();
            if (name != null) return name;
        }

        // Default behaviour.
        return super.getDisplayName();
    }
}
