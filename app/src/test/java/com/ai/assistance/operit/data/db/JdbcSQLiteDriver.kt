package com.ai.assistance.operit.data.db

import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import java.sql.Connection as JdbcConnection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.Types

/**
 * A pure-JVM SQLite driver for unit tests: implements the androidx.sqlite KMP
 * [SQLiteDriver]/[SQLiteConnection]/[SQLiteStatement] interfaces on top of
 * org.xerial:sqlite-jdbc, so the database implementations that Android Room 2.8 generates
 * (kapt output) can be opened, migrated and read against a real database on the JVM.
 *
 * Tests only; the running app still uses the Android platform driver.
 *
 * Conventions:
 * - Column indices match androidx native (sqlite3 C API), starting at 0; JDBC starts at 1,
 *   so 1 is added internally.
 * - Transaction commands (BEGIN/END/ROLLBACK/SAVEPOINT/RELEASE) are translated at the JDBC
 *   layer to avoid sqlite-jdbc's internal state conflicts with explicit transaction SQL.
 * - Statements without a result set (DDL, PRAGMA assignments and so on) make sqlite-jdbc's
 *   executeQuery throw "query does not return ResultSet"; that is treated as step() returning
 *   false, while other SQLExceptions propagate.
 */
class JdbcSQLiteDriver : SQLiteDriver {
    override fun open(fileName: String): SQLiteConnection = JdbcSQLiteConnection(fileName)
}

class JdbcSQLiteConnection(fileName: String) : SQLiteConnection {

    private val connection: JdbcConnection =
        DriverManager.getConnection("jdbc:sqlite:$fileName").apply {
            // Room's connection pool opens several connections on the same file; on Windows
            // sqlite-jdbc's journal file deletion (SQLITE_IOERR_DELETE) can be briefly locked by
            // other connections or an antivirus. In-memory journal mode avoids journal files
            // entirely (tests only, no crash recovery is needed).
            createStatement().use { it.execute("PRAGMA journal_mode = MEMORY") }
        }

    override fun prepare(sql: String): SQLiteStatement {
        val trimmed = sql.trim()
        return when {
            trimmed.startsWith("BEGIN ") ->
                TransactionStatement { beginJdbcTransaction() }
            trimmed == "END TRANSACTION" ->
                TransactionStatement { endJdbcTransaction(commit = true) }
            trimmed.startsWith("ROLLBACK TRANSACTION TO SAVEPOINT") ||
                trimmed.startsWith("SAVEPOINT ") ||
                trimmed.startsWith("RELEASE SAVEPOINT ") ->
                // Room's connection pool implements nested transactions through SAVEPOINT
                // (statements like `SAVEPOINT '1'`): execute them verbatim and do not split the
                // name; sqlite-jdbc's JDBC Savepoint API has escaping problems.
                TransactionStatement { executeRawSql(trimmed) }
            trimmed == "ROLLBACK TRANSACTION" ->
                TransactionStatement { endJdbcTransaction(commit = false) }
            else -> JdbcSQLiteStatement(connection.prepareStatement(sql))
        }
    }

    override fun inTransaction(): Boolean = !connection.autoCommit

    override fun close() {
        connection.close()
    }

    private fun executeRawSql(sql: String) {
        try {
            connection.createStatement().use { it.execute(sql) }
        } catch (e: Exception) {
            throw IllegalStateException("raw sql failed: [$sql]", e)
        }
    }

    private fun beginJdbcTransaction() {
        if (connection.autoCommit) {
            connection.autoCommit = false
        }
    }

    private fun endJdbcTransaction(commit: Boolean) {
        if (connection.autoCommit) return
        if (commit) connection.commit() else connection.rollback()
        connection.autoCommit = true
    }

