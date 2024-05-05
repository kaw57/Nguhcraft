package org.nguh.nguhcraft.server

import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.logging.LogUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateAvatarEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.GameMode
import org.jetbrains.annotations.Contract
import org.nguh.nguhcraft.Colours
import org.nguh.nguhcraft.Commands
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.server.ServerUtils.Server
import org.slf4j.Logger
import java.util.function.Consumer
import kotlin.concurrent.Volatile
import java.io.File
import java.util.EnumSet

@Environment(EnvType.SERVER)
class Discord : ListenerAdapter() {
    override fun onButtonInteraction(E: ButtonInteractionEvent) {
        if (!Ready) return

        // Ignore any interactions that we don’t recognise.
        val ID = E.componentId
        if (!ID.startsWith(BUTTON_ID_LINK)) return

        // Try to retrieve the player we need to link to.
        val SP = ServerUtils.PlayerByUUID(ID.substring(BUTTON_ID_LINK.length))
        if (SP == null) E.reply("Invalid player!").setEphemeral(true).queue()

        // Sanity check. Overriding the link would not be the end of the
        // world, so we don’t check for that elsewhere, but we might as
        // well check for it here.
        else if (SP.isLinked) E.reply("This account is already linked to a Discord account!")
            .setEphemeral(true).queue()

        // If the account is not a server member, ignore.
        else AgmaSchwaGuild.retrieveMemberById(E.user.idLong).queue({ M ->
            // I don’t think it’s possible to get here w/ 'M' being null,
            // but I’d rather not take any chances in this particular part
            // of the code base...
            if (M == null) {
                E.reply("You are not a member of the server!").setEphemeral(true).queue()
                return@queue
            }

            // Last-minute check because race conditions.
            if (!IsAllowedToLink(M)) {
                E.reply("You must have at least the @ŋimp role to link your account!")
                    .setEphemeral(true).queue()
                return@queue
            }

            // Link the player. Take care to do this in the main thread.
            Server().execute { PerformLink(SP, M) }
            E.reply("Done!").setEphemeral(true).queue()
        }, { Error ->
            if (Error is ErrorResponseException && Error.errorResponse == ErrorResponse.UNKNOWN_MEMBER) {
                E.reply("You are not a member of the server!").setEphemeral(true).queue()
            } else {
                E.reply("Error: ${Error.message}\n\nAre you a member of the Agma Schwa Discord server?")
                    .setEphemeral(true)
                    .queue()
            }
        })
    }

    override fun onGuildMemberRemove(E: GuildMemberRemoveEvent) {
        if (Ready) UpdateLinkedPlayer(E.user.idLong) { PerformUnlink(it) }
    }

    override fun onGuildMemberRoleAdd(E: GuildMemberRoleAddEvent) {
        if (Ready) MemberRoleChanged(E.member)
    }

    override fun onGuildMemberRoleRemove(E: GuildMemberRoleRemoveEvent) {
        if (Ready) MemberRoleChanged(E.member)
    }

    override fun onGuildMemberUpdate(E: GuildMemberUpdateEvent) {
        val M = E.member
        UpdateLinkedPlayer(M.idLong) { UpdatePlayer(it, M) }
    }

    override fun onGuildMemberUpdateAvatar(E: GuildMemberUpdateAvatarEvent) {
        if (!Ready) return
        val URL = E.newAvatarUrl
        UpdateLinkedPlayer(E.member.idLong) { it.discordAvatarURL = URL }
    }

    override fun onGuildMemberUpdateNickname(E: GuildMemberUpdateNicknameEvent) {
        if (!Ready) return
        UpdateLinkedPlayer(E.member.idLong) {
            var Name = E.newNickname
            if (Name == null) Name = E.member.effectiveName
            it.discordName = Name
            ServerUtils.UpdatePlayerName(it)
        }
    }

