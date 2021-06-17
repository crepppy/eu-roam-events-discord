package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.SteamUtils
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.core.entity.interaction.user
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder
import dev.kord.rest.builder.interaction.allowedMentions
import dev.kord.rest.builder.interaction.embed

object GetSteamCommand : Command {
    override val requiredArguments: ApplicationCommandCreateBuilder.() -> Unit
        get() = {
            user("user", "The user to find the steam of (otherwise your own)")
        }
    override val description: String
        get() = "Get a link to a discord user's steam"

    override suspend fun execute(interaction: CommandInteraction) {
        val ack = interaction.acknowledgePublic()
        val discord = interaction.command.options["user"]?.user() ?: interaction.user
        val steamUser = SteamUtils.getSteamProfileFromDiscord(discord.id.value)

        if (steamUser == null) {
            ack.followUp {
                content = ":x: ${discord.mention} has not got their steam linked!"
                allowedMentions {
                    users += interaction.user.id
                    repliedUser = true
                }
            }
        } else {
            ack.followUp {
                embed {
                    title = steamUser.personaname
                    description = steamUser.profileurl
                    url = steamUser.profileurl
                    image = steamUser.avatarfull
                    field("Discord", inline = true) { discord.mention }
                    field("Steam64", inline = true) { steamUser.steamid }
                }
            }
        }

    }
}