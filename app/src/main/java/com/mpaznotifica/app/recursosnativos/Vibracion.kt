package com.mpaznotifica.app.recursosnativos

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// recurso nativo: vibrador (permiso VIBRATE en el manifest)
object Vibracion {

    @Suppress("DEPRECATION")
    fun confirmar(context: Context) {
        try {
            val vibrador = obtenerVibrador(context) ?: return
            if (!vibrador.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrador.vibrate(150)
            }
        } catch (e: Exception) {
            // si no puede vibrar no pasa nada
        }
    }

    @Suppress("DEPRECATION")
    private fun obtenerVibrador(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
