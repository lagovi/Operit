package com.ai.assistance.operit.data.mcp

import android.content.Context
import com.ai.assistance.operit.R
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Parsed standard MCP configuration used by both UI and market imports. */
internal data class McpConfigImport(
    val servers: List<McpImportedServer>
)

internal sealed interface McpImportedServer {
    val id: String
    val disabled: Boolean
}

internal data class StdioMcpImportedServer(
    override val id: String,
    val command: String,
    val args: List<String>,
    val env: Map<String, String>,
    val autoApprove: List<String>,
    override val disabled: Boolean
) : McpImportedServer

internal data class RemoteMcpImportedServer(
    override val id: String,
    val endpoint: String,
    val connectionType: String,
    val headers: Map<String, String>,
    override val disabled: Boolean
) : McpImportedServer

/**
 * Parses the public MCP configuration format without conflating HTTP transports with stdio.
 *
 * Fork: every rejection message used to be a hardcoded Chinese literal, and all
 * three call sites let the exception propagate to a user-visible error, so a
 * malformed config produced Chinese in an English UI. The messages are now
 * resources, which means a Context has to reach the parser rather than the
 * failure being reported as an opaque code.
 */
internal object McpConfigImportParser {
    private const val STREAMABLE_HTTP_TYPE = "streamable_http"
    private const val SSE_TYPE = "sse"
    private const val STDIO_TYPE = "stdio"

    fun parse(context: Context, jsonConfig: String): McpConfigImport {
        val root = try {
            JsonParser.parseString(jsonConfig)
        } catch (e: Exception) {
            throw IllegalArgumentException(context.getString(R.string.mcp_config_not_valid_json), e)
        }

        require(root.isJsonObject) { context.getString(R.string.mcp_config_root_must_be_object) }
        val mcpServers = root.asJsonObject.requiredObject(context, "mcpServers")
        require(mcpServers.entrySet().isNotEmpty()) { context.getString(R.string.mcp_config_servers_empty) }

        return McpConfigImport(
            servers = mcpServers.entrySet().map { (serverId, configElement) ->
                parseServer(context, serverId, configElement)
            }
        )
    }

    private fun parseServer(context: Context, serverId: String, configElement: JsonElement): McpImportedServer {
        require(serverId.isNotBlank()) { context.getString(R.string.mcp_config_empty_server_id) }
        require(configElement.isJsonObject) { context.getString(R.string.mcp_config_server_must_be_object, serverId) }

        val config = configElement.asJsonObject
        val declaredType = config.optionalString(context, "type")
        return if (config.has("command")) {
            parseStdioServer(context, serverId, config, declaredType)
        } else {
            parseRemoteServer(context, serverId, config, declaredType)
        }
    }

    private fun parseStdioServer(context: Context, serverId: String,
        config: JsonObject,
        declaredType: String?
    ): StdioMcpImportedServer {
        require(declaredType == null || declaredType == STDIO_TYPE) {
            context.getString(R.string.mcp_config_command_with_non_stdio, serverId)
        }

        return StdioMcpImportedServer(
            id = serverId,
            command = config.requiredNonBlankString(context, "command", serverId),
            args = config.optionalStringList(context, "args", serverId),
            env = config.optionalStringMap(context, "env", serverId),
            autoApprove = config.optionalStringList(context, "autoApprove", serverId),
            disabled = config.optionalBoolean(context, "disabled", serverId)
        )
    }

    private fun parseRemoteServer(context: Context, serverId: String,
        config: JsonObject,
        declaredType: String?
    ): RemoteMcpImportedServer {
        val connectionType = when (declaredType) {
            STREAMABLE_HTTP_TYPE -> "httpStream"
            SSE_TYPE -> SSE_TYPE
            STDIO_TYPE -> throw IllegalArgumentException(context.getString(R.string.mcp_config_missing_command, serverId))
            null -> throw IllegalArgumentException(context.getString(R.string.mcp_config_missing_command_or_type, serverId))
            else -> throw IllegalArgumentException(
                context.getString(R.string.mcp_config_unsupported_transport, serverId, declaredType)
            )
        }

        return RemoteMcpImportedServer(
            id = serverId,
            endpoint = config.requiredNonBlankString(context, "url", serverId),
            connectionType = connectionType,
            headers = config.optionalStringMap(context, "headers", serverId),
            disabled = config.optionalBoolean(context, "disabled", serverId)
        )
    }

    private fun JsonObject.requiredObject(context: Context, field: String): JsonObject {
        val value = get(field)
            ?: throw IllegalArgumentException(context.getString(R.string.mcp_config_field_missing, field))
        require(value.isJsonObject) { context.getString(R.string.mcp_config_field_must_be_object, field) }
        return value.asJsonObject
    }

    private fun JsonObject.requiredNonBlankString(context: Context, field: String, serverId: String): String {
        val value = optionalString(context, field)
            ?: throw IllegalArgumentException(context.getString(R.string.mcp_config_server_missing_field, serverId, field))
        require(value.isNotBlank()) { context.getString(R.string.mcp_config_field_must_not_be_blank, serverId, field) }
        return value.trim()
    }

    private fun JsonObject.optionalString(context: Context, field: String): String? {
        val value = get(field) ?: return null
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString) { context.getString(R.string.mcp_config_field_must_be_string, field) }
        return value.asString
    }

    private fun JsonObject.optionalBoolean(context: Context, field: String, serverId: String): Boolean {
        val value = get(field) ?: return false
        require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) {
            context.getString(R.string.mcp_config_field_must_be_boolean, serverId, field)
        }
        return value.asBoolean
    }

    private fun JsonObject.optionalStringList(context: Context, field: String, serverId: String): List<String> {
        val value = get(field) ?: return emptyList()
        require(value.isJsonArray) { context.getString(R.string.mcp_config_field_must_be_string_array, serverId, field) }
        return value.asJsonArray.mapIndexed { index, item ->
            require(item.isJsonPrimitive && item.asJsonPrimitive.isString) {
                context.getString(R.string.mcp_config_array_item_must_be_string, serverId, field, index)
            }
            item.asString
        }
    }

    private fun JsonObject.optionalStringMap(context: Context, field: String, serverId: String): Map<String, String> {
        val value = get(field) ?: return emptyMap()
        require(value.isJsonObject) { context.getString(R.string.mcp_config_field_must_be_object_for_server, serverId, field) }
        return value.asJsonObject.entrySet().associate { (key, item) ->
            require(key.isNotBlank()) { context.getString(R.string.mcp_config_field_empty_key, serverId, field) }
            require(item.isJsonPrimitive && item.asJsonPrimitive.isString) {
                context.getString(R.string.mcp_config_entry_must_be_string, serverId, field, key)
            }
            key to item.asString
        }
    }
}
