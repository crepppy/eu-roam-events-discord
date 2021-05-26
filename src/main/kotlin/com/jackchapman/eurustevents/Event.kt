package com.jackchapman.eurustevents

import dev.kord.common.Color
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
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
    saved: Boolean = false,
    vipNotice: Long = 2 * 60 * 60,
    boosterNotice: Long = 5 * 60
) : KoinComponent {
    enum class Exclusivity {
        EVERYONE, BOOSTER, VIP;
    }

    constructor(
        teamSize: Int,
        maxTeams: Int,
        roster: Long,
        startDate: Long,
        signups: Exclusivity,
        teams: MutableList<Team>
    ) : this(teamSize, maxTeams, true) {
        this.roster = roster
        this.startDate = startDate
        this.signups = signups
        this.teams.addAll(teams)
    }

    @delegate:Transient
    private val client by inject<Kord>()
    var roster: Long? = null
    var startDate = -1L
    var signups = Exclusivity.VIP
    val teams = mutableListOf<Team>() // Discord ID

    val rosterEmbed: EmbedBuilder
        get() = EmbedBuilder().apply {
            title = "Roam Events Roster"
            teams.forEach { team ->
                field(team.name, true) { team.allMembers.joinToString("\n") { "<@$it>" } }
            }
            description = "Make sure your steam is up to date by doing `!getsteam`"
        }

    init {
        if (!saved) {
            runBlocking {
                sendAnnouncement()
            }
            val timer = Timer()
            timer.schedule((vipNotice - boosterNotice) * 1000) {
                signups = Exclusivity.BOOSTER
                runBlocking {
                    sendAnnouncement()
                }
            }
            timer.schedule(vipNotice * 1000) {
                signups = Exclusivity.EVERYONE
                runBlocking {
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
                    "@everyone Signups have now started for Sunday's event at 6pm GMT. Teams are chosen on a first come first served basis. Use `!team` in a commands channel to sign up. If you need to substitute a member, you can do so by running `!sub`"
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
