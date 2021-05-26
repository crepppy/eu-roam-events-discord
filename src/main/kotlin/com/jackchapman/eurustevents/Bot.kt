package com.jackchapman.eurustevents

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.jackchapman.eurustevents.commands.Command
import com.sksamuel.hoplite.ConfigLoader
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.live.live
import dev.kord.core.live.onReactionAdd
import dev.kord.core.on
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.x.emoji.Emojis
import dev.kord.x.emoji.addReaction
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import java.io.ByteArrayInputStream
import java.io.File
import java.util.regex.Pattern

const val PREFIX = "!"

const val CHANNEL_DEV = 830202318154104853L
const val CHANNEL_EVENT_CHAT = 824619878920224808L
const val CHANNEL_ROSTER = 823168748487573525L
const val CHANNEL_ANNOUNCEMENTS = 769951838811455500L
const val CHANNEL_SIGNUPS = 769955313725997086L
const val CHANNEL_VIP = 788150240321994839L
const val CHANNEL_BOOSTER = 783650182251806720L

const val ROLE_VIP = 775402668637290508L
const val ROLE_BOOSTER = 770040521669345392L
const val ROLE_MANAGER = 769948301709279264L
const val ROLE_REPRESENTATIVE = 778796138505044030L

const val CATEGORY_EVENT = 769954049474166784L
const val GUILD_EURE = 769946284970606622L

val PING_REGEX: Pattern = Pattern.compile("<@!?(\\d+)>")

suspend fun main() {
    val config = ConfigLoader().loadConfigOrThrow<Config>(File("config.toml"))
    val client = Kord(config.discord.token)

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

    val packageName = Command::class.qualifiedName!!.split(".").dropLast(1).joinToString(".")
    val reflections = Reflections(packageName, SubTypesScanner())
    val commands = reflections.getSubTypesOf(Command::class.java).map { it.kotlin.objectInstance!! }
        .associateBy { it.name.toLowerCase() }

    WebServer.run(config.server.port)

    client.on<MessageCreateEvent> {
        if (message.author == null || message.author?.isBot == true) return@on
        if (System.getenv("EURE_DEBUG") != null && message.channelId.value != CHANNEL_DEV) return@on
        if (!message.content.startsWith(PREFIX)) return@on
        val command = commands[message.content.drop(1).split(' ', '\n', limit = 2)[0].toLowerCase()] ?: return@on
        if (command.guildOnly && message.getGuildOrNull() == null) return@on
        if (command.adminOnly && message.getAuthorAsMember()?.isManager() != true) {
            message.replyError {
                title = "You cannot run this command"
                description = "This command is restricted to Manager role"
            }
            return@on
        }
        if (command.eventOnly && SignupManager.currentEvent == null) {
            message.replyError {
                title = "No event running"
                description =
                    "We run events every Sunday. Announcements are made every Wednesday in <#$CHANNEL_SIGNUPS>."
            }
            return@on
        }
        try {
            command.execute(message)
        } catch (e: IllegalArgumentException) {
            message.replyError {
                title = "Incorrect format"
                description = """
                    Correct Format:
                    `${command.format}`
                """.trimIndent()
            }
        }
    }

    val curEvent = File("event.json")
    if (curEvent.exists()) {
        val gson = Gson()
        val event = gson.fromJson(curEvent.readText(), SaveEvent::class.java)
        SignupManager.currentEvent = Event(
            event.teamSize, event.maxTeams, event.roster, event.startDate, event.signups, event.teams
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

suspend fun Message.replyError(builder: EmbedBuilder.() -> Unit) {
    reply {
        embed = EmbedBuilder().apply {
            color = Color(0xFF0000)
            builder(this)
        }
    }
}

suspend fun Message.confirm(id: Long, run: suspend Message.(ReactionAddEvent) -> Unit): Message {
    addReaction(Emojis.whiteCheckMark)
    addReaction(Emojis.x)
    live().onReactionAdd { event ->
        if (event.userId.value == id) {
            when (event.emoji.name) {
                Emojis.whiteCheckMark.unicode -> run(event)
                Emojis.x.unicode -> delete()
            }
        }
    }
    return this
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