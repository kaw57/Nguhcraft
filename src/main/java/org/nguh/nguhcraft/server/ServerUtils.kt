package org.nguh.nguhcraft.server

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*

@Environment(EnvType.SERVER)
object ServerUtils {
    @JvmStatic
    fun Broadcast(Except: ServerPlayerEntity, P: CustomPayload) {
        for (Player in Server().playerManager.playerList)
            if (Player != Except)
                ServerPlayNetworking.send(Player, P)
    }

    @JvmStatic
    fun Broadcast(P: CustomPayload) {
        for (Player in Server().playerManager.playerList)
            ServerPlayNetworking.send(Player, P)
    }

    fun PlayerByUUID(ID: String?): ServerPlayerEntity? {
        return try { Server().playerManager.getPlayer(UUID.fromString(ID)) }
        catch (E: RuntimeException) { null }
    }

    fun Server() = FabricLoader.getInstance().gameInstance as MinecraftServer
}