    private class TransactionStatement(private val action: () -> Unit) : SQLiteStatement {
        override fun bindBlob(index: Int, value: ByteArray) = Unit
        override fun bindDouble(index: Int, value: Double) = Unit
        override fun bindLong(index: Int, value: Long) = Unit
        override fun bindText(index: Int, value: String) = Unit
        override fun bindNull(index: Int) = Unit
        override fun getBlob(index: Int): ByteArray = ByteArray(0)
        override fun getDouble(index: Int): Double = 0.0
        override fun getLong(index: Int): Long = 0L
        override fun getText(index: Int): String = ""
        override fun isNull(index: Int): Boolean = true
        override fun getColumnCount(): Int = 0
        override fun getColumnName(index: Int): String = ""
        override fun getColumnType(index: Int): Int = SQLITE_DATA_NULL
        override fun step(): Boolean {
            action()
            return false
        }
        override fun reset() = Unit
        override fun clearBindings() = Unit
        override fun close() = Unit
    }
}

private class JdbcSQLiteStatement(
    private val statement: PreparedStatement,
) : SQLiteStatement {

    private var resultSet: ResultSet? = null
    private var executed = false

    override fun bindBlob(index: Int, value: ByteArray) = statement.setBytes(index, value)

    override fun bindDouble(index: Int, value: Double) = statement.setDouble(index, value)

    override fun bindLong(index: Int, value: Long) = statement.setLong(index, value)

    override fun bindText(index: Int, value: String) = statement.setString(index, value)

    override fun bindNull(index: Int) = statement.setNull(index, Types.NULL)

    override fun getBlob(index: Int): ByteArray = resultSetOrThrow().getBytes(index + 1) ?: ByteArray(0)

    override fun getDouble(index: Int): Double = resultSetOrThrow().getDouble(index + 1)

    override fun getLong(index: Int): Long = resultSetOrThrow().getLong(index + 1)

    override fun getText(index: Int): String = resultSetOrThrow().getString(index + 1)

    override fun isNull(index: Int): Boolean = resultSetOrThrow().getObject(index + 1) == null

    override fun getColumnCount(): Int = metadataOrNull()?.columnCount ?: 0

    override fun getColumnName(index: Int): String =
        metadataOrNull()?.getColumnName(index + 1) ?: ""

    override fun getColumnType(index: Int): Int {
        val meta = metadataOrNull() ?: return SQLITE_DATA_NULL
        return when (meta.getColumnType(index + 1)) {
            Types.INTEGER, Types.SMALLINT, Types.TINYINT, Types.BIGINT, Types.BIT, Types.BOOLEAN ->
                SQLITE_DATA_INTEGER
            Types.REAL, Types.FLOAT, Types.DOUBLE, Types.NUMERIC, Types.DECIMAL ->
                SQLITE_DATA_FLOAT
            Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY ->
                SQLITE_DATA_BLOB
            Types.NULL ->
                SQLITE_DATA_NULL
            else ->
                SQLITE_DATA_TEXT
        }
    }

    override fun step(): Boolean {
        ensureExecuted()
        return resultSet?.next() ?: false
    }

    /**
     * Execute the query as early as possible: Room-generated code reads column metadata
     * (getColumnIndexOrThrow) before step(), and sqlite-jdbc's PreparedStatement.getMetaData()
     * opens another statement on the same connection, which causes "inconsistent internal
     * state". Metadata access therefore triggers the real execution, and step() afterwards
     * reads rows straight from the result set.
     */
    private fun ensureExecuted() {
        if (executed) return
        executed = true
        try {
            resultSet = statement.executeQuery()
        } catch (e: SQLException) {
            // sqlite-jdbc's executeQuery throws for statements without a result set (DDL,
            // PRAGMA assignments, INSERT/UPDATE/DELETE and so on) and does **not** execute the
            // statement; execute() is used here to actually run it (the message wording varies
            // between versions).
            val message = e.message.orEmpty()
            if (message.contains("does not return", ignoreCase = true)) {
                statement.execute()
            } else {
                throw e
            }
        }
    }

    override fun reset() {
        resultSet?.close()
        resultSet = null
        executed = false
    }

    override fun clearBindings() {
        statement.clearParameters()
    }

    override fun close() {
        resultSet?.close()
        statement.close()
    }

    private fun resultSetOrThrow(): ResultSet =
        resultSet ?: throw IllegalStateException("statement has not been stepped")

    /** Column metadata: use the result set metadata when executed, otherwise execute first. */
    private fun metadataOrNull(): ResultSetMetaData? {
        if (resultSet == null) {
            ensureExecuted()
        }
        return runCatching { resultSet?.metaData }.getOrNull()
    }
}
