package com.studyflow.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyflow.service.StudyTimerService
import com.studyflow.ui.components.ModeBadge
import com.studyflow.ui.components.PrimaryButton
import com.studyflow.ui.components.SecondaryButton

@Composable
fun SessionScreen(
    state: SessionUiState,
    onStop: () -> Unit,
    onBreak: () -> Unit,
    onResume: () -> Unit,
    onRatingSubmitted: (mood: Int, focus: Int, note: String) -> Unit,
    onDismissRating: () -> Unit,
) {
    val isActive = state.timerState == StudyTimerService.TimerState.ACTIVE
    val isBreak = state.timerState == StudyTimerService.TimerState.ON_BREAK
    val hasActive = state.activeSession != null || state.timerState != StudyTimerService.TimerState.IDLE

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // Mode badge + subject title
            if (state.subject.isNotBlank()) {
                ModeBadge(state.mode)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.subject,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                state.chapter?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(64.dp))

            // Circular timer
            TimerCircle(
                elapsedMs = computeElapsedMs(state),
                isBreak = isBreak,
                isActive = isActive,
                breakRemainingMs = if (isBreak) (state.breakEndEpochMs - System.currentTimeMillis()).coerceAtLeast(0L) else 0L,
            )

            Spacer(Modifier.height(48.dp))

            // Action buttons
            if (hasActive) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        isActive -> SecondaryButton(
                            text = "Take Break",
                            icon = Icons.Default.Coffee,
                            onClick = onBreak,
                        )
                        isBreak -> PrimaryButton(
                            text = "Resume",
                            icon = Icons.Default.PlayArrow,
                            onClick = onResume,
                        )
                    }
                    PrimaryButton(
                        text = "Stop Session",
                        icon = Icons.Default.Stop,
                        onClick = onStop,
                        containerColor = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Text(
                    "No active session. Say \"Hey Google, start studying Math\" or tap a subject on Home.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // Rating sheet
        AnimatedVisibility(
            visible = state.showRatingSheet,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            RatingSheet(onSubmit = onRatingSubmitted, onDismiss = onDismissRating)
        }
    }
}

private fun computeElapsedMs(state: SessionUiState): Long {
    if (state.startEpochMs == 0L) return 0L
    val now = System.currentTimeMillis()
    val raw = now - state.startEpochMs - state.accumulatedBreakMs
    return raw.coerceAtLeast(0L)
}

@Composable
private fun TimerCircle(
    elapsedMs: Long,
    isBreak: Boolean,
    isActive: Boolean,
    breakRemainingMs: Long,
) {
    val displayMs = if (isBreak) breakRemainingMs else elapsedMs
    val text = StudyTimerService.formatMs(displayMs)

    val infinite = rememberInfiniteTransition()
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(
        Modifier
            .size(280.dp)
            .scale(if (isActive) pulse else 1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 56.sp,
                ),
                color = if (isBreak) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    isBreak -> "ON BREAK"
                    isActive -> "STUDYING"
                    else -> "IDLE"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RatingSheet(
    onSubmit: (mood: Int, focus: Int, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var mood by remember { mutableStateOf(0) }
    var focus by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }

    val moodLabels = listOf("\uD83D\uDE34", "\uD83D\uDE15", "\uD83D\uDE10", "\uD83D\uDE42", "\uD83D\uDD25")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("How did it go?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            Text("Mood", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moodLabels.forEachIndexed { i, label ->
                    AssistChip(
                        onClick = { mood = i + 1 },
                        label = { Text(label, fontSize = 22.sp) },
                        colors = if (mood == i + 1)
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        else AssistChipDefaults.assistChipColors(),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Focus", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row {
                repeat(5) { i ->
                    IconButton(onClick = { focus = i + 1 }) {
                        Text(
                            if (focus > i) "\u2605" else "\u2606",
                            fontSize = 28.sp,
                            color = if (focus > i) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Add a note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss) { Text("Skip") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onSubmit(mood, focus, note) }) { Text("Save & Close") }
            }
        }
    }
}
