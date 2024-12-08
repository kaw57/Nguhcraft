package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.WorldSavePath
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.protect.ProtectionManager
import java.nio.file.Path
import kotlin.io.path.inputStream

/** Handle server setup and shutdown. */
object SessionSetup {
    private val LOGGER = LogUtils.getLogger()

    @JvmStatic
    fun ActOnStart(S: MinecraftServer) {
        LoadServerState(S)
    }

     @JvmStatic
     fun ActOnShutdown(S: MinecraftServer) {
         SaveServerState(S)
     }

    @JvmStatic
    fun LoadExtraWorldData(SW: ServerWorld) {
        LOGGER.info("Loading nguhcraft world data for {}", SW.registryKey.value)
        try {
            val Path = NguhWorldSaveFile(SW)
            val Nguhcraft = NguhWorldSaveDir(SW)
            val Tag = NbtIo.readCompressed(Path, NbtSizeTracker.ofUnlimitedBytes())

            // Load.
            ProtectionManager.LoadRegions(SW, Nguhcraft, Tag)
        } catch (E: Exception) {
            LOGGER.error("Nguhcraft: Failed to load extra world data: ${E.message}")
        }
    }

    private fun LoadServerState(S: MinecraftServer) {
        LOGGER.info("Setting up server state")

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

    private fun NguhWorldSaveFile(SW: ServerWorld) = SW.server.getSavePath(WorldSavePath.ROOT).resolve(
        "nguhcraft.extraworlddata.${SW.registryKey.value.path}.dat"
    )

    private fun NguhWorldSaveDir(SW: ServerWorld) = SW.server.getSavePath(WorldSavePath.ROOT)
        .resolve("nguhcraft")
        .resolve(SW.registryKey.value.path)

    @JvmStatic
    fun SaveExtraWorldData(SW: ServerWorld) {
        try {
            val Tag = NbtCompound()
            val Path = NguhWorldSaveFile(SW)
            val Nguhcraft = NguhWorldSaveDir(SW)

            // Save.
            ProtectionManager.SaveRegions(SW, Nguhcraft, Tag)

            // Write to disk.
            NbtIo.writeCompressed(Tag, Path)
        } catch (E: Exception) {
            LOGGER.error("Nguhcraft: Failed to save extra world data: ${E.message}")
        }
    }

    private fun SavePath(S: MinecraftServer): Path {
        return S.getSavePath(WorldSavePath.ROOT).resolve("nguhcraft.dat")
    }

    private fun SaveServerState(S: MinecraftServer) {
        LOGGER.info("Shutting down server")
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