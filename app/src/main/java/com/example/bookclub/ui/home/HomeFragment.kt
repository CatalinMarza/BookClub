package com.example.bookclub.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookclub.R
import com.example.bookclub.ui.club.ClubsAdapter
import com.example.bookclub.ui.club.ClubsViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: ClubsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler: RecyclerView = view.findViewById(R.id.recyclerClubs)
        val empty: TextView = view.findViewById(R.id.txtEmpty)

        val adapter = ClubsAdapter(
            onPrimaryClick = { ui ->
                if (ui.isMember) {
                    val action = HomeFragmentDirections.actionHomeFragmentToClubDetailFragment(
                        clubId = ui.club.id,
                        title = ui.club.title,
                        coverUrl = ui.club.coverUrl ?: ""
                    )

                    findNavController().navigate(action)
                } else {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            viewModel.joinClub(ui.club.id)

                            Toast.makeText(
                                requireContext(),
                                getString(R.string.joined_club),
                                Toast.LENGTH_SHORT
                            ).show()

                            val action = HomeFragmentDirections.actionHomeFragmentToClubDetailFragment(
                                clubId = ui.club.id,
                                title = ui.club.title,
                                coverUrl = ui.club.coverUrl ?: ""
                            )

                            findNavController().navigate(action)
                        } catch (t: Throwable) {
                            Toast.makeText(
                                requireContext(),
                                t.message ?: getString(R.string.join_failed),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },

            onLeaveClick = { ui ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        viewModel.leaveClub(ui.club.id)

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.left_club),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (t: Throwable) {
                        Toast.makeText(
                            requireContext(),
                            t.message ?: getString(R.string.leave_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },

            onReviewClick = { ui ->
                val bundle = Bundle().apply {
                    putLong("clubId", ui.club.id)
                    putString("title", ui.club.title)
                }

                findNavController().navigate(
                    R.id.clubReviewFragment,
                    bundle
                )
            },

            onBookDetailsClick = { ui ->
                val bundle = Bundle().apply {
                    putString("workId", ui.club.workId)
                    putString("title", ui.club.title)
                    putString("author", ui.club.author)
                    putString("coverUrl", ui.club.coverUrl ?: "")
                }

                findNavController().navigate(
                    R.id.bookDetailFragment,
                    bundle
                )
            },

            onCardClick = { ui ->
                if (ui.isMember) {
                    val action = HomeFragmentDirections.actionHomeFragmentToClubDetailFragment(
                        clubId = ui.club.id,
                        title = ui.club.title,
                        coverUrl = ui.club.coverUrl ?: ""
                    )

                    findNavController().navigate(action)
                }
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiClubs.collect { list ->
                adapter.submitList(list)
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}