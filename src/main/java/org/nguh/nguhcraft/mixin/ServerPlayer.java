package org.nguh.nguhcraft.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.server.NguhcraftServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayer implements NguhcraftServerPlayer {
    @Unique private long DiscordId = 0;
    @Unique private int DiscordColour = 0;
    @Unique private String DiscordName = "";
    @Unique private Text DiscordDisplayName = null;

    @Override public long getDiscordId() { return DiscordId; }
    @Override public void setDiscordId(long id) { DiscordId = id; }

    @Override public String getDiscordName() { return DiscordName; }
    @Override public void setDiscordName(String name) { DiscordName = name; }

    @Override public int getDiscordColour() { return DiscordColour; }
    @Override public void setDiscordColour(int colour) { DiscordColour = colour; }

    @Override public Text getDiscordDisplayName() { return DiscordDisplayName; }
    @Override public void setDiscordDisplayName(Text name) { DiscordDisplayName = name; }

    @Override public boolean isLinked() { return DiscordId != 0; }
}
