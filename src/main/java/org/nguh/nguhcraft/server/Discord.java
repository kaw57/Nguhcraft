package org.nguh.nguhcraft.server;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateAvatarEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.Colours;
import org.nguh.nguhcraft.Utils;
import org.slf4j.Logger;
import java.util.function.Consumer;

import static org.nguh.nguhcraft.Commands.Exn;
import static org.nguh.nguhcraft.server.ServerUtils.Server;
import static org.nguh.nguhcraft.server.ServerUtils.UpdatePlayerName;

@Environment(EnvType.SERVER)
public class Discord extends ListenerAdapter {
    static private final long INVALID_ID = 0;

    /**
     * The '[Discord]' {@link Text Text} used in chat messages
     */
    static private final Text DISCORD_COMPONENT = Text
            .literal("[").withColor(Colours.DeepKoamaru)
            .append(Text.literal("Discord").withColor(Colours.Lavender))
            .append(Text.literal("] ").withColor(Colours.DeepKoamaru));

    static private final Text REPLY_COMPONENT = Text
            .literal("[Reply] ").withColor(Colours.Lavender);

    static private final Text IMAGE_COMPONENT = Text
            .literal("[Image] ").withColor(Colours.Lavender);

    static private final Text INTERNAL_ERROR_PLEASE_RELINK = Text
            .literal("Sorry, we couldn’t fetch your account info from Discord. Please relink your account")
            .formatted(Formatting.RED);

    static private final SimpleCommandExceptionType NEED_ŊIMP
        = Exn("You need to have at least the @ŋimp role on the Discord server to play on this server!");

    static private final SimpleCommandExceptionType MUST_ENABLE_DMS
        = Exn("You must enable DMs from server members to link your account!");

    static private final Logger LOGGER = LogUtils.getLogger();
    static private final String BUTTON_ID_LINK = "ng_lnk:";
    static private final String[] DEFAULT_AVATARS = new String[] {
            "https://cdn.discordapp.com/embed/avatars/0.png",
            "https://cdn.discordapp.com/embed/avatars/1.png",
            "https://cdn.discordapp.com/embed/avatars/2.png",
            "https://cdn.discordapp.com/embed/avatars/3.png",
            "https://cdn.discordapp.com/embed/avatars/4.png",
            "https://cdn.discordapp.com/embed/avatars/5.png",
    };

    static private JDA Client;
    static private Webhook MessageWebhook;
    static private TextChannel MessageChannel;
    static private Guild AgmaSchwaGuild;
    static private Role NguhcrafterRole;
    static private Role ŊimpRole;
    static private volatile String ServerAvatarURL = DEFAULT_AVATARS[0];
    static private volatile boolean Ready = false;

    static public void Start() throws Exception {
/*        var BotConfig = YamlConfiguration.loadConfiguration(Files.newBufferedReader(Paths.get("discord-bot-config.yml")));
        var Token = BotConfig.getString("token");
        var GuildID = BotConfig.getLong("guild-id");
        var MessageChannelID = BotConfig.getLong("channel-id");
        var WebhookID = BotConfig.getLong("webhook-id");
        var PlayerRoleID = BotConfig.getLong("player-role-id");
        var RequiredRoleID = BotConfig.getLong("required-role-id");

        if (
                Token == null ||
                        MessageChannelID == INVALID_ID ||
                        WebhookID == INVALID_ID ||
                        GuildID == INVALID_ID ||
                        PlayerRoleID == INVALID_ID ||
                        RequiredRoleID == INVALID_ID
        ) throw new Exception("Invalid bot config.");

        var Intents = EnumSet.allOf(GatewayIntent.class);
        Intents.add(GatewayIntent.GUILD_MEMBERS);
        Client = JDABuilder.createDefault(BotConfig.getString("token"))
                .setEnabledIntents(Intents)
                .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.EMOJI)
                .setMemberCachePolicy(MemberCachePolicy.ONLINE)
                .setActivity(Activity.playing("Minecraft"))
                .setBulkDeleteSplittingEnabled(false)
                .addEventListeners(new Discord())
                .build();
        Client.awaitReady();

        try {
            AgmaSchwaGuild = Objects.requireNonNull(Client.getGuildById(GuildID), "guild");
            MessageChannel = Objects.requireNonNull(Client.getTextChannelById(MessageChannelID), "channel");
            MessageWebhook = Client.retrieveWebhookById(WebhookID).complete();
            NguhcrafterRole = Objects.requireNonNull(AgmaSchwaGuild.getRoleById(PlayerRoleID), "player role");
            ŊimpRole = Objects.requireNonNull(AgmaSchwaGuild.getRoleById(RequiredRoleID), "required role");
        } catch (NullPointerException E) {
            throw new Exception("Invalid bot config: Invalid " + E.getMessage() + " ID.");
        }

        var URL = AgmaSchwaGuild.getIconUrl();
        if (URL != null) ServerAvatarURL = URL;
        else ServerAvatarURL = Client.getSelfUser().getEffectiveAvatarUrl();

        Ready = true;
        SendSimpleEmbed(null, "Starting server...", Colours.Lavender);*/
    }

