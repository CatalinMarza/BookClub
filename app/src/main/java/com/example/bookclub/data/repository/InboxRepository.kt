// file: com/example/bookclub/data/repository/InboxRepository.kt
package com.example.bookclub.data.repository

import com.example.bookclub.data.db.InboxEntity
import com.example.bookclub.data.db.dao.InboxDao
import com.example.bookclub.ui.inbox.InboxUi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.Instant

class InboxRepository(
    private val inboxDao: InboxDao,
    private val clubLookup: suspend (Long) -> ClubLite? = { _ -> null }
) {

    fun listUiForUser(userId: Long): Flow<List<InboxUi>> =
        inboxDao.listForUser(userId).map { list ->
            coroutineScope {
                list.map { entity ->
                    async { entity.toUi(clubLookup) }
                }.map { it.await() }
            }
        }

    suspend fun markRead(id: Long) {
        inboxDao.markRead(id)
    }

    suspend fun markAllRead(userId: Long) {
        inboxDao.markAllRead(userId)
    }

    suspend fun deleteById(id: Long) {
        inboxDao.deleteById(id)
    }
}

data class ClubLite(
    val title: String,
    val coverUrl: String?
)

private fun JSONObject.optStringOrNull(key: String): String? =
    optString(key).takeIf { it.isNotBlank() }

private fun JSONObject.optLongPositive(key: String): Long? =
    optLong(key, 0L).takeIf { it > 0 }

private suspend fun InboxEntity.toUi(
    clubLookup: suspend (Long) -> ClubLite?
): InboxUi {
    val payload = try {
        JSONObject(payloadJson ?: "{}")
    } catch (_: Throwable) {
        JSONObject()
    }

    val clubId: Long = payload.optLongPositive("clubId") ?: 0L

    val payloadType = payload.optStringOrNull("type")
    val entityType = type.takeIf { it.isNotBlank() }

    val notificationType = (payloadType ?: entityType)
        ?.trim()
        ?.lowercase()

    var titleFromPayload: String? = payload.optStringOrNull("title")
    var coverFromPayload: String? = payload.optStringOrNull("coverUrl")

    val startAt: Instant? = payload.optStringOrNull("startAt")
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }

    if (titleFromPayload.isNullOrBlank() || coverFromPayload.isNullOrBlank()) {
        val club = if (clubId > 0) {
            runCatching { clubLookup(clubId) }.getOrNull()
        } else {
            null
        }

        if (titleFromPayload.isNullOrBlank()) {
            titleFromPayload = club?.title
        }

        if (coverFromPayload.isNullOrBlank()) {
            coverFromPayload = club?.coverUrl
        }
    }

    val finalTitle = titleFromPayload ?: if (clubId > 0) {
        "Club #$clubId"
    } else {
        "Untitled"
    }

    val message = when (notificationType) {
        "comment_reply" -> "A răspuns la un comentariu"
        "commentreply" -> "A răspuns la un comentariu"
        "comment_reply_notification" -> "A răspuns la un comentariu"
        "comment_reply_created" -> "A răspuns la un comentariu"

        "new_comment" -> "Comentariu nou în club"
        "comment" -> "Comentariu nou în club"
        "comment_created" -> "Comentariu nou în club"

        "club_starting" -> "Clubul urmează să înceapă"
        "club_started" -> "Clubul a început"
        "club_start" -> "Clubul a început"
        "started" -> "Clubul a început"

        "club_closed" -> "Clubul s-a încheiat"
        "club_ended" -> "Clubul s-a încheiat"
        "ended" -> "Clubul s-a încheiat"

        else -> "Notificare pentru club"
    }

    return InboxUi(
        id = id,
        clubId = clubId,
        title = finalTitle,
        message = message,
        coverUrl = coverFromPayload,
        startAt = startAt,
        createdAt = createdAt,
        isRead = isRead
    )
}