package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.getWhitelist
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.CommandInteraction

object WhitelistCommand : Command {
    override val admin: Boolean
        get() = true
    override val description: String
        get() = "Get a copy of an up-to-date server whitelist"

    override suspend fun execute(interaction: CommandInteraction) {
        interaction.respondPublic {
            addFile("whitelist.json", getWhitelist())
        }
    }
}