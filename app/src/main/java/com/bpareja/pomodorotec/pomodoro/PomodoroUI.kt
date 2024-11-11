package com.bpareja.pomodorotec.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel = viewModel()) {
    val timeLeft = viewModel.timeLeft.observeAsState("25:00")
    val isRunning = viewModel.isRunning.observeAsState(false)
    val currentPhase = viewModel.currentPhase.observeAsState(Phase.FOCUS)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (currentPhase.value) {
                Phase.FOCUS -> "Tiempo de concentración"
                Phase.BREAK -> "Tiempo de descanso"
                else -> ""
            },
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = timeLeft.value,
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row {

            Button(onClick = { viewModel.startTimer() }, enabled = !isRunning.value) {
                Text("Iniciar")
            }

            /*Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.pauseTimer() }, enabled = isRunning.value) {
                Text("Pausar")
            }*/

            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.resetTimer() }) {
                Text("Reiniciar")
            }

        }

    }

}