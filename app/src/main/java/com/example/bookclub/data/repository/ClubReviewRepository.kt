package com.example.bookclub.data.repository

import com.example.bookclub.data.db.ClubReviewEntity
import com.example.bookclub.data.db.dao.ClubReviewDao
import java.time.Instant

// Repository-ul valideaza datele review-ului și salveaza in room
// Verificarile de acces pentru formular sunt facute în ClubReviewFragment.
class ClubReviewRepository(
    private val dao: ClubReviewDao
) {
    fun reviewsForClub(clubId: Long) =
        dao.getReviewsForClub(clubId)

    fun averageRatingForClub(clubId: Long) =
        dao.getAverageRatingForClub(clubId)

    fun reviewCountForClub(clubId: Long) =
        dao.getReviewCountForClub(clubId)

    suspend fun hasUserReviewedClub(
        clubId: Long,
        userId: Long
    ): Boolean {
        return dao.hasUserReviewedClub(clubId, userId)
    }

    suspend fun addReview(
        clubId: Long,
        reviewerUserId: Long,
        rating: Int,
        comment: String
    ) {
        if (rating !in 1..5) {
            throw IllegalArgumentException("Rating must be between 1 and 5")
        }

        if (comment.isBlank()) {
            throw IllegalArgumentException("Comment cannot be empty")
        }

        val alreadyReviewed = dao.hasUserReviewedClub(
            clubId = clubId,
            userId = reviewerUserId
        )

        if (alreadyReviewed) {
            throw IllegalStateException("You already reviewed this club")
        }

        dao.insert(
            ClubReviewEntity(
                clubId = clubId,
                reviewerUserId = reviewerUserId,
                rating = rating,
                comment = comment.trim(),
                createdAt = Instant.now()
            )
        )
    }
}