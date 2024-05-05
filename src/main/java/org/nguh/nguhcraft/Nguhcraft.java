package org.nguh.nguhcraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.nguh.nguhcraft.packets.ClientboundLinkUpdatePacket;

public class Nguhcraft implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(ClientboundLinkUpdatePacket.ID, ClientboundLinkUpdatePacket.CODEC);
    }
}
