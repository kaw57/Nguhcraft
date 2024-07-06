package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.util.WorldSavePath
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.server.ServerUtils.Server
import java.nio.file.Path
import kotlin.io.path.inputStream

object ServerSetup {
    private val LOGGER = LogUtils.getLogger()

    @JvmStatic
    fun ActOnStart() {
        LOGGER.info("Setting up server state")
        LoadPersistentState()
    }

     @JvmStatic
     fun ActOnShutdown() {
         LOGGER.info("Shutting down server")
         SavePersistentState()
     }

    private fun LoadPersistentState() {
        try {
            // Read from disk.
            val Tag = NbtIo.readCompressed(SavePath().inputStream(), NbtSizeTracker.ofUnlimitedBytes())

            // Load data.
            SyncedGameRule.Load(Tag)
        } catch (E: Exception) {
            LOGGER.warn("Nguhcraft: Failed to load persistent state; using defaults: ${E.message}")
        }
    }

    private fun SavePath(): Path {
        return Server().getSavePath(WorldSavePath.ROOT).resolve("nguhcraft.dat")
    }

    private fun SavePersistentState() {
        val Tag = NbtCompound()

        // Save data.
        SyncedGameRule.Save(Tag)

        // And write to disk.
        try {
            NbtIo.writeCompressed(Tag, SavePath())
        } catch (E: Exception) {
            LOGGER.error("Nguhcraft: Failed to save persistent state")
            E.printStackTrace()
        }
    }
}