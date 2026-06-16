// file: com/example/bookclub/data/repository/CommentsRepository.kt
package com.example.bookclub.data.repository

import com.example.bookclub.data.db.CommentEntity
import com.example.bookclub.data.db.InboxEntity
import com.example.bookclub.data.db.VoteEntity
import com.example.bookclub.data.db.dao.CommentDao
import com.example.bookclub.data.db.dao.InboxDao
import com.example.bookclub.data.db.dao.VoteDao
import com.example.bookclub.data.model.ClubComment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.Instant

class CommentsRepository(
    private val commentDao: CommentDao,
    private val voteDao: VoteDao,
    private val inboxDao: InboxDao
) {

    /** Top-level comments + autor. */
    fun getComments(clubId: Long): Flow<List<ClubComment>> =
        commentDao.getTopLevelWithAuthor(clubId).map { rows ->
            rows.map { row ->
                ClubComment(
                    id = row.id,
                    clubId = row.clubId,
                    userId = row.userId,
                    authorName = row.authorNickname?.takeIf { it.isNotBlank() }
                        ?: row.authorEmail?.takeIf { it.isNotBlank() }
                        ?: "Anonymous",
                    content = row.content,
                    createdAt = row.createdAt
                )
            }
        }

    /** Replies pentru un comentariu. */
    fun getRepliesFlow(parentId: Long): Flow<List<ClubComment>> =
        commentDao.getReplies(parentId).map { rows ->
            rows.map { e ->
                ClubComment(
                    id = e.id,
                    clubId = e.clubId,
                    userId = e.userId,
                    authorName = null,
                    content = e.content,
                    createdAt = e.createdAt
                )
            }
        }

    /** Inserare comentariu sau reply. */
    suspend fun insertComment(
        clubId: Long,
        userId: Long,
        content: String,
        parentId: Long? = null
    ): Long {
        val commentId = commentDao.insert(
            CommentEntity(
                clubId = clubId,
                userId = userId,
                content = content.trim(),
                createdAt = Instant.now(),
                parentId = parentId
            )
        )

        if (parentId != null) {
            createReplyNotificationIfNeeded(
                clubId = clubId,
                replyCommentId = commentId,
                parentId = parentId,
                replierUserId = userId
            )
        }

        return commentId
    }

    /** Creează notificare când cineva răspunde la comentariul altui user. */
    private suspend fun createReplyNotificationIfNeeded(
        clubId: Long,
        replyCommentId: Long,
        parentId: Long,
        replierUserId: Long
    ) {
        val parent = commentDao.getById(parentId)

        if (parent == null) {
            return
        }

        if (parent.userId == replierUserId) {
            return
        }

        val payload = JSONObject()
            .put("type", "comment_reply")
            .put("clubId", clubId)
            .put("commentId", replyCommentId)
            .put("parentId", parentId)
            .toString()

        inboxDao.insert(
            InboxEntity(
                userId = parent.userId,
                type = "COMMENT_REPLY",
                payloadJson = payload,
                isRead = false,
                createdAt = Instant.now()
            )
        )
    }

    /** Vote pentru comentariu: 1 sau -1. */
    suspend fun vote(commentId: Long, userId: Long, value: Int) {
        val normalizedValue = if (value >= 0) 1 else -1

        voteDao.upsert(
            VoteEntity(
                commentId = commentId,
                userId = userId,
                value = normalizedValue,
                createdAt = Instant.now()
            )
        )
    }

    /** Ia replicile o singură dată, cu autor. */
    suspend fun getRepliesOnce(parentId: Long): List<ClubComment> =
        commentDao.getRepliesWithAuthor(parentId).map { row ->
            ClubComment(
                id = row.id,
                clubId = row.clubId,
                userId = row.userId,
                authorName = row.authorNickname?.takeIf { it.isNotBlank() }
                    ?: row.authorEmail?.takeIf { it.isNotBlank() }
                    ?: "Anonymous",
                content = row.content,
                createdAt = row.createdAt
            )
        }

    /** Numărul de replici pentru un comentariu. */
    suspend fun countReplies(parentId: Long): Int =
        commentDao.countReplies(parentId)
}