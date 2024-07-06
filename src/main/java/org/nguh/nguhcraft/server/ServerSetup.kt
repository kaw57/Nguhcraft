package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.server.MinecraftServer
import net.minecraft.util.WorldSavePath
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.protect.ProtectionManager
import java.nio.file.Path
import kotlin.io.path.inputStream

/**
* Handle server setup and shutdown.
*
* Note that functions in here MUST NOT use the `Server()` function
* to retrieve the current server instance as the shutdown code in
* particular runs in a context where the integrated server is no
* longer accessible through the client.
*/
object ServerSetup {
    private val LOGGER = LogUtils.getLogger()

    @JvmStatic
    fun ActOnStart(S: MinecraftServer) {
        LOGGER.info("Setting up server state")
        LoadPersistentState(S)
    }

     @JvmStatic
     fun ActOnShutdown(S: MinecraftServer) {
         LOGGER.info("Shutting down server")
         SavePersistentState(S)
     }

    private fun LoadPersistentState(S: MinecraftServer) {
        // Reset defaults.
        SyncedGameRule.Reset()
        WarpManager.Reset()

        // Load saved state.
        try {
            // Read from disk.
            val Tag = NbtIo.readCompressed(
                SavePath(S).inputStream(),
                NbtSizeTracker.ofUnlimitedBytes()
            )

            // Load data.
            SyncedGameRule.Load(Tag)
            WarpManager.Load(Tag)
        } catch (E: Exception) {
            LOGGER.warn("Nguhcraft: Failed to load persistent state; using defaults: ${E.message}")
        }
    }

    private fun SavePath(S: MinecraftServer): Path {
        return S.getSavePath(WorldSavePath.ROOT).resolve("nguhcraft.dat")
    }

    private fun SavePersistentState(S: MinecraftServer) {
        val Tag = NbtCompound()

        // Save data.
        SyncedGameRule.Save(Tag)
        WarpManager.Save(Tag)

        // And write to disk.
        try {
            NbtIo.writeCompressed(Tag, SavePath(S))
        } catch (E: Exception) {
            LOGGER.error("Nguhcraft: Failed to save persistent state")
            E.printStackTrace()
        }
    }
}