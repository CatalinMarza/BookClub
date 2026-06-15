package com.example.bookclub.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.bookclub.data.db.FollowUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowUserDao {

    @Insert
    suspend fun follow(entity: FollowUserEntity)

    @Query("""
        DELETE FROM follow_user 
        WHERE followerId = :follower 
        AND `following` = :following
    """)
    suspend fun unfollow(follower: Long, following: Long)

    @Query("""
        SELECT `following` 
        FROM follow_user 
        WHERE followerId = :userId
    """)
    fun getFollowingIds(userId: Long): Flow<List<Long>>

    @Query("""
        SELECT followerId 
        FROM follow_user 
        WHERE `following` = :userId
    """)
    fun getFollowerIds(userId: Long): Flow<List<Long>>
}