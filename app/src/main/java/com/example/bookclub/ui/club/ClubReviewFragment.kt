package com.example.bookclub.ui.club

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.bookclub.R
import com.example.bookclub.data.ServiceLocator
import com.example.bookclub.data.model.ClubStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClubReviewFragment : Fragment(R.layout.fragment_club_review) {

    private var clubId: Long = -1L
    private var clubTitle: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvClubTitle: TextView = view.findViewById(R.id.tvClubTitle)
        val tvReviewSummary: TextView = view.findViewById(R.id.tvReviewSummary)
        val tvExistingReviews: TextView = view.findViewById(R.id.tvExistingReviews)
        val tvFormTitle: TextView = view.findViewById(R.id.tvFormTitle)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val etComment: EditText = view.findViewById(R.id.etReviewComment)
        val btnSubmit: Button = view.findViewById(R.id.btnSubmitReview)

        clubId = arguments?.getLong("clubId") ?: -1L
        clubTitle = arguments?.getString("title") ?: ""

        if (clubId == -1L) {
            Toast.makeText(requireContext(), "Club not found", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        tvClubTitle.text = clubTitle

        val session = ServiceLocator.sessionManager(requireContext()).get()

        if (session == null) {
            Toast.makeText(requireContext(), "You must be logged in", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val clubsRepository = ServiceLocator.clubsRepository(requireContext())
        val reviewRepository = ServiceLocator.clubReviewRepository(requireContext())

        fun disableReviewForm(message: String) {
            tvFormTitle.text = message
            ratingBar.isEnabled = false
            etComment.isEnabled = false
            btnSubmit.isEnabled = false
            btnSubmit.text = message
            btnSubmit.visibility = View.GONE
            ratingBar.visibility = View.GONE
            etComment.visibility = View.GONE
        }

        fun enableReviewForm() {
            tvFormTitle.text = "Add your review"
            ratingBar.isEnabled = true
            etComment.isEnabled = true
            btnSubmit.isEnabled = true
            btnSubmit.text = "Submit review"
            btnSubmit.visibility = View.VISIBLE
            ratingBar.visibility = View.VISIBLE
            etComment.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val canReview = withContext(Dispatchers.IO) {
                val club = clubsRepository.getClub(clubId)

                if (club == null) {
                    return@withContext "Club not found"
                }

                val isMember = clubsRepository.isMember(session.userId, clubId)

                if (!isMember) {
                    return@withContext "Only members can review this club"
                }

                val clubClosed = club.status == ClubStatus.CLOSED ||
                        java.time.Instant.now().isAfter(club.closeAt)

                if (!clubClosed) {
                    return@withContext "Review available after club ends"
                }

                val alreadyReviewed = reviewRepository.hasUserReviewedClub(
                    clubId = clubId,
                    userId = session.userId
                )

                if (alreadyReviewed) {
                    return@withContext "Review already submitted"
                }

                null
            }

            if (canReview == null) {
                enableReviewForm()
            } else {
                disableReviewForm(canReview)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                reviewRepository.reviewsForClub(clubId).collect { reviews ->
                    if (reviews.isEmpty()) {
                        tvReviewSummary.text = "No reviews yet"
                        tvExistingReviews.text = ""
                    } else {
                        val average = reviews.map { it.rating }.average()

                        tvReviewSummary.text =
                            "Rating: %.1f / 5 from %d review%s".format(
                                average,
                                reviews.size,
                                if (reviews.size == 1) "" else "s"
                            )

                        tvExistingReviews.text = reviews.joinToString(separator = "\n\n") { review ->
                            val stars = "★".repeat(review.rating) + "☆".repeat(5 - review.rating)
                            "$stars\n${review.comment}"
                        }
                    }
                }
            }
        }

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = etComment.text.toString().trim()

            if (rating == 0) {
                Toast.makeText(requireContext(), "Select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (comment.isEmpty()) {
                Toast.makeText(requireContext(), "Write a comment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        reviewRepository.addReview(
                            clubId = clubId,
                            reviewerUserId = session.userId,
                            rating = rating,
                            comment = comment
                        )
                    }

                    Toast.makeText(
                        requireContext(),
                        "Review saved",
                        Toast.LENGTH_SHORT
                    ).show()

                    disableReviewForm("Review already submitted")

                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        e.message ?: "Could not save review",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}