    override fun onGuildUpdateIcon(E: GuildUpdateIconEvent) {
        ServerAvatarURL = when {
            E.newIconUrl != null -> E.newIconUrl!!
            E.oldIconUrl != null -> E.oldIconUrl!!
            else -> Client.selfUser.effectiveAvatarUrl
        }
    }

    override fun onMessageReceived(E: MessageReceivedEvent) {
        // Prevent infinite loops and only consider messages in the designated channel.
        if (!Ready) return
        val A = E.author
        if (E.isWebhookMessage || A.isBot || E.channel.idLong != MessageChannel.idLong) return
        val M = E.member
        val Name = A.effectiveName
        val Colour = M?.colorRaw ?: Colours.Grey
        val Mess = E.message
        val Content = Mess.contentDisplay
        val HasAttachments = Mess.attachments.isNotEmpty()
        val HasReference = Mess.messageReference != null
        val S = Server()
        Server().execute {
            val Comp = DISCORD_COMPONENT.copy().append(Text.literal(Name).append(": ").withColor(Colour))
            if (HasReference) Comp.append(REPLY_COMPONENT)
            if (HasAttachments) Comp.append(IMAGE_COMPONENT)
            Comp.append(Text.literal(Content).formatted(Formatting.WHITE))
            S.playerManager.broadcast(Comp, false)
        }
    }

    companion object {
        private const val INVALID_ID: Long = 0

        /**
         * The '[Discord]' [Text] used in chat messages
         */
        private val DISCORD_COMPONENT: Text = Text
            .literal("[").withColor(Colours.DeepKoamaru)
            .append(Text.literal("Discord").withColor(Colours.Lavender))
            .append(Text.literal("] ").withColor(Colours.DeepKoamaru))

        private val REPLY_COMPONENT: Text = Text
            .literal("[Reply] ").withColor(Colours.Lavender)

        private val IMAGE_COMPONENT: Text = Text
            .literal("[Image] ").withColor(Colours.Lavender)

        private val INTERNAL_ERROR_PLEASE_RELINK: Text = Text
            .literal("Sorry, we couldn’t fetch your account info from Discord. Please relink your account")
            .formatted(Formatting.RED)

        private val NEED_NGIMP : SimpleCommandExceptionType
            = Commands.Exn("You need to have at least the @ŋimp role on the Discord server to play on this server!")

        private val MUST_ENABLE_DMS : SimpleCommandExceptionType
            = Commands.Exn("You must enable DMs from server members to link your account!")

        private val LOGGER: Logger = LogUtils.getLogger()
        private const val BUTTON_ID_LINK = "ng_lnk:"
        private val DEFAULT_AVATARS = arrayOf(
            "https://cdn.discordapp.com/embed/avatars/0.png",
            "https://cdn.discordapp.com/embed/avatars/1.png",
            "https://cdn.discordapp.com/embed/avatars/2.png",
            "https://cdn.discordapp.com/embed/avatars/3.png",
            "https://cdn.discordapp.com/embed/avatars/4.png",
            "https://cdn.discordapp.com/embed/avatars/5.png",
        )

        private lateinit var Client: JDA
        private lateinit var MessageWebhook: Webhook
        private lateinit var MessageChannel: TextChannel
        private lateinit var AgmaSchwaGuild: Guild
        private lateinit var NguhcrafterRole: Role
        private lateinit var NgimpRole: Role

        @Volatile
        private var ServerAvatarURL: String = DEFAULT_AVATARS[0]

        @Volatile
        private var Ready = false

        /// Config file format.
        @Serializable
        data class ConfigFile(
            val token: String,
            val guildId: Long,
            val channelId: Long,
            val webhookId: Long,
            val playerRoleId: Long,
            val requiredRoleId: Long
        )

        @Throws(Exception::class)
        @JvmStatic
        fun Start() {
            // Load the bot config.
            val Config = Json.decodeFromString<ConfigFile>(File("config.json").readText())
            val Intents = EnumSet.allOf(GatewayIntent::class.java).also { it.add(GatewayIntent.GUILD_MEMBERS) }
            Client = JDABuilder.createDefault(Config.token)
                .setEnabledIntents(Intents)
                .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.EMOJI)
                .setMemberCachePolicy(MemberCachePolicy.ONLINE)
                .setActivity(Activity.playing("Minecraft"))
                .setBulkDeleteSplittingEnabled(false)
                .addEventListeners(Discord())
                .build()
            Client.awaitReady()

            fun <T> Get(Text: String, Callback: () -> T) = Callback() ?: throw Exception("Invalid $Text ID.")
            AgmaSchwaGuild = Get("guild") { Client.getGuildById(Config.guildId) }
            MessageChannel = Get("channel") { Client.getTextChannelById(Config.channelId) }
            MessageWebhook = Get("webhook") { Client.retrieveWebhookById(Config.webhookId).complete() }
            NguhcrafterRole = Get("player role") { AgmaSchwaGuild.getRoleById(Config.playerRoleId) }
            NgimpRole = Get("required role") { AgmaSchwaGuild.getRoleById(Config.requiredRoleId) }
            ServerAvatarURL = AgmaSchwaGuild.iconUrl ?: Client.selfUser.effectiveAvatarUrl
            Ready = true
            SendSimpleEmbed(null, "Starting server...", Colours.Lavender);
        }

