package com.studyflow.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyflow.data.db.entity.StudySession
import com.studyflow.ui.components.ModeBadge
import com.studyflow.ui.theme.modeColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: HistoryViewModel = hiltViewModel()) {
    val sessions by vm.sessions.collectAsState()
    var query by remember { mutableStateOf("") }
    var filterSubject by remember { mutableStateOf<String?>(null) }

    val filtered = sessions.filter { s ->
        (filterSubject == null || s.subject.equals(filterSubject, true)) &&
        (query.isBlank() ||
            s.subject.contains(query, true) ||
            (s.chapter?.contains(query, true) == true) ||
            (s.notes?.contains(query, true) == true))
    }

    val subjectsAvailable = sessions.map { it.subject }.distinct().sorted()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search sessions") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filterSubject == null,
                onClick = { filterSubject = null },
                label = { Text("All") },
            )
            subjectsAvailable.take(8).forEach { sub ->
                FilterChip(
                    selected = filterSubject == sub,
                    onClick = { filterSubject = sub },
                    label = { Text(sub) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filtered, key = { it.id }) { s ->
                HistoryRow(s, onDelete = { vm.delete(s.id) })
            }
        }
    }
}

@Composable
private fun HistoryRow(s: StudySession, onDelete: () -> Unit) {
    val df = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(modeColor(s.mode)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    ModeBadge(s.mode)
                }
                s.chapter?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    df.format(Date(s.startTime)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${s.durationSeconds / 60} min", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (s.source == "voice") "Voice" else "Manual",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
