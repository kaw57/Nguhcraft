package org.nguh.nguhcraft.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

@Environment(EnvType.SERVER)
public interface NguhcraftServerPlayer {
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

    boolean isLinked();
}
