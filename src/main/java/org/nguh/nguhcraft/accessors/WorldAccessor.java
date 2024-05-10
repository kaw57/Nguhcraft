package org.nguh.nguhcraft.accessors;

import org.nguh.nguhcraft.protect.Region;

import java.util.List;

public interface WorldAccessor {
    void AddRegion(Region R);
    List<Region> getRegions();
}
