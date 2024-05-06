package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient

@Environment(EnvType.CLIENT)
object ClientUtils {
    /** 2000 is reasonable, and we can still send it to Discord this way. */
    const val MAX_CHAT_LENGTH = 2000

    /** Get the client instance. */
    fun Client(): MinecraftClient = MinecraftClient.getInstance()
}