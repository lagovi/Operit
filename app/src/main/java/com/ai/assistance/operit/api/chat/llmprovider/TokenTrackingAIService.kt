package com.ai.assistance.operit.api.chat.llmprovider

import android.content.Context
import com.ai.assistance.operit.core.chat.hooks.PromptTurn
import com.ai.assistance.operit.data.model.ModelOption
import com.ai.assistance.operit.data.model.ModelParameter
import com.ai.assistance.operit.data.model.TokenUsageRecordEntity
import com.ai.assistance.operit.data.model.ToolPrompt
import com.ai.assistance.operit.data.model.LlmIoLogEntity
import com.ai.assistance.operit.data.stats.LlmIoLogRepository
import com.ai.assistance.operit.data.stats.ProviderUsageSnapshot
import com.ai.assistance.operit.data.stats.TokenUsageRepository
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.stream.RevisableTextStream
import com.ai.assistance.operit.util.stream.SharedStream
import com.ai.assistance.operit.util.stream.Stream
import com.ai.assistance.operit.util.stream.StreamCollector
import com.ai.assistance.operit.util.stream.TextStreamEvent
import com.ai.assistance.operit.util.stream.TextStreamEventCarrier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Records successful formal-inference requests with provider-confirmed usage.
 *
 * @param functionTag labels every request in the LLM I/O log: a FunctionType
 * name for functional services, CHAT for the chat service, AD_HOC for ad-hoc
 * custom-config services. Each managed service instance serves exactly one
 * function (MultiServiceManager caches per FunctionType), so a
 * construction-time tag stays accurate without touching call sites.
 */
