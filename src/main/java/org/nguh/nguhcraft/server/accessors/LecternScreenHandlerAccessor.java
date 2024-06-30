package org.nguh.nguhcraft.server.accessors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.SERVER)
public interface LecternScreenHandlerAccessor {
    void Nguhcraft$SetLecternPos(BlockPos Pos);
}
