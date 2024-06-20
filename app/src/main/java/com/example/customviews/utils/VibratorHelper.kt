package com.example.customviews.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

class VibratorHelper(context: Context) {

    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @Suppress("DEPRECATION")
    fun vibrate(milliseconds: Long = 13) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (ex: Exception) {
                vibrator.vibrate(milliseconds)
            }
        } else {
            vibrator.vibrate(milliseconds)
        }
    }

    fun vibrateWaveform(milliseconds: Long = 13) {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, milliseconds, 0, milliseconds), -1)
        vibrator.vibrate(effect)
    }
}