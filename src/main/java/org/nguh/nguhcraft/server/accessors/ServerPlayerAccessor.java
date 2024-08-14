package org.nguh.nguhcraft.server.accessors;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.Home;

import java.util.List;

public interface ServerPlayerAccessor {
    String TAG_ROOT = "Nguhcraft";
    String TAG_HOMES = "Homes";
    String TAG_VANISHED = "Vanished";
    String TAG_IS_MODERATOR = "IsModerator";
    String TAG_BYPASSES_REGION_PROTECTION = "BypassesRegionProtection";

    boolean getVanished();
    void setVanished(boolean vanished);

    boolean isModerator();
    void setIsModerator(boolean IsModerator);

    boolean getBypassesRegionProtection();
    void setBypassesRegionProtection(boolean bypassesProtection);

    List<Home> Homes();

    void LoadGeneralNguhcraftNbt(@NotNull NbtCompound nbt);
}
