package com.ai.assistance.operit.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged LLM round-trip: request turns plus the collected response.
 * Bodies are stored as-is (this is the user's own device and data); secrets
 * travel in HTTP headers, which are sanitized elsewhere and never logged here.
 */
@Entity(
    tableName = "llm_io_log",
    indices = [
        Index(value = ["timestampMs"]),
        Index(value = ["modelId", "timestampMs"]),
        Index(value = ["function", "timestampMs"]),
    ],
)
data class LlmIoLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMs: Long,
    /** Function label: CHAT, a FunctionType name (TRANSLATION, SUMMARY, ...), or AD_HOC. */
    val function: String,
    /** "provider:model" string, e.g. "openai_generic:gemini/gemini-flash-lite-latest". */
    val modelId: String,
    val configId: String,
    /** JSON: turns (role/content/tool), model parameters, stream/thinking flags. Capped. */
    val requestJson: String,
    /** Full collected response text. Capped. May hold a partial stream on error. */
    val responseText: String,
    val promptTokens: Long? = null,
    val completionTokens: Long? = null,
    val latencyMs: Long? = null,
    /** Null on success; otherwise a short failure reason (never a stack trace). */
    val error: String? = null,
)
