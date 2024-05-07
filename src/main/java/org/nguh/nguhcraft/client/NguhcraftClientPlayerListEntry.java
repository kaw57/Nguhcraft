package org.nguh.nguhcraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public interface NguhcraftClientPlayerListEntry {
    void setNameAboveHead(Text name);
    Text getNameAboveHead();
}
