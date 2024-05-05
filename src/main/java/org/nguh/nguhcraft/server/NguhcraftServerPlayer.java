package org.nguh.nguhcraft.server;

import net.minecraft.text.Text;

public interface NguhcraftServerPlayer {
    long getDiscordId();
    void setDiscordId(long id);

    int getDiscordColour();
    void setDiscordColour(int colour);

    String getDiscordName();
    void setDiscordName(String name);

    String getDiscordAvatarURL();
    void setDiscordAvatarURL(String url);

    Text getDiscordDisplayName();
    void setDiscordDisplayName(Text name);

    boolean isLinked();
}
