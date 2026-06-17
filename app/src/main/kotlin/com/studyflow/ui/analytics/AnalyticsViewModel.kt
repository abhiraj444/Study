package com.studyflow.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.data.db.dao.DayTotal
import com.studyflow.data.db.dao.ModeTotal
import com.studyflow.data.db.dao.SubjectTotal
import com.studyflow.domain.usecase.GetWeeklyStatsUseCase
import com.studyflow.domain.usecase.WeeklyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AnalyticsRange(val days: Int, val label: String) {
    WEEK(7, "Week"),
    MONTH(30, "Month"),
    ALL_TIME(365, "All Time"),
}

data class AnalyticsUiState(
    val range: AnalyticsRange = AnalyticsRange.WEEK,
    val stats: WeeklyStats? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getStats: GetWeeklyStatsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = _state

    init { load(AnalyticsRange.WEEK) }

    fun setRange(r: AnalyticsRange) = load(r)

    private fun load(r: AnalyticsRange) {
        viewModelScope.launch {
            _state.value = AnalyticsUiState(range = r, stats = getStats(r.days))
        }
    }
}
