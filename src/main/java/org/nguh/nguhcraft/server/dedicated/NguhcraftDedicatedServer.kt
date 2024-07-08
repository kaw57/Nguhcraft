package org.nguh.nguhcraft.server.dedicated

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(EnvType.SERVER)
class NguhcraftDedicatedServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        // Nop.
    }
}