        @JvmStatic
        fun Stop() {
            if (!Ready) return
            Ready = false
            SendSimpleEmbed(null, "Shutting down...", Colours.Lavender)
            Client.shutdown()
        }

        fun BroadcastAdvancement(SP: ServerPlayerEntity?, AdvancementMessage: Text) {
            if (!Ready) return
            try {
                val Text = AdvancementMessage.string
                SendSimpleEmbed(SP, Text, Colours.Lavender)
            } catch (E: Exception) {
                E.printStackTrace()
                LOGGER.error("Failed to send advancement message: {}", E.message)
            }
        }

        fun BroadcastDeathMessage(SP: ServerPlayerEntity, DeathMessage: Text) {
            if (!Ready) return

            // Convoluted setup to both resend an abbreviated message if the actual
            // one ends up being too long (Minecraft does this too) and not let an
            // exception escape in any case.
            try {
                val Text = DeathMessage.string
                SendSimpleEmbed(SP, Text, Colours.Red)
            } catch (E: Exception) {
                if (E is IllegalArgumentException || E is ErrorResponseException) {
                    // Death message was too long.
                    val S = DeathMessage.asTruncatedString(256)
                    val Msg = Text.translatable("death.attack.even_more_magic", SP.displayName)
                    val Abbr = Text.translatable("death.attack.message_too_long", Text.literal(S))
                    SendSimpleEmbed(SP, "$Msg\n\n$Abbr", Colours.Black)
                    return
                }

                E.printStackTrace()
                LOGGER.error("Failed to send death message: {}", E.message)
            }
        }

        fun BroadcastJoinQuitMessage(SP: ServerPlayerEntity, Joined: Boolean) {
            if (!Ready) return
            try {
                val Name = if (SP.isLinked) SP.discordName
                else SP.nameForScoreboard
                val Text = "$Name ${if (Joined) "joined" else "left"} the game"
                SendSimpleEmbed(SP, Text, if (Joined) Colours.Green else Colours.Red)
            } catch (E: Exception) {
                E.printStackTrace()
                LOGGER.error("Failed to send join/quit message: {}", E.message)
            }
        }

        /**
         * Discord requires at least one non-combining non-whitespace character in
         * usernames; this returns true if this character makes a username valid.
         */
        private fun CreatesValidDiscordUserName(C: Char): Boolean {
            return when (Character.getType(C).toByte()) {
                Character.UNASSIGNED,
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
                Character.MODIFIER_SYMBOL -> false
                else -> true
            }
        }

