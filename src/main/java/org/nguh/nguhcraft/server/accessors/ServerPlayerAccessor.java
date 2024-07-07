package org.nguh.nguhcraft.server.accessors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.Home;

import java.util.List;

public interface ServerPlayerAccessor {
    String TAG_ROOT = "Nguhcraft";
    String TAG_HOMES = "Homes";
    String TAG_VANISHED = "Vanished";
    String TAG_BYPASSES_REGION_PROTECTION = "BypassesRegionProtection";

    boolean getVanished();
    void setVanished(boolean vanished);

    boolean getBypassesRegionProtection();
    void setBypassesRegionProtection(boolean bypassesProtection);

    List<Home> Homes();

    void LoadGeneralNguhcraftNbt(@NotNull NbtCompound nbt);
}
