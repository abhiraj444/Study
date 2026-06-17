package com.studyflow.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studyflow.data.datastore.UserPreferencesDataStore
import com.studyflow.ui.analytics.AnalyticsScreen
import com.studyflow.ui.history.HistoryScreen
import com.studyflow.ui.home.HomeScreen
import com.studyflow.ui.navigation.BottomNav
import com.studyflow.ui.navigation.Destinations
import com.studyflow.ui.onboarding.OnboardingFlow
import com.studyflow.ui.settings.SettingsScreen
import com.studyflow.ui.subjects.SubjectsScreen
import com.studyflow.ui.theme.StudyFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyFlowTheme {
                val onboardingDone by prefs.onboardingComplete.collectAsStateWithLifecycle(initialValue = true)
                val scope = rememberCoroutineScope()
                if (!onboardingDone) {
                    OnboardingFlow(onFinished = {
                        scope.launch { prefs.setOnboardingComplete(true) }
                    })
                } else {
                    StudyFlowRoot()
                }
            }
        }
    }
}

@Composable
private fun StudyFlowRoot() {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = { BottomNav(nav) },
    ) { inner ->
        Surface(Modifier.padding(inner)) {
            NavHost(navController = nav, startDestination = Destinations.HOME) {
                composable(Destinations.HOME) { HomeScreen() }
                composable(Destinations.HISTORY) { HistoryScreen() }
                composable(Destinations.ANALYTICS) { AnalyticsScreen() }
                composable(Destinations.SUBJECTS) { SubjectsScreen() }
                composable(Destinations.SETTINGS) { SettingsScreen() }
            }
        }
    }
}
