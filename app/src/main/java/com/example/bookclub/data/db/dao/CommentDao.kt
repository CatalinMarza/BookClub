package com.example.bookclub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.bookclub.data.db.CommentEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CommentDao {

    @Insert
    suspend fun insert(comment: CommentEntity): Long

    // Comentarii principale împreună cu autorul.
    @Query("""
        SELECT 
            c.id            AS id,
            c.clubId        AS clubId,
            c.userId        AS userId,
            c.content       AS content,
            c.createdAt     AS createdAt,
            u.nickname      AS authorNickname,
            u.email         AS authorEmail
        FROM comment c
        JOIN user u ON u.id = c.userId
        WHERE c.clubId = :clubId AND c.parentId IS NULL
        ORDER BY c.createdAt DESC
    """)
    fun getTopLevelWithAuthor(clubId: Long): Flow<List<CommentWithAuthor>>

    // Comentarii cu autorul și reply-urile.: parent desc, apoi replies asc
    @Query("""
        SELECT 
            c.id            AS id,
            c.clubId        AS clubId,
            c.userId        AS userId,
            c.content       AS content,
            c.createdAt     AS createdAt,
            c.parentId      AS parentId,
            u.nickname      AS authorNickname,
            u.email         AS authorEmail
        FROM comment c
        JOIN user u ON u.id = c.userId
        WHERE c.clubId = :clubId
        ORDER BY 
          CASE WHEN c.parentId IS NULL THEN c.createdAt ELSE (SELECT p.createdAt FROM comment p WHERE p.id = c.parentId) END DESC,
          CASE WHEN c.parentId IS NULL THEN 0 ELSE 1 END,
          c.createdAt ASC
    """)
    fun getAllWithAuthor(clubId: Long): Flow<List<CommentWithAuthorFull>>

    @Query("SELECT * FROM comment WHERE parentId = :parentId ORDER BY createdAt ASC")
    fun getReplies(parentId: Long): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comment WHERE id = :id")
    suspend fun getById(id: Long): CommentEntity?

    @Query("""
    SELECT 
        c.id            AS id,
        c.clubId        AS clubId,
        c.userId        AS userId,
        c.content       AS content,
        c.createdAt     AS createdAt,
        u.nickname      AS authorNickname,
        u.email         AS authorEmail
    FROM comment c
    JOIN user u ON u.id = c.userId
    WHERE c.parentId = :parentId
    ORDER BY c.createdAt ASC
""")
    suspend fun getRepliesWithAuthor(parentId: Long): List<CommentWithAuthor>

    @Query("SELECT COUNT(*) FROM comment WHERE parentId = :parentId")
    suspend fun countReplies(parentId: Long): Int
}

/** DTO-uri pentru JOIN */
data class CommentWithAuthor(
    val id: Long,
    val clubId: Long,
    val userId: Long,
    val content: String,
    val createdAt: Instant,
    val authorNickname: String?,
    val authorEmail: String?
)

data class CommentWithAuthorFull(
    val id: Long,
    val clubId: Long,
    val userId: Long,
    val content: String,
    val createdAt: Instant,
    val parentId: Long?,           // ✅ important pentru reply
    val authorNickname: String?,
    val authorEmail: String?
)
