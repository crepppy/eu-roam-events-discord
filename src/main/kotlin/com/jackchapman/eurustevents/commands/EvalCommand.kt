package com.jackchapman.eurustevents.commands

//object EvalCommand : Command {
//    private val codePattern = Pattern.compile("```.*\\n([\\s\\S]+)```")
//    override val admin: Boolean
//        get() = true
//    override val description: String
//        get() = "Evaluates the code sent"
//
//    override suspend fun execute(interaction: CommandInteraction) {
//        val code: String
//        if (message.attachments.isNotEmpty()) {
//            val url = message.attachments.first().url
//            URL(url).openConnection().getInputStream().use { stream ->
//                stream.bufferedReader().use { reader ->
//                    code = reader.readText()
//                }
//            }
//        } else {
//            val possibleCode = message.content.substringAfter(" ")
//            val matcher = codePattern.matcher(possibleCode)
//            code = if (matcher.find()) matcher.group(1) else possibleCode
//        }
//
//        val engine = ScriptEngineManager().getEngineByExtension("kts")!!
//        engine.put("signups", SignupManager)
//        engine.put("guild", message.getGuild())
//        engine.put("msg", message)
//
//        val imports = Regex("(import .+\\n?)+").find(code)?.value ?: ""
//
//        val value = engine.eval("""
//            import kotlinx.coroutines.runBlocking
//            import dev.kord.core.*
//            import dev.kord.common.*
//            $imports
//            
//            runBlocking {
//                ${code.substringAfter(imports)}
//            }
//        """.trimIndent())?.toString() ?: "Code ran without return"
//
//        message.reply {
//            content = value
//        }
//    }
//}