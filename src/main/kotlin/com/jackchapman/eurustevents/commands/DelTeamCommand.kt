package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.CATEGORY_EVENT
import com.jackchapman.eurustevents.SignupManager
import com.jackchapman.eurustevents.replyError
import com.jackchapman.eurustevents.sf
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.Category
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter

object DelTeamCommand : Command {
    override val guildOnly: Boolean
        get() = true
    override val eventOnly: Boolean
        get() = true
    override val requiredArguments: String
        get() = "[role or member id]"
    override val adminOnly: Boolean
        get() = true

    override suspend fun execute(message: Message) {
        require(message.mentionedRoleIds.isNotEmpty() || message.mentionedUserIds.isNotEmpty())
        val event = SignupManager.currentEvent!!

        val team = if (message.mentionedRoleIds.isNotEmpty()) {
            event.teams.find { it.role == message.mentionedRoleIds.first().value }
        } else {
            event.teams.find { message.mentionedUserIds.first().value in it.allMembers }
        }
        if (team == null) {
            message.replyError {
                title = "Team does not exist"
                description = "Ensure you have tagged the right user / role"
            }
            return
        }
        val role = message.getGuild().getRole(team.role.sf)
        event.teams -= team
        message.getGuild().getChannelOf<Category>(CATEGORY_EVENT.sf).channels.filter {
            it.permissionOverwrites.any { overwrite ->
                overwrite.data.id == role.id && overwrite.data.allowed.contains(Permission.Connect)
            }
        }.collect { it.delete() }
        role.delete()

        message.reply {
            content = """
                :white_check_mark: Deleted ${team.name}!
            """.trimIndent()
        }
    }
}