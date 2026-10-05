package com.ai.assistance.operit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ai.assistance.operit.data.model.LlmIoLogEntity

@Dao
abstract class LlmIoLogDao {

    @Insert
    abstract suspend fun insert(record: LlmIoLogEntity): Long

    @Query(
        """
        SELECT * FROM llm_io_log
        WHERE (:modelId IS NULL OR modelId = :modelId)
        ORDER BY timestampMs DESC, id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun recent(
        modelId: String?,
        limit: Int,
        offset: Int,
    ): List<LlmIoLogEntity>

    @Query("SELECT DISTINCT modelId FROM llm_io_log ORDER BY modelId")
    abstract suspend fun distinctModels(): List<String>

    @Query("SELECT * FROM llm_io_log WHERE id = :id")
    abstract suspend fun get(id: Long): LlmIoLogEntity?

    @Query("SELECT COUNT(*) FROM llm_io_log")
    abstract suspend fun count(): Long

    @Query("SELECT COALESCE(SUM(LENGTH(requestJson) + LENGTH(responseText)), 0) FROM llm_io_log")
    abstract suspend fun totalChars(): Long

    @Query("DELETE FROM llm_io_log WHERE id = :id")
    abstract suspend fun delete(id: Long): Int

    @Query("DELETE FROM llm_io_log")
    abstract suspend fun deleteAll(): Int

    /** Keeps only the newest [keep] rows, drops everything older. */
    @Query(
        """
        DELETE FROM llm_io_log
        WHERE id NOT IN (
            SELECT id FROM llm_io_log ORDER BY timestampMs DESC, id DESC LIMIT :keep
        )
        """
    )
    abstract suspend fun pruneToNewest(keep: Int): Int

    /** Deletes the oldest [limit] rows. Returns the deleted row count. */
    @Query(
        """
        DELETE FROM llm_io_log
        WHERE id IN (
            SELECT id FROM llm_io_log ORDER BY timestampMs ASC, id ASC LIMIT :limit
        )
        """
    )
    abstract suspend fun deleteOldest(limit: Int): Int
}
