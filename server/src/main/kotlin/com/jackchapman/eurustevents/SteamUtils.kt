package com.jackchapman.eurustevents

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openid4java.consumer.ConsumerManager
import java.util.*
import java.util.regex.Pattern

object SteamUtils : KoinComponent {
    val config by inject<Config>()

    const val STEAM_OPENID = "https://steamcommunity.com/openid"
    val consumerManager = ConsumerManager().apply {
        maxAssocAttempts = 0
    }
    val discovered by lazy {
        consumerManager.associate(consumerManager.discover(STEAM_OPENID))
    }

    private const val STEAM_TOKEN = "61D32486638F2A784642783C6EE0F836"

    private val ID_URL = Pattern.compile("https?://steamcommunity.com/profiles/(\\d{17}).*")
    private val CUSTOM_URL = Pattern.compile("https?://steamcommunity.com/id/.+")
    private val httpClient = HttpClient {
        install(JsonFeature)
    }

    fun login(discordId: Long, responseToken: String): String {
        // todo remove on timer
        val randId = UUID.randomUUID().toString().replace("-", "")
        WebServer.steamLink[randId] = WebServer.LinkInfo(discordId, responseToken)
        return config.server.root + "/auth/$randId"
    }

    suspend fun getSteamProfileFromDiscord(discord: Long): SteamUserResponse? {
        return getSteamProfile(transaction {
            RustPlayers.select { RustPlayers.discordId eq discord }.map { it[RustPlayers.steamId] }.firstOrNull()?.toString()
        })
    }

    suspend fun getSteamProfile(steam: String?): SteamUserResponse? {
        if(steam == null) return null
        val id: Long = when {
            ID_URL.matcher(steam).matches() -> // Given URL with ID
                steam.substringAfter("profiles/").substringBefore("/").toLong()
            steam.toLongOrNull() != null -> // Given steam ID
                steam.toLong()
            else -> { // Make request to get steam id
                val vanity: String =
                    if (CUSTOM_URL.matcher(steam).matches()) steam.substringAfter("id/")
                        .substringBefore("/") else steam
                val resp =
                    httpClient.get<VanityDTO?>("https://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=$STEAM_TOKEN&vanityurl=$vanity")
                resp?.response?.steamid?.toLong() ?: throw IllegalArgumentException()
            }
        }

        val verify =
            httpClient.get<SummaryDTO?>("https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=$STEAM_TOKEN&steamids=$id")?.response

        if (verify == null || verify.players.isEmpty()) throw IllegalArgumentException()

        return verify.players[0]
    }

}

data class VanityDTO(val response: VanityUrlResponse)
data class VanityUrlResponse(val steamid: String, val success: Int)

data class SummaryDTO(val response: PlayerSummaryResponse)
data class PlayerSummaryResponse(val players: List<SteamUserResponse>)

data class SteamUserResponse(
    val steamid: String,
    val personaname: String,
    val profileurl: String,
    val avatarfull: String,
)
