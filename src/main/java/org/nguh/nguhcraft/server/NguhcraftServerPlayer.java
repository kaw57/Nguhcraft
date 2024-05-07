package org.nguh.nguhcraft.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.SERVER)
public interface NguhcraftServerPlayer {
    boolean getVanished();
    void setVanished(boolean vanished);

    long getDiscordId();
    void setDiscordId(long id);

    int getDiscordColour();
    void setDiscordColour(int colour);

    String getDiscordName();
    void setDiscordName(String name);

    String getDiscordAvatarURL();
    void setDiscordAvatarURL(String url);

    Text getNguhcraftDisplayName();
    void setNguhcraftDisplayName(Text name);

    void LoadNguhcraftNbt(@NotNull NbtCompound nbt);

    boolean isLinked();
}
