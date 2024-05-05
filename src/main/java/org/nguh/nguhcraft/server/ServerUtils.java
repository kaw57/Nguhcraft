package org.nguh.nguhcraft.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

@Environment(EnvType.SERVER)
public class ServerUtils {
    @Nullable
    public static ServerPlayerEntity PlayerByUUID(String ID) {
        try {
            return Server().getPlayerManager().getPlayer(UUID.fromString(ID));
        } catch (RuntimeException E) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    static MinecraftServer Server() {
        return (MinecraftServer) FabricLoader.getInstance().getGameInstance();
    }

    public static void UpdatePlayerName(ServerPlayerEntity SP) {
/*        var S = MinecraftServer.getServer();
        var SB = S.getScoreboard();
        var UUID = SP.getStringUUID();
        if (SP.isLinked()) {
            assert SP.discord_display_name != null;
            SP.display_name_component = Component.literal(SP.discord_display_name)
                    .withColor(SP.discord_role_colour);

            var Suffix = Component.literal("]");
            var Prefix = Component.empty()
                    .append(SP.vanished ? Detail.VANISHED_COMPONENT : CommonComponents.EMPTY)
                    .append(SP.display_name_component)
                    .append(" [");

            // This is complete and utter jank, but it’s the only way to change
            // a player’s name tag in Minecraft: get the default scoreboard and
            // create a team for this player; set the prefix to 'CustomName [',
            // and the suffix to ']'.
            var Team = SB.getPlayerTeam(UUID);
            if (Team == null) Team = SB.addPlayerTeam(UUID);
            Team.setPlayerPrefix(Prefix);
            Team.setPlayerSuffix(Suffix);
            SB.addPlayerToTeam(SP.getScoreboardName(), Team);

            // Also set the player list name and the custom name.
            SP.setCustomNameVisible(true);
            SP.setCustomName(SP.display_name_component);
            SP.listName = Prefix.copy().append(SP.getName()).append("]");

            // Chat messages use *yet another* cached name, so update that as well.
            SP.adventure$displayName = net.kyori.adventure.text.Component.text(SP.discord_display_name)
                    .color(net.kyori.adventure.text.format.TextColor.color(SP.discord_role_colour));
        } else {
            SP.display_name_component = SP.getName();
            SP.setCustomName(SP.getName());
            SP.setCustomNameVisible(false);
            SP.listName = SP.getName();
            SP.adventure$displayName = net.kyori.adventure.text.Component.text(SP.getScoreboardName());
            var Team = SB.getPlayerTeam(UUID);
            if (Team != null) SB.removePlayerTeam(Team);
        }

        // Cache the name in case we need it later and/or the player goes offline.
        NguhPlayerList.UpdateCacheEntry(SP);

        // Tell the other players that this player’s name has changed.
        for (ServerPlayer P : S.getPlayerList().players) {
            P.connection.send(new ClientboundPlayerInfoUpdatePacket(
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                    SP
            ));
        }*/
    }
}
