package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.getWhitelist
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message

object WhitelistCommand : Command {
    override val guildOnly: Boolean
        get() = true
    override val eventOnly: Boolean
        get() = true
    override val adminOnly: Boolean
        get() = true

    override suspend fun execute(message: Message) {
        message.reply {
            addFile("whitelist.json", getWhitelist())
        }
    }
}