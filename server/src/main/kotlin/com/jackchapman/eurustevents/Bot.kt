package com.jackchapman.eurustevents

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.jackchapman.eurustevents.commands.Command
import com.sksamuel.hoplite.ConfigLoader
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.InteractionType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.followUp
import dev.kord.core.entity.Member
import dev.kord.core.entity.interaction.CommandInteraction
import dev.kord.core.entity.interaction.ComponentInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.ByteArrayInputStream
import java.io.File

const val CHANNEL_EVENT_CHAT = 824619878920224808L
const val CHANNEL_ROSTER = 823168748487573525L
const val CHANNEL_SIGNUPS = 769955313725997086L
const val CHANNEL_VIP = 788150240321994839L
const val CHANNEL_BOOSTER = 783650182251806720L

const val ROLE_VIP = 775402668637290508L
const val ROLE_BOOSTER = 770040521669345392L
const val ROLE_MANAGER = 769948301709279264L
const val ROLE_REPRESENTATIVE = 778796138505044030L

const val GUILD_EURE = 769946284970606622L

@KordPreview
suspend fun main() {
    val config = ConfigLoader().loadConfigOrThrow<Config>(File("../config.toml"))
    val client = Kord(config.discord.token)
    val debug = System.getenv("EURE_DEBUG") != null

    Database.connect(HikariDataSource(HikariConfig().apply {
        config.database.apply {
            jdbcUrl = "jdbc:mysql://$host:$port/$database"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = user
            password = pass
        }
    }))

    transaction {
        SchemaUtils.createMissingTablesAndColumns(RustPlayers)
    }


    val module = module {
        single { config }
        single { client }
    }

    startKoin {
        modules(module)
    }

    val slashCommands = client.slashCommands.createGuildApplicationCommands(GUILD_EURE.sf) {
        Command.GLOBAL_COMMANDS.values.forEach { cmd ->
            command(cmd.name, cmd.description) {
                defaultPermission = !cmd.admin
                cmd.requiredArguments(this)
            }
        }
    }.toList()

    client.slashCommands.bulkEditApplicationCommandPermissions(client.selfId, GUILD_EURE.sf) {
        slashCommands.filter { slashCmd -> slashCmd.name in Command.GLOBAL_COMMANDS.filter { it.value.admin } }
            .forEach {
                command(it.id) {
                    role(ROLE_MANAGER.sf)
                    user(139068524105564161.sf)
                }
            }
    }

    WebServer.run(config.server.port)

    client.on<InteractionCreateEvent> {
        if (interaction.type == InteractionType.ApplicationCommand) {
            if (debug && !interaction.user.asMember(GUILD_EURE.sf).isManager()) {
                interaction.acknowledgeEphemeral().followUp {
                    content = ":x: This feature is currently not enabled"
                }
            } else {
                Command.COMMANDS[interaction.data.data.name.value!!]?.execute(interaction as CommandInteraction)
            }
        } else if (interaction.type == InteractionType.Component) {
            val id = interaction.data.data.customId.value!!.split("_")
            if (id[0] == "steam") {
                val steam = id[1].toLong()
                val userId = interaction.user.id
                transaction {
                    RustPlayers.insertOrUpdate(RustPlayers.steamId) {
                        it[discordId] = userId.value
                        it[steamId] = steam
                    }
                }
                (interaction as ComponentInteraction).acknowledgePublicDeferredMessageUpdate().followUp {
                    content = """
                        **:white_check_mark: Successfully linked!**
                        https://steamcommunity.com/profiles/$steam
                    """.trimIndent()
                }

            }
        }
    }

    val curEvent = File("event.json")
    if (curEvent.exists()) {
        val gson = Gson()
        val event = gson.fromJson(curEvent.readText(), SaveEvent::class.java)
        SignupManager.currentEvent = Event(
            event.teamSize, event.maxTeams, event.roster, event.startDate, event.signups, event.teams, true
        )
    }

    client.login()
}

fun saveEventToFile() {
    if (SignupManager.currentEvent == null) return
    val gson = Gson()
    val json = gson.toJson(SignupManager.currentEvent)
    File("event.json").writeText(json)
}

fun Member.isManager(): Boolean = roleIds.contains(ROLE_MANAGER.sf) || id.value == 139068524105564161L

val Long.sf: Snowflake
    get() = Snowflake(this)

operator fun String.times(times: Int): Array<String> {
    return (1..times).map { this }.toTypedArray()
}

fun getWhitelist(): ByteArrayInputStream {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val admins = listOf(269820747252236289L)
    val whitelistedDiscord = (SignupManager.currentEvent!!.teams.flatMap { it.allMembers } + admins).toSet()
    return gson.toJson(JsonObject().apply {
        add(
            "Whitelisted", gson.toJsonTree(
                transaction {
                    RustPlayers.select {
                        RustPlayers.discordId inList whitelistedDiscord
                    }.map {
                        JsonObject().apply {
                            addProperty("name", "")
                            addProperty("steamId", it[RustPlayers.steamId].toString())
                        }
                    }
                }
            )
        )
    }).byteInputStream()
}