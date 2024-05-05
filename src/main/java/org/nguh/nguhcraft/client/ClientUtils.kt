package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient

@Environment(EnvType.CLIENT)
object ClientUtils {
    fun Client() = MinecraftClient.getInstance()
}