package com.ai.assistance.operit.api.chat.llmprovider

import org.junit.Before
import org.junit.After
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.data.collects.ModelThinkingConfigDefaults
import com.ai.assistance.operit.data.model.ApiProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiThinkingConfigTest {
    private var previousSystemLogEnabled = true
    private var previousFileLogEnabled = true

    @Before
    fun disableAndroidLogging() {
        previousSystemLogEnabled = AppLogger.enableSystemLog
        previousFileLogEnabled = AppLogger.enableFileLogging
        AppLogger.enableSystemLog = false
        AppLogger.enableFileLogging = false
    }

    @After
    fun restoreAndroidLogging() {
        AppLogger.enableSystemLog = previousSystemLogEnabled
        AppLogger.enableFileLogging = previousFileLogEnabled
    }
    private val thinkingConfigurations =
        ModelThinkingConfigDefaults.forProvider(ApiProviderType.GOOGLE.name)

    @Test
    fun mapsModelOptionToGeminiLevel() {
        val config = GeminiThinkingConfig.fromOption("gemini-3-pro", "HIGH", thinkingConfigurations)
        assertEquals("HIGH", config.thinkingLevel)
    }

    @Test
    fun requestsThoughtSummariesForAnEnabledOption() {
        val config = GeminiThinkingConfig.fromOption("gemini-3-pro", "HIGH", thinkingConfigurations)
        assertTrue(config.includeThoughts)
    }
}
