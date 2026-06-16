// file: com/example/bookclub/ui/inbox/InboxFragment.kt
package com.example.bookclub.ui.inbox

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookclub.R
import kotlinx.coroutines.launch

class InboxFragment : Fragment(R.layout.fragment_inbox) {

    private val vm: InboxViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler: RecyclerView = view.findViewById(R.id.recyclerInbox)
        val btnMarkAll: Button = view.findViewById(R.id.btnMarkAll)

        val adapter = InboxAdapter(
            onClick = { item ->
                vm.markRead(item.id)

                val action = InboxFragmentDirections
                    .actionInboxFragmentToClubDetailFragment(
                        clubId = item.clubId,
                        title = item.title,
                        coverUrl = item.coverUrl ?: ""
                    )

                findNavController().navigate(action)
            },
            onDeleteClick = { item ->
                vm.deleteNotification(item.id)
            }
        )

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnMarkAll.setOnClickListener {
            vm.markAllRead()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.items.collect { list ->
                    adapter.submitList(list)

                    val hasUnread = list.any { !it.isRead }

                    btnMarkAll.isEnabled = hasUnread
                    btnMarkAll.alpha = if (hasUnread) 1f else 0.45f
                }
            }
        }
    }
}