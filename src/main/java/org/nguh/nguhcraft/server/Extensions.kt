package org.nguh.nguhcraft.server

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor

val ServerPlayerEntity.isLinked get() = (this as ServerPlayerAccessor).isLinked
val ServerPlayerEntity.isOperator get() = hasPermissionLevel(4)
val ServerPlayerEntity.isLinkedOrOperator get() = isLinked || isOperator

var ServerPlayerEntity.discordId
    get() = (this as ServerPlayerAccessor).discordId
    set(value) { (this as ServerPlayerAccessor).discordId = value }
var ServerPlayerEntity.discordColour
    get() = (this as ServerPlayerAccessor).discordColour
    set(value) { (this as ServerPlayerAccessor).discordColour = value }
var ServerPlayerEntity.discordName: String?
    get() = (this as ServerPlayerAccessor).discordName
    set(value) { (this as ServerPlayerAccessor).discordName = value }
var ServerPlayerEntity.discordAvatarURL: String?
    get() = (this as ServerPlayerAccessor).discordAvatarURL
    set(value) { (this as ServerPlayerAccessor).discordAvatarURL = value }
var ServerPlayerEntity.discordDisplayName: Text?
    get() = (this as ServerPlayerAccessor).nguhcraftDisplayName
    set(value) { (this as ServerPlayerAccessor).nguhcraftDisplayName = value }
