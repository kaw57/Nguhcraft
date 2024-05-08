package org.nguh.nguhcraft.server.accessors;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.SERVER)
public interface ServerPlayerAccessor {
    String TAG_ROOT = "Nguhcraft";
    String TAG_LAST_KNOWN_NAME = "LastKnownMinecraftName";
    String TAG_VANISHED = "Vanished";
    String TAG_DISCORD_ID = "DiscordID";
    String TAG_DISCORD_COLOUR = "DiscordRoleColour";
    String TAG_DISCORD_NAME = "DiscordName";
    String TAG_DISCORD_AVATAR = "DiscordAvatar";

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
    boolean isLinkedOrOperator();
}
