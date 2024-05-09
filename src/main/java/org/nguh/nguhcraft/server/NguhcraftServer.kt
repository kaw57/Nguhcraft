package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.util.WorldSavePath
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.packets.ServerboundChatPacket
import org.nguh.nguhcraft.server.ServerUtils.Server
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.system.exitProcess


@Environment(EnvType.SERVER)
class NguhcraftServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        try {
            LOGGER.info("Initialising server")
            Discord.Start()
            Commands.Register()
            RegisterPacketHandlers()
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()

        private fun RegisterPacketHandlers() {
            ServerPlayNetworking.registerGlobalReceiver(ServerboundChatPacket.ID) { Packet, Context ->
                NetworkHandler.HandleChatMessage(Packet.Message, Context)
            }
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

        @JvmStatic
        fun Shutdown() {
            LOGGER.info("Shutting down server")
            Discord.Stop()
            SavePersistentState()
        }

        /** Called once the server instance has been created. */
        @JvmStatic
        fun Setup() {
            LOGGER.info("Setting up server state")
            LoadPersistentState()
        }
    }
}
