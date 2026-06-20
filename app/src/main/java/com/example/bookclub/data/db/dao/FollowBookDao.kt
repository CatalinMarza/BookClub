package com.example.bookclub.data.db.dao

import androidx.room.*
import com.example.bookclub.data.db.FollowBookEntity
import kotlinx.coroutines.flow.Flow

// DAO pentru tabela follow_book. Gestionează cărțile urmărite de useri.
@Dao
interface FollowBookDao {
    @Upsert
    suspend fun follow(e: FollowBookEntity)

    @Query("DELETE FROM follow_book WHERE userId = :userId AND workId = :workId")
    suspend fun unfollow(userId: Long, workId: String)

    @Query("SELECT workId FROM follow_book WHERE userId = :userId")
    fun getFollowedWorkIds(userId: Long): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM follow_book WHERE userId = :userId AND workId = :workId)")
    suspend fun hasFollow(userId: Long, workId: String): Boolean

    @Query("SELECT userId FROM follow_book WHERE workId = :workId")
    suspend fun getFollowerIdsForWork(workId: String): List<Long>
}
