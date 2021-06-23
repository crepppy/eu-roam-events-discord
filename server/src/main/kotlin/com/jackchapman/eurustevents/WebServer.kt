package com.jackchapman.eurustevents

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jackchapman.eurustevents.commands.Command
import dev.kord.core.Kord
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.EphemeralInteractionResponseBehavior
import dev.kord.core.behavior.interaction.followUpPublic
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.exception.EntityNotFoundException
import io.ktor.application.*
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
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


object WebServer : KoinComponent {
    data class LinkInfo(val id: Long, val token: String)

    private val config by inject<Config>()
    private val client by inject<Kord>()

    private fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential {
        // Load client secrets.
        val inp: InputStream = WebServer::class.java.classLoader.getResourceAsStream("credentials.json")
            ?: throw FileNotFoundException("Credential file not found")
        val clientSecrets = GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), InputStreamReader(inp))

        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT,
            JacksonFactory.getDefaultInstance(),
            clientSecrets,
            listOf(SheetsScopes.SPREADSHEETS, SheetsScopes.DRIVE_FILE, SheetsScopes.DRIVE)
        ).setDataStoreFactory(FileDataStoreFactory(File("tokens")))
            .setAccessType("offline")
            .build()
        val receiver = LocalServerReceiver.Builder().setHost("127.0.0.1").setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    val flow = MutableSharedFlow<SSEEvent>()
    val steamLink = mutableMapOf<String, LinkInfo>()
    fun run(port: Int = 80) {
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) { gson() }
            install(WebSockets)

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
                    post("/event") { // Starts the event that was in signup phase
                        if (SignupManager.currentEvent == null || SignupManager.currentEvent?.startDate != -1L) {
                            call.respond(HttpStatusCode.Forbidden)
                            return@post
                        }

                        val fileLocation = "***REMOVED***/oxide/config/SimpleWhitelist.json"
                        val conn =
                            URL("***REMOVED***/$fileLocation;type=i").openConnection()
                        conn.getOutputStream().use {
                            getWhitelist().copyTo(it)
                        }

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
                        guild.getChannelOf<GuildMessageChannel>(CHANNEL_ROSTER.sf).getMessage(event.roster!!.sf).delete()
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

                        withContext(Dispatchers.IO) {
                            val gson = Gson()
                            val fileLocation = "***REMOVED***/oxide/data/KDRData.json"
                            val conn = URL("***REMOVED***/$fileLocation").openConnection()
                            val data = conn.getInputStream().use { stream ->
                                stream.bufferedReader().use { reader ->
                                    gson.fromJson(
                                        reader.readText(),
                                        JsonObject::class.java
                                    )["Players"].asJsonArray.map { gson.fromJson(it.asJsonObject, KDData::class.java) }
                                        .associateBy { it.id }
                                }
                            }

                            // Build a new authorized API client service.
                            val transport = GoogleNetHttpTransport.newTrustedTransport()
                            val service =
                                Sheets.Builder(transport, JacksonFactory.getDefaultInstance(), getCredentials(transport))
                                    .setApplicationName("EventStatSheets")
                                    .build()

                            val time = LocalDateTime.ofEpochSecond(event.startDate, 0, ZoneOffset.UTC)
                            val spreadsheet = Spreadsheet().setProperties(
                                SpreadsheetProperties().setTitle(
                                    "Event - ${time.format(DateTimeFormatter.ISO_DATE_TIME)}"
                                )
                            )
                            val spread = service.spreadsheets().create(spreadsheet).setFields("spreadsheetId").execute()
                            val headers = listOf(
                                "Team",
                                "Name",
                                "Steam64ID",
                                "Discord ID",
                                "Kills",
                                "Team's Kills",
                                "Team's AKs",
                                "Total Team Points"
                            )
                            val values = mutableListOf(headers)
                            var currentRow = 2
                            val allMember = SignupManager.currentEvent!!.teams.flatMap { it.allMembers }.toSet()
                            val steamPlayers = transaction {
                                RustPlayers.select {
                                    RustPlayers.discordId inList allMember
                                }.associate { it[RustPlayers.discordId] to it[RustPlayers.steamId] }
                            }
                            event.teams.forEach { team ->
                                team.allMembers.forEach {
                                    values.add(
                                        listOf(
                                            team.name,
                                            client.getGuild(GUILD_EURE.sf)!!.getMemberOrNull(it.sf)?.displayName ?: "Not In Server",
                                            steamPlayers[it].toString(),
                                            it.toString(),
                                            (data[steamPlayers[it]]?.kills ?: 0).toString(),
                                            *("" * 3)
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
                            val valueRange = ValueRange().setValues(values.toList())
                            val winningTeams = ValueRange().setValues(
                                listOf(listOf("Winning Teams"),
                                    listOf("=SORTN(A2:A,${event.teamSize},FALSE, H2:H,FALSE)"))
                            )
                            service.spreadsheets().values().append(spread.spreadsheetId, "A1:H", valueRange)
                                .setValueInputOption("USER_ENTERED").execute()

                            service.spreadsheets().values().append(spread.spreadsheetId, "J1:J2", winningTeams)
                                .setValueInputOption("USER_ENTERED").execute()

                            val autoWidth = Request().setAutoResizeDimensions(
                                AutoResizeDimensionsRequest().setDimensions(
                                    DimensionRange().setDimension("A1:H")
                                )
                            )

                            val backgroundFormat = Request().setAddConditionalFormatRule(
                                AddConditionalFormatRuleRequest().setRule(
                                    ConditionalFormatRule().setRanges(
                                        listOf(
                                            GridRange().setStartColumnIndex(0).setEndColumnIndex(1).setStartRowIndex(0).setEndRowIndex(currentRow)
                                        )
                                    ).setBooleanRule(
                                        BooleanRule().setCondition(
                                            BooleanCondition().setType("TEXT_EQ")
                                                .setValues(listOf(ConditionValue().setUserEnteredValue("Team")))
                                        ).setFormat(
                                            CellFormat().setBackgroundColor(
                                                Color().setRed(0.6431372549F).setGreen(0.76078431372F).setBlue(0.95686274509F)
                                            )
                                        )
                                    )
                                ).setIndex(0)
                            )

                            val boldHeadings = Request().setAddConditionalFormatRule(
                                AddConditionalFormatRuleRequest().setRule(
                                    ConditionalFormatRule().setRanges(
                                        listOf(
                                            GridRange().setStartRowIndex(0).setEndRowIndex(1)
                                        )
                                    ).setBooleanRule(
                                        BooleanRule().setCondition(
                                            BooleanCondition().setType("NOT_BLANK").setValues(emptyList())
                                        ).setFormat(
                                            CellFormat().setTextFormat(TextFormat().setBold(true))
                                        )
                                    )
                                ).setIndex(1)
                            )
                            service.spreadsheets().batchUpdate(
                                spread.spreadsheetId,
                                BatchUpdateSpreadsheetRequest().setRequests(
                                    listOf(
//                                    autoWidth,
                                        backgroundFormat,
                                        boldHeadings
                                    )
                                )
                            ).execute()
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
                        
                        val teamStr = event.teams.joinToString("\n") { it.allMembers.joinToString(" ") + " " + it.name }
                        send("${event.teamSize}\n$teamStr")
                        incoming.consumeAsFlow().mapNotNull { it as? Frame.Text }.collect { frame ->
                            // player
                            // kill / depo
                            // player / amount
                            val (teamName, op, option) = frame.readText().split("\n", limit = 3)
                            val team = event.teams.find { it.name == teamName } ?: return@collect

                            if (team !in event.scores) event.scores[team] = GameScore(0, 0)
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