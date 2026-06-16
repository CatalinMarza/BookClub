// file: com/example/bookclub/ui/inbox/InboxViewModel.kt
package com.example.bookclub.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.data.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ServiceLocator.inboxRepository(app)
    private val session = ServiceLocator.sessionManager(app)

    private val userId: Long
        get() = session.currentUserId ?: 1L

    val items: StateFlow<List<InboxUi>> =
        repo.listUiForUser(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    fun markRead(id: Long) {
        viewModelScope.launch {
            repo.markRead(id)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repo.markAllRead(userId)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repo.deleteById(id)
        }
    }
}