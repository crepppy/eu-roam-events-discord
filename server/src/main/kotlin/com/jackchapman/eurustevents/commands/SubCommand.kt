package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.RustPlayers
import com.jackchapman.eurustevents.SignupManager
import com.jackchapman.eurustevents.isManager
import com.jackchapman.eurustevents.sf
import dev.kord.common.Color
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.core.entity.interaction.user
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder
import dev.kord.rest.builder.interaction.embed

object SubCommand : Command {
    override val requiredArguments: ApplicationCommandCreateBuilder.() -> Unit
        get() = {
            defaultPermission = false
            user("old", "The member to sub out") { required = true }
            user("new", "The member to sub in") { required = true }
        }
    override val description: String
        get() = "Sub a different person in to your team. Note: You must be team representative to do this"

    override suspend fun execute(interaction: CommandInteraction) {
        val ack = interaction.acknowledgePublic()
        val oldNew = listOf(
            interaction.command.options["old"]!!.user().id.value,
            interaction.command.options["new"]!!.user().id.value,
        )
        val needsToLink = oldNew - RustPlayers.getLinked(oldNew).map { it.discordId }
        val guild = interaction.kord.getGuild(interaction.data.guildId.value!!)!!
        val author = guild.getMember(interaction.user.id)

        val team = SignupManager.currentEvent!!.teams.find { it.leader == interaction.user.id.value }
            ?: if (author.isManager()) {
                SignupManager.currentEvent!!.teams.find { it.allMembers.contains(oldNew[0]) }
            } else {
                null
            }

        if (team == null) {
            ack.followUp {
                embed {
                    color = Color(0xFF0000)
                    title = "No Team"
                    description = "This person is not currently in a team"
                }
            }
            return
        }
        if (SignupManager.currentEvent!!.startDate != -1L && !author.isManager()) {
            ack.followUp {
                embed {
                    color = Color(0xFF0000)
                    title = "Event Started"
                    description =
                        "The event has already started so you can no longer make substitutions. Contact an admin if this is an issue"
                }
            }
            return
        }

        if (needsToLink.isNotEmpty()) {
            ack.followUp {
                content =
                    ":x: ${needsToLink.joinToString(", ") { "<@$it>" }} must link their steam by running `/steam` before they can play."
            }
            return
        }

        if (!team.members.contains(oldNew[0])) {
            ack.followUp {
                embed {
                    color = Color(0xFF0000)
                    title = "Not team leader!"
                    description =
                        ":x: <@${oldNew[0]}> is not in your team or is the team leader!"
                }
            }
            return
        }
        val alreadyPlaying = SignupManager.currentEvent!!.teams.flatMap { it.allMembers }.filter { it == oldNew[1] }
        if (alreadyPlaying.isNotEmpty()) {
            ack.followUp {
                val names = (oldNew + interaction.user.id.value).filter { it in alreadyPlaying }
                    .distinct()
                    .joinToString(", ") { "<@$it>" }
                content =
                    ":x: $names are already signed up for the event. Ask their leader to sub them out to play in the event"
            }
            return
        }
        team.members.remove(oldNew[0])
        team.members.add(oldNew[1])

        guild.getMember(oldNew[0].sf).removeRole(team.role.sf)
        guild.getMember(oldNew[1].sf).addRole(team.role.sf)

        SignupManager.currentEvent!!.updateRoster()
        ack.followUp {
            content = ":white_check_mark: Subbed <@${oldNew[0]}> for <@${oldNew[1]}>!"
        }

    }
}