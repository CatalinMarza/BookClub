package com.example.bookclub.ui.club

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.data.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface CreateState {
    object Idle : CreateState
    object Loading : CreateState
    data class Success(val clubId: Long) : CreateState
    data class Error(val t: Throwable) : CreateState
}

class CreateClubViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ServiceLocator.clubsRepository(app)

    private val _state = MutableStateFlow<CreateState>(CreateState.Idle)
    val state: StateFlow<CreateState> = _state

    fun createClub(
        adminId: Long,
        workId: String,
        title: String,
        author: String,
        description: String?,
        coverUrl: String?,
        startAt: Instant,
        closeAt: Instant
    ) = viewModelScope.launch {
        _state.value = CreateState.Loading

        try {
            val id = repo.createClub(
                adminId = adminId,
                workId = workId,
                title = title,
                author = author,
                coverUrl = coverUrl,
                description = description,
                startAt = startAt,
                closeAt = closeAt
            )

            _state.value = CreateState.Success(id)
        } catch (t: Throwable) {
            _state.value = CreateState.Error(t)
        }
    }

    fun reset() {
        _state.value = CreateState.Idle
    }
}