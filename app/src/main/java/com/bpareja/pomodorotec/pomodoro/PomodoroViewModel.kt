package com.bpareja.pomodorotec.pomodoro

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.widget.RemoteViews
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

    companion object {
        var instance: PomodoroViewModel? = null
            private set
    }

    init {
        instance = this
    }

    private val context = getApplication<Application>().applicationContext

    private val _timeLeft = MutableLiveData("25:00")
    val timeLeft: LiveData<String> = _timeLeft

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private val _currentPhase = MutableLiveData(Phase.FOCUS)
    val currentPhase: LiveData<Phase> = _currentPhase

    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingInMillis: Long = 1 * 60 * 1000L // Tiempo inicial para FOCUS
    private var isPaused = false // Variable para controlar el estado de pausa

    var initialTimeInMillis: Long = 0



    // Función para iniciar la sesión de concentración
    private val FOCUS_TIME_IN_MILLIS: Long = 1 * 60 * 1000 // 25 minutos
    private val BREAK_TIME_IN_MILLIS: Long = 1 * 60 * 1000 // 5 minutos



    fun startFocusSession() {
        _currentPhase.value = Phase.FOCUS
        initialTimeInMillis = FOCUS_TIME_IN_MILLIS // Establece el tiempo inicial de la fase de concentración
        timeRemainingInMillis = FOCUS_TIME_IN_MILLIS
        _timeLeft.value = "25:00"
        showNotification("Inicio de Concentración", "25:00", 0)
        startTimer()
    }

    fun startBreakSession() {
        _currentPhase.value = Phase.BREAK
        initialTimeInMillis = BREAK_TIME_IN_MILLIS // Establece el tiempo inicial de la fase de descanso
        timeRemainingInMillis = BREAK_TIME_IN_MILLIS
        _timeLeft.value = "05:00"
        showNotification("Inicio de Descanso", "05:00", 0)
        startTimer()
    }


    // Inicia o reanuda el temporizador
    fun startTimer() {
        if (_isRunning.value == true && !isPaused) return // Si ya está corriendo y no está pausado, no hace nada
        _isRunning.value = true
        countDownTimer = object : CountDownTimer(timeRemainingInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingInMillis = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeText = String.format("%02d:%02d", minutes, seconds)

                // Calcula el progreso basado en la fase actual
                val progress = ((initialTimeInMillis - millisUntilFinished).toFloat() / initialTimeInMillis * 100).toInt()

                // Actualiza el tiempo restante
                _timeLeft.value = timeText

                // Actualiza la notificación con el progreso
                showNotification(
                    title = when (_currentPhase.value) {
                        Phase.FOCUS -> "Tiempo de concentración"
                        Phase.BREAK -> "Tiempo de descanso"
                        else -> "Pomodoro"
                    },
                    timeRemainingText = timeText,
                    progress = progress,
                    playSound = false
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
                        _timeLeft.value = "1:00"
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

    // Pausa o reanuda el temporizador
    fun togglePauseResume() {
        if (isPaused) {
            isPaused = false
            startTimer() // Reanudar el temporizador
        } else {
            pauseTimer() // Pausar el temporizador
        }
    }

    // Pausa el temporizador
    fun pauseTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
        isPaused = true // Marcar como pausado
        showNotification(
            title = "Pausado",
            timeRemainingText = _timeLeft.value ?: "00:00",
            playSound = false
        )
    }

    // Restablece el temporizador
    fun resetTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
        isPaused = false
        _currentPhase.value = Phase.FOCUS
        timeRemainingInMillis = FOCUS_TIME_IN_MILLIS
        _timeLeft.value = "25:00"

        // Actualiza la notificación para reflejar el estado detenido
        showNotification(
            title = "Pomodoro Detenido",
            timeRemainingText = "25:00",
            progress = 0
        )
    }


    // Muestra la notificación
    @SuppressLint("RemoteViewLayout")
    private fun showNotification(title: String, timeRemainingText: String, progress: Int = 0, playSound: Boolean = false) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Acciones para los botones
        val toggleIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_TOGGLE_PAUSE_RESUME"
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_STOP"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Carga el diseño personalizado para la notificación
        val notificationLayout = RemoteViews(context.packageName, R.layout.notification_timer)
        notificationLayout.setTextViewText(R.id.notification_title, title)
        notificationLayout.setTextViewText(R.id.notification_timer, timeRemainingText)
        notificationLayout.setProgressBar(R.id.notification_progress, 100, progress, false)

        //Determinar valor de boton pause
        val pauseResumeText = when {
            !_isRunning.value!! -> "Iniciar" // Cuando el temporizador está detenido
            isPaused -> "Reanudar"          // Cuando está pausado
            else -> "Pausa"                 // Cuando está corriendo
        }

        val builder = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.pomodoro)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()) // Usa el estilo personalizado
            .setCustomContentView(notificationLayout) // Configura el layout para la notificación
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_pause, pauseResumeText, togglePendingIntent) // Botón de Pausa
            .addAction(R.drawable.ic_stop, "Detener", stopPendingIntent) // Botón de Detener

            // Agrega la barra de progreso
            .setProgress(100, progress, false) // Establece el progreso (valor de 0 a 100)

        // Configuración de sonido y vibración
        if (!playSound) {
            builder.setOnlyAlertOnce(true)
            builder.setVibrate(longArrayOf(0, 500, 500, 500))
        } else {
            builder.setVibrate(longArrayOf(0, 500, 500, 500))
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
