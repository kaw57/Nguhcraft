package org.nguh.nguhcraft.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nguh.nguhcraft.server.NguhcraftServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public abstract class NguhcraftServerPlayerMixin implements NguhcraftServerPlayer {
    private long DiscordId = 0;
    private int DiscordColour = 0;
    private String DiscordName = "";
    private Text DiscordDisplayName = null;

    @Override
    public long getDiscordId() { return DiscordId; }

    @Override
    public boolean isLinked() { return DiscordId != 0; }

    @Override
    public String getDiscordName() { return DiscordName; }

    @Override
    public Text getDiscordDisplayName() { return DiscordDisplayName; }

    @Override
    public int getDiscordColour() { return DiscordColour; }
}
