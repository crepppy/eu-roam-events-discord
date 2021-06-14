package com.jackchapman.eurustevents.commands

import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.CommandInteraction

object PingCommand : Command {
    override val description: String
        get() = "Checks if the bot is online"

    override suspend fun execute(interaction: CommandInteraction) {
        interaction.respondPublic {
            content = "Pong! :ping_pong:"
        }
    }
}