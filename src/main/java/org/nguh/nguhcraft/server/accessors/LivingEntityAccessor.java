package org.nguh.nguhcraft.server.accessors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.server.HypershotContext;

public interface LivingEntityAccessor {
    void setHypershotContext(HypershotContext context);
    @Nullable HypershotContext getHypershotContext();
}
