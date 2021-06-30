package com.jackchapman.eurustevents

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jackchapman.eurustevents.commands.Command
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.EphemeralInteractionResponseBehavior
import dev.kord.core.behavior.interaction.followUpPublic
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.exception.EntityNotFoundException
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.html.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


object WebServer : KoinComponent {
    data class LinkInfo(val id: Long, val token: String)

    private val config by inject<Config>()
    private val client by inject<Kord>()

    val flow = MutableSharedFlow<SSEEvent>()
    val steamLink = mutableMapOf<String, LinkInfo>()
    fun run(port: Int = 80) {
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) { gson() }
            install(WebSockets)
            install(AutoHeadResponse)
            install(Authentication) {
                basic("auth") {
                    realm = "Access event admin endpoints"
                    validate { credentials ->
                        if (credentials.password == config.server.password) UserIdPrincipal(credentials.name)
                        else null
                    }
                }
            }

            routing {
                route("/api") {
                    get("/auth/{user}") {
                        if ("openid.claimed_id" in call.request.queryParameters) {
                            // Returned from steam auth, link user and steam id
                            val (discordId, token) = steamLink[call.request.path().substringAfter("auth/")]!!
                            val steamId =
                                call.request.queryParameters["openid.claimed_id"]?.substringAfterLast("id/")?.toLongOrNull()
                            if (steamId == null) {
                                call.respond(HttpStatusCode.BadRequest)
                                return@get
                            }
                            transaction {
                                RustPlayers.insertOrUpdate(RustPlayers.steamId) {
                                    it[RustPlayers.discordId] = discordId
                                    it[RustPlayers.steamId] = steamId
                                }
                            }
                            call.respondHtml {
                                head {
                                    title { +"Authorized!" }
                                }
                                body {
                                    h1 { +"Authorized!" }
                                    p { +"You can now close this tab" }
                                }
                            }
                            EphemeralInteractionResponseBehavior(client.selfId, token, client).followUpPublic {
                                content = """
                                    **:white_check_mark: Successfully linked!**
                                    https://steamcommunity.com/profiles/$steamId
                                """.trimIndent()
                            }
                        } else {
                            // Redirect user to the authentication page
                            call.respondRedirect(
                                SteamUtils.consumerManager.authenticate(
                                    SteamUtils.discovered,
                                    config.server.root + call.request.path()
                                ).getDestinationUrl(true) ?: ""
                            )
                        }
                    }
                    authenticate("auth") {
                        post("/event") { // Starts the event that was in signup phase
                            if (SignupManager.currentEvent == null || SignupManager.currentEvent?.startDate != -1L) {
                                call.respond(HttpStatusCode.Forbidden)
                                return@post
                            }

                            config.rust.ftp?.let { url ->
                                val conn = URL("${url}/oxide/config/SimpleWhitelist.json;type=i").openConnection()
                                conn.getOutputStream().use {
                                    getWhitelist().copyTo(it)
                                }
                            }
                            SignupManager.currentEvent!!.scores.putAll(SignupManager.currentEvent!!.teams.associateWith { GameScore() })
                            SignupManager.currentEvent!!.startDate =
                                LocalDateTime.now(ZoneId.of("GMT+0")).toEpochSecond(ZoneOffset.UTC)
                            saveEventToFile()
                        }
                        delete("/teams") {
                            val event = SignupManager.currentEvent
                            val guild = client.getGuild(GUILD_EURE.sf)!!
                            if (event == null) {
                                call.respond(HttpStatusCode.Forbidden)
                                return@delete
                            }

                            call.respond(HttpStatusCode.OK)

                            val time = LocalDateTime.ofEpochSecond(event.startDate, 0, ZoneOffset.UTC)
                            event.teams.forEach { team ->
                                try {
                                    team.delete(guild)
                                } catch (e: EntityNotFoundException) {
                                }
                            }
                            File("event.json").renameTo(File("event-${time.format(DateTimeFormatter.ISO_DATE_TIME)}"))
                            guild.getChannelOf<GuildMessageChannel>(CHANNEL_ROSTER.sf).getMessage(event.roster!!.sf)
                                .delete()
                            SignupManager.currentEvent = null
                        }
                        delete("/event") { //Ends the event
                            val event = SignupManager.currentEvent
                            if (event == null || event.startDate == -1L) {
                                call.respond(HttpStatusCode.Forbidden)
                                return@delete
                            }

                            call.respond(HttpStatusCode.OK)

                            client.slashCommands.getGuildApplicationCommands(GUILD_EURE.sf)
                                .filter { slashCmd -> slashCmd.name in Command.EVENT_COMMANDS.map { it.key } }
                                .collect { it.delete() }

                            if (config.rust.ftp == null) return@delete
                            withContext(Dispatchers.IO) {
                                val gson = Gson()
                                val conn = URL("${config.rust.ftp}/oxide/data/KDRData.json").openConnection()
                                val data = conn.getInputStream().use { stream ->
                                    stream.bufferedReader().use { reader ->
                                        gson.fromJson(
                                            reader.readText(),
                                            JsonObject::class.java
                                        )["Players"].asJsonArray.map {
                                            gson.fromJson(it.asJsonObject,
                                                KDData::class.java)
                                        }
                                            .associateBy { it.id }
                                    }
                                }

                                val headers = listOf(
                                    "Team",
                                    "Name",
                                    "Steam64ID",
                                    "Discord ID",
                                    "Kills",
                                    "Team's Kills",
                                    "Team's AKs",
                                    "Total Team Points",
                                    "",
                                    "Winning Teams",
                                )
                                val values = mutableListOf(headers)
                                var currentRow = 2
                                val allMember = SignupManager.currentEvent!!.teams.flatMap { it.allMembers }.toSet()
                                val steamPlayers = transaction {
                                    RustPlayers.select {
                                        RustPlayers.discordId inList allMember
                                    }.associate { it[RustPlayers.discordId] to it[RustPlayers.steamId] }
                                }
                                val guild = client.getGuild(GUILD_EURE.sf)!!
                                event.teams.forEach { team ->
                                    team.allMembers.forEach {
                                        values.add(
                                            listOf(
                                                team.name,
                                                guild.getMemberOrNull(it.sf)?.displayName
                                                    ?: "Not In Server",
                                                steamPlayers[it].toString(),
                                                it.toString(),
                                                (data[steamPlayers[it]]?.kills ?: 0).toString()
                                            )
                                        )
                                    }
                                    values.add(
                                        listOf(
                                            team.name,
                                            *("" * 4),
                                            "=SUM(E${currentRow.also { currentRow += team.members.size }}:E$currentRow)",
                                            "0",
                                            "=F${++currentRow}+(G$currentRow)/3"
                                        )
                                    )
                                    values.add(listOf())
                                    currentRow += 2
                                }
                                values[1] =
                                    values[1] + listOf(*("" * 4), "=SORTN(A2:A,${event.teamSize},FALSE,H2:H,FALSE)")
                                guild.getChannelOf<TextChannel>(CHANNEL_DEV.sf).createMessage {
                                    val curTime = LocalDateTime.ofEpochSecond(event.startDate, 0, ZoneOffset.UTC)
                                    addFile(
                                        "Event - ${curTime.format(DateTimeFormatter.ISO_DATE_TIME)}.csv",
                                        values.joinToString("\n") {
                                            it.joinToString(",") { f -> "\"$f\"" }
                                        }.byteInputStream()
                                    )
                                }
                            }
                        }
                        post("/signups") { // Starts new event
                            if (SignupManager.currentEvent != null) {
                                // todo 403 message
                                call.respond(HttpStatusCode.Forbidden)
                                return@post
                            }
                            val data = call.receive<SignupData>()
                            SignupManager.currentEvent = Event(
                                data.teamSize,
                                data.maxTeams,
                                vipNotice = 2 * 60 * 60,
                                boosterNotice = 5 * 60
                            )
                        }

                        webSocket("/game") {
                            val event = SignupManager.currentEvent
                            if (event == null || event.startDate == -1L) {
                                call.respond(HttpStatusCode.Forbidden)
                                return@webSocket
                            }

                            val teamStr =
                                event.teams.joinToString("\n") { it.allMembers.joinToString(" ") + " " + it.name }
                            send("${event.teamSize}\n$teamStr")
                            incoming.consumeAsFlow().mapNotNull { it as? Frame.Text }.collect { frame ->
                                // player
                                // kill / depo
                                // player / amount
                                val (teamName, op, option) = frame.readText().split("\n", limit = 3)
                                val team = event.teams.find { it.name == teamName } ?: return@collect

                                if (team !in event.scores) event.scores[team] = GameScore()
                                val score = event.scores[team]!!

                                val data = when (EventType.valueOf(op.uppercase())) {
                                    EventType.KILL -> {
                                        score.kills += 1
                                        val (killerName, killedName) = option.split(" ")
                                        """
                                        ${score.kills}
                                        $killerName
                                        $killedName
                                    """.trimIndent()
                                    }
                                    EventType.GUN -> {
                                        score.guns += option.toInt()
                                        """
                                        ${score.guns}
                                    """.trimIndent()
                                    }
                                }

                                flow.emit(SSEEvent(op + "\n" + team.name + "\n" + data))
                            }
                        }
                    }
                    get("/game") {
                        val event = SignupManager.currentEvent
                        if (event == null || event.startDate == -1L) {
                            call.respond(HttpStatusCode.Forbidden)
                            return@get
                        }
                        val sub = flow.produceIn(CoroutineScope(Dispatchers.Default))
                        call.response.cacheControl(CacheControl.NoCache(null))
                        try {
                            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                                for (team in event.teams) {
                                    write("data: team\n")
                                    write("data: ${team.name}\n")
                                    write("data: ${event.scores[team]?.kills ?: 0}\n")
                                    write("data: ${event.scores[team]?.guns ?: 0}\n")

                                    write("\n")
                                    flush()
                                }
                                sub.consumeEach { event ->
                                    if (event.id != null) {
                                        write("id: ${event.id}\n")
                                    }
                                    if (event.event != null) {
                                        write("event: ${event.event}\n")
                                    }
                                    for (dataLine in event.data.lines()) {
                                        write("data: $dataLine\n")
                                    }
                                    write("\n")
                                    flush()
                                }
                            }
                        } finally {
                            sub.cancel()
                        }
                    }
                }
                static("/") {
                    resources("dist")
                    defaultResource("dist/index.html")
                }
            }
        }.start()
    }
}

data class SignupData(val teamSize: Int, val maxTeams: Int)
data class KDData(val name: String, val id: Long, val kills: Int, val deaths: Int, val ratio: Float)

enum class EventType {
    KILL, GUN
}

data class SSEEvent(val data: String, val event: String? = null, val id: String? = null)