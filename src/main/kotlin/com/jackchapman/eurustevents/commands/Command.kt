package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.PREFIX
import dev.kord.core.entity.Message

interface Command {
    val name: String
        get() = this::class.simpleName!!.substringBefore("Command").toLowerCase()
    val requiredArguments: String
        get() = ""
    val guildOnly: Boolean
        get() = true
    val eventOnly: Boolean
        get() = false
    val adminOnly:  Boolean
        get() = false

    suspend fun execute(message: Message)

    val format: String
        get() = "$PREFIX$name $requiredArguments"
}

val Message.args: List<String>
    get() = content.split(" ").drop(1)