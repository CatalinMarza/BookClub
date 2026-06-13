package com.example.bookclub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bookclub.data.db.ClubReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubReviewDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(review: ClubReviewEntity)

    @Query("""
        SELECT * FROM club_review
        WHERE clubId = :clubId
        ORDER BY createdAt DESC
    """)
    fun getReviewsForClub(clubId: Long): Flow<List<ClubReviewEntity>>

    @Query("""
        SELECT AVG(rating)
        FROM club_review
        WHERE clubId = :clubId
    """)
    fun getAverageRatingForClub(clubId: Long): Flow<Double?>

    @Query("""
        SELECT COUNT(*)
        FROM club_review
        WHERE clubId = :clubId
    """)
    fun getReviewCountForClub(clubId: Long): Flow<Int>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM club_review
            WHERE clubId = :clubId
            AND reviewerUserId = :userId
        )
    """)
    suspend fun hasUserReviewedClub(clubId: Long, userId: Long): Boolean
}