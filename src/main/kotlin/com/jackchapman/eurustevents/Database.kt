package com.jackchapman.eurustevents

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

object RustPlayers : Table() {
    val discordId = long("discord_id")
    val steamId = long("steam_id")

    override val primaryKey = PrimaryKey(discordId)

    fun getLinked(ids: Collection<Long>): List<RustPlayer> {
        return transaction {
            RustPlayers.select {
                discordId inList (ids)
            }.map { RustPlayer(it[discordId], it[steamId]) }
        }
    }
}

data class RustPlayer(val discordId: Long, val steamId: Long)

// https://github.com/JetBrains/Exposed/issues/167
fun <T : Table> T.insertOrUpdate(vararg onDuplicateUpdateKeys: Column<*>, body: T.(InsertStatement<Number>) -> Unit) =
    InsertOrUpdate<Number>(onDuplicateUpdateKeys, this).apply {
        body(this)
        execute(TransactionManager.current())
    }

class InsertOrUpdate<Key : Any>(
    private val onDuplicateUpdateKeys: Array<out Column<*>>,
    table: Table,
    isIgnore: Boolean = true
) : InsertStatement<Key>(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String {
        val onUpdateSQL = if (onDuplicateUpdateKeys.isNotEmpty()) {
            " ON DUPLICATE KEY UPDATE " + onDuplicateUpdateKeys.joinToString {
                "${transaction.identity(it)}=VALUES(${
                    transaction.identity(
                        it
                    )
                })"
            }
        } else ""
        return super.prepareSQL(transaction) + onUpdateSQL
    }
}