    static public void Stop() {
        if (!Ready) return;
        Ready = false;
        SendSimpleEmbed(null, "Shutting down...", Colours.Lavender);
        Client.shutdown();
    }


    static public void BroadcastAdvancement(ServerPlayerEntity SP, Text AdvancementMessage) {
        if (!Ready) return;
        try {
            var Text = AdvancementMessage.getString();
            SendSimpleEmbed(SP, Text, Colours.Lavender);
        } catch (Exception E) {
            E.printStackTrace();
            LOGGER.error("Failed to send advancement message: {}", E.getMessage());
        }
    }

    static public void BroadcastDeathMessage(ServerPlayerEntity SP, Text DeathMessage) {
        if (!Ready) return;

        // Convoluted setup to both resend an abbreviated message if the actual
        // one ends up being too long (Minecraft does this too) and not let an
        // exception escape in any case.
        try {
            try {
                var Text = DeathMessage.getString();
                SendSimpleEmbed(SP, Text, Colours.Red);
            } catch (IllegalArgumentException | ErrorResponseException E) {
                // Death message was too long.
                String S = DeathMessage.asTruncatedString(256);
                var Msg = Text.translatable("death.attack.even_more_magic", SP.getDisplayName());
                var Abbr = Text.translatable("death.attack.message_too_long", Text.literal(S));
                SendSimpleEmbed(SP, "%s\n\n%s".formatted(Msg, Abbr), Colours.Black);
            }
        } catch (Exception E) {
            E.printStackTrace();
            LOGGER.error("Failed to send death message: {}", E.getMessage());
        }
    }

    static public void BroadcastJoinQuitMessage(ServerPlayerEntity SP, boolean Joined) {
        if (!Ready) return;
        try {
            var Name = ((NguhcraftServerPlayer)SP).isLinked() ? ((NguhcraftServerPlayer) SP).getDiscordName() : SP.getNameForScoreboard();
            var Text = "%s %s the game".formatted(Name, Joined ? "joined" : "left");
            SendSimpleEmbed(SP, Text, Joined ? Colours.Green : Colours.Red);
        } catch (Exception E) {
            E.printStackTrace();
            LOGGER.error("Failed to send join/quit message: {}", E.getMessage());
        }
    }

    /**
     * Discord requires at least one non-combining non-whitespace character in
     * usernames; this returns true if this character makes a username valid.
     */
    static private boolean CreatesValidDiscordUserName(char C) {
        return switch (Character.getType(C)) {
            case Character.UNASSIGNED,
                 Character.MODIFIER_LETTER,
                 Character.NON_SPACING_MARK,
                 Character.COMBINING_SPACING_MARK,
                 Character.SPACE_SEPARATOR,
                 Character.LINE_SEPARATOR,
                 Character.PARAGRAPH_SEPARATOR,
                 Character.CONTROL,
                 Character.FORMAT,
                 Character.PRIVATE_USE,
                 Character.SURROGATE,
                 Character.CONNECTOR_PUNCTUATION,
                 Character.MODIFIER_SYMBOL -> false;
            default -> true;
        };
    }

    /**
     * Get a default avatar that is dependent on some positive
     * number 'Which'; if the number is too large, it will be
     * reduced to a valid index.
     */
    static public String DefaultAvatarURL(int Which) {
        return DEFAULT_AVATARS[Which % DEFAULT_AVATARS.length];
    }

