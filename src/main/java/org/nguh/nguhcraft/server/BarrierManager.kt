package org.nguh.nguhcraft.server

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.World
import org.nguh.nguhcraft.Nbt
import org.nguh.nguhcraft.NbtListOf
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.XZRect
import org.nguh.nguhcraft.network.ClientboundSyncBarriersPacket
import org.nguh.nguhcraft.server.accessors.ManagerAccessor
import org.nguh.nguhcraft.set

val MinecraftServer.BarrierManager get(): BarrierManager
    = (this as ManagerAccessor).`Nguhcraft$GetBarrierManager`()

data class Barrier(val Colour: Int, val W: RegistryKey<World>, val XZ: XZRect)

/** Manages barriers synced to the client. */
class BarrierManager(private val S: MinecraftServer) {
    private val Barriers = mutableMapOf<String, Barrier>()

    /** Load barriers from Nbt. */
    fun Load(Parent: NbtCompound) {
        if (!Parent.contains(TAG_ROOT, NbtElement.LIST_TYPE.toInt())) return
        val BarrierList = Parent.getList(TAG_ROOT, NbtElement.COMPOUND_TYPE.toInt())
        for (Tag in BarrierList) {
            val Name = (Tag as NbtCompound).getString(TAG_BARRIER_NAME)
            val XZ = XZRect.Load(Tag)
            val Colour = Tag.getInt(TAG_BARRIER_COLOUR)
            val W = Utils.DeserialiseWorld(Tag.get(TAG_BARRIER_WORLD)!!)
            Barriers[Name] = Barrier(Colour, W, XZ)
        }
    }

    /** Delete all barriers. */
    fun Reset() = Barriers.clear()

    /** Save barriers to Nbt. */
    fun Save(Parent: NbtCompound) {
        Parent.put(TAG_ROOT, NbtListOf {
            for ((K, B) in Barriers) add(Nbt {
                set(TAG_BARRIER_NAME, K)
                B.XZ.SaveXZRect(this)
                set(TAG_BARRIER_COLOUR, B.Colour)
                set(TAG_BARRIER_WORLD, Utils.SerialiseWorld(B.W))
            })
        })
    }

    /** Sync state to a client. */
    fun Send(SP: ServerPlayerEntity) {
        ServerPlayNetworking.send(SP, ClientboundSyncBarriersPacket(Barriers.values.toList()))
    }

    /** Set a barrier. */
    fun Set(Name: String, XZ: XZRect, Colour: Int, W: RegistryKey<World>) {
        Barriers[Name] = Barrier(Colour, W, XZ)
        Sync()
    }

    private fun Sync() {
        S.Broadcast(ClientboundSyncBarriersPacket(Barriers.values.toList()))
    }

    companion object {
        private const val TAG_ROOT = "Barriers"
        private const val TAG_BARRIER_NAME = "Name"
        private const val TAG_BARRIER_COLOUR = "Colour"
        private const val TAG_BARRIER_WORLD = "World"
    }
}