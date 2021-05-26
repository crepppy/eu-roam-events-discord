package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.RustPlayers
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object GetDiscordCommand : Command {
    override val requiredArguments: String
        get() = ""
    override val adminOnly: Boolean
        get() = true

    override suspend fun execute(message: Message) {
        require(message.args.isNotEmpty())

        val discordIds = transaction {
            RustPlayers.select { RustPlayers.steamId inList message.args.mapNotNull { it.toLongOrNull() } }.associate { it[RustPlayers.steamId] to it[RustPlayers.discordId] }
        }

        message.reply {
            content = discordIds.map { "**${it.key}** is linked to the user <@${it.value}>" }.joinToString("\n")
        }
    }

}