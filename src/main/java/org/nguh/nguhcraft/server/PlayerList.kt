package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.WorldSavePath
import org.nguh.nguhcraft.Colours
import org.nguh.nguhcraft.Utils.Normalised
import org.nguh.nguhcraft.mixin.MinecraftServerAccessor
import org.nguh.nguhcraft.server.ServerUtils.Server
import java.io.File
import java.util.*

/**
* Server player list that can also handle offline players.
*
* Prefer to use the normal player list unless you absolutely
* need access to offline player data.
*/
@Environment(EnvType.SERVER)
class PlayerList private constructor(private val ByID: HashMap<UUID, Entry>) : Iterable<PlayerList.Entry> {
    class Entry(
        val ID: UUID,
        val DiscordID: Long,
        val DiscordColour: Int,
        val MinecraftName: String,
        val DiscordName: String
    ) {
        val NormalisedDiscordName: String = Normalised(DiscordName)
        val isLinked: Boolean get() = DiscordID != Discord.INVALID_ID
        override fun toString() = MinecraftName.ifEmpty { ID.toString() }
    }

    private val Data = ByID.values.toTypedArray()
    init { Data.sortBy { it.toString().lowercase() } }

    /** Find a player by a condition.  */
    fun find(Pred: (Entry) -> Boolean) = Data.find(Pred)

    /** Find a player by UUID.  */
    fun find(ID: UUID) = ByID.getOrDefault(ID, null)

    override fun iterator() = Data.iterator()
    companion object {
        private val LOGGER = LogUtils.getLogger()

        /**
         * Cache for offline player data; this is not used if the player is online.
         *
         * For a UUID:
         *
         *   - If get() returns null, the player has not been cached yet.
         *   - If get() returns NULL_ENTRY, the player has been cached as not found.
         *   - If get() returns a valid entry, the player has been cached as found.
         */
        private val CACHE = HashMap<UUID, Entry?>()

        /** Null entry. */
        private val NULL_ENTRY = Entry(UUID(0, 0), Discord.INVALID_ID, 0, "", "")

        /** Player data directory */
        private val PlayerDataDir get() = (Server() as MinecraftServerAccessor)
            .session.getDirectory(WorldSavePath.PLAYERDATA).toFile()

        /** Retrieve Nguhcraft-specific data for all players, even if they’re offline.  */
        fun AllPlayers(): PlayerList {
            val S = Server()
            val DatFiles = PlayerDataDir.list { _, name -> name.endsWith(".dat") }
            val PlayerData = HashMap<UUID, Entry>()

            // Enumerate all players for which we have an entry, adding them to the list.
            assert(S.isOnThread) { "Must run on the server thread" }
            for (F in DatFiles!!) {
                try {
                    val ID = UUID.fromString(F.substring(0, F.length - 4))
                    AddPlayerData(PlayerData, ID)
                }

                // Filename parsing failed. Ignore random garbage in this directory.
                catch (ignored: IllegalArgumentException) { }
                catch (ignored: IndexOutOfBoundsException) { }
            }

            // Also add online players that haven’t been saved yet.
            for (P in S.playerManager.playerList) AddPlayerData(PlayerData, P.uuid)
            return PlayerList(PlayerData)
        }

        /** Retrieve Nguhcraft-specific data for a player that is online.  */
        fun Player(SP: ServerPlayerEntity): Entry {
            // Should always be a cache hit if they’re online.
            val Data = CACHE[SP.uuid]
            if (Data != null && Data != NULL_ENTRY) return Data

            // If not, add them to the cache and return the data.
            return UpdateCacheEntry(SP)
        }

        /** Override the cache entry for a player that is online.  */
        @JvmStatic
        fun UpdateCacheEntry(SP: ServerPlayerEntity): Entry {
            val NewData = Entry(
                SP.uuid,
                SP.discordId,
                SP.discordColour,
                SP.nameForScoreboard,
                SP.discordName ?: ""
            )

            CACHE[SP.uuid] = NewData
            return NewData
        }

        /** Add a player’s data to a set, fetching it from wherever appropriate.  */
        private fun AddPlayerData(Map: HashMap<UUID, Entry>, PlayerID: UUID) {
            if (Map.containsKey(PlayerID)) return

            // If we’ve cached their data, use that.
            val Data = CACHE[PlayerID]
            if (Data != null) {
                if (Data != NULL_ENTRY) Map[PlayerID] = Data
                return
            }

            // Otherwise, load the player from disk. If this fails, there is nothing we can do.
            val Nbt = ReadPlayerData(PlayerID)
            if (Nbt == null) {
                CACHE[PlayerID] = NULL_ENTRY
                return
            }

            // There no longer is a way to get a player’s name from their UUID (thanks
            // a lot for that, Mojang), so we have to store it ourselves.
            var Name = ""
            var DiscordName = ""
            var DiscordID = Discord.INVALID_ID
            var RoleColour: Int = Colours.Grey
            if (Nbt.contains("Nguhcraft")) {
                val Nguhcraft = Nbt.getCompound("Nguhcraft")
                Name = Nguhcraft.getString("LastKnownMinecraftName")
                DiscordID = Nguhcraft.getLong("DiscordID")
                RoleColour = Nguhcraft.getInt("DiscordRoleColour")
                DiscordName = Nguhcraft.getString("DiscordName")
            }

            // Once upon a time, this was a paper server; remnants of that should still be
            // in the player data; don’t rely on them for the name, but use them if we don’t
            // have anything else.
            if (Name.isEmpty() && Nbt.contains("bukkit"))
                Name = Nbt.getCompound("bukkit").getString("lastKnownName")

            // Get the rest of the data from the tag and cache it.
            val NewData = Entry(
                PlayerID,
                DiscordID,
                RoleColour,
                Name,
                DiscordName
            )

            // Save the data and add it to the map.
            CACHE[PlayerID] = NewData
            Map[PlayerID] = NewData
        }

        private fun ReadPlayerData(PlayerID: UUID): NbtCompound? {
            val file = File(PlayerDataDir, "$PlayerID.dat")
            if (file.exists() && file.isFile) {
                try {
                    return NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes())
                } catch (_: Exception) {
                    LOGGER.warn("Failed to load player data for {}", PlayerID)
                }
            }
            return null
        }
    }
}