    static public void ForwardChatMessage(@Nullable ServerPlayerEntity SP, String Message) {
        try {
            // Message sent by the server.
            if (SP == null) {
                SendSimpleEmbed(null, Message, Colours.Lavender);
                return;
            }

            // Message sent by a linked user.
            Message(SP, Message);
        } catch (Exception E) {
            E.printStackTrace();
            LOGGER.error("Failed to forward chat message to Discord: {}", E.getMessage());
        }
    }

    static public boolean IsAllowedToLink(Member M) {
        return M.getRoles().contains(ŊimpRole);
    }

    static public void Link(ServerCommandSource Source, ServerPlayerEntity SP, long ID) throws CommandSyntaxException {
        if (!Ready) return;
        var Member = MemberByID(Source, ID);
        if (Member == null) return;

        // Check if the player is allowed to link their account.
        if (!IsAllowedToLink(Member))
            throw NEED_ŊIMP.create();

        // Some geniuses may have decided to make it so people can’t DM them;
        // display an error in that case instead of crashing.
        var Name = SP.getNameForScoreboard();
        var Content = "Press the button below to link your Minecraft account '" + Name + "' to your Discord account.";
        try {
            var Ch = Member.getUser().openPrivateChannel().complete();
            var Mess = Ch.sendMessage(Content)
                .setActionRow(Button.success(BUTTON_ID_LINK + SP.getUuidAsString(), "Confirm"))
                .complete();

            Source.sendMessage(Text.literal("Follow this link and press the button to link your Minecraft account '"
                + Name + "' to your Discord account '" + Member.getEffectiveName() + "': ")
                .withColor(Colours.Green)
                .append(Utils.LINK)
                .styled((style) -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, Mess.getJumpUrl()))));
        } catch (ErrorResponseException E) {
            if (E.getErrorResponse() == ErrorResponse.CANNOT_SEND_TO_USER)
                throw MUST_ENABLE_DMS.create();

            // No idea what this is; rethrow.
            throw E;
        }
    }

    @Nullable
    static public ServerPlayerEntity LinkedPlayerForMember(long ID) {
        for (var SP : Server().getPlayerManager().getPlayerList())
            if (((NguhcraftServerPlayer)SP).getDiscordId() == ID)
                return SP;
        return null;
    }

    @Nullable
    static public Member MemberByID(ServerCommandSource Source, long ID) {
        try {
            return AgmaSchwaGuild.retrieveMemberById(ID).complete();
        } catch (ErrorResponseException E) {
            Source.sendError(switch (E.getErrorResponse()) {
                case UNKNOWN_USER -> Text.literal("Unknown user: " + ID);
                case UNKNOWN_MEMBER -> Text.literal("Unknown member: " + ID);
                default -> Text.literal("Internal Error: " + E.getMessage());
            });
            return null;
        }
    }

    /**
     * Get the linked member for a player.
     *
     * @param SP the player
     * @return the linked member, or null if the player is not linked or there is an error.
     */
    @Nullable
    static private Member MemberForPlayer(ServerPlayerEntity SP) {
        if (!((NguhcraftServerPlayer)SP).isLinked()) return null;
        try {
            return AgmaSchwaGuild.retrieveMemberById(((NguhcraftServerPlayer) SP).getDiscordId()).complete();
        } catch (ErrorResponseException E) {
            return null;
        }
    }

    static private void MemberRoleChanged(Member M) {
        var Colour = M.getColorRaw();
        UpdateLinkedPlayer(M.getIdLong(), (SP) -> {
            var NSP = (NguhcraftServerPlayer)SP;
            if (Colour != NSP.getDiscordColour()) {
                Server().execute(() -> {
                    NSP.setDiscordColour(Colour);
                    UpdatePlayerName(SP);
                });
            }
        });
    }

    /**
     * Send a message to {@link Discord#MessageChannel} using {@link Discord#MessageWebhook}
     *
     * @param Username  the username to be used for this message
     * @param AvatarURL the avatar to be used for this message
     * @param Content   the message content
     */
    public static void Message(String Username, @Nullable String AvatarURL, String Content) {
        if ("".equals(AvatarURL)) AvatarURL = null;
        MessageWebhook.sendMessage(S(Content))
                .setUsername(SanitiseUsername(Username))
                .setAvatarUrl(AvatarURL)
                .queue();
    }

    /**
     * Send a message as a player.
     */
    public static void Message(ServerPlayerEntity SP, String Content) {
        var NSP = (NguhcraftServerPlayer)SP;
        if (NSP.isLinked()) Message(NSP.getDiscordName(), NSP.getDiscordAvatarURL(), Content);
        else {
            var N = SP.getNameForScoreboard();
            Message(N + " (unlinked)", DefaultAvatarURL(N.length()), Content);
        }
    }

    private static void PerformUnlink(ServerPlayerEntity SP) {
        var ID = ((NguhcraftServerPlayer)SP).getDiscordId();
        UpdatePlayer(SP, INVALID_ID, null, null, Colours.Grey);

        // Remove the @ŋguhcrafter role.
        AgmaSchwaGuild.removeRoleFromMember(UserSnowflake.fromId(ID), NguhcrafterRole).queue();
    }

    private static void PerformLink(ServerPlayerEntity SP, Member M) {
        var DiscordMsg = "%s is now linked to %s".formatted(
            SP.getNameForScoreboard(),
            M.getUser().getName()
        );

        UpdatePlayer(SP, M);

        // Move the player out of adventure mode.
        if (SP.interactionManager.getGameMode() == GameMode.ADVENTURE)
            SP.changeGameMode(GameMode.SURVIVAL);

        // Give them the @ŋguhcrafter role.
        AgmaSchwaGuild.addRoleToMember(M, NguhcrafterRole).queue();

        // Broadcast the change to all players and send a message to Discord.
        SendSimpleEmbed(null, DiscordMsg, Colours.Green);
        Server().getPlayerManager().broadcast(Text.empty()
            .append(Text.literal(SP.getNameForScoreboard()).formatted(Formatting.AQUA))
            .append(Text.literal(" is now linked to ").formatted(Formatting.GREEN))
            .append(Text.literal(M.getEffectiveName()).withColor(M.getColorRaw())),
            false
        );
    }

    /**
     * An abbreviation of {@code Sanitise(SerialiseLegacyString(s))}
     *
     * @param s a string
     * @return the return value of {@code Sanitise(SerialiseLegacyString(s))}
     *
     * @see #Sanitise(String)
     * @see #SerialiseLegacyString(String)
     */
    private static String S(@Nullable String s) {
        return s == null ? "null" : Sanitise(SerialiseLegacyString(s));
    }

    /**
     * Sanitise mentions by inserting a zero-width space after each @ sign in {@code s}
     *
     * @param s the String to be sanitised
     * @return a copy of {@code s}, with all mentions sanitised
     */
    @Nullable
    @Contract(value = "null -> null; !null -> !null")
    static String Sanitise(@Nullable String s) {
        return s == null ? null : s.replace("@", "@\u200B");
    }

    /**
     * Sanitise a username for use as the author of a webhook.
     */
    static String SanitiseUsername(String Username) {
        Username = S(Username);
        var HasActualLetter = false;
        for (var C : Username.toCharArray()) {
            if (CreatesValidDiscordUserName(C)) {
                HasActualLetter = true;
                break;
            }
        }

        if (!HasActualLetter) Username = "[Invalid Username] " + Username;
        return Username;
    }

    /**
     * Send a message containing text and an embed
     *
     * @param Username  the username to be used for this message
     * @param AvatarURL the avatar to be used for this message
     * @param IconURL   the icon to be used for the embed
     * @param Content   the message content
     * @param Colour    the colour of the embed
     */
    public static void SendEmbed(
            String Username,
            @Nullable String AvatarURL,
            @Nullable String IconURL,
            String Content,
            int Colour
    ) {
        if ("".equals(AvatarURL)) AvatarURL = null;
        if ("".equals(IconURL)) IconURL = null;

        // Discord username may not consist entirely of combining characters or whitespace.
        Username = SanitiseUsername(Username);
        var E = new EmbedBuilder()
                .setAuthor(S(Content), null, IconURL)
                .setColor(Colour)
                .build();

        MessageWebhook.sendMessageEmbeds(E)
                .setUsername(Username)
                .setAvatarUrl(AvatarURL)
                .queue();
    }

    /**
     * Send an embed to the server. Used for join, quit, death, and link messages.
     *
     * @param SP     the player whose name should be used
     * @param Text   the message content
     * @param Colour the colour of the embed
     */
    static public void SendSimpleEmbed(@Nullable ServerPlayerEntity SP, String Text, int Colour) {
        SendEmbed(
                "[Server]",
                ServerAvatarURL,
                SP == null ? null : ((NguhcraftServerPlayer)SP).getDiscordAvatarURL(),
                Text,
                Colour
        );
    }

    /**
     * Remove all § colour codes from a String
     *
     * @param s the string
     * @return a copy of {@code s}, with all § colour codes removed
     */
    @Nullable
    @Contract(value = "null -> null; !null -> !null")
    static String SerialiseLegacyString(@Nullable String s) {
        return s == null ? null : s.replaceAll("§.", "");
    }

    static public void Unlink(ServerCommandSource S, ServerPlayerEntity SP) {
        if (!Ready) return;
        PerformUnlink(SP);
        S.sendMessage(Text.literal("Unlinked ")
            .append(SP.getName())
            .append(" from Discord.")
            .formatted(Formatting.YELLOW)
        );

        var DiscordMsg = "%s is no longer linked".formatted(SP.getNameForScoreboard());
        SendSimpleEmbed(SP, DiscordMsg, Colours.Red);
    }

    /**
     * Get the player linked to the Discord account with the given ID, and
     * if it exists, queue F to run in the main thread to update the player.
     */
    static public void UpdateLinkedPlayer(long ID, Consumer<ServerPlayerEntity> F) {
        var SP = LinkedPlayerForMember(ID);
        if (SP == null) return;
        Server().execute(() -> F.accept(SP));
    }

    /**
     * Refresh a player after (un)linking them.
     * <p>
     * This sets a player’s display name and refreshes their commands.
     */
    static public void UpdatePlayer(
        ServerPlayerEntity SP,
        long ID,
        @Nullable String DisplayName,
        @Nullable String AvatarURL,
        int NameColour
    ) {
        // Sanity check.
        assert Server().isOnThread() : "Must link on tick thread";

        // Dew it.
        var NSP = (NguhcraftServerPlayer)SP;
        NSP.setDiscordId(ID);
        NSP.setDiscordName(DisplayName);
        NSP.setDiscordColour(NameColour);
        NSP.setDiscordAvatarURL(AvatarURL);
        UpdatePlayerName(SP);

        // The 'link'/'unlink' options are only available if the player is
        // unlinked/linked, so we need to refresh the player’s commands.
        Server().getCommandManager().sendCommandTree(SP);
    }

    static public void UpdatePlayer(ServerPlayerEntity SP, Member M) {
        UpdatePlayer(
            SP,
            M.getIdLong(),
            M.getEffectiveName(),
            M.getEffectiveAvatarUrl(),
            M.getColorRaw()
        );
    }

    /**
     * Re-fetch a linked player’s info from Discord. Called after a player
     * first joins.
     */
    static public void UpdatePlayerAsync(ServerPlayerEntity SP) {
        var NSP = (NguhcraftServerPlayer)SP;
        if (!NSP.isLinked()) {
            UpdatePlayerName(SP);
            return;
        }

        // Retrieve the member to see if they’re still on the server and because
        // their name, avatar, etc. may have changed since we last saw them.
        AgmaSchwaGuild.retrieveMemberById(NSP.getDiscordId())
            .useCache(false)
            .queue((M) -> Server().execute(() -> {
            if (M == null) PerformUnlink(SP);
            else UpdatePlayer(SP, M);
        }), (failure) -> Server().execute(() -> {
            // Failure likely indicates that the player is no longer on the
            // server; unlink them just in case and log the error.
            PerformUnlink(SP);
            SP.sendMessage(INTERNAL_ERROR_PLEASE_RELINK);
            failure.printStackTrace();
            LOGGER.error("Failed to fetch Discord info for player '{}': {}", SP.getName(), failure.getMessage());
        }));
    }

    @Override
    public void onButtonInteraction(final ButtonInteractionEvent E) {
        if (!Ready) return;
        var ID = E.getComponentId();
        if (ID.startsWith(BUTTON_ID_LINK)) {
            var SP = ServerUtils.PlayerByUUID(ID.substring(BUTTON_ID_LINK.length()));
            if (SP == null) E.reply("Invalid player!").setEphemeral(true).queue();

            // Sanity check. Overriding the link would not be the end of the
            // world, so we don’t check for that elsewhere, but we might as
            // well check for it here.
            else if (((NguhcraftServerPlayer)SP).isLinked()) E.reply(
                "This account is already linked to a Discord account!"
            ).setEphemeral(true).queue();

            // If the account is not a server member, ignore.
            else AgmaSchwaGuild.retrieveMemberById(E.getUser().getIdLong()).queue((M) -> {
                // I don’t think it’s possible to get here w/ 'M' being null,
                // but I’d rather not take any chances in this particular part
                // of the code base...
                if (M == null) {
                    E.reply("You are not a member of the server!").setEphemeral(true).queue();
                    return;
                }

                // Last-minute check because race conditions.
                if (!IsAllowedToLink(M)) {
                    E.reply("You must have at least the @ŋimp role to link your account!")
                        .setEphemeral(true).queue();
                    return;
                }

                // Link the player. Take care to do this in the main thread.
                Server().execute(() -> PerformLink(SP, M));
                E.reply("Done!").setEphemeral(true).queue();
            }, (Error) -> {
                if (
                    Error instanceof ErrorResponseException T &&
                    T.getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER
                ) {
                    E.reply("You are not a member of the server!").setEphemeral(true).queue();
                } else {
                    E.reply("Error: %s\n\nAre you a member of the Agma Schwa Discord server?"
                        .formatted(Error.getMessage()))
                        .setEphemeral(true)
                        .queue();
                }
            });
        }
    }

    @Override
    public void onGuildMemberRemove(final GuildMemberRemoveEvent E) {
        if (!Ready) return;
        UpdateLinkedPlayer(E.getUser().getIdLong(), Discord::PerformUnlink);
    }

    @Override
    public void onGuildMemberRoleAdd(final GuildMemberRoleAddEvent E) {
        if (!Ready) return;
        MemberRoleChanged(E.getMember());
    }

    @Override
    public void onGuildMemberRoleRemove(final GuildMemberRoleRemoveEvent E) {
        if (!Ready) return;
        MemberRoleChanged(E.getMember());
    }

    @Override
    public void onGuildMemberUpdate(final GuildMemberUpdateEvent E) {
        var M = E.getMember();
        UpdateLinkedPlayer(M.getIdLong(), (SP) -> UpdatePlayer(SP, M));
    }

    @Override
    public void onGuildMemberUpdateAvatar(final GuildMemberUpdateAvatarEvent E) {
        if (!Ready) return;
        var URL = E.getNewAvatarUrl();
        UpdateLinkedPlayer(E.getMember().getIdLong(), (SP) ->
            ((NguhcraftServerPlayer)SP).setDiscordAvatarURL(URL)
        );
    }

    @Override
    public void onGuildMemberUpdateNickname(final GuildMemberUpdateNicknameEvent E) {
        if (!Ready) return;
        UpdateLinkedPlayer(E.getMember().getIdLong(), (SP) -> {
            var Name = E.getNewNickname();
            if (Name == null) Name = E.getMember().getEffectiveName();
            ((NguhcraftServerPlayer)SP).setDiscordName(Name);
            UpdatePlayerName(SP);
        });
    }

    @Override
    public void onGuildUpdateIcon(final GuildUpdateIconEvent E) {
        ServerAvatarURL = E.getNewIconUrl() != null ? E.getNewIconUrl()
            : E.getOldIconUrl() != null ? E.getOldIconUrl()
            : Client.getSelfUser().getEffectiveAvatarUrl();
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent E) {
        // Prevent infinite loops and only consider messages in the designated channel.
        if (!Ready) return;
        var A = E.getAuthor();
        if (
            E.isWebhookMessage() ||
            A.isBot() ||
            E.getChannel().getIdLong() != MessageChannel.getIdLong()
        ) return;

        var M = E.getMember();
        var Name = A.getEffectiveName();
        var Colour = M == null ? Colours.Grey : M.getColorRaw();
        var Mess = E.getMessage();
        var Content = Mess.getContentDisplay();
        var HasAttachments = !Mess.getAttachments().isEmpty();
        var HasReference = Mess.getMessageReference() != null;
        var S = Server();
        Server().execute(() -> {
            var Comp = DISCORD_COMPONENT.copy().append(Text.literal(Name).append(": ").withColor(Colour));
            if (HasReference) Comp.append(REPLY_COMPONENT);
            if (HasAttachments) Comp.append(IMAGE_COMPONENT);
            Comp.append(Text.literal(Content).formatted(Formatting.WHITE));
            S.getPlayerManager().broadcast(Comp, false);
        });
    }
}

