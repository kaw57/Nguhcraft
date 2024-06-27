package org.nguh.nguhcraft.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class NguhcraftDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator FDG) {
        NguhcraftDataGeneratorImpl.Run(FDG);
    }
}
