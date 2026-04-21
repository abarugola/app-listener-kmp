package org.example.app_listener_kmp.ui.features.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.app_listener_kmp.data.AndroidBlockConfigRepository
import org.example.app_listener_kmp.platform.FocusModeScheduler
import org.example.app_listener_kmp.platform.SleepModeScheduler

@Composable
actual fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val repository = AndroidBlockConfigRepository(context)

    // Estado del Modo Sueño — leemos el valor guardado en SharedPreferences
    // para que el toggle refleje la configuración real al abrir la pantalla
    var sleepModeEnabled by remember {
        mutableStateOf(repository.isSleepModeEnabled())
    }

    var focusModeEnabled by remember {
        mutableStateOf(repository.isFocusModeEnabled())
    }

    val hasApps = remember { repository.getBlockedPackages().isNotEmpty() }

    // Leemos las horas guardadas para mostrarlas en los botones
    val focusRange = remember { repository.getFocusRange() }
    var focusStartHour by remember { mutableIntStateOf(focusRange.startHour) }
    var focusStartMinute by remember {mutableIntStateOf(focusRange.startMinute)}
    var focusEndHour by remember { mutableIntStateOf(focusRange.endHour)}
    var focusEndMinute by remember { mutableIntStateOf(focusRange.endMinute)}

    val sleepRange = remember { repository.getSleepRange() }
    var sleepStartHour by remember { mutableIntStateOf(sleepRange.startHour) }
    var sleepStartMinute by remember { mutableIntStateOf(sleepRange.startMinute) }
    var sleepEndHour by remember { mutableIntStateOf(sleepRange.endHour) }
    var sleepEndMinute by remember { mutableIntStateOf(sleepRange.endMinute) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Header con botón de regreso
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar"
                )
            }
            Text(
                text = "Configuración",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card Modo Enfoque
        ModeCard(
            emoji = "🎯",
            title = "Modo Enfoque",
            subtitle = if (hasApps) "Pomodoro · 25 min / 5 min descanso"
            else "Configura apps a bloquear primero",
            enabled = focusModeEnabled && hasApps, // solo activo si hay apps
            onToggle = { enabled ->
                if (hasApps) {
                    focusModeEnabled = enabled
                    repository.setFocusModeEnabled(enabled)
                    if (enabled) FocusModeScheduler.schedule(context)
                    else FocusModeScheduler.cancel(context)
                }
            }
        ) {

            Spacer(modifier = Modifier.height(12.dp))
            TimeSelector(
                label = "Inicio de enfoque",
                hour = focusStartHour,
                minute = focusStartMinute,
                onTimeSelected = { h, m ->
                    focusStartHour = h
                    focusStartMinute = m
                    repository.setFocusRange(repository.getFocusRange().copy(startHour = h, startMinute = m))
                    if (focusModeEnabled) FocusModeScheduler.schedule(context)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TimeSelector(
                label = "Fin de enfoque",
                hour = focusEndHour,
                minute = focusEndMinute,
                onTimeSelected = { h, m ->
                    focusEndHour = h
                    focusEndMinute = m
                    repository.setFocusRange(repository.getFocusRange().copy(endHour = h, endMinute = m))
                    if (focusModeEnabled) FocusModeScheduler.schedule(context)
                }
            )
        }


        Spacer(modifier = Modifier.height(12.dp))

        // Card Modo Sueño
        ModeCard(
            emoji = "🌙",
            title = "Modo Sueño",
            subtitle = if (hasApps) "Bloqueo por horario"
                    else "Configura apps a bloquear primero",
            enabled = sleepModeEnabled && hasApps,
            onToggle = { enabled ->
                if (hasApps) {
                    sleepModeEnabled = enabled
                    repository.setSleepModeEnabled(enabled)
                    if (enabled) SleepModeScheduler.schedule(context)
                    else SleepModeScheduler.cancel(context)
                }
            }
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            TimeSelector(
                label = "Hora de dormir",
                hour = sleepStartHour,
                minute = sleepStartMinute,
                onTimeSelected = { h, m ->
                    sleepStartHour = h
                    sleepStartMinute = m
                    repository.setSleepRange(repository.getSleepRange().copy(startHour = h, startMinute = m))
                    if (sleepModeEnabled) SleepModeScheduler.schedule(context)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TimeSelector(
                label = "Hora de despertar",
                hour = sleepEndHour,
                minute = sleepEndMinute,
                onTimeSelected = { h, m ->
                    sleepEndHour = h
                    sleepEndMinute = m
                    repository.setSleepRange(repository.getSleepRange().copy(endHour = h, endMinute = m))
                    if (sleepModeEnabled) SleepModeScheduler.schedule(context)
                }
            )
        }
    }
}

// Composable reutilizable para mostrar y seleccionar una hora
// Usamos TimePickerDialog que es el selector de hora nativo de Android
@Composable
fun TimeSelector(
    label: String,
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        // Botón que muestra la hora actual y abre el picker al tocarlo
        TextButton(onClick = {
            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    onTimeSelected(selectedHour, selectedMinute)
                },
                hour,
                minute,
                true // true = formato 24 horas, false = AM/PM
            ).show()
        }) {
            // Formateamos la hora con ceros a la izquierda para que se vea bien
            // %02d significa "muestra el número con al menos 2 dígitos, rellena con 0"
            Text(
                text = "%02d:%02d".format(hour, minute),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Card genérica para cada modo — la reutilizamos para Enfoque y Sueño
@Composable
fun ModeCard(
    emoji: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$emoji $title",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            content()
        }
    }
}
