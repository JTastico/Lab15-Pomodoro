package com.bpareja.pomodorotec.pomodoro

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CountDownTimer
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bpareja.pomodorotec.MainActivity
import com.bpareja.pomodorotec.R

enum class Phase {
    FOCUS, BREAK
}

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    private val _timeLeft = MutableLiveData("25:00")
    val timeLeft: LiveData<String> = _timeLeft

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _currentPhase = MutableLiveData(Phase.FOCUS)
    val currentPhase: LiveData<Phase> = _currentPhase

    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingInMillis: Long = 1 * 60 * 1000L // Tiempo inicial para FOCUS

    // Función para iniciar la sesión de concentración
    fun startFocusSession() {
        _currentPhase.value = Phase.FOCUS
        timeRemainingInMillis = 1 * 60 * 1000L // Ajusta a 2 minutos para pruebas
        _timeLeft.value = "02:00"
        showNotification("Inicio de Concentración Tiempo", "La sesión de concentración ha comenzado. ver el tiempo")
        startTimer()
    }

    // Función para iniciar la sesión de descanso
    private fun startBreakSession() {
        _currentPhase.value = Phase.BREAK
        timeRemainingInMillis = 1 * 60 * 1000L // 5 minutos para descanso
        _timeLeft.value = "05:00"
        showNotification("Inicio de Descanso", "La sesión de descanso ha comenzado.")
        startTimer()
    }

    // Inicia o reanuda el temporizador
    fun startTimer() {
        if (_isRunning.value == true) return

        _isRunning.value = true
        countDownTimer = object : CountDownTimer(timeRemainingInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingInMillis = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeText = String.format("%02d:%02d", minutes, seconds)

                // Actualiza el tiempo restante en el LiveData
                _timeLeft.value = timeText

                // Actualiza la notificación cada segundo sin sonido ni vibración
                showNotification(
                    title = when (_currentPhase.value) {
                        Phase.FOCUS -> "Tiempo de concentración"
                        Phase.BREAK -> "Tiempo de descanso"
                        else -> "Pomodoro"
                    },
                    timeRemainingText = timeText,
                    playSound = false // Sin sonido ni vibración durante la actualización por segundo
                )
            }

            override fun onFinish() {
                _isRunning.value = false

                // Cambia de fase y reproduce sonido y vibración
                when (_currentPhase.value) {
                    Phase.FOCUS -> {
                        _currentPhase.value = Phase.BREAK
                        timeRemainingInMillis = 5 * 60 * 1000L // Duración del descanso
                        _timeLeft.value = "05:00"
                    }
                    Phase.BREAK -> {
                        _currentPhase.value = Phase.FOCUS
                        timeRemainingInMillis = 25 * 60 * 1000L // Duración de concentración
                        _timeLeft.value = "25:00"
                    }
                    else -> {}
                }

                // Mostrar notificación con sonido y vibración al cambiar de fase
                showNotification(
                    title = when (_currentPhase.value) {
                        Phase.FOCUS -> "Tiempo de concentración"
                        Phase.BREAK -> "Tiempo de descanso"
                        else -> "Pomodoro"
                    },
                    timeRemainingText = _timeLeft.value ?: "00:00",
                    playSound = true // Sonido y vibración al cambiar de fase
                )

                // Inicia la nueva sesión automáticamente
                startTimer()
            }
        }.start()
    }



    // Pausa el temporizador
    fun pauseTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
    }

    // Restablece el temporizador
    fun resetTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
        _currentPhase.value = Phase.FOCUS
        timeRemainingInMillis = 25 * 60 * 1000L // Restablece a 25 minutos
        _timeLeft.value = "25:00"
    }

    // Muestra la notificación
    private fun showNotification(title: String, timeRemainingText: String, playSound: Boolean = false) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.pomodoro)
            .setContentTitle(title)
            .setContentText("Tiempo restante: $timeRemainingText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Si no se debe reproducir sonido ni vibración, evita alertas repetitivas
        if (!playSound) {
            builder.setOnlyAlertOnce(true)
        }

        // Si playSound es verdadero, añade vibración (y el sonido será automático)
        if (playSound) {
            builder.setVibrate(longArrayOf(0, 500, 500, 500)) // Vibración personalizada
        }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(MainActivity.NOTIFICATION_ID, builder.build())
        }
    }

}