package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.World;
import org.nguh.nguhcraft.accessors.WorldAccessor;
import org.nguh.nguhcraft.protect.Region;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(World.class)
public abstract class WorldMixin implements WorldAccessor {
    /** Regions in this world. */
    @Unique private final List<Region> Regions = new ArrayList<>();

    /** Add a region. */
    @Override public void AddRegion(Region R) {
        for (var Region : Regions)
            if (Region.getName().equalsIgnoreCase(R.getName()))
                throw new IllegalArgumentException("Region with name " + R.getName() + " already exists in this world.");
        Regions.add(R);
    }

    /** Get the regions. */
    @Override public List<Region> getRegions() { return Regions; }
}