        /**
         * Get a default avatar that is dependent on some positive
         * number 'Which'; if the number is too large, it will be
         * reduced to a valid index.
         */
        private fun DefaultAvatarURL(Which: Int): String = DEFAULT_AVATARS[Which % DEFAULT_AVATARS.size]

        fun ForwardChatMessage(SP: ServerPlayerEntity?, Message: String?) {
            try {
                // Message sent by the server.
                if (SP == null) {
                    SendSimpleEmbed(null, Message, Colours.Lavender)
                    return
                }

                // Message sent by a linked user.
                Message(SP, Message)
            } catch (E: Exception) {
                E.printStackTrace()
                LOGGER.error("Failed to forward chat message to Discord: {}", E.message)
            }
        }

        /** Check if a server member is allowed to link their account at all. */
        private fun IsAllowedToLink(M: Member): Boolean = M.roles.contains(NgimpRole)

        /** Attempt to initiate a link of a player to a member w/ the given ID */
        @Throws(CommandSyntaxException::class)
        fun Link(Source: ServerCommandSource, SP: ServerPlayerEntity, ID: Long) {
            if (!Ready) return
            val Member = MemberByID(Source, ID) ?: return

            // Check if the player is allowed to link their account.
            if (!IsAllowedToLink(Member)) throw NEED_NGIMP.create()

            // Some geniuses may have decided to make it so people can’t DM them;
            // display an error in that case instead of crashing.
            val Name = SP.nameForScoreboard
            val Content = "Press the button below to link your Minecraft account '$Name' to your Discord account."
            try {
                val Ch = Member.user.openPrivateChannel().complete()
                val Mess = Ch.sendMessage(Content)
                    .setActionRow(Button.success(BUTTON_ID_LINK + SP.uuidAsString, "Confirm"))
                    .complete()

                Source.sendMessage(
                    Text.literal(
                        "Follow this link and press the button to link your Minecraft account '"
                        + Name + "' to your Discord account '" + Member.effectiveName + "': "
                    )
                    .withColor(Colours.Green)
                    .append(Utils.LINK)
                    .styled { style -> style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, Mess.jumpUrl)) })
            } catch (E: ErrorResponseException) {
                if (E.errorResponse == ErrorResponse.CANNOT_SEND_TO_USER) throw MUST_ENABLE_DMS.create()

                // No idea what this is; rethrow.
                throw E
            }
        }

        private fun LinkedPlayerForMember(ID: Long): ServerPlayerEntity?
            = Server().playerManager.playerList.find { (it as NguhcraftServerPlayer).discordId == ID}

        private fun MemberByID(Source: ServerCommandSource, ID: Long): Member? {
            try {
                return AgmaSchwaGuild.retrieveMemberById(ID).complete()
            } catch (E: ErrorResponseException) {
                Source.sendError(
                    when (E.errorResponse) {
                        ErrorResponse.UNKNOWN_USER -> Text.literal("Unknown user: $ID")
                        ErrorResponse.UNKNOWN_MEMBER -> Text.literal("Unknown member: $ID")
                        else -> Text.literal("Internal Error: " + E.message)
                    }
                )
                return null
            }
        }

        /**
         * Get the linked member for a player.
         *
         * @param SP the player
         * @return the linked member, or null if the player is not linked or there is an error.
         */
        private fun MemberForPlayer(SP: ServerPlayerEntity): Member? {
            if (!SP.isLinked) return null
            return try {
                AgmaSchwaGuild.retrieveMemberById(SP.discordId).complete()
            } catch (E: ErrorResponseException) {
                null
            }
        }

        private fun MemberRoleChanged(M: Member) {
            val Colour = M.colorRaw
            UpdateLinkedPlayer(M.idLong) { SP: ServerPlayerEntity ->
                if (Colour != SP.discordColour) {
                    Server().execute {
                        SP.discordColour = Colour
                        ServerUtils.UpdatePlayerName(SP)
                    }
                }
            }
        }

        /**
         * Send a message to [Discord.MessageChannel] using [Discord.MessageWebhook]
         *
         * @param Username  the username to be used for this message
         * @param AvatarURL the avatar to be used for this message
         * @param Content   the message content
         */
        private fun Message(Username: String, AvatarURL: String?, Content: String?) {
            var AvatarURL = AvatarURL
            if ("" == AvatarURL) AvatarURL = null
            MessageWebhook.sendMessage(S(Content))
                .setUsername(SanitiseUsername(Username))
                .setAvatarUrl(AvatarURL)
                .queue()
        }

        /**
         * Send a message as a player.
         */
        private fun Message(SP: ServerPlayerEntity, Content: String?) {
            if (SP.isLinked) Message(SP.discordName!!, SP.discordAvatarURL, Content)
            else {
                val N = SP.nameForScoreboard
                Message("$N (unlinked)", DefaultAvatarURL(N.length), Content)
            }
        }

        private fun PerformUnlink(SP: ServerPlayerEntity) {
            val ID = (SP as NguhcraftServerPlayer).discordId
            UpdatePlayer(SP, INVALID_ID, null, null, Colours.Grey)

            // Remove the @ŋguhcrafter role.
            AgmaSchwaGuild.removeRoleFromMember(UserSnowflake.fromId(ID), NguhcrafterRole).queue()
        }

        private fun PerformLink(SP: ServerPlayerEntity, M: Member) {
            val DiscordMsg = "${SP.nameForScoreboard} is now linked to ${M.user.name}"
            UpdatePlayer(SP, M)

            // Move the player out of adventure mode.
            if (SP.interactionManager.gameMode == GameMode.ADVENTURE) SP.changeGameMode(GameMode.SURVIVAL)

            // Give them the @ŋguhcrafter role.
            AgmaSchwaGuild.addRoleToMember(M, NguhcrafterRole).queue()

            // Broadcast the change to all players and send a message to Discord.
            SendSimpleEmbed(null, DiscordMsg, Colours.Green)
            Server().playerManager.broadcast(
                Text.empty()
                    .append(Text.literal(SP.nameForScoreboard).formatted(Formatting.AQUA))
                    .append(Text.literal(" is now linked to ").formatted(Formatting.GREEN))
                    .append(Text.literal(M.effectiveName).withColor(M.colorRaw)),
                false
            )
        }

        /**
         * An abbreviation of `Sanitise(SerialiseLegacyString(s))`
         *
         * @param s a string
         * @return the return value of `Sanitise(SerialiseLegacyString(s))`
         *
         * @see .Sanitise
         * @see .SerialiseLegacyString
         */
        private fun S(s: String?) = if (s == null) "null" else Sanitise(SerialiseLegacyString(s))!!

        /**
         * Sanitise mentions by inserting a zero-width space after each @ sign in `s`
         *
         * @param s the String to be sanitised
         * @return a copy of `s`, with all mentions sanitised
         */
        @Contract(value = "null -> null; !null -> !null")
        fun Sanitise(s: String?) = s?.replace("@", "@\u200B")

        /**
         * Sanitise a username for use as the author of a webhook.
         */
        private fun SanitiseUsername(Username: String): String {
            val Sanitised = S(Username)
            return if (Sanitised.any { CreatesValidDiscordUserName(it) }) Sanitised.take(80)
            else "[Invalid Username] $Sanitised".take(80)
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
        private fun SendEmbed(
            Username: String,
            AvatarURL: String?,
            IconURL: String?,
            Content: String?,
            Colour: Int
        ) {
            val E = EmbedBuilder()
                .setAuthor(S(Content), null, IconURL?.takeIf { it.isNotEmpty() })
                .setColor(Colour)
                .build()

            MessageWebhook.sendMessageEmbeds(E)
                .setUsername(SanitiseUsername(Username))
                .setAvatarUrl(AvatarURL?.takeIf { it.isNotEmpty() })
                .queue()
        }

        /**
         * Send an embed to the server. Used for join, quit, death, and link messages.
         *
         * @param SP     the player whose name should be used
         * @param Text   the message content
         * @param Colour the colour of the embed
         */
        private fun SendSimpleEmbed(SP: ServerPlayerEntity?, Text: String?, Colour: Int) {
            SendEmbed(
                "[Server]",
                ServerAvatarURL,
                if (SP == null) null else (SP as NguhcraftServerPlayer).discordAvatarURL,
                Text,
                Colour
            )
        }

        /**
         * Remove all § colour codes from a String
         *
         * @param s the string
         * @return a copy of `s`, with all § colour codes removed
         */
        @Contract(value = "null -> null; !null -> !null")
        fun SerialiseLegacyString(s: String?) = s?.replace("§.".toRegex(), "")

        fun Unlink(S: ServerCommandSource, SP: ServerPlayerEntity) {
            if (!Ready) return
            PerformUnlink(SP)
            S.sendMessage(
                Text.literal("Unlinked ")
                    .append(SP.name)
                    .append(" from Discord.")
                    .formatted(Formatting.YELLOW)
            )

            val DiscordMsg = "${SP.nameForScoreboard} is no longer linked"
            SendSimpleEmbed(SP, DiscordMsg, Colours.Red)
        }

        /**
         * Get the player linked to the Discord account with the given ID, and
         * if it exists, queue F to run in the main thread to update the player.
         */
        fun UpdateLinkedPlayer(ID: Long, F: Consumer<ServerPlayerEntity>) {
            val SP = LinkedPlayerForMember(ID) ?: return
            Server().execute { F.accept(SP) }
        }

        /**
         * Refresh a player after (un)linking them.
         *
         *
         * This sets a player’s display name and refreshes their commands.
         */
        private fun UpdatePlayer(
            SP: ServerPlayerEntity,
            ID: Long,
            DisplayName: String?,
            AvatarURL: String?,
            NameColour: Int
        ) {
            // Sanity check.
            assert(Server().isOnThread) { "Must link on tick thread" }

            // Dew it.
            SP.discordId = ID
            SP.discordName = DisplayName
            SP.discordColour = NameColour
            SP.discordAvatarURL = AvatarURL
            ServerUtils.UpdatePlayerName(SP)

            // The 'link'/'unlink' options are only available if the player is
            // unlinked/linked, so we need to refresh the player’s commands.
            Server().commandManager.sendCommandTree(SP)
        }

        private fun UpdatePlayer(SP: ServerPlayerEntity, M: Member) = UpdatePlayer(
            SP,
            M.idLong,
            M.effectiveName,
            M.effectiveAvatarUrl,
            M.colorRaw
        )

        /**
         * Re-fetch a linked player’s info from Discord. Called after a player
         * first joins.
         */
        fun UpdatePlayerAsync(SP: ServerPlayerEntity) {
            if (!SP.isLinked) {
                ServerUtils.UpdatePlayerName(SP)
                return
            }

            // Retrieve the member to see if they’re still on the server and because
            // their name, avatar, etc. may have changed since we last saw them.
            AgmaSchwaGuild.retrieveMemberById(SP.discordId).useCache(false).queue({ M: Member? ->
                Server().execute {
                    if (M == null) PerformUnlink(SP)
                    else UpdatePlayer(SP, M)
                }
            }, { failure ->
                Server().execute {
                    // Failure likely indicates that the player is no longer on the
                    // server; unlink them just in case and log the error.
                    PerformUnlink(SP)
                    SP.sendMessage(INTERNAL_ERROR_PLEASE_RELINK)
                    failure.printStackTrace()
                    LOGGER.error("Failed to fetch Discord info for player '${SP.name}': ${failure.message}")
                }
            })
        }
    }
}

