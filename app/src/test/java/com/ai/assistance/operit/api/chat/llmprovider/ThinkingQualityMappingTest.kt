package com.ai.assistance.operit.api.chat.llmprovider

import com.ai.assistance.operit.data.collects.ModelThinkingConfigDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkingQualityMappingTest {
    private fun mapping(providerTypeId: String, modelName: String): ThinkingQualityMapping =
        ThinkingQualityMappingRegistry.resolve(
            providerTypeId = providerTypeId,
            modelName = modelName,
            thinkingConfigurations = ModelThinkingConfigDefaults.forProvider(providerTypeId)
        )

    @Test
    fun xaiMapsSupportedModelsToReasoningEfforts() {
        val mapping = mapping("XAI", "grok-4.6")

        assertEquals(ThinkingQualityControl.LEVELS, mapping.control)
        assertEquals("reasoning_effort", mapping.parameterLabel)
        assertEquals(listOf("low", "medium", "high", "xhigh"), mapping.options.map { it.displayLabel })
        assertEquals("high", mapping.textValueFor("high"))
    }

    @Test
    fun xaiKeepsGenericControlsForNewGrokModels() {
        val mapping = mapping("XAI", "grok-3-mini")

        assertEquals(ThinkingQualityControl.LEVELS, mapping.control)
        assertTrue(mapping.options.isNotEmpty())
    }

    @Test
    fun openAiUsesFiveNamedEffortValues() {
        val mapping = mapping("OPENAI", "gpt-5.6-luna")

        assertEquals(listOf("low", "medium", "high", "xhigh", "max"), mapping.options.map { it.displayLabel })
        assertEquals("high", mapping.textValueFor("high"))
    }

    @Test
    fun providersUseModelSpecificWireValues() {
        val gemini = mapping("GOOGLE", "gemini-3-flash")

        assertEquals(listOf("MINIMAL", "LOW", "MEDIUM", "HIGH"), gemini.options.map { it.displayLabel })
    }

    @Test
    fun providerSpecificModelMatchingControlsLevelSupport() {
        val gptOss = mapping("NVIDIA", "gpt-oss-120b")
        val otherNvidiaModel = mapping("NVIDIA", "nemotron-future")

        assertTrue(gptOss.options.isNotEmpty())
        assertTrue(otherNvidiaModel.options.isNotEmpty())
    }
}
