package com.studyflow.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studyflow.data.db.entity.Subject

private val SUBJECT_COLORS = listOf(
    "#7C6AF7", "#48C9B0", "#5B8AF0", "#F0955B",
    "#B05BF0", "#F05B5B", "#5BF0B7", "#F0D55B",
)

@Composable
fun SubjectsScreen(vm: SubjectsViewModel = hiltViewModel()) {
    val subjects by vm.subjects.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Subjects", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                FilledIconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects, key = { it.id }) { subject ->
                    SubjectRow(
                        subject = subject,
                        onArchive = { vm.archive(subject.id) },
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddSubjectSheet(
            onAdd = { name, color, goal ->
                vm.addSubject(name, color, "book", goal)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun SubjectRow(subject: Subject, onArchive: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(14.dp).clip(RoundedCornerShape(50))
                    .background(parseColor(subject.colorHex)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Daily goal: ${subject.dailyGoalMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Default.Archive, contentDescription = "Archive")
            }
        }
    }
}

@Composable
private fun AddSubjectSheet(onAdd: (name: String, color: String, goal: Int) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var colorIndex by remember { mutableStateOf(0) }
    var goal by remember { mutableStateOf(60) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Add Subject", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Subject name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Text("Color", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SUBJECT_COLORS.forEachIndexed { i, hex ->
                    val selected = colorIndex == i
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(50))
                            .background(parseColor(hex))
                            .clickable { colorIndex = i },
                    ) {
                        if (selected) {
                            Box(
                                Modifier.fillMaxSize().padding(8.dp)
                                    .background(MaterialTheme.colorScheme.onPrimary)
                                    .clip(RoundedCornerShape(50)),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Daily goal: ${goal} minutes", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = goal.toFloat(),
                onValueChange = { goal = it.toInt() },
                valueRange = 15f..480f,
                steps = 0,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onAdd(name.ifBlank { "Subject" }, SUBJECT_COLORS[colorIndex], goal) },
                    enabled = name.isNotBlank(),
                ) { Text("Add") }
            }
        }
    }
}

private fun parseColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color(0xFF7C6AF7))
