package org.nguh.nguhcraft.client.accessors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public interface ClientPlayerListEntryAccessor {
    void setNameAboveHead(Text name);
    Text getNameAboveHead();
}
