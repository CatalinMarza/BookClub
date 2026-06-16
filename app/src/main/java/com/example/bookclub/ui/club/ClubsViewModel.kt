// file: com/example/bookclub/ui/club/ClubsViewModel.kt
package com.example.bookclub.ui.club

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.data.ServiceLocator
import com.example.bookclub.data.db.BookClubEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class ClubsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ServiceLocator.clubsRepository(app)
    private val reviewRepo = ServiceLocator.clubReviewRepository(app)
    private val session = ServiceLocator.sessionManager(app)

    private val userId: Long
        get() = session.currentUserId ?: 1L

    val uiClubs: StateFlow<List<UiClub>> =
        combine(
            repo.listAll(),
            repo.listForUser(userId)
        ) { all, mine ->
            all to mine
        }.flatMapLatest { (all, mine) ->
            if (all.isEmpty()) {
                flowOf(emptyList())
            } else {
                val mineIds = mine.map { it.id }.toSet()

                val clubFlows = all.map { club ->
                    combine(
                        reviewRepo.averageRatingForClub(club.id),
                        reviewRepo.reviewCountForClub(club.id)
                    ) { average, count ->
                        UiClub(
                            club = club,
                            isMember = club.id in mineIds,
                            averageRating = average,
                            reviewCount = count,
                            currentUserReviewed = false
                        )
                    }
                }

                combine(clubFlows) { array ->
                    array.toList()
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val clubs: StateFlow<List<BookClubEntity>> =
        repo.listAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun createClub(
        adminId: Long,
        workId: String,
        title: String,
        author: String,
        coverUrl: String?,
        description: String?,
        startAt: Instant,
        closeAt: Instant
    ) {
        viewModelScope.launch {
            repo.createClub(
                adminId = adminId,
                workId = workId,
                title = title,
                author = author,
                coverUrl = coverUrl,
                description = description,
                startAt = startAt,
                closeAt = closeAt
            )
        }
    }

    fun joinClub(clubId: Long) {
        viewModelScope.launch {
            repo.joinClub(userId, clubId)
        }
    }

    fun leaveClub(clubId: Long) {
        viewModelScope.launch {
            repo.leaveClub(userId, clubId)
        }
    }
}