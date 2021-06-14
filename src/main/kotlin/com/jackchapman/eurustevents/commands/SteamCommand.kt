package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.SteamUserResponse
import com.jackchapman.eurustevents.SteamUtils
import com.jackchapman.eurustevents.WebServer
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.core.entity.interaction.string
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder
import dev.kord.rest.builder.interaction.actionRow
import dev.kord.rest.builder.interaction.allowedMentions
import dev.kord.rest.builder.interaction.embed

object SteamCommand : Command {
    override val requiredArguments: ApplicationCommandCreateBuilder.() -> Unit
        get() = {
            string("steam",
                "Your Steam64 ID or link. If left out, the bot will DM you a link to authenticate through steam.")
        }
    override val description: String
        get() = "Link your steam account to the EURE discord through logging in with steam or providing your ID"

    override suspend fun execute(interaction: CommandInteraction) {
        val ack = interaction.acknowledgeEphemeral()
        val steam = interaction.command.options["steam"]?.string()
        if (steam == null) {
            ack.followUp {
                actionRow {
                    linkButton(SteamUtils.login(interaction.user.id.value, interaction.token)) {
                        label = "Click here to link steam"
                    }
                }
                content =
                    "If you do not wish to link your steam this way you can also run `/steam` with your steam id / link"
            }
        } else {
            val steamUser: SteamUserResponse
            try {
                steamUser = SteamUtils.getSteamProfile(steam)!!
            } catch (e: IllegalArgumentException) {
                ack.followUp {
                    content = ":x: This is not a valid steam id / link"
                }
                return
            }
            
            ack.followUp {
                embed {
                    title = "Is this profile correct?"
                    description = steamUser.profileurl
                    url = steamUser.profileurl
                    image = steamUser.avatarfull
                    field("Username", inline = true) { steamUser.personaname.ifBlank { "Blank" } }
                    field("ID", inline = true) { steamUser.steamid }
                }
                actionRow {
                    interactionButton(ButtonStyle.Success, "steam_${steamUser.steamid}") { label = "Confirm" }
                }
                allowedMentions {
                    repliedUser = true
                }
            }
        }

    }
}



