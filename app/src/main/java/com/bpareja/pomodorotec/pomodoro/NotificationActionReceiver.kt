package com.bpareja.pomodorotec.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "ACTION_TOGGLE_PAUSE_RESUME" -> {
                // Llama a la función togglePauseResume() en el ViewModel
                PomodoroViewModel.instance?.togglePauseResume()
            }
            "ACTION_STOP" -> {
                // Lógica para detener el temporizador si es necesario
                PomodoroViewModel.instance?.resetTimer()
            }
        }
    }
}
