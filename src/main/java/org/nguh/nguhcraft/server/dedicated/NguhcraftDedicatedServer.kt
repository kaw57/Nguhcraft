package org.nguh.nguhcraft.server.dedicated

import com.mojang.logging.LogUtils
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.dedicated.MinecraftDedicatedServer
import kotlin.system.exitProcess

@Environment(EnvType.SERVER)
class NguhcraftDedicatedServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        try {
            LOGGER.info("Initialising server")
            Discord.Start(FabricLoader.getInstance() as MinecraftDedicatedServer)
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }
}
