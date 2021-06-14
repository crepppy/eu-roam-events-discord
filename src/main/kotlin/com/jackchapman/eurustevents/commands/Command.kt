package com.jackchapman.eurustevents.commands

import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder

interface Command {
    companion object {
        val GLOBAL_COMMANDS =
            listOf(GetDiscordCommand, GetSteamCommand, PingCommand, SteamCommand).associateBy(Command::name)
        val EVENT_COMMANDS =
            listOf(DelTeamCommand, SubCommand, TeamCommand, WhitelistCommand).associateBy(Command::name)
        val COMMANDS = GLOBAL_COMMANDS + EVENT_COMMANDS
    }

    val name: String
        get() = this::class.simpleName!!.substringBefore("Command").toLowerCase()
    val requiredArguments: ApplicationCommandCreateBuilder.() -> Unit
        get() = {}
    val admin: Boolean
        get() = false
    val description: String

    suspend fun execute(interaction: CommandInteraction)

}