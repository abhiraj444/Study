package com.studyflow.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.data.db.entity.Subject
import com.studyflow.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val repo: SessionRepository,
) : ViewModel() {
    val subjects: StateFlow<List<Subject>> = repo.observeActiveSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSubject(name: String, color: String, icon: String, goalMin: Int) = viewModelScope.launch {
        if (repo.getSubjectByName(name) == null) {
            repo.upsertSubject(Subject(name = name, colorHex = color, iconName = icon, dailyGoalMinutes = goalMin))
        }
    }
    fun archive(id: Long) = viewModelScope.launch { repo.archiveSubject(id) }
    fun update(s: Subject) = viewModelScope.launch { repo.upsertSubject(s) }
}
