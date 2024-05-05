package org.nguh.nguhcraft;

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.text.Text;

public class Commands {
    public static SimpleCommandExceptionType Exn(String message) {
        return new SimpleCommandExceptionType(Text.literal(message));
    }
}
