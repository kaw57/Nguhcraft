package org.nguh.nguhcraft.server

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.entity.Entity
import net.minecraft.inventory.ContainerLock
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.TeleportTarget
import org.nguh.nguhcraft.mixin.protect.LockableContainerBlockEntityAccessor
import java.util.*

fun Entity.Teleport(ToWorld: ServerWorld, OnTopOf: BlockPos) {
    val Vec = Vec3d(OnTopOf.x.toDouble(), OnTopOf.y.toDouble() + 1, OnTopOf.z.toDouble())
    teleportTo(TeleportTarget(ToWorld, Vec, Vec3d.ZERO, 0F, 0F, TeleportTarget.NO_OP))
}

fun Entity.Teleport(ToWorld: ServerWorld, To: Vec3d, Yaw: Float, Pitch: Float) {
    teleportTo(TeleportTarget(ToWorld, To, Vec3d.ZERO, Yaw, Pitch, TeleportTarget.NO_OP))
}

fun LockableContainerBlockEntity.UpdateLock(NewLock: ContainerLock) {
    (this as LockableContainerBlockEntityAccessor).lock = NewLock
    (world as ServerWorld).chunkManager.markForUpdate(pos)
    markDirty()
}

/** Send a packet to every client except one. */
fun MinecraftServer.Broadcast(Except: ServerPlayerEntity, P: CustomPayload) {
    for (Player in playerManager.playerList)
        if (Player != Except)
            ServerPlayNetworking.send(Player, P)
}

/** Send a packet to every client in a world. */
fun MinecraftServer.Broadcast(W: ServerWorld, P: CustomPayload) {
    for (Player in W.players)
        ServerPlayNetworking.send(Player, P)
}

/** Send a packet to every client. */
fun MinecraftServer.Broadcast(P: CustomPayload) {
    for (Player in playerManager.playerList)
        ServerPlayNetworking.send(Player, P)
}

/** Get a player by Name. */
fun MinecraftServer.PlayerByName(Name: String): ServerPlayerEntity? {
    return playerManager.getPlayer(Name)
}

/** Get a player by UUID. */
fun MinecraftServer.PlayerByUUID(ID: String?): ServerPlayerEntity? {
    return try { playerManager.getPlayer(UUID.fromString(ID)) }
    catch (E: RuntimeException) { null }
}