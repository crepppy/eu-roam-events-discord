package com.jackchapman.eurustevents

import com.jackchapman.eurustevents.commands.Command
import com.jackchapman.eurustevents.commands.SubCommand
import com.jackchapman.eurustevents.commands.TeamCommand
import dev.kord.common.Color
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import kotlin.concurrent.schedule

object SignupManager {
    var currentEvent: Event? = null
}

data class Team(
    val name: String,
    val role: Long,
    val voice: Long,
    val leader: Long,
    val members: MutableSet<Long> /* Discord ID */
) {
    suspend fun delete(guild: GuildBehavior) {
        guild.getMember(leader.sf).removeRole(ROLE_REPRESENTATIVE.sf)
        guild.getChannel(voice.sf).delete()
        guild.getRole(role.sf).delete()
    }

    val allMembers: Set<Long>
        get() = (members + leader).toSet()
}

data class SaveEvent(
    val teamSize: Int,
    val maxTeams: Int,
    val roster: Long,
    val startDate: Long,
    val signups: Event.Exclusivity,
    val teams: MutableList<Team>
)

class Event(
    val teamSize: Int,
    val maxTeams: Int,
    var roster: Long? = null,
    var startDate: Long = -1,
    var signups: Exclusivity = Exclusivity.VIP,
    val teams: MutableList<Team> = mutableListOf(),
    saved: Boolean = false,
    vipNotice: Long = 2 * 60 * 60,
    boosterNotice: Long = 5 * 60,
) : KoinComponent {
    enum class Exclusivity {
        EVERYONE, BOOSTER, VIP;
    }

    @delegate:Transient
    private val client by inject<Kord>()

    val rosterEmbed: EmbedBuilder
        get() = EmbedBuilder().apply {
            title = "Roam Events Roster"
            teams.forEach { team ->
                field(team.name, true) { team.allMembers.joinToString("\n") { "<@$it>" } }
            }
            description = "Make sure your steam is up to date by doing `/getsteam`"
        }

    init {
        runBlocking {
            Command.EVENT_COMMANDS.values.forEach { cmd ->
                client.slashCommands.createGuildApplicationCommand(GUILD_EURE.sf, cmd.name, cmd.description) {
                    defaultPermission = !cmd.admin
                    cmd.requiredArguments(this)
                    if (cmd is TeamCommand) {
                        if (signups == Exclusivity.EVERYONE) defaultPermission = true
                        for (i in 1 until teamSize) {
                            user("member$i",
                                "A member in your team. Do not tag the same member twice") {
                                required = true
                            }
                        }
                        user("member${teamSize}",
                            "A member in your team. Do not tag the same member twice")
                    }
                }.editPermissions {
                    if (cmd.admin) {
                        role(ROLE_MANAGER.sf)
                        user(139068524105564161.sf)
                    }
                    if (cmd is TeamCommand && signups != Exclusivity.EVERYONE) {
                        role(ROLE_MANAGER.sf)
                        user(139068524105564161.sf)
                        role(ROLE_VIP.sf)
                        if (signups == Exclusivity.BOOSTER) {
                            role(ROLE_BOOSTER.sf)
                        }
                    }
                    if (cmd is SubCommand) {
                        role(ROLE_MANAGER.sf)
                        role(ROLE_REPRESENTATIVE.sf)
                        user(139068524105564161.sf)
                    }
                }
            }
        }
        if (!saved) {
            runBlocking {
                sendAnnouncement()
            }
            val timer = Timer()
            timer.schedule((vipNotice - boosterNotice) * 1000) {
                signups = Exclusivity.BOOSTER
                runBlocking {
                    client.slashCommands.getGuildApplicationCommands(GUILD_EURE.sf)
                        .first { it.name == TeamCommand.name }.editPermissions {
                            role(CHANNEL_BOOSTER.sf)
                        }

                    sendAnnouncement()
                }
            }
            timer.schedule(vipNotice * 1000) {
                signups = Exclusivity.EVERYONE
                runBlocking {
                    client.slashCommands.createGuildApplicationCommand(GUILD_EURE.sf,
                        TeamCommand.name,
                        TeamCommand.description) {
                        TeamCommand.requiredArguments(this)
                        this.defaultPermission = true
                        for (i in 1 until teamSize) {
                            user("member$i",
                                "A member in your team. Do not tag the same member twice") {
                                required = true
                            }
                        }
                        user("member${teamSize}",
                            "A member in your team. Do not tag the same member twice")
                    }
                    sendAnnouncement()
                }
            }
        }
    }

    private suspend fun sendAnnouncement() {
        val channel = when (signups) {
            Exclusivity.VIP -> CHANNEL_VIP
            Exclusivity.BOOSTER -> CHANNEL_BOOSTER
            Exclusivity.EVERYONE -> CHANNEL_SIGNUPS
        }
        saveEventToFile()
        client.getChannelOf<GuildMessageChannel>(channel.sf)!!.createMessage {
            embed {
                title = "Signups have started!"
                color = Color(0x0000FF)
                description =
                    "@everyone Signups have now started for Sunday's event at 6pm GMT. Teams are chosen on a first come first served basis. Use `/team` in a commands channel to sign up. If you need to substitute a member, you can do so by running `/sub`"
            }
            content = "@everyone"
        }
    }

    suspend fun updateRoster() {
        val channel = client.getGuild(GUILD_EURE.sf)!!.getChannelOf<TextChannel>(CHANNEL_ROSTER.sf)
        if (roster == null) {
            // Create message
            roster = channel.createMessage {
                embed = rosterEmbed
            }.id.value
        } else {
            channel.getMessage(roster!!.sf).edit {
                embed = rosterEmbed
            }
        }
        saveEventToFile()
    }
}
