package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.SignupManager
import dev.kord.common.Color
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.core.entity.interaction.role
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder
import dev.kord.rest.builder.interaction.embed

object DelTeamCommand : Command {
    override val requiredArguments: ApplicationCommandCreateBuilder.() -> Unit
        get() = {
            role("team", "The role of the team to delete") { required = true }
        }
    override val admin: Boolean
        get() = true
    override val description: String
        get() = "Removes the team from the current events roster"

    override suspend fun execute(interaction: CommandInteraction) {
        val ack = interaction.acknowledgePublic()
        val event = SignupManager.currentEvent!!

        val role = interaction.command.options["team"]!!.role()
        val team = event.teams.find { it.role == role.id.value }
        if (team == null) {
            ack.followUp {
                embed {
                    color = Color(0xFF0000)
                    title = "Team does not exist"
                    description = "Ensure you have tagged the right role"
                }
            }
            return
        }
        event.teams -= team
        team.delete(role.guild)

        ack.followUp {
            content = ":white_check_mark: Deleted ${team.name}!"
        }
    }
}