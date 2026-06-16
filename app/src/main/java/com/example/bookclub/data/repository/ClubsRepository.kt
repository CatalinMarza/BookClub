// file: com/example/bookclub/data/repository/ClubsRepository.kt
package com.example.bookclub.data.repository

import com.example.bookclub.data.db.BookClubEntity
import com.example.bookclub.data.db.CommentEntity
import com.example.bookclub.data.db.InboxEntity
import com.example.bookclub.data.db.MembershipEntity
import com.example.bookclub.data.db.dao.BookClubDao
import com.example.bookclub.data.db.dao.CommentDao
import com.example.bookclub.data.db.dao.FollowBookDao
import com.example.bookclub.data.db.dao.InboxDao
import com.example.bookclub.data.db.dao.MembershipDao
import com.example.bookclub.data.model.ClubComment
import com.example.bookclub.data.model.ClubStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.Instant

class ClubsRepository(
    private val clubDao: BookClubDao,
    private val membershipDao: MembershipDao,
    private val followBookDao: FollowBookDao,
    private val inboxDao: InboxDao,
    private val commentDao: CommentDao
) {
    fun listAll(): Flow<List<BookClubEntity>> = clubDao.getAllOrderByStart()

    fun search(query: String): Flow<List<BookClubEntity>> = clubDao.search(query)

    fun listForUser(userId: Long): Flow<List<BookClubEntity>> =
        membershipDao.getClubsForUser(userId)

    fun listForFollowedBooks(userId: Long): Flow<List<BookClubEntity>> =
        clubDao.listForFollowedBooks(userId)

    fun clubFlow(clubId: Long) =
        clubDao.getByIdFlow(clubId)

    fun membershipsForUser(userId: Long): Flow<Set<Long>> =
        membershipDao.getClubIdsForUser(userId).map { it.toSet() }

    suspend fun isMember(userId: Long, clubId: Long): Boolean =
        membershipDao.isMember(userId, clubId)

    suspend fun createClub(
        adminId: Long,
        workId: String,
        title: String,
        author: String,
        coverUrl: String?,
        description: String?,
        startAt: Instant,
        closeAt: Instant
    ): Long {
        if (clubDao.existsActiveForWork(workId)) {
            throw IllegalStateException("Active club already exists for this work")
        }

        if (!closeAt.isAfter(startAt)) {
            throw IllegalArgumentException("End date must be after start date")
        }

        val now = Instant.now()

        val status = if (startAt.isAfter(now)) {
            ClubStatus.SCHEDULED
        } else {
            ClubStatus.LIVE
        }

        val clubId = clubDao.insert(
            BookClubEntity(
                id = 0L,
                workId = workId,
                title = title,
                author = author,
                coverUrl = coverUrl,
                description = description,
                createdBy = adminId,
                status = status,
                startAt = startAt,
                closeAt = closeAt
            )
        )

        val followers = followBookDao.getFollowerIdsForWork(workId)

        val payload = """
            {
              "type":"NEW_CLUB_FOR_FOLLOWED_BOOK",
              "clubId":$clubId,
              "workId":${JSONObject.quote(workId)},
              "title":${JSONObject.quote(title)},
              "coverUrl":${JSONObject.quote(coverUrl ?: "")},
              "startAt":${JSONObject.quote(startAt.toString())}
            }
        """.trimIndent()

        val inboxNow = Instant.now()

        followers.forEach { uid ->
            inboxDao.insert(
                InboxEntity(
                    userId = uid,
                    type = "NEW_CLUB_FOR_FOLLOWED_BOOK",
                    payloadJson = payload,
                    isRead = false,
                    createdAt = inboxNow
                )
            )
        }

        return clubId
    }

    suspend fun joinClub(userId: Long, clubId: Long) {
        membershipDao.upsert(
            MembershipEntity(
                clubId = clubId,
                userId = userId
            )
        )

        val club = clubDao.getById(clubId)

        val payload = if (club != null) {
            """
            {
              "type":"JOIN_CONFIRMED",
              "clubId":$clubId,
              "title":${JSONObject.quote(club.title)},
              "coverUrl":${JSONObject.quote(club.coverUrl ?: "")},
              "startAt":${JSONObject.quote(club.startAt.toString())}
            }
            """.trimIndent()
        } else {
            """{"type":"JOIN_CONFIRMED","clubId":$clubId}"""
        }

        inboxDao.insert(
            InboxEntity(
                userId = userId,
                type = "JOIN_CONFIRMED",
                payloadJson = payload,
                isRead = false,
                createdAt = Instant.now()
            )
        )
    }

    suspend fun leaveClub(userId: Long, clubId: Long) {
        membershipDao.delete(userId, clubId)
    }

    fun commentsFlow(clubId: Long): Flow<List<ClubComment>> =
        commentDao.getTopLevelWithAuthor(clubId).map { list ->
            list.map { e ->
                ClubComment(
                    id = e.id,
                    clubId = e.clubId,
                    userId = e.userId,
                    content = e.content,
                    authorName = e.authorNickname?.takeIf { it.isNotBlank() } ?: e.authorEmail,
                    createdAt = e.createdAt
                )
            }
        }

    suspend fun getClub(id: Long): BookClubEntity? =
        clubDao.getById(id)

    fun isLive(club: BookClubEntity): Boolean {
        val now = Instant.now()

        return now.isAfter(club.startAt) && now.isBefore(club.closeAt)
    }

    suspend fun addComment(
        clubId: Long,
        userId: Long,
        content: String,
        parentId: Long? = null
    ) {
        commentDao.insert(
            CommentEntity(
                id = 0L,
                clubId = clubId,
                userId = userId,
                content = content,
                createdAt = Instant.now(),
                parentId = parentId
            )
        )
    }

    suspend fun getLiteById(id: Long): ClubLite? =
        clubDao.getById(id)?.let {
            ClubLite(
                title = it.title,
                coverUrl = it.coverUrl
            )
        }
}