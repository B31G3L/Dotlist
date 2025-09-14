package de.beigel.list.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

class HapticFeedbackManager(private val context: Context) {

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Moderne Haptic Patterns für bessere UX
    fun taskCompleted() {
        performHapticFeedback(HapticPattern.SUCCESS)
    }

    fun taskAdded() {
        performHapticFeedback(HapticPattern.LIGHT_IMPACT)
    }

    fun taskDeleted() {
        performHapticFeedback(HapticPattern.HEAVY_IMPACT)
    }

    fun taskSwipe() {
        performHapticFeedback(HapticPattern.SELECTION)
    }

    fun buttonPress() {
        performHapticFeedback(HapticPattern.LIGHT_IMPACT)
    }

    fun celebration() {
        performHapticFeedback(HapticPattern.CELEBRATION)
    }

    fun error() {
        performHapticFeedback(HapticPattern.ERROR)
    }

    private fun performHapticFeedback(pattern: HapticPattern) {
        if (!vibrator.hasVibrator()) return

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ mit modernen Haptic APIs
                vibrator.vibrate(pattern.modernEffect)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                // Android 8+ mit VibrationEffect
                vibrator.vibrate(pattern.basicEffect)
            }
            else -> {
                // Fallback für ältere Versionen
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern.legacyDuration)
            }
        }
    }

    private enum class HapticPattern(
        val modernEffect: VibrationEffect,
        val basicEffect: VibrationEffect,
        val legacyDuration: Long
    ) {
        LIGHT_IMPACT(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            } else {
                VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
            },
            VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE),
            10L
        ),

        HEAVY_IMPACT(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            } else {
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            },
            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE),
            50L
        ),

        SUCCESS(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            } else {
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            },
            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE),
            30L
        ),

        SELECTION(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            } else {
                VibrationEffect.createOneShot(5, VibrationEffect.DEFAULT_AMPLITUDE)
            },
            VibrationEffect.createOneShot(5, VibrationEffect.DEFAULT_AMPLITUDE),
            5L
        ),

        ERROR(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
            } else {
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            },
            VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1),
            200L
        ),

        CELEBRATION(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 100, 50, 150, 50, 200), -1)
            } else {
                VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
            },
            VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 100, 50, 150, 50, 200), -1),
            400L
        )
    }
}

@Composable
fun rememberHapticFeedback(): HapticFeedbackManager {
    val context = LocalContext.current
    return remember { HapticFeedbackManager(context) }
}

// Compose Haptic Extensions für moderne APIs
@Composable
fun HapticFeedback.performModernSuccess() {
    this.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun HapticFeedback.performModernSelection() {
    this.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

@Composable
fun HapticFeedback.performModernImpact() {
    // Verwende neueste HapticFeedbackType APIs wenn verfügbar
    try {
        this.performHapticFeedback(HapticFeedbackType.LongPress)
    } catch (e: Exception) {
        // Fallback für ältere Versionen
        this.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}