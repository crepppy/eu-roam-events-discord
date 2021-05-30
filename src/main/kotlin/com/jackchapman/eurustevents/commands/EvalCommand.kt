package com.jackchapman.eurustevents.commands

import com.jackchapman.eurustevents.SignupManager
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import java.io.StringWriter
import java.net.URL
import java.util.regex.Pattern
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import kotlin.system.measureTimeMillis

object EvalCommand : Command {
    private val codePattern = Pattern.compile("```.*\\n([\\s\\S]+)```")
    override val adminOnly: Boolean
        get() = true

    override suspend fun execute(message: Message) {
        val code: String
        if (message.attachments.isNotEmpty()) {
            val url = message.attachments.first().url
            URL(url).openConnection().getInputStream().use { stream ->
                stream.bufferedReader().use { reader ->
                    code = reader.readText()
                }
            }
        } else {
            val possibleCode = message.content.substringAfter(" ")
            val matcher = codePattern.matcher(possibleCode)
            code = if (matcher.find()) matcher.group(1) else possibleCode
        }

        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
        engine.put("signups", SignupManager)
        engine.put("guild", message.getGuildOrNull())
        engine.put("msg", message)

        val value = engine.eval("""
            import kotlinx.coroutines.runBlocking
            import dev.kord.core.*
            import dev.kord.common.*

            runBlocking {
                $code
            }
        """.trimIndent())?.toString() ?: "Code ran without return"

        message.reply {
            content = value
        }
    }
}