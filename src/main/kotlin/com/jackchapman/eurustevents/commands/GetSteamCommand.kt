package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.SteamUtils
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Message

object GetSteamCommand : Command {
    override val requiredArguments: String
        get() = "@member"

    override suspend fun execute(message: Message) {
        val list = if (message.mentionedUserIds.isEmpty())
            mapOf(message.author!!.id to SteamUtils.getSteamProfileFromDiscord(message.author!!.id.value))
        else
            message.mentionedUserIds.associateWith { SteamUtils.getSteamProfileFromDiscord(it.value) }

        list.forEach { (discord, steamUser) ->
            if (steamUser == null) {
                message.channel.createMessage(":x: <@${discord.value}> has not got their steam linked!")
            } else {
                message.channel.createEmbed {
                    title = steamUser.personaname
                    description = steamUser.profileurl
                    url = steamUser.profileurl
                    image = steamUser.avatarfull
                    field("Discord", inline = true) { "<@${discord.value}>" }
                    field("Steam64", inline = true) { steamUser.steamid }
                }
            }
        }
    }
}