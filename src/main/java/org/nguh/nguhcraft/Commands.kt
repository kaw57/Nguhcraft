package org.nguh.nguhcraft

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.text.Text

object Commands {
    fun Exn(message: String): SimpleCommandExceptionType {
        return SimpleCommandExceptionType(Text.literal(message))
    }
}
