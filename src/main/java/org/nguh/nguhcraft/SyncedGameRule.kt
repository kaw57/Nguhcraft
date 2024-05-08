package org.nguh.nguhcraft

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.GameRules
import org.nguh.nguhcraft.packets.ClientboundSyncGameRulesPacket
import org.nguh.nguhcraft.server.ServerUtils

/**
* Global setting that is synced to the client.
* <p>
* This is like a game rule, except that 1. it is global and not
* per-world, and 2. any change to one of these is synced to the
* client.
*/
enum class SyncedGameRule(
    /** Flag for this rule. */
    private val Flag: Long,

    /** Name of this rule. */
    val Name: String,

    /** Default value for this rule. */
    default: Boolean
) {
    END_ENABLED(1, "EndEnabled", false);

    /** Current value. */
    @Volatile
    private var Value: Boolean = default

    /** Set the value. */
    @Environment(EnvType.SERVER)
    fun Set(NewValue: Boolean = true) {
        if (Value == NewValue) return
        Value = NewValue
        Sync()
    }

    /** Check if this rule is set. */
    fun IsSet() = Value

    companion object {
        private const val TAG_NAME = "SyncedGameRules"

        /** Encode the game rules into a packet. */
        @Environment(EnvType.SERVER)
        private fun Encode() = ClientboundSyncGameRulesPacket(entries.fold(0L) { acc, rule ->
            if (rule.Value) acc or rule.Flag else acc
        })

        /** Load the rules from disk. */
        @Environment(EnvType.SERVER)
        fun Load(LoadData: NbtCompound) {
            val Tag = LoadData.getCompound(TAG_NAME)
            for (R in entries)
                if (Tag.contains(R.Name))
                    R.Value = Tag.getBoolean(R.Name)
        }

        /** Save the rules to disk. */
        @Environment(EnvType.SERVER)
        fun Save(SaveData: NbtCompound) {
            val Tag = NbtCompound()
            for (R in entries) Tag.putBoolean(R.Name, R.Value)
            SaveData.put(TAG_NAME, Tag)
        }

        /** Send the game rules to a player. */
        @JvmStatic
        @Environment(EnvType.SERVER)
        fun Send(SP: ServerPlayerEntity) = ServerPlayNetworking.send(SP, Encode())

        /** Sync the game rules. */
        @Environment(EnvType.SERVER)
        fun Sync() = ServerUtils.Broadcast(Encode())

        /** Update the game rules. */
        @JvmStatic
        @Environment(EnvType.CLIENT)
        fun Update(Packet: ClientboundSyncGameRulesPacket) = entries.forEach {
            it.Value = Packet.Flags and it.Flag != 0L
        }
    }
}