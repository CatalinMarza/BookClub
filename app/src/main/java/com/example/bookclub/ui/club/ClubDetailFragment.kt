package com.example.bookclub.ui.club

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.bookclub.R
import com.example.bookclub.data.ServiceLocator
import com.example.bookclub.data.model.ClubComment
import com.example.bookclub.data.model.ClubStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClubDetailFragment : Fragment(R.layout.fragment_club_detail) {

    private val args: ClubDetailFragmentArgs by navArgs()
    private val vm: ClubDetailViewModel by viewModels()

    private var replyTo: ClubComment? = null

    private val expanded = mutableSetOf<Long>()
    private val repliesCache = mutableMapOf<Long, List<ClubComment>>()
    private val repliesCount = mutableMapOf<Long, Int>()

    private lateinit var adapter: ThreadedCommentsAdapter
    private var latestParents: List<ClubComment> = emptyList()

    private var commentsAllowed: Boolean = false
    private var notAllowedReason: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imgCover: ImageView = view.findViewById(R.id.imgCover)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)

        val tvReviewSummary: TextView = view.findViewById(R.id.tvReviewSummary)
        val tvReviewsList: TextView = view.findViewById(R.id.tvReviewsList)

        tvTitle.text = args.title

        imgCover.load(args.coverUrl) {
            placeholder(R.drawable.ic_book_placeholder)
            error(R.drawable.ic_book_placeholder)
            crossfade(true)
        }

        val session = ServiceLocator.sessionManager(requireContext()).get()
        val repo = ServiceLocator.clubsRepository(requireContext())
        val reviewRepository = ServiceLocator.clubReviewRepository(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                reviewRepository.reviewsForClub(args.clubId).collect { reviews ->
                    if (reviews.isEmpty()) {
                        tvReviewSummary.text = "No reviews yet"
                        tvReviewsList.text = ""
                    } else {
                        val average = reviews.map { it.rating }.average()

                        tvReviewSummary.text =
                            "Club rating: %.1f / 5 from %d reviews".format(
                                average,
                                reviews.size
                            )

                        tvReviewsList.text = reviews.joinToString(separator = "\n\n") { review ->
                            val stars = "★".repeat(review.rating) + "☆".repeat(5 - review.rating)
                            "$stars\n${review.comment}"
                        }
                    }
                }
            }
        }

        val replyBar: View = view.findViewById(R.id.replyBar)
        val tvReplyingTo: TextView = view.findViewById(R.id.tvReplyingTo)
        val btnCancelReply: Button = view.findViewById(R.id.btnCancelReply)

        fun clearReply() {
            replyTo = null
            replyBar.isVisible = false
            tvReplyingTo.text = ""
        }

        btnCancelReply.setOnClickListener {
            clearReply()
        }

        val recycler: RecyclerView = view.findViewById(R.id.recyclerComments)

        adapter = ThreadedCommentsAdapter(
            onReplyClick = { comment ->
                if (!commentsAllowed) {
                    notAllowedReason?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                    return@ThreadedCommentsAdapter
                }

                replyTo = comment

                tvReplyingTo.text = getString(
                    R.string.replying_to_fmt,
                    comment.authorName ?: getString(R.string.anonymous)
                )

                replyBar.isVisible = true
            },
            onToggleParent = { parentId, currentlyExpanded ->
                toggleParent(parentId, currentlyExpanded)
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val etComment: EditText = view.findViewById(R.id.etComment)
        val btnSend: Button = view.findViewById(R.id.btnSend)

        val commentSection: View = view.findViewById(R.id.commentSection)
        val tvCommentsDisabled: TextView = view.findViewById(R.id.tvCommentsDisabled)

        commentSection.isVisible = true
        tvCommentsDisabled.isVisible = false
        etComment.isEnabled = true
        btnSend.isEnabled = true

        viewLifecycleOwner.lifecycleScope.launch {
            val sessionOk = session != null
            val userId = session?.userId

            val club = withContext(Dispatchers.IO) {
                repo.getClub(args.clubId)
            }

            val liveOk = club != null &&
                    club.status == ClubStatus.LIVE &&
                    repo.isLive(club)

            val memberOk = if (userId != null && club != null) {
                withContext(Dispatchers.IO) {
                    repo.isMember(userId, club.id)
                }
            } else {
                false
            }

            commentsAllowed = sessionOk && liveOk && memberOk

            notAllowedReason = when {
                !sessionOk -> getString(R.string.err_not_logged_in)
                club == null -> getString(R.string.err_club_not_found)
                !liveOk -> getString(R.string.comments_disabled)
                !memberOk -> getString(R.string.err_only_members_comment)
                else -> null
            }

            if (commentsAllowed) {
                commentSection.isVisible = true
                tvCommentsDisabled.isVisible = false
                etComment.isEnabled = true
                btnSend.isEnabled = true
            } else {
                commentSection.isVisible = false
                tvCommentsDisabled.isVisible = true
                etComment.isEnabled = false
                btnSend.isEnabled = false
                clearReply()
            }
        }

        btnSend.setOnClickListener {
            if (!commentsAllowed) {
                notAllowedReason?.let { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            val content = etComment.text?.toString()?.trim().orEmpty()

            if (content.isEmpty()) {
                return@setOnClickListener
            }

            val userId = session?.userId

            if (userId == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.err_not_logged_in),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val parentId = replyTo?.id

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    vm.addCommentAwait(
                        clubId = args.clubId,
                        userId = userId,
                        content = content,
                        parentId = parentId
                    )

                    etComment.text?.clear()
                    clearReply()

                    if (parentId != null) {
                        expanded.add(parentId)
                        refreshReplies(parentId)
                    }
                } catch (t: Throwable) {
                    Toast.makeText(
                        requireContext(),
                        t.message ?: getString(R.string.err_comment_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        vm.observeComments(args.clubId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.comments.collect { parents ->
                    latestParents = parents
                    buildAndShowThread()
                }
            }
        }
    }

    private fun refreshReplies(parentId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val replies = withContext(Dispatchers.IO) {
                vm.fetchReplies(parentId)
            }

            repliesCache[parentId] = replies
            repliesCount[parentId] = replies.size

            buildAndShowThread()
        }
    }

    private fun toggleParent(parentId: Long, currentlyExpanded: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (currentlyExpanded) {
                expanded.remove(parentId)
                buildAndShowThread()
            } else {
                expanded.add(parentId)

                if (!repliesCache.containsKey(parentId)) {
                    val replies = withContext(Dispatchers.IO) {
                        vm.fetchReplies(parentId)
                    }

                    repliesCache[parentId] = replies
                    repliesCount[parentId] = replies.size
                }

                buildAndShowThread()
            }
        }
    }

    private fun buildAndShowThread() {
        val items = mutableListOf<ThreadItem>()

        for (parent in latestParents) {
            val count = repliesCount[parent.id]

            items += ThreadItem.Parent(
                comment = parent,
                isExpanded = parent.id in expanded,
                repliesCount = count
            )

            if (parent.id in expanded) {
                repliesCache[parent.id].orEmpty().forEach { child ->
                    items += ThreadItem.Reply(child)
                }
            } else if (count == null) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val c = vm.countReplies(parent.id)
                    repliesCount[parent.id] = c

                    withContext(Dispatchers.Main) {
                        buildAndShowThread()
                    }
                }
            }
        }

        adapter.submitList(items)
    }
}