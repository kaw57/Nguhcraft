package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    /**
    * Disable profile keys.
    *
    * @author Sirraide
    * @reason We don’t do any signing, so this is useless.
    */
    @Overwrite
    public ProfileKeys getProfileKeys() { return ProfileKeys.MISSING; }

    /**
    * Also disable telemetry because why not.
    *
    * @author Sirraide
    * @reason No reason to keep this enabled.
    * */
    @Overwrite
    public boolean isTelemetryEnabledByApi() { return false; }
}
