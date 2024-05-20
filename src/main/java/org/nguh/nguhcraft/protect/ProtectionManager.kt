package org.nguh.nguhcraft.protect

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.VehicleEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.network.RegistryByteBuf
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.client.accessors.AbstractClientPlayerEntityAccessor
import org.nguh.nguhcraft.packets.ClientboundSyncProtectionMgrPacket
import org.nguh.nguhcraft.server.ServerUtils
import org.nguh.nguhcraft.server.isLinked

object ProtectionManager {
    private val TAG_REGIONS = "Regions"

    /** Current manager state. */
    @Volatile private var S = State()

    /**
     * This function is the intended way to add a region to a world.
     *
     * This can throw just to ensure we never add a region with the same name twice
     * because that would be a pretty serious error.
     *
     * @throws IllegalArgumentException If the region name is already taken.
     * @returns Whether the region was successfully added. This can fail
     * if the region name is already taken.
     */
    @Throws(IllegalArgumentException::class)
    fun AddRegionToWorld(W: World, R: Region) : Boolean {
        val Regions = RegionList(W)
        if (Regions.any { it.Name == R.Name }) throw IllegalArgumentException("Region name already taken!")
        Regions.add(R)
        if (!W.isClient) Sync()
        return true
    }

    /**
    * Check if a player is allowed to break, start breaking, or place a
    * block at this block position.
    */
    @JvmStatic
    fun AllowBlockModify(PE: PlayerEntity, W: World, Pos: BlockPos) : Boolean {
        // Player is operator. Always allow.
        if (PE.hasPermissionLevel(4)) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Block is within the bounds of a protected region. Deny.
        if (IsProtectedBlock(W, Pos)) return false

        // Otherwise, allow.
        return true
    }

    /** Check if a player is allowed to interact (= right-click) with a block. */
    @JvmStatic
    fun AllowBlockInteract(PE: PlayerEntity, W: World, Pos: BlockPos) : Boolean {
        // Player is operator. Always allow.
        if (PE.hasPermissionLevel(4)) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Block is within the bounds of a protected region. Deny.
        if (IsProtectedBlock(W, Pos)) return false

        // Otherwise, allow.
        return true
    }

    /** Check if a player is allowed to interact (= right-click) with an entity. */
    @JvmStatic
    fun AllowEntityInteract(PE: PlayerEntity, E: Entity) : Boolean {
        // Player is operator. Always allow.
        if (PE.hasPermissionLevel(4)) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Entity is a vehicle; check region flags.
        if (E is VehicleEntity) {
            val R = FindRegionContainingBlock(E.world, E.blockPos) ?: return true
            return R.AllowsVehicleUse()
        }

        // Check regular region flags.
        val R = FindRegionContainingBlock(E.world, E.blockPos) ?: return true
        return R.AllowsEntityInteraction()
    }

    /**
     * This function is the intended way to delete a region from a world.
     *
     * @returns Whether a region was successfully deleted. This can fail
     * if the region does not exist.
     */
    fun DeleteRegionFromWorld(W: World, Name: String) : Boolean {
        val Regions = RegionList(W)
        val Index = Regions.indexOfFirst { it.Name == Name }
        if (Index == -1) return false
        Regions.removeAt(Index)
        if (!W.isClient) Sync()
        return true
    }

    /** Get the regions for a world. */
    fun GetRegions(W: World): List<Region> = RegionList(W)

    /** Check if a player is linked. */
    fun IsLinked(PE: PlayerEntity) = when (PE) {
        is ServerPlayerEntity -> PE.isLinked
        is ClientPlayerEntity -> (PE as AbstractClientPlayerEntityAccessor).isLinked
        else -> false
    }

    /** Check if a block is within a protected region. */
    @JvmStatic
    fun IsProtectedBlock(W: World, Pos: BlockPos): Boolean {
        val R = FindRegionContainingBlock(W, Pos) ?: return false
        return !R.AllowsBlockModification()
    }

