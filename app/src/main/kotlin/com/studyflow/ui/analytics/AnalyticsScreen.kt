package com.studyflow.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyflow.ui.theme.ModeMockTest
import com.studyflow.ui.theme.ModePractice
import com.studyflow.ui.theme.ModeRevision
import com.studyflow.ui.theme.ModeTheory
import com.studyflow.ui.theme.modeColor

@Composable
fun AnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val stats = state.stats

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Range selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalyticsRange.values().forEach { r ->
                FilterChip(
                    selected = state.range == r,
                    onClick = { vm.setRange(r) },
                    label = { Text(r.label) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (stats == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        // Stat cards
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Sessions", "${stats.totalSessions}", Modifier.weight(1f))
            StatCard("Avg Length", "${stats.averageSessionSeconds / 60} min", Modifier.weight(1f))
            StatCard("Longest", "${stats.longestSessionSeconds / 60} min", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        stats.mostProductiveHour?.let { h ->
            val hour12 = if (h.hour == 0) 12 else if (h.hour > 12) h.hour - 12 else h.hour
            val ampm = if (h.hour < 12) "AM" else "PM"
            StatCard("Best Hour", "$hour12:00 $ampm", Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(20.dp))

        // Daily bar chart (text-based bars)
        ChartCard("Daily Study") {
            val max = (stats.dailyTotals.maxOfOrNull { it.total } ?: 1L).coerceAtLeast(1)
            stats.dailyTotals.forEach { d ->
                BarRow(label = d.day.takeLast(5), value = d.total, max = max)
            }
            if (stats.dailyTotals.isEmpty()) {
                Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Subject breakdown
        ChartCard("By Subject") {
            val max = (stats.subjectTotals.maxOfOrNull { it.total } ?: 1L).coerceAtLeast(1)
            stats.subjectTotals.forEach { s ->
                BarRow(label = s.subject, value = s.total, max = max, color = modeColor(null))
            }
            if (stats.subjectTotals.isEmpty()) {
                Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Mode breakdown
        ChartCard("By Mode") {
            val max = (stats.modeTotals.maxOfOrNull { it.total } ?: 1L).coerceAtLeast(1)
            stats.modeTotals.forEach { m ->
                BarRow(
                    label = m.mode.replaceFirstChar { it.uppercase() },
                    value = m.total,
                    max = max,
                    color = when (m.mode) {
                        "theory" -> ModeTheory
                        "practice" -> ModePractice
                        "revision" -> ModeRevision
                        "mock_test" -> ModeMockTest
                        else -> modeColor(null)
                    },
                )
            }
            if (stats.modeTotals.isEmpty()) {
                Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ColumnScope.BarRow(label: String, value: Long, max: Long, color: Color = MaterialTheme.colorScheme.primary) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
        Box(
            Modifier
                .weight(1f)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((value.toFloat() / max).coerceIn(0f, 1f))
                    .height(20.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("${value / 60}m", style = MaterialTheme.typography.bodyMedium)
    }
}
