package com.ai.assistance.operit.ui.features.chat.components

import java.util.Locale
import kotlin.math.floor

/**
 * Computes the cache hit rate: cached input / input, with two decimals truncated
 * (never rounded up). For example 99.999% renders as 99.99% and never rolls over
 * to 100.00%. When the input is 0 the rate is meaningless, so it returns "-".
 */
internal fun formatCacheHitRate(cachedInput: Long, input: Long): String {
    if (input <= 0L) return "-"
    val rate = cachedInput.coerceAtLeast(0L) * 100.0 / input
    // +1e-9 only compensates for floating point representation error so that 95.88 is not truncated to 95.87
    val truncated = floor(rate * 100.0 + 1e-9) / 100.0
    return String.format(Locale.US, "%.2f%%", truncated)
}
