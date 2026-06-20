package com.example.bookclub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bookclub.data.db.ClubReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClubReviewDao {
    //Inserează un review în tabela club_review
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(review: ClubReviewEntity)

    //Returnează toate reviewurile pentru un club specificat
    @Query("""
        SELECT * FROM club_review
        WHERE clubId = :clubId
        ORDER BY createdAt DESC
    """)
    fun getReviewsForClub(clubId: Long): Flow<List<ClubReviewEntity>>

    //Returnează ratingul mediu pentru un club specificat
    @Query("""
        SELECT AVG(rating)
        FROM club_review
        WHERE clubId = :clubId
    """)
    fun getAverageRatingForClub(clubId: Long): Flow<Double?>

    //Returnează numărul de reviewuri pentru un club specificat
    @Query("""
        SELECT COUNT(*)
        FROM club_review
        WHERE clubId = :clubId
    """)
    fun getReviewCountForClub(clubId: Long): Flow<Int>

    //Returnează dacă un utilizator specificat a lasat un review la un club
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM club_review
            WHERE clubId = :clubId
            AND reviewerUserId = :userId
        )
    """)
    suspend fun hasUserReviewedClub(clubId: Long, userId: Long): Boolean
}