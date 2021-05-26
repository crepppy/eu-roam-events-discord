package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.*
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message

object SubCommand : Command {
    override val requiredArguments: String
        get() = "@old @new"
    override val eventOnly: Boolean
        get() = true

    override suspend fun execute(message: Message) {
        require(message.mentionedUserIds.size == 2)

        val ids = message.mentionedUserIds.map { it.value }
        val needsToLink = ids - RustPlayers.getLinked(ids).map { it.discordId }

        val oldNew =
            message.args.map { PING_REGEX.matcher(it) }.filter { it.matches() }.map { it.group(1).toLong() }.toList()

        val team = SignupManager.currentEvent!!.teams.find { it.leader == message.author!!.id.value }
            ?: if (message.getAuthorAsMember()?.isManager() == true) {
                SignupManager.currentEvent!!.teams.find { it.allMembers.contains(oldNew[0]) }
            } else {
                null
            }

        if (team == null) {
            message.replyError {
                title = "No Team"
                description =
                    "You must be the representative of a team to make a sub. Speak to your team representative to make a sub"
            }
            return
        }
        if (SignupManager.currentEvent!!.startDate != -1L && message.getAuthorAsMember()
                ?.isManager() != true
        ) { //todo bypass this for manager
            message.replyError {
                title = "Event Started"
                description =
                    "The event has already started so you can no longer make substitutions. Contact an admin if this is an issue"
            }
            return
        }

        if (needsToLink.isNotEmpty()) {
            message.reply {
                content =
                    ":x: ${needsToLink.joinToString(", ") { "<@$it>" }} must link their steam by running `!steam` before they can play."
            }
            return
        }

        if (!team.members.contains(oldNew[0])) {
            message.reply {
                content =
                    ":x: <@${oldNew[0]}> is not in your team or is the team leader!"
            }
            return
        }
        val alreadyPlaying = SignupManager.currentEvent!!.teams.flatMap { it.allMembers }.filter { it == oldNew[1] }
        if (alreadyPlaying.isNotEmpty()) {
            message.reply {
                val names = (message.mentionedUserBehaviors + message.author!!).filter { it.id.value in alreadyPlaying }
                    .distinct()
                    .joinToString(", ") { it.mention }
                content =
                    ":x: $names are already signed up for the event. Ask their leader to sub them out to play in the event"
            }
            return
        }
        team.members.remove(oldNew[0])
        team.members.add(oldNew[1])

        message.getGuild().getMember(oldNew[0].sf).removeRole(team.role.sf)
        message.getGuild().getMember(oldNew[1].sf).addRole(team.role.sf)

        SignupManager.currentEvent!!.updateRoster()
        message.reply {
            content = """
                :white_check_mark: Subbed <@${oldNew[0]}> for <@${oldNew[1]}>!
            """.trimIndent()
        }

    }
}