    /** Check if this entity is protected from attacks by a player. */
    @JvmStatic
    fun IsProtectedEntity(AttackingPlayer: PlayerEntity, AttackedEntity: Entity): Boolean {
        // Player is operator. Always allow.
        if (AttackingPlayer.hasPermissionLevel(4)) return false

        // Player is not linked. Always deny.
        if (!IsLinked(AttackingPlayer)) return true

        // Entity is a player. Check PvP flag.
        if (AttackedEntity is PlayerEntity) {
            val Regions = RegionList(AttackedEntity.world)
            val R = Regions.find { it.Contains(AttackedEntity.blockPos) } ?: return false
            return !R.AllowsPvP()
        }

        // Entity is a mob. Check friendly attack flag.
        if (AttackedEntity !is Monster) {
            val Regions = RegionList(AttackedEntity.world)
            val R = Regions.find { it.Contains(AttackedEntity.blockPos) } ?: return false
            return !R.AllowsAttackingFriendlyEntities()
        }

        // Otherwise, allow.
        return false
    }

    /**
    * Load regions from a tag.
    *
    * The existing list of regions is cleared.
    */
    fun LoadRegions(W: World, Tag: NbtCompound) {
        val RegionsTag = Tag.getList(TAG_REGIONS, NbtElement.COMPOUND_TYPE.toInt())
        val Regions = RegionList(W)
        RegionsTag.forEach { Regions.add(Region(it as NbtCompound)) }
    }

    /** Find the region that contains a block. */
    private fun FindRegionContainingBlock(W: World, Pos: BlockPos) = RegionList(W).find { it.Contains(Pos) }

    /** Get the regions for a world. */
    private fun RegionList(W: World): MutableList<Region> {
        if (W.registryKey == World.OVERWORLD) return S.OverworldRegions
        if (W.registryKey == World.NETHER) return S.NetherRegions
        if (W.registryKey == World.END) return S.EndRegions
        throw IllegalArgumentException("Unknown world type!")
    }

    /** Save regions to a tag. */
    fun SaveRegions(W: World, Tag: NbtCompound) {
        val RegionsTag = Tag.getList(TAG_REGIONS, NbtElement.COMPOUND_TYPE.toInt())
        RegionList(W).forEach { RegionsTag.add(it.Save()) }
        Tag.put(TAG_REGIONS, RegionsTag)
    }

    /** Send data to the client. */
    @JvmStatic
    fun Send(SP: ServerPlayerEntity) {
        ServerPlayNetworking.send(SP, ClientboundSyncProtectionMgrPacket(S))
    }

    /** Sync regions to the clients. */
    @Environment(EnvType.SERVER)
    fun Sync() {
        ServerUtils.Broadcast(ClientboundSyncProtectionMgrPacket(S))
    }

    /** Overwrite the region list of a world. */
    @Environment(EnvType.CLIENT)
    fun UpdateState(Packet: ClientboundSyncProtectionMgrPacket) {
        S = Packet.Data
    }

    /** Internal manager state. */
    class State() {
        /** Regions that are currently in each dimension. */
        var OverworldRegions: MutableList<Region> = mutableListOf()
        var NetherRegions: MutableList<Region> = mutableListOf()
        var EndRegions: MutableList<Region> = mutableListOf()

        /** Deserialise the state from a packet. */
        constructor(B: RegistryByteBuf): this() {
            OverworldRegions = ReadRegionList(B)
            NetherRegions = ReadRegionList(B)
            EndRegions = ReadRegionList(B)
        }

        /** Dump a string representation of the state. */
        override fun toString(): String {
            var S = "ProtectionManager.State {\n"
            for (R in OverworldRegions) S += "  Overworld: $R\n"
            for (R in NetherRegions) S += "  Nether: $R\n"
            for (R in EndRegions) S += "  End: $R\n"
            S += "}"
            return S
        }

        /** Read a list of regions from a packet. */
        private fun ReadRegionList(B: RegistryByteBuf): MutableList<Region> {
            val Count = B.readInt()
            val List = mutableListOf<Region>()
            for (I in 0 until Count) List.add(Region(B))
            return List
        }

        /** Serialise the state to a packet. */
        fun Write(B: RegistryByteBuf) {
            WriteRegionList(B, OverworldRegions)
            WriteRegionList(B, NetherRegions)
            WriteRegionList(B, EndRegions)
        }

        /** Write a list of regions to a packet. */
        private fun WriteRegionList(B: RegistryByteBuf, List: List<Region>) {
            B.writeInt(List.size)
            List.forEach { it.Write(B) }
        }
    }
}
