package org.nguh.nguhcraft.server.accessors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public interface ServerPlayerAccessor {
    String TAG_ROOT = "Nguhcraft";
    String TAG_VANISHED = "Vanished";
    String TAG_BYPASSES_REGION_PROTECTION = "BypassesRegionProtection";

    boolean getVanished();
    void setVanished(boolean vanished);

    boolean getBypassesRegionProtection();
    void setBypassesRegionProtection(boolean bypassesProtection);

    void LoadGeneralNguhcraftNbt(@NotNull NbtCompound nbt);
}
