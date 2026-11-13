package com.ai.assistance.operit.util

import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.io.PrintWriter
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * ANR monitor that tracks and records main-thread blocks that may lead to an ANR.
 * 
 * @param context application context
 * @param coroutineScope scope used to start the monitoring coroutine
 * @param tag log tag
 */
class AnrMonitor(
    private val context: Context, 
    private val coroutineScope: CoroutineScope,
    private val tag: String = "AnrMonitor"
) {
    companion object {
        // Default threshold settings
        private const val ANR_THRESHOLD_MS = 1000L     // 1 s, the standard ANR threshold
        private const val WARNING_THRESHOLD_MS = 500L // 0.5 s, warning threshold
        private const val SAMPLING_INTERVAL_MS = 100L  // 100 ms sampling interval
        private const val MAX_STACK_TRACES = 10        // Maximum number of retained stack traces

        // Main thread name
        private const val MAIN_THREAD_NAME = "main"
    }
    
    private val running = AtomicBoolean(false)
    private val lastResponseTime = AtomicLong(System.currentTimeMillis())
    private var monitoringJob: Job? = null
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    // Capture throttle: while an ANR lasts, the 100 ms samples only trigger one full dump per cooldown window
    private val captureThrottle = AnrCaptureThrottle(AnrCaptureBudget.CAPTURE_COOLDOWN_MS)
    
    // Standby path: use a ScheduledExecutorService if the coroutine misbehaves
    private var scheduledExecutor: ScheduledExecutorService? = null
    
    // Record the ANR count and severity
    private val anrCount = AtomicInteger(0)
    private val warningCount = AtomicInteger(0)
    private val maxBlockDuration = AtomicLong(0)
    
    // Stack trace history
    private val stackTraces = mutableListOf<Pair<Long, String>>()
    
    // Track caller information
    private val callerInfo = ConcurrentHashMap<String, String>()

    // Whether this stall has already been counted: one block counts as one ANR, not one per 100 ms
    private val stallReported = AtomicBoolean(false)

    // Last main thread reference obtained
    private var mainThread: Thread? = null
    
    // Stack signature of the previous ANR, used for deduplication (only leading frames are compared, independent of dump text size)
    private var lastAnrSignature: String? = null
    
    /**
     * Starts ANR monitoring
     */
    fun start() {
        if (running.getAndSet(true)) {
            AppLogger.w(tag, "The ANR monitor is already running")
            return
        }
        
        AppLogger.d(tag, "Starting ANR monitor")
        lastResponseTime.set(System.currentTimeMillis())
        stallReported.set(false)
        lastAnrSignature = null
        
        // Try to obtain the main thread reference
        try {
            mainThread = getMainThread()
            AppLogger.d(tag, "Main thread reference obtained: $mainThread")
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to obtain the main thread reference", e)
        }
        
        try {
            // Try to start coroutine monitoring
            monitoringJob = coroutineScope.launch(Dispatchers.Default) {
                while (running.get()) {
                    checkMainThreadHealth()
                    delay(SAMPLING_INTERVAL_MS)
                }
            }
        } catch (e: Exception) {
            // If the coroutine fails to start, use the thread pool as the standby path
            AppLogger.e(tag, "The coroutine failed to start, monitoring with the standby thread pool", e)
            startUsingExecutor()
        }
    }
    
    /**
     * Starts monitoring with a ScheduledExecutorService (standby path)
     */
    private fun startUsingExecutor() {
        if (scheduledExecutor == null || scheduledExecutor?.isShutdown == true) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor { r ->
                val t = Thread(r, "AnrMonitor-Watchdog")
                t.priority = Thread.MAX_PRIORITY
                t.isDaemon = true
                t
            }
        }
        
        scheduledExecutor?.scheduleAtFixedRate({
            if (running.get()) {
                checkMainThreadHealth()
            } else {
                scheduledExecutor?.shutdown()
            }
        }, 0, SAMPLING_INTERVAL_MS, TimeUnit.MILLISECONDS)
    }
    
    /**
     * Stops ANR monitoring
     */
    fun stop() {
        if (!running.getAndSet(false)) {
            return
        }
        
        AppLogger.d(tag, "Stopping ANR monitor; results: ANR count=${anrCount.get()}, warning count=${warningCount.get()}, longest block=${maxBlockDuration.get()}ms")
        monitoringJob?.cancel()
        scheduledExecutor?.shutdown()
        
        // Save the report if any ANR was recorded
        if (anrCount.get() > 0 || warningCount.get() > 0) {
            saveAnrReport()
        }
    }
    
    /**
     * Reports a normal main thread response
     */
    fun reportThreadHealthy() {
        lastResponseTime.set(System.currentTimeMillis())
    }
    
    /**
     * Reports a slow main thread response
     */
    fun reportSlowResponse(responseTime: Long) {
        if (responseTime > WARNING_THRESHOLD_MS) {
            warningCount.incrementAndGet()
            if (responseTime > maxBlockDuration.get()) {
                maxBlockDuration.set(responseTime)
            }
            
            if (responseTime > ANR_THRESHOLD_MS) {
                val anrCount = anrCount.incrementAndGet()
                AppLogger.e(tag, "Possible ANR detected! Response time: ${responseTime}ms, ANR number ${anrCount}")
                captureFullThreadDump()
            } else {
                AppLogger.w(tag, "Slow main thread response: ${responseTime}ms")
            }
        }
    }
    
    /**
     * Adds caller information to help trace the origin of an ANR
     */
    fun addCallerInfo(key: String, info: String) {
        callerInfo[key] = "[$key] $info (${SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())})"
    }
    
    /**
     * Checks main thread health
     */
    private fun checkMainThreadHealth() {
        mainThreadHandler.post {
            reportThreadHealthy()
        }

        val now = System.currentTimeMillis()
        val lastResponse = lastResponseTime.get()
        val timeSinceLastResponse = now - lastResponse

        if (timeSinceLastResponse <= WARNING_THRESHOLD_MS) {
            // The main thread responds again: this stall is over, so the next stall captures afresh instead of being treated as a repeated ANR forever
            if (stallReported.getAndSet(false)) {
                lastAnrSignature = null
            }
            return
        }

        // The main thread may be blocked
        val message = context.getString(R.string.anr_main_thread_not_responding, timeSinceLastResponse)

        if (timeSinceLastResponse > ANR_THRESHOLD_MS) {
            // Count one ANR per stall: otherwise the 100 ms sampling turns one stall into dozens
            if (stallReported.compareAndSet(false, true)) {
                AppLogger.e(tag, "$message - possible ANR!")
                anrCount.incrementAndGet()
            }

            // Record the stack trace using the enhanced capture path
            captureFullThreadDump()

            if (timeSinceLastResponse > maxBlockDuration.get()) {
                maxBlockDuration.set(timeSinceLastResponse)
            }
        } else {
            // Above the warning threshold but below the ANR threshold
            AppLogger.w(tag, "$message - warning")
            warningCount.incrementAndGet()
        }
    }
    
    /**
     * Captures the main thread stack
     */
    private fun captureMainThreadStack() {
        mainThreadHandler.post {
            try {
                val stackTrace = AnrCaptureBudget.formatFrames(Thread.currentThread().stackTrace, skipFrames = 3)

                val timeStamp = System.currentTimeMillis()
                val trace = Pair(timeStamp, stackTrace)

                synchronized(stackTraces) {
                    stackTraces.add(trace)
                    // Bound the stack history size
                    if (stackTraces.size > MAX_STACK_TRACES) {
                        stackTraces.removeAt(0)
                    }
                }

                // Analyse the stack
                val analysis = analyzeStackTrace(stackTrace)

                AppLogger.e(tag, "Main thread stack trace:\n$stackTrace\n$analysis")
            } catch (e: Exception) {
                AppLogger.e(tag, "Failed to capture the stack", e)
            }
        }
    }
    
    /**
     * Obtains a reference to the main thread instance
     */
    private fun getMainThread(): Thread? {
        try {
            // Approach 1: through the thread bound to the Looper
            Looper.getMainLooper().thread?.let { return it }
            
            // Approach 2: scan all threads for the one named main
            val threadGroup = Thread.currentThread().threadGroup ?: return null
            val threadCount = threadGroup.activeCount()
            val threads = arrayOfNulls<Thread>(threadCount)
            threadGroup.enumerate(threads)
            
            return threads.filterNotNull().find { it.name == MAIN_THREAD_NAME }
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to obtain the main thread", e)
            return null
        }
    }
    
    /**
     * Captures a full thread dump, covering the main thread and other important threads
     */
    private fun captureFullThreadDump() {
        // Do not capture again inside the cooldown: while the main thread stays blocked, only one of the 100 ms samples reaches this point
        if (!captureThrottle.tryAcquire(SystemClock.elapsedRealtime())) {
            return
        }

        try {
            val stack = readMainThreadStack()
            if (stack == null) {
                AppLogger.e(tag, "ANR detected, but the main thread reference is unavailable\n${context.getString(R.string.anr_cannot_get_main_thread_ref)}")
                return
            }

            // Check for a repeated ANR first, so no cost is paid twice for string building and log writes
            if (stack.signature == lastAnrSignature) {
                AppLogger.w(tag, "Repeated ANR detected, skipping output")
                return
            }
            lastAnrSignature = stack.signature

            val sbDump = StringBuilder(AnrCaptureBudget.MAX_DUMP_CHARS / 4)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

            sbDump.append(context.getString(R.string.anr_thread_dump_header, dateFormat.format(Date())))
            sbDump.append(context.getString(R.string.anr_main_thread_section, stack.dump))

            // Append the main thread analysis
            val analysis = analyzeStackTrace(stack.dump)
            sbDump.append(context.getString(R.string.anr_analysis_section, analysis))

            // Collect and append caller information
            if (callerInfo.isNotEmpty()) {
                sbDump.append(context.getString(R.string.anr_recent_call_info))
                callerInfo.forEach { (_, info) -> sbDump.append("$info\n") }
                sbDump.append("\n")
            }

            val dumpText = AnrCaptureBudget.clampDump(sbDump)

            // Save the thread dump
            val timestamp = System.currentTimeMillis()
            val trace = Pair(timestamp, dumpText)

            // Update the stack trace history
            synchronized(stackTraces) {
                stackTraces.add(trace)
                if (stackTraces.size > MAX_STACK_TRACES) {
                    stackTraces.removeAt(0)
                }
            }

            // Write to the log
            AppLogger.e(tag, "ANR detected! Full thread dump:\n$dumpText")

        } catch (oom: OutOfMemoryError) {
            // OutOfMemoryError is an Error, not an Exception; letting it escape takes the monitor and the process down
            AppLogger.e(tag, "Out of memory while capturing the thread dump; skipped this capture")
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to capture the thread dump", e)
            // Try the older capture path on failure
            captureMainThreadStack()
        }
    }

    /**
     * Reads the main thread stack once: one stackTrace array per sample, shared by the dedup signature and the dump.
     *
     * @return null when the main thread reference is unavailable.
     */
    private fun readMainThreadStack(): MainThreadStack? {
        val thread = mainThread ?: getMainThread() ?: return null
        mainThread = thread

        val frames = try {
            thread.stackTrace
        } catch (e: Exception) {
            AppLogger.e(tag, "Cannot read the main thread stack", e)
            return MainThreadStack(
                signature = "unreadable:${e.message.orEmpty()}",
                dump = context.getString(R.string.anr_cannot_get_main_thread_stack, e.message ?: ""),
            )
        }

        val stackText = AnrCaptureBudget.formatFrames(frames)
        return MainThreadStack(
            signature = AnrCaptureBudget.stackSignature(frames),
            dump = context.getString(R.string.anr_main_thread_with_state, thread.name, thread.state, stackText),
        )
    }

    private class MainThreadStack(val signature: String, val dump: String)
    
    /**
     * Analyses a stack trace and lists the packages in it (only the com.ai.assistance.operit package is kept)
     */
    private fun analyzeStackTrace(stackTrace: String): String {
        val analysis = StringBuilder()
        val targetPackage = "com.ai.assistance.operit"
        val lines = mutableListOf<String>()
        
        for (line in stackTrace.lines()) {
            // Match the stack line format: at package.Class.method(File.java:line)
            val atIndex = line.indexOf("at ")
            if (atIndex >= 0) {
                val stackPart = line.substring(atIndex + 3).trim()
                // Keep only frames from the com.ai.assistance.operit package
                if (stackPart.startsWith(targetPackage)) {
                    lines.add(line.trim())
                }
            }
        }
        
        // Emit the captured stack lines
        if (lines.isNotEmpty()) {
            analysis.append(context.getString(R.string.anr_package_calls, targetPackage, lines.size))
            lines.forEach { line ->
                analysis.append("$line\n")
            }
        } else {
            analysis.append(context.getString(R.string.anr_cannot_extract_package_info, targetPackage))
        }
        
        return analysis.toString()
    }
    
    /**
     * Saves the ANR report to a file
     */
    private fun saveAnrReport() {
        try {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val fileName = "anr_report_${timestamp}.txt"
            
            val file = File(context.getExternalFilesDir("anr_reports"), fileName)
            file.parentFile?.mkdirs()
            
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos).use { writer ->
                    writer.write(context.getString(R.string.anr_report_header))
                    writer.write(context.getString(R.string.anr_report_time, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())))
                    writer.write(context.getString(R.string.anr_report_anr_count, anrCount.get()))
                    writer.write(context.getString(R.string.anr_report_warning_count, warningCount.get()))
                    writer.write(context.getString(R.string.anr_report_max_block, maxBlockDuration.get()))

                    writer.write(context.getString(R.string.anr_system_info_header))
                    writer.write(context.getString(R.string.anr_android_version, android.os.Build.VERSION.SDK_INT))
                    writer.write(context.getString(R.string.anr_device, android.os.Build.MANUFACTURER, android.os.Build.MODEL))
                    writer.write(context.getString(R.string.anr_memory_info))
                    val rt = Runtime.getRuntime()
                    writer.write(context.getString(R.string.anr_max_memory, rt.maxMemory() / 1024 / 1024))
                    writer.write(context.getString(R.string.anr_allocated_memory, rt.totalMemory() / 1024 / 1024))
                    writer.write(context.getString(R.string.anr_free_memory, rt.freeMemory() / 1024 / 1024))

                    writer.write(context.getString(R.string.anr_stack_trace_history_header))
                    synchronized(stackTraces) {
                        stackTraces.forEach { (time, stack) ->
                            val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(time))
                            writer.write(context.getString(R.string.anr_time, timeStr))
                            writer.write("$stack\n\n")
                        }
                    }

                    writer.write(context.getString(R.string.anr_caller_info_header))
                    callerInfo.forEach { (_, info) -> 
                        writer.write("$info\n") 
                    }
                }
            }
            
            AppLogger.i(tag, "ANR report saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            AppLogger.e(tag, "Failed to save the ANR report", e)
        }
    }
} 