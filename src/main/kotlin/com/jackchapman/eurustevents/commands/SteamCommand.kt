package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.*
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import org.jetbrains.exposed.sql.transactions.transaction

object SteamCommand : Command {
    override val requiredArguments: String
        get() = "[steam id / link]"
    override val guildOnly: Boolean
        get() = false

    override suspend fun execute(message: Message) {
        if (message.args.isEmpty()) {
            val dm = message.author!!.getDmChannelOrNull() ?: throw IllegalArgumentException()
            // No ID specified, authenticate via URL
            dm.createMessage {
                embed {
                    title = "Click here to link your steam"
                    description =
                        "If you do not wish to link your steam this way you can also run `!steam` with your steam id / link "
                    url = SteamUtils.login(message.author!!.id.value)
                    color = Color(0x00FF00)
                }
            }
        } else {
            val steamUser: SteamUserResponse
            try {
                 steamUser = SteamUtils.getSteamProfile(message.args[0])!!
            } catch (e: IllegalArgumentException) {
                message.reply { content = ":x: This is not a valid steam id / link" }
                return
            }
            val discord =
                if (message.args.size == 2 && message.mentionedUserIds.isNotEmpty() && message.getAuthorAsMember()
                        ?.isManager() == true
                ) {
                    message.mentionedUserIds.first().value
                } else {
                    message.author!!.id.value
                }
            message.reply {
                embed {
                    title = "Is this profile correct?"
                    description = steamUser.profileurl
                    url = steamUser.profileurl
                    image = steamUser.avatarfull
                    field("Username", inline = true) { steamUser.personaname.ifBlank { "Blank" } }
                    field("ID", inline = true) { steamUser.steamid }
                }
            }.confirm(message.author!!.id.value) {
                transaction {
                    RustPlayers.insertOrUpdate(RustPlayers.steamId) {
                        it[discordId] = discord
                        it[steamId] = steamUser.steamid.toLong()
                    }
                }
                delete()
                message.reply {
                    content = """
                        **:white_check_mark: Successfully linked!**
                        ${steamUser.profileurl}
                    """.trimIndent()
                }
            }
        }

    }
}

data class VanityDTO(val response: VanityUrlResponse)
data class VanityUrlResponse(val steamid: String, val success: Int)

data class SummaryDTO(val response: PlayerSummaryResponse)
data class PlayerSummaryResponse(val players: List<SteamUserResponse>)

data class SteamUserResponse(
    val steamid: String,
    val personaname: String,
    val profileurl: String,
    val avatarfull: String
)




