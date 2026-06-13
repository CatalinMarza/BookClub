package com.example.bookclub.ui.club

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bookclub.R
import com.example.bookclub.data.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClubReviewFragment : Fragment(R.layout.fragment_club_review) {

    private var clubId: Long = -1L
    private var clubTitle: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvClubTitle: TextView = view.findViewById(R.id.tvClubTitle)
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

        viewLifecycleOwner.lifecycleScope.launch {
            val alreadyReviewed = withContext(Dispatchers.IO) {
                reviewRepository.hasUserReviewedClub(
                    clubId = clubId,
                    userId = session.userId
                )
            }

            if (alreadyReviewed) {
                Toast.makeText(
                    requireContext(),
                    "You already reviewed this club",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
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
                    val isMember = withContext(Dispatchers.IO) {
                        clubsRepository.isMember(session.userId, clubId)
                    }

                    if (!isMember) {
                        Toast.makeText(
                            requireContext(),
                            "Only members can review this club",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

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

                    findNavController().popBackStack()

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