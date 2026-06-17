package com.studyflow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val dailyGoal by vm.dailyGoalMinutes.collectAsState(initial = 120)
    val pomodoroWork by vm.pomodoroWork.collectAsState(initial = 25)
    val pomodoroBreak by vm.pomodoroBreak.collectAsState(initial = 5)
    val defaultMode by vm.defaultMode.collectAsState(initial = "theory")
    val theme by vm.theme.collectAsState(initial = "system")
    val firstDay by vm.firstDayOfWeek.collectAsState(initial = "mon")
    val reminderDaily by vm.reminderDailyEnabled.collectAsState(initial = true)
    val reminderStreak by vm.reminderStreakEnabled.collectAsState(initial = true)
    val maxBreak by vm.maxBreakMinutes.collectAsState(initial = 15)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        SectionCard("Study") {
            SliderRow("Daily goal: ${dailyGoal / 60}h ${dailyGoal % 60}m", dailyGoal.toFloat(), 30f..480f) {
                vm.setDailyGoal(it.toInt())
            }
            SliderRow("Pomodoro work: $pomodoroWork min", pomodoroWork.toFloat(), 10f..60f) {
                vm.setPomodoroWork(it.toInt())
            }
            SliderRow("Pomodoro break: $pomodoroBreak min", pomodoroBreak.toFloat(), 3f..30f) {
                vm.setPomodoroBreak(it.toInt())
            }
            DropdownRow("Default mode", listOf("theory", "practice", "revision", "mock_test"), defaultMode) {
                vm.setDefaultMode(it)
            }
            SliderRow("Max break before nudge: $maxBreak min", maxBreak.toFloat(), 5f..60f) {
                vm.setMaxBreak(it.toInt())
            }
        }

        SectionCard("Appearance") {
            DropdownRow("Theme", listOf("system", "light", "dark"), theme) { vm.setTheme(it) }
            DropdownRow("First day of week", listOf("mon", "sun"), firstDay) { vm.setFirstDayOfWeek(it) }
        }

        SectionCard("Reminders") {
            SwitchRow("Daily study reminder", reminderDaily) { vm.setReminderDailyEnabled(it) }
            SwitchRow("Streak-at-risk reminder", reminderStreak) { vm.setReminderStreakEnabled(it) }
        }

        SectionCard("Data") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.exportCsv() }) { Text("Export CSV") }
                OutlinedButton(onClick = { vm.clearAll() }, colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                )) { Text("Clear all data") }
            }
        }

        Text("StudyFlow v1.0",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun DropdownRow(label: String, options: List<String>, value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { expanded = true }) { Text(value.replaceFirstChar { it.uppercase() }) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.replaceFirstChar { it.uppercase() }) },
                        onClick = { onChange(opt); expanded = false },
                    )
                }
            }
        }
    }
}
