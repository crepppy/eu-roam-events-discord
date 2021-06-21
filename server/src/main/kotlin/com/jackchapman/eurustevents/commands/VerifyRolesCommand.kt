package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.GUILD_EURE
import com.jackchapman.eurustevents.SignupManager
import com.jackchapman.eurustevents.sf
import dev.kord.core.entity.interaction.CommandInteraction

object VerifyRolesCommand : Command {
    override val description: String
        get() = "Ensures everyone playing has their team role"
    override val admin: Boolean
        get() = true

    override suspend fun execute(interaction: CommandInteraction) {
        val ack = interaction.acknowledgePublic()
        val event = SignupManager.currentEvent!!
        val guild = interaction.kord.getGuild(GUILD_EURE.sf)!!
        event.teams.forEach {  team ->
            val role = team.role.sf
            team.members.map { guild.getMember(it.sf) }.filter { role !in it.roleIds }.forEach { 
                it.addRole(role)
            }
        }
        ack.delete()
    }
}