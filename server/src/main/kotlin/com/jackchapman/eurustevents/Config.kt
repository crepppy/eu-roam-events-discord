package com.jackchapman.eurustevents

data class DatabaseConfig(val host: String, val port: Int, val user: String, val pass: String, val database: String)
data class DiscordConfig(val token: String)
data class ServerConfig(
    val port: Int,
    val root: String = "http://127.0.0.1${if (port == 443 || port == 80) "" else ":$port"}",
    val password: String
)
data class RustConfig(val ftp: String? = null)
data class Config(val discord: DiscordConfig, val database: DatabaseConfig, val server: ServerConfig, val rust: RustConfig)
