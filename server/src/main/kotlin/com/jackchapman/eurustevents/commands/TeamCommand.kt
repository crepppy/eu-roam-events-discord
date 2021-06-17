package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.*
import dev.kord.common.Color
import dev.kord.common.entity.Overwrite
import dev.kord.common.entity.OverwriteType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.createVoiceChannel
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.entity.channel.CategorizableChannel
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.core.entity.interaction.string
import dev.kord.rest.builder.interaction.ApplicationCommandCreateBuilder
import dev.kord.rest.builder.interaction.embed

object TeamCommand : Command {
    override val requiredArguments: ApplicationCommandCreateBuilder.() -> Unit
        get() = {
            defaultPermission = false
            string("team-name", "The name of your team") { required = true }
        }
    override val description: String
        get() = "Sign your team up for the roam events"

    override suspend fun execute(interaction: CommandInteraction) {
        val ack = interaction.acknowledgePublic()
        val event = SignupManager.currentEvent!!

        if (event.teams.size == event.maxTeams || event.startDate != -1L) {
            ack.followUp {
                embed {
                    color = Color(0xFF0000)
                    title = "Signups Closed"
                    description =
                        "The maximum number of teams for this event has already signed up. Try again next event."
                }
            }
            return
        }
        val authorId = interaction.user.id.value
        val ids = (listOf(authorId) + interaction.command.resolved!!.users!!.keys.map { it.value }).distinct().take(10).toSet()

        if (ids.size != event.teamSize) {
            ack.followUp {
                embed {
                    color = Color(0xFF0000)
                    title = "Not Enough Members"
                    description = "Please tag ${event.teamSize - 1} other members to create a team with, excluding yourself"
                }
            }
            return
        }

        val teamName = interaction.command.options["team-name"]!!.string()

        val alreadyPlaying = event.teams.flatMap { it.allMembers }.filter { it in ids }

        if (alreadyPlaying.isNotEmpty()) {
            ack.followUp {
                val names = alreadyPlaying.joinToString(", ") { "<@$it>" }
                content =
                    ":x: $names are already signed up for the event. Ask their leader to sub them out to play in the event"
            }
            return
        }

        // Check steam links
        val steamLinks = RustPlayers.getLinked(ids)
        val needsToLink = ids - steamLinks.map { it.discordId }

        if (needsToLink.isNotEmpty()) {
            ack.followUp {
                content =
                    ":x: ${needsToLink.joinToString(", ") { "<@$it>" }} must link their steam by running `/steam` before they can play."
            }
            return
        }

        // Confirm
        if (event.teams.flatMap { it.allMembers }.any { it in ids }) return
        val guild = interaction.kord.getGuild(interaction.data.guildId.value!!)!!
        val role = guild.createRole {
            name = teamName
            mentionable = true
        }
        val roster = guild.getChannelOf<CategorizableChannel>(CHANNEL_ROSTER.sf)
        val eventChat = guild.getChannelOf<CategorizableChannel>(CHANNEL_EVENT_CHAT.sf)

        val voice = eventChat.category?.createVoiceChannel(teamName) {
            permissionOverwrites.add(
                Overwrite(
                    role.id,
                    OverwriteType.Role,
                    Permissions(Permission.ViewChannel, Permission.Connect, Permission.Speak),
                    Permissions()
                )
            )
            permissionOverwrites.add(
                Overwrite(
                    guild.everyoneRole.id,
                    OverwriteType.Role,
                    Permissions(Permission.ViewChannel),
                    Permissions(Permission.Connect)
                )
            )
        }!!.id.value

        val team = Team(teamName, role.id.value, voice, authorId, (ids - authorId).toMutableSet())
        event.teams += team
        event.updateRoster()
        team.allMembers.map { guild.getMember(it.sf) }.forEach { it.addRole(role.id) }
        interaction.user.asMember(guild.id).addRole(ROLE_REPRESENTATIVE.sf)
        roster.editRolePermission(role.id) {
            allowed = Permissions(Permission.ViewChannel, Permission.ReadMessageHistory)
            denied = Permissions(Permission.SendMessages)
        }
        roster.editRolePermission(guild.everyoneRole.id) {
            allowed = Permissions()
            denied = Permissions(Permission.SendMessages, Permission.ViewChannel, Permission.ReadMessageHistory)
        }

        eventChat.editRolePermission(role.id) {
            allowed = Permissions(Permission.ViewChannel, Permission.ReadMessageHistory, Permission.SendMessages)
        }
        eventChat.editRolePermission(guild.everyoneRole.id) {
            allowed = Permissions()
            denied = Permissions(Permission.SendMessages, Permission.ViewChannel, Permission.ReadMessageHistory)
        }
        ack.followUp {
            content = ":white_check_mark: Created team **$teamName**!".trimIndent()
        }
    }
}