class TokenTrackingAIService(
    private val delegate: AIService,
    context: Context,
    private val configId: String,
    private val functionTag: String? = null,
) : AIService {
    private val appContext = context.applicationContext
    private val repository = TokenUsageRepository.getInstance(appContext)
    private val logRepository = LlmIoLogRepository.getInstance(appContext)
    private val activeRequests = ConcurrentHashMap.newKeySet<RequestTracker>()
    private val cancellationLock = Any()
    private var cancellationEpoch = 0L

    override val inputTokenCount: Long get() = delegate.inputTokenCount
    override val cachedInputTokenCount: Long get() = delegate.cachedInputTokenCount
    override val outputTokenCount: Long get() = delegate.outputTokenCount
    override val providerModel: String get() = delegate.providerModel

    override fun resetTokenCounts() = delegate.resetTokenCounts()
    override fun cancelStreaming() {
        synchronized(cancellationLock) {
            cancellationEpoch += 1
            activeRequests.forEach { request -> request.cancel() }
        }
        delegate.cancelStreaming()
    }
    override suspend fun getModelsList(context: Context): Result<List<ModelOption>> =
        delegate.getModelsList(context)

    override suspend fun calculateInputTokens(
        chatHistory: List<PromptTurn>,
        availableTools: List<ToolPrompt>?,
    ): Long = delegate.calculateInputTokens(chatHistory, availableTools)

    override fun release() = delegate.release()

    override suspend fun sendMessage(
        context: Context,
        chatHistory: List<PromptTurn>,
        modelParameters: List<ModelParameter<*>>,
        enableThinking: Boolean,
        stream: Boolean,
        availableTools: List<ToolPrompt>?,
        preserveThinkInHistory: Boolean,
        onTokensUpdated: suspend (input: Long, cachedInput: Long, output: Long) -> Unit,
        onUsageReported: (suspend (ProviderUsageSnapshot, attempt: Int) -> Unit)?,
        onNonFatalError: suspend (error: String) -> Unit,
        enableRetry: Boolean,
        recordTokenUsage: Boolean,
        onUsageFinalized: (suspend (attempt: Int?) -> Unit)?,
    ): Stream<String> {
        val request = RequestTracker(configId = configId, providerModel = providerModel)
        val requestEpoch = synchronized(cancellationLock) { cancellationEpoch }
        val onStarted: () -> Unit = {
            synchronized(cancellationLock) {
                if (cancellationEpoch != requestEpoch) {
                    request.cancel()
                }
                activeRequests.add(request)
                Unit
            }
        }
        val onFinished: () -> Unit = {
            synchronized(cancellationLock) {
                activeRequests.remove(request)
                Unit
            }
        }
        val inner =
            if (!recordTokenUsage) {
                delegate.sendMessage(
                    context = context,
                    chatHistory = chatHistory,
                    modelParameters = modelParameters,
                    enableThinking = enableThinking,
                    stream = stream,
                    availableTools = availableTools,
                    preserveThinkInHistory = preserveThinkInHistory,
                    onTokensUpdated = onTokensUpdated,
                    onUsageReported = onUsageReported,
                    onNonFatalError = onNonFatalError,
                    enableRetry = enableRetry,
                    recordTokenUsage = false,
                    onUsageFinalized = onUsageFinalized,
                )
            } else {
                delegate.sendMessage(
                    context = context,
                    chatHistory = chatHistory,
                    modelParameters = modelParameters,
                    enableThinking = enableThinking,
                    stream = stream,
                    availableTools = availableTools,
                    preserveThinkInHistory = preserveThinkInHistory,
                    onTokensUpdated = onTokensUpdated,
                    onUsageReported = { usage, attempt ->
                        request.onUsage(usage, attempt)
                        forwardUsageObserver(onUsageReported, usage, attempt)
                    },
                    onNonFatalError = onNonFatalError,
                    enableRetry = enableRetry,
                    recordTokenUsage = true,
                    onUsageFinalized = { attempt ->
                        request.onSuccess(attempt)
                        forwardUsageFinalizedObserver(onUsageFinalized, attempt)
                    },
                )
            }
        val logSession =
            if (recordTokenUsage && logRepository.isEnabled()) {
                IoLogSession(
                    function = functionTag ?: LlmIoLogRepository.FUNCTION_CHAT,
                    modelId = providerModel,
                    configId = configId,
                    requestJson =
                        LlmIoLogRepository.formatRequest(
                            turns = chatHistory,
                            parameters = modelParameters,
                            enableThinking = enableThinking,
                            stream = stream,
                        ),
                    startedAtMs = System.currentTimeMillis(),
                )
            } else {
                null
            }
        return wrapStream(
            inner = inner,
            request = request,
            onStarted = onStarted,
            onFinished = onFinished,
            logSession = logSession,
        )
    }

    override suspend fun testConnection(context: Context): Result<String> =
        delegate.testConnection(context)

    private suspend fun forwardUsageObserver(
        observer: (suspend (ProviderUsageSnapshot, Int) -> Unit)?,
        usage: ProviderUsageSnapshot,
        attempt: Int,
    ) {
        val callback = observer ?: return
        try {
            callback(usage, attempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "usage observer failed", e)
        }
    }

    private suspend fun forwardUsageFinalizedObserver(
        observer: (suspend (Int?) -> Unit)?,
        attempt: Int?,
    ) {
        val callback = observer ?: return
        try {
            callback(attempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "usage completion observer failed", e)
        }
    }

    private fun wrapStream(
        inner: Stream<String>,
        request: RequestTracker,
        onStarted: () -> Unit,
        onFinished: () -> Unit,
        logSession: IoLogSession?,
    ): Stream<String> =
        if (inner is TextStreamEventCarrier) {
            TrackingRevisableStream(
                inner,
                inner.eventChannel,
                request,
                repository,
                logRepository,
                onStarted,
                onFinished,
                logSession
            )
        } else {
            TrackingStream(
                inner,
                request,
                repository,
                logRepository,
                onStarted,
                onFinished,
                logSession
            )
        }

    private class TrackingStream(
        private val inner: Stream<String>,
        private val request: RequestTracker,
        private val repository: TokenUsageRepository,
        private val logRepository: LlmIoLogRepository,
        private val onStarted: () -> Unit,
        private val onFinished: () -> Unit,
        private val logSession: IoLogSession?,
    ) : Stream<String> {
        override val isLocked: Boolean get() = inner.isLocked
        override val bufferedCount: Int get() = inner.bufferedCount
        override suspend fun lock() = inner.lock()
        override suspend fun unlock() = inner.unlock()
        override fun clearBuffer() = inner.clearBuffer()

        override suspend fun collect(collector: StreamCollector<String>) {
            // runTrackedCollect is an outer member, unreachable from a nested
            // class; route through the companion bridge instead.
            runCollectBridge(
                inner = inner,
                collector = collector,
                request = request,
                repository = repository,
                logRepository = logRepository,
                logSession = logSession,
                onStarted = onStarted,
                onFinished = onFinished,
            )
        }
    }

    private class TrackingRevisableStream(
        private val inner: Stream<String>,
        override val eventChannel: SharedStream<TextStreamEvent>,
        private val request: RequestTracker,
        private val repository: TokenUsageRepository,
        private val logRepository: LlmIoLogRepository,
        private val onStarted: () -> Unit,
        private val onFinished: () -> Unit,
        private val logSession: IoLogSession?,
    ) : RevisableTextStream {
        override val isLocked: Boolean get() = inner.isLocked
        override val bufferedCount: Int get() = inner.bufferedCount
        override suspend fun lock() = inner.lock()
        override suspend fun unlock() = inner.unlock()
        override fun clearBuffer() = inner.clearBuffer()

        override suspend fun collect(collector: StreamCollector<String>) {
            runCollectBridge(
                inner = inner,
                collector = collector,
                request = request,
                repository = repository,
                logRepository = logRepository,
                logSession = logSession,
                onStarted = onStarted,
                onFinished = onFinished,
            )
        }
    }

    /**
     * Accumulates one request/response pair for the I/O log. Chunk appends
     * are capped; failures record a short reason with the partial response.
     */
    private class IoLogSession(
        private val function: String,
        private val modelId: String,
        private val configId: String,
        private val requestJson: String,
        private val startedAtMs: Long,
    ) {
        private val responseBuilder = StringBuilder()
        private var truncated = false
        private var failed = false
        private var failureReason: String? = null
        private var persisted = false
        private val lock = Any()

        fun appendResponse(chunk: String) {
            synchronized(lock) {
                val room = LlmIoLogRepository.MAX_RESPONSE_CHARS - responseBuilder.length
                if (room <= 0) {
                    truncated = true
                    return
                }
                if (chunk.length > room) {
                    responseBuilder.append(chunk, 0, room)
                    truncated = true
                } else {
                    responseBuilder.append(chunk)
                }
            }
        }

        fun fail(reason: String) {
            synchronized(lock) {
                failed = true
                if (failureReason == null) failureReason = reason
            }
        }

        /** Builds the row exactly once; concurrent/double completion is dropped. */
        fun toEntity(usage: TokenUsageRecordEntity?): LlmIoLogEntity? {
            synchronized(lock) {
                if (persisted) return null
                persisted = true
            }
            val responseSnapshot: String
            val failedSnapshot: Boolean
            val reasonSnapshot: String?
            synchronized(lock) {
                responseSnapshot =
                    buildString {
                        append(responseBuilder)
                        if (truncated) append(LlmIoLogRepository.TRUNCATION_MARKER)
                    }
                failedSnapshot = failed
                reasonSnapshot = failureReason
            }
            return LlmIoLogEntity(
                timestampMs = startedAtMs,
                function = function,
                modelId = modelId,
                configId = configId,
                requestJson = requestJson,
                responseText = responseSnapshot,
                promptTokens =
                    usage?.let {
                        LlmIoLogRepository.promptTokensOf(
                            uncachedInputTokens = it.uncachedInputTokens,
                            cachedInputTokens = it.cachedInputTokens,
                            cacheWriteTokens = it.cacheWriteTokens,
                            totalInputTokens = it.totalInputTokens,
                        )
                    },
                completionTokens = usage?.outputTokens,
                latencyMs = System.currentTimeMillis() - startedAtMs,
                error = if (failedSnapshot) reasonSnapshot ?: "failed" else null,
            )
        }
    }

    private class RequestTracker(
        private val configId: String,
        private val providerModel: String,
    ) {
        private val startedAtMs = System.currentTimeMillis()
        private val lock = Any()
        private val attempts = linkedMapOf<Int, ProviderUsageSnapshot>()
        private val finished = AtomicBoolean(false)
        private val cancelled = AtomicBoolean(false)
        private var successfulAttempt: Int? = null

        fun onUsage(usage: ProviderUsageSnapshot, attempt: Int) {
            synchronized(lock) {
                val key = attempt.coerceAtLeast(1)
                attempts[key] = merge(attempts[key], usage)
            }
        }

        fun onSuccess(attempt: Int?) {
            synchronized(lock) {
                successfulAttempt = attempt?.coerceAtLeast(1)
            }
        }

        fun cancel() {
            cancelled.set(true)
        }

        fun throwIfCancelled() {
            if (cancelled.get()) throw CancellationException("AI request was cancelled")
        }

        fun finish(): TokenUsageRecordEntity? {
            if (cancelled.get()) return null
            val snapshot =
                synchronized(lock) {
                    successfulAttempt?.let(attempts::get)
                } ?: return null
            if (cancelled.get()) return null
            if (!snapshot.hasKnownFields()) return null

            val separator = providerModel.indexOf(':')
            require(separator > 0 && separator < providerModel.lastIndex) {
                "provider:model is required for token usage events"
            }
            // 推理不再作为独立统计列落库；provider 明确将其独立于 output 上报时，
            // 先并入持久化 output，保持总量与费用口径完整。
            val reportedOutputTokens = snapshot.outputTokens
            val persistedOutputTokens = when {
                reportedOutputTokens != null && snapshot.reasoningIncludedInOutput == false ->
                    snapshot.reasoningTokens?.let { saturatedAdd(reportedOutputTokens, it) }
                reportedOutputTokens != null -> reportedOutputTokens
                else -> null
            }
            if (snapshot.uncachedInputTokens == null &&
                snapshot.cachedInputTokens == null &&
                snapshot.cacheWriteTokens == null &&
                snapshot.totalInputTokens == null &&
                persistedOutputTokens == null
            ) {
                return null
            }
            return TokenUsageRecordEntity(
                occurredAtMs = startedAtMs,
                configId = configId,
                provider = providerModel.substring(0, separator),
                model = providerModel.substring(separator + 1),
                requestCount = 1L,
                uncachedInputTokens = snapshot.uncachedInputTokens,
                cachedInputTokens = snapshot.cachedInputTokens,
                cacheWriteTokens =
                    if (snapshot.cacheWriteSeparateBilling) snapshot.cacheWriteTokens else 0L,
                totalInputTokens = snapshot.totalInputTokens,
                outputTokens = persistedOutputTokens,
            )
        }

        fun markPersisted(): Boolean = !cancelled.get() && finished.compareAndSet(false, true)

        private fun merge(
            previous: ProviderUsageSnapshot?,
            update: ProviderUsageSnapshot,
        ): ProviderUsageSnapshot {
            if (previous == null || update.completeSnapshot) return update
            return update.copy(
                uncachedInputTokens = update.uncachedInputTokens ?: previous.uncachedInputTokens,
                cachedInputTokens = update.cachedInputTokens ?: previous.cachedInputTokens,
                cacheWriteTokens = update.cacheWriteTokens ?: previous.cacheWriteTokens,
                totalInputTokens = update.totalInputTokens ?: previous.totalInputTokens,
                outputTokens = update.outputTokens ?: previous.outputTokens,
                reasoningTokens = update.reasoningTokens ?: previous.reasoningTokens,
            )
        }
    }

    companion object {
        private const val TAG = "TokenTrackingAIService"

        /**
         * Shared collect path for both stream wrappers (nested classes cannot
         * reach outer members). Delivers chunks untouched, then persists token
         * usage and the I/O log row; the writes run after the last chunk was
         * emitted, so logging never delays the visible stream.
         */
        private suspend fun runCollectBridge(
            inner: Stream<String>,
            collector: StreamCollector<String>,
            request: RequestTracker,
            repository: TokenUsageRepository,
            logRepository: LlmIoLogRepository,
            logSession: IoLogSession?,
            onStarted: () -> Unit,
            onFinished: () -> Unit,
        ) {
            try {
                onStarted()
                try {
                    request.throwIfCancelled()
                } catch (e: CancellationException) {
                    // Cancelled before the first chunk: log the attempt, then propagate.
                    logSession?.fail("cancelled")
                    logSession?.let { persistIoLog(logRepository, it, null) }
                    throw e
                }
                try {
                    inner.collect { value ->
                        logSession?.appendResponse(value)
                        collector.emit(value)
                    }
                } catch (e: Throwable) {
                    // Stream failed mid-flight: keep the partial response, then propagate.
                    logSession?.fail(
                        if (e is CancellationException) {
                            "cancelled"
                        } else {
                            e.message?.take(512) ?: e.javaClass.simpleName
                        }
                    )
                    // finish() must not mask the in-flight failure with its own.
                    val usage = runCatching { request.finish() }.getOrNull()
                    logSession?.let { persistIoLog(logRepository, it, usage) }
                    throw e
                }
                try {
                    currentCoroutineContext().ensureActive()
                } catch (e: CancellationException) {
                    // Cancel raced a completed stream: the response was fully
                    // delivered, still log it before propagating.
                    logSession?.fail("cancelled")
                    val usage = runCatching { request.finish() }.getOrNull()
                    logSession?.let { persistIoLog(logRepository, it, usage) }
                    throw e
                }
                val usage = request.finish()
                persist(repository, request, usage)
                logSession?.let { persistIoLog(logRepository, it, usage) }
            } finally {
                onFinished()
            }
        }

        /** Bodies stay in Room only; logcat gets the outcome, never content. */
        private suspend fun persistIoLog(
            logRepository: LlmIoLogRepository,
            session: IoLogSession,
            usage: TokenUsageRecordEntity?,
        ) {
            withContext(Dispatchers.IO + NonCancellable) {
                try {
                    session.toEntity(usage)?.let { logRepository.record(it) }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "llm io log insert failed", e)
                }
            }
        }

        private suspend fun persist(
            repository: TokenUsageRepository,
            request: RequestTracker,
            record: TokenUsageRecordEntity?,
        ) {
            if (record != null && request.markPersisted()) persist(repository, record)
        }

        private suspend fun persist(
            repository: TokenUsageRepository,
            record: TokenUsageRecordEntity,
        ) {
            withContext(Dispatchers.IO + NonCancellable) {
                try {
                    repository.record(record)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "token usage insert failed", e)
                }
            }
        }

        private fun saturatedAdd(left: Long, right: Long): Long =
            if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
    }
}
