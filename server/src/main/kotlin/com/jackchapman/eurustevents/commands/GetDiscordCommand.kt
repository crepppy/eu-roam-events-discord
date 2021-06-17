package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.RustPlayers
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.core.entity.interaction.string
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object GetDiscordCommand : Command {
    override val requiredArguments: ApplicationCommandCreateBuilder.() -> Unit
        get() = {
            string("steam-id", "The users Steam64 ID") { required = true }
        }
    override val description: String
        get() = "Find the discord user of a linked steam account"

    override suspend fun execute(interaction: CommandInteraction) {
        val ack = interaction.acknowledgePublic()
        val steam = transaction {
            RustPlayers.select { RustPlayers.steamId eq interaction.command.options["steam-id"]!!.string().toLong() }
                .map { it[RustPlayers.steamId] to it[RustPlayers.discordId] }.first()
        }

        ack.followUp {
            content = "**${steam.first}** is linked to the user <@${steam.second}>"
        }
    }

}