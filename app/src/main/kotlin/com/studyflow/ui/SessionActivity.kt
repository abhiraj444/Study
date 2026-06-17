package com.studyflow.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.studyflow.service.StudyTimerService
import com.studyflow.ui.session.SessionScreen
import com.studyflow.ui.theme.StudyFlowTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point for Google Assistant / Gemini App Actions.
 *
 * How it works (spec §3.3):
 *
 * 1. Assistant matches a voice query to a `<capability>` in shortcuts.xml
 *    (e.g. "Hey Google, start studying Math" → com.studyflow.START_STUDY).
 * 2. Assistant fires an `android.intent.action.VIEW` intent targeting this
 *    activity, carrying extras:
 *      - subject, chapter, mode        (for START_STUDY)
 *      - action = "STOP"               (for STOP_STUDY)
 *      - breakDuration = "10 minutes"  (for TAKE_BREAK)
 * 3. We forward those extras to [SessionViewModel], which calls the
 *    corresponding use case. The foreground service picks it up from there.
 *
 * Because `launchMode="singleTop"`, a second voice command while this
 * activity is already on top will be delivered via `onNewIntent` —
 * so we override it and route through the same handler.
 *
 * Testing locally (without Google Assistant):
 *   adb shell am start -n com.studyflow/.ui.SessionActivity \
 *       --es subject "Math" --es chapter "Trigonometry" --es mode "theory"
 *
 *   adb shell am start -n com.studyflow/.ui.SessionActivity \
 *       --es action "STOP"
 */
@AndroidEntryPoint
class SessionActivity : ComponentActivity() {

    private val viewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleVoiceIntent(intent)
        setContent {
            StudyFlowTheme {
                val uiState by viewModel.uiState.collectAsState()
                SessionScreen(
                    state = uiState,
                    onStop = viewModel::onStopClicked,
                    onBreak = viewModel::onBreakClicked,
                    onResume = viewModel::onResumeClicked,
                    onRatingSubmitted = viewModel::submitRating,
                    onDismissRating = viewModel::dismissRating,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Important: don't re-trigger Compose setContent here — just route extras.
        handleVoiceIntent(intent)
    }

    private fun handleVoiceIntent(intent: Intent?) {
        intent ?: return
        val subject = intent.getStringExtra("subject")
        val chapter = intent.getStringExtra("chapter")
        val mode = intent.getStringExtra("mode")
        val action = intent.getStringExtra("action")
        val breakDuration = intent.getStringExtra("breakDuration")

        when {
            action.equals("STOP", ignoreCase = true) -> viewModel.handleVoiceStop()
            breakDuration != null -> viewModel.handleVoiceBreak(breakDuration)
            subject != null -> viewModel.handleVoiceStart(subject, chapter, mode)
        }
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        // If a session is active, treat Back as "go home" not "kill the app"
        // so the foreground service can keep running. The user can stop via
        // the notification.
        moveTaskToBack(true)
    }
}
