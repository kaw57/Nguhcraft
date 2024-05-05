package org.nguh.nguhcraft.server

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import org.nguh.nguhcraft.server.Discord.Companion.Start
import kotlin.system.exitProcess

@Environment(EnvType.SERVER)
class NguhcraftServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        try {
            Start()
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }
}
