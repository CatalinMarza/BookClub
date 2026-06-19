package com.example.bookclub.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.bookclub.data.db.BookClubEntity
import com.example.bookclub.data.db.MembershipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MembershipDao {

    @Upsert
    suspend fun upsert(m: MembershipEntity)

    @Query("DELETE FROM membership WHERE userId = :userId AND clubId = :clubId")
    suspend fun delete(userId: Long, clubId: Long)

    // Returnează cluburile în care userul este membru.
    @Query("""
        SELECT bc.* FROM bookclub bc
        INNER JOIN membership m ON m.clubId = bc.id
        WHERE m.userId = :userId
        ORDER BY bc.startAt ASC
    """)
    fun getClubsForUser(userId: Long): Flow<List<BookClubEntity>>

    // Returnează id-urile cluburilor în care userul este membru.
    @Query("SELECT clubId FROM membership WHERE userId = :userId")
    fun getClubIdsForUser(userId: Long): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM membership WHERE userId = :userId AND clubId = :clubId)")
    suspend fun isMember(userId: Long, clubId: Long): Boolean
}
