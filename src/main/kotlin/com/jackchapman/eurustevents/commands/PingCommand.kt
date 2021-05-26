package com.jackchapman.eurustevents.commands

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message

object PingCommand : Command {
    override suspend fun execute(message: Message) {
        message.reply {
            content = "Pong! :ping_pong:"
        }
    }
}