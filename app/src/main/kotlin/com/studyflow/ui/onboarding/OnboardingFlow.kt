package com.studyflow.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studyflow.ui.components.PrimaryButton

@Composable
fun OnboardingFlow(onFinished: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        when (step) {
            0 -> WelcomeStep(onNext = { step = 1 })
            1 -> SubjectsStep(onNext = { step = 2 }, onBack = { step = 0 })
            2 -> GoalStep(onNext = { step = 3 }, onBack = { step = 1 })
            3 -> VoiceStep(onFinish = onFinished, onBack = { step = 2 })
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("StudyFlow", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Study smarter. Track everything.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        Bullet("Voice-first — start sessions with \"Hey Google\"")
        Bullet("Track subjects, chapters, theory vs practice")
        Bullet("Analytics, streaks, and smart reminders")
        Spacer(Modifier.height(40.dp))
        PrimaryButton(text = "Get Started", onClick = onNext)
    }
}

@Composable
private fun Bullet(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectsStep(onNext: () -> Unit, onBack: () -> Unit) {
    val presets = listOf("Math", "Physics", "Chemistry", "Biology", "History", "Geography", "Polity", "Economics", "English", "Current Affairs")
    val selected = remember { mutableStateListOf<String>() }

    Column(Modifier.fillMaxSize()) {
        Text("Pick your subjects", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { s ->
                FilterChip(
                    selected = s in selected,
                    onClick = {
                        if (s in selected) selected.remove(s) else selected.add(s)
                    },
                    label = { Text(s) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "Next", onClick = onNext)
        }
    }
}

@Composable
private fun GoalStep(onNext: () -> Unit, onBack: () -> Unit) {
    var goalMin by remember { mutableStateOf(120) }
    Column(Modifier.fillMaxSize()) {
        Text("Daily Goal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("${goalMin / 60}h ${goalMin % 60}m",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Slider(
            value = goalMin.toFloat(),
            onValueChange = { goalMin = it.toInt() },
            valueRange = 30f..480f,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "Next", onClick = onNext)
        }
    }
}

@Composable
private fun VoiceStep(onFinish: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Enable Voice", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "Once installed, try saying:\n\n" +
            "\u201CHey Google, start studying Math chapter 3 theory\u201D\n\n" +
            "Then later:\n\n" +
            "\u201CHey Google, I am done studying\u201D",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "Done", onClick = onFinish)
        }
    }
}
