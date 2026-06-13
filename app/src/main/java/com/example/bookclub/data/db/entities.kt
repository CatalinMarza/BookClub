package com.example.bookclub.data.db

import androidx.room.*
import com.example.bookclub.data.model.ClubStatus
import java.time.Instant

// Entitati Room (tabele) si indecsi
@Entity(tableName = "user",
    indices = [Index("email", unique = true), Index("nickname", unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val password: String,
    val nickname: String,
    val role: String,
    val createdAt: Instant
)

@Entity(tableName = "bookclub",
    indices = [Index("status"), Index("startAt"), Index("workId"), Index("createdBy")])
data class BookClubEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workId: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val description: String?,
    val createdBy: Long,
    val status: ClubStatus,
    val startAt: Instant,
    val closeAt: Instant
)

@Entity(
    tableName = "membership",
    primaryKeys = ["clubId", "userId"],
    indices = [Index("userId")]
)
data class MembershipEntity(
    val clubId: Long,
    val userId: Long
)

@Entity(
    tableName = "comment",
    indices = [Index("clubId"), Index("parentId")]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clubId: Long,
    val userId: Long,
    val content: String,
    val createdAt: Instant,
    val parentId: Long?
)

@Entity(
    tableName = "vote",
    primaryKeys = ["commentId", "userId"],
    indices = [Index("commentId")]
)
data class VoteEntity(
    val commentId: Long,
    val userId: Long,
    val value: Int,
    val createdAt: Instant
)

@Entity(
    tableName = "follow_user",
    primaryKeys = ["followerId", "following"],
    indices = [Index("following")]
)
data class FollowUserEntity(
    val followerId: Long,
    val following: Long
)

@Entity(
    tableName = "follow_book",
    primaryKeys = ["userId", "workId"],
    indices = [Index("workId")]
)
data class FollowBookEntity(
    val userId: Long,
    val workId: String
)

@Entity(
    tableName = "inbox",
    indices = [Index("userId"), Index("isRead"), Index("createdAt")]
)


data class InboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val type: String,
    val payloadJson: String,
    val isRead: Boolean,
    val createdAt: Instant
)

@Entity(
    tableName = "club_review",
    indices = [
        Index("clubId"),
        Index("reviewerUserId")
    ]
)
data class ClubReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clubId: Long,
    val reviewerUserId: Long,
    val rating: Int,
    val comment: String,
    val createdAt: Instant
)