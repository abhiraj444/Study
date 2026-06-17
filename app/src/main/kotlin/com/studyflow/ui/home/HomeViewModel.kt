package com.studyflow.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.data.datastore.UserPreferencesDataStore
import com.studyflow.data.db.entity.StudySession
import com.studyflow.data.db.entity.Streak
import com.studyflow.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val todayTotalSeconds: Long = 0L,
    val dailyGoalMinutes: Int = 120,
    val activeSession: StudySession? = null,
    val todaySessions: List<StudySession> = emptyList(),
    val streak: Streak? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SessionRepository,
    private val prefs: UserPreferencesDataStore,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        repo.observeActive(),
        repo.observeForDate(LocalDate.now()),
        repo.observeStreak(),
        prefs.dailyGoalMinutes,
    ) { active, today, streak, goal ->
        HomeUiState(
            todayTotalSeconds = today.sumOf { it.durationSeconds },
            dailyGoalMinutes = goal,
            activeSession = active,
            todaySessions = today,
            streak = streak,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun deleteSession(id: Long) = viewModelScope.launch { repo.deleteById(id) }
}
