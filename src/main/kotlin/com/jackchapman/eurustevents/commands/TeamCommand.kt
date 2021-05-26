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
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.CategorizableChannel
import dev.kord.x.emoji.Emojis

object TeamCommand : Command {
    override val requiredArguments: String
        get() = "[team name] @member1 @member2 ..."
    override val eventOnly: Boolean
        get() = true

    override suspend fun execute(message: Message) {
        val event = SignupManager.currentEvent!!

        val roles = message.getAuthorAsMember()?.roleIds
        val canCreate = when {
            event.signups.ordinal <= Event.Exclusivity.VIP.ordinal && roles?.contains(ROLE_VIP.sf) == true -> true
            event.signups.ordinal <= Event.Exclusivity.BOOSTER.ordinal && roles?.contains(ROLE_BOOSTER.sf) == true -> true
            event.signups.ordinal <= Event.Exclusivity.EVERYONE.ordinal -> true
            else -> false
        }
        if (!canCreate) {
            message.replyError {
                title = "Signups Restricted"
                description = "Signups are currently restricted to members with <@&${
                    when (event.signups) {
                        Event.Exclusivity.VIP -> ROLE_VIP
                        Event.Exclusivity.BOOSTER -> ROLE_BOOSTER
                        else -> throw UnsupportedOperationException()
                    }
                }> role"
            }
            return
        }
        if (event.teams.size == event.maxTeams || event.startDate != -1L) {
            message.replyError {
                title = "Signups Closed"
                description = "The maximum number of teams for this event has already signed up. Try again next event."
            }
            return
        }
        val authorId = message.author!!.id.value
        val ids = (listOf(authorId) + message.mentionedUserIds.map { it.value }).distinct().take(event.teamSize).toSet()
        require(ids.size >= event.teamSize)

        val teamNameList = mutableListOf<String>()
        for (arg in message.args) {
            if (arg.matches(PING_REGEX.toRegex())) break
            teamNameList += arg
        }
        val teamName = teamNameList.joinToString(" ").trim()
        require(teamName.isNotBlank())

        val alreadyPlaying = event.teams.flatMap { it.allMembers }.filter { it in ids }

        if (alreadyPlaying.isNotEmpty()) {
            message.reply {
                val names = (message.mentionedUserBehaviors + message.author!!).filter { it.id.value in alreadyPlaying }
                    .joinToString(", ") { it.mention }
                content =
                    ":x: $names are already signed up for the event. Ask their leader to sub them out to play in the event"
            }
            return
        }

        // Check steam links
        val steamLinks = RustPlayers.getLinked(ids)
        val needsToLink = ids - steamLinks.map { it.discordId }

        if (needsToLink.isNotEmpty()) {
            message.reply {
                content =
                    ":x: ${needsToLink.joinToString(", ") { "<@$it>" }} must link their steam by running `!steam` before they can play."
            }
            return
        }

        // Confirm
        message.reply {
            embed {
                color = Color(0x00FF00)
                title = "Create team: $teamName?"
                description = "React with ${Emojis.whiteCheckMark} to create team"
                steamLinks.forEach {
                    field("https://steamcommunity.com/profiles/${it.steamId}", inline = true) { "<@${it.discordId}>" }
                }
            }
        }.confirm(authorId) {
            delete()
            if (event.teams.flatMap { it.allMembers }.any { it in ids }) return@confirm
            val guild = getGuild()
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
            message.getAuthorAsMember()!!.addRole(ROLE_REPRESENTATIVE.sf)
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

            message.reply {
                content = """
                    :white_check_mark: Created team **$teamName**!
                """.trimIndent()
            }
        }
    }
}