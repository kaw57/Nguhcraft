package org.nguh.nguhcraft.client.accessors

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.text.Text

@Environment(EnvType.CLIENT)
class ClientDisplayData(
    var Lines: List<Text> = listOf()
)

@Environment(EnvType.CLIENT)
interface ClientDisplayDataAccessor {
    fun `Nguhcraft$GetDisplayData`(): ClientDisplayData
}