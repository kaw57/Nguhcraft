package org.nguh.nguhcraft.server

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

val ServerPlayerEntity.isLinked get() = (this as NguhcraftServerPlayer).isLinked
var ServerPlayerEntity.discordId
    get() = (this as NguhcraftServerPlayer).discordId
    set(value) { (this as NguhcraftServerPlayer).discordId = value }
var ServerPlayerEntity.discordColour
    get() = (this as NguhcraftServerPlayer).discordColour
    set(value) { (this as NguhcraftServerPlayer).discordColour = value }
var ServerPlayerEntity.discordName: String?
    get() = (this as NguhcraftServerPlayer).discordName
    set(value) { (this as NguhcraftServerPlayer).discordName = value }
var ServerPlayerEntity.discordAvatarURL: String?
    get() = (this as NguhcraftServerPlayer).discordAvatarURL
    set(value) { (this as NguhcraftServerPlayer).discordAvatarURL = value }
var ServerPlayerEntity.discordDisplayName: Text
    get() = (this as NguhcraftServerPlayer).discordDisplayName
    set(value) { (this as NguhcraftServerPlayer).discordDisplayName = value }
