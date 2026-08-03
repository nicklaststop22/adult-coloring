package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("inkzen_prefs", Context.MODE_PRIVATE)

    private val _isProUser = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _lastGenTime = MutableStateFlow(prefs.getLong(KEY_LAST_GEN_TIME, 0L))
    val lastGenTime: StateFlow<Long> = _lastGenTime.asStateFlow()

    fun setProUser(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO, isPro).apply()
        _isProUser.value = isPro
    }

    fun recordGenerationTime(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_GEN_TIME, timestamp).apply()
        _lastGenTime.value = timestamp
    }

    fun getRemainingCooldownSeconds(): Int {
        if (_isProUser.value) return 0
        val elapsed = System.currentTimeMillis() - _lastGenTime.value
        val remainingMs = FREE_COOLDOWN_MS - elapsed
        return if (remainingMs > 0) (remainingMs / 1000).toInt() + 1 else 0
    }

    fun canGenerate(batchCount: Int = 1): CooldownCheckResult {
        if (_isProUser.value) {
            return if (batchCount in 1..5) CooldownCheckResult.Allowed
            else CooldownCheckResult.BatchLimitExceeded(5)
        }

        if (batchCount > 1) {
            return CooldownCheckResult.ProRequiredForBatch
        }

        val remaining = getRemainingCooldownSeconds()
        return if (remaining <= 0) {
            CooldownCheckResult.Allowed
        } else {
            CooldownCheckResult.CooldownActive(remaining)
        }
    }

    companion object {
        private const val KEY_IS_PRO = "is_pro_user"
        private const val KEY_LAST_GEN_TIME = "last_gen_time"
        const val FREE_COOLDOWN_MS = 60_000L // 1 minute free tier cooldown
    }
}

sealed class CooldownCheckResult {
    object Allowed : CooldownCheckResult()
    data class CooldownActive(val secondsRemaining: Int) : CooldownCheckResult()
    object ProRequiredForBatch : CooldownCheckResult()
    data class BatchLimitExceeded(val maxAllowed: Int) : CooldownCheckResult()
}
