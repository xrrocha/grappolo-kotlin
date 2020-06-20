package grappolo.util

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

class Database(jdbcTemplate: String, parameters: Map<String, Any?>) {

    companion object {
        private val STATEMENT_SEPARATOR_REGEX = "\\s*;(\\r?\\n)*".toRegex()
        private val PARAMETER_NAME_REGEX = """\{[_\p{Alpha}][_\p{Alnum}]*}""".toRegex()

        internal fun splitScript(script: String): List<String> =
                script
                        .trim()
                        .split(STATEMENT_SEPARATOR_REGEX)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
    }

    private val connection: Connection by lazy {
        val jdbcUrl = parameters.keys.fold(jdbcTemplate) { jdbcUrl, name ->
            jdbcUrl.replace("{$name}", parameters[name]?.toString() ?: "")
        }
        DriverManager.getConnection(jdbcUrl, Properties().apply {
            parameters.forEach { entry -> setProperty(entry.key, entry.value?.toString() ?: "") }
        })
    }

    fun close(): Unit = connection.close()

    sealed class StatementResult {
        data class UpdateStatementResult(val count: Int) : StatementResult()
        data class QueryStatementResult(val rows: Sequence<Map<String, Any?>>) : StatementResult()
    }

    fun executeScript(script: String, paramMap: Map<String, Any?> = mapOf()): List<Pair<String, StatementResult>> =
            splitScript(script).map { sqlCommand ->
                val (result, _) = execute(sqlCommand, paramMap)
                Pair(sqlCommand, result)
            }

    fun execute(sql: String, paramMap: Map<String, Any?> = mapOf()): Pair<StatementResult, PreparedStatement> {
        val (expandedSql, paramValues) = parseQuery(sql, paramMap)
        val statement = connection.prepareStatement(expandedSql)
        paramValues.withIndex().forEach { (index, paramValue) -> statement.setObject(index + 1, paramValue) }
        val result =
                if (statement.execute()) {
                    StatementResult.QueryStatementResult(traverseResultSet(statement.resultSet))
                } else {
                    StatementResult.UpdateStatementResult(statement.updateCount)
                }
        return Pair(result, statement)
    }

    fun executeQuery(sql: String, paramMap: Map<String, Any?> = mapOf()): Sequence<Map<String, Any?>> {
        val (expandedSql, paramValues) = parseQuery(sql, paramMap)
        val statement = connection.prepareStatement(expandedSql)
        paramValues.withIndex().forEach { (index, paramValue) -> statement.setObject(index + 1, paramValue) }
        val resultSet = statement.executeQuery()
        return traverseResultSet(resultSet)
    }

    private fun traverseResultSet(resultSet: ResultSet): Sequence<Map<String, Any?>> {
        val metaData = resultSet.metaData
        return generateSequence {
            if (!resultSet.next()) {
                null
            } else {
                (1..metaData.columnCount).toList().map { index ->
                    metaData.getColumnName(index) to resultSet.getObject(index)
                }
                        .toMap()
            }
        }
    }

    fun insert(tableName: String, columns: Map<String, Any?>): Int {
        val columnList = columns.toList().sortedBy { it.first }
        val sql = """
          INSERT INTO $tableName(${columns.toList().joinToString(", ") { it.first }})
          VALUES(${columnList.joinToString(", ") { "{${it.first}}" }})
        """
        return executeUpdate(sql, columns)
    }

    fun update(
            tableName: String,
            keyColumn: String,
            column: Pair<String, Any>,
            vararg columns: Pair<String, Any>
    ): Int {
        val allColumns = listOf(column) + columns
        require(allColumns.map { it.first }.contains(keyColumn)) { "No value provided for key $keyColumn" }
        val sql = """
          UPDATE $tableName
          SET    ${allColumns.joinToString(", ") { "${it.first} = {${it.first}}" }}
          WHERE  $keyColumn = {$keyColumn}
        """
        return executeUpdate(sql, allColumns.toMap())
    }

    fun update(tableName: String, keyColumn: String, keyValue: Any): Int {
        val sql = """
          DELETE $tableName
          WHERE  $keyColumn = {$keyValue}
        """
        return executeUpdate(sql, mapOf(keyColumn to keyValue))
    }

    private fun executeUpdate(sql: String, paramMap: Map<String, Any?> = mapOf()): Int {
        val statement = prepareStatement(sql, paramMap)
        return statement.executeUpdate()
    }

    private fun prepareStatement(sql: String, paramMap: Map<String, Any?> = mapOf()): PreparedStatement {

        val (expandedSql, paramValues) = parseQuery(sql, paramMap)
        val statement = connection.prepareStatement(expandedSql)
        paramValues
                .withIndex()
                .forEach { (index, paramValue) ->
                    statement.setObject(index + 1, paramValue)
                }

        return statement
    }

    private fun parseQuery(sql: String, params: Map<String, Any?>): Pair<String, List<Any?>> {
        val expandedSql = PARAMETER_NAME_REGEX.replace(sql, "?")
        val paramList =
                PARAMETER_NAME_REGEX.findAll(sql)
                        .map { with(it.value) { substring(1, length - 1) } }
                        .map { paramName -> params[paramName] }
                        .toList()
        return Pair(expandedSql, paramList)
    }
}



