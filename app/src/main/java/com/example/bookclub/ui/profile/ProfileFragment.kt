package com.example.bookclub.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookclub.R
import com.example.bookclub.data.ServiceLocator
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = ServiceLocator.sessionManager(requireContext())
        val session = sessionManager.get()
        val userId = session?.userId ?: return

        val txtName: TextView = view.findViewById(R.id.txt_name)
        val txtEmail: TextView = view.findViewById(R.id.txt_email)
        val recycler: RecyclerView = view.findViewById(R.id.recyclerFollowed)
        val txtFollowTitle: TextView = view.findViewById(R.id.txt_follow_title)

        txtName.text = session.nickname
        txtEmail.text = session.email

        val adapter = FollowedAdapter()
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val repo = ServiceLocator.clubsRepository(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repo.listForFollowedBooks(userId).collect { clubs ->
                if (clubs.isNotEmpty()) {
                    txtFollowTitle.visibility = View.VISIBLE
                    recycler.visibility = View.VISIBLE
                    adapter.submitList(clubs)
                } else {
                    txtFollowTitle.visibility = View.GONE
                    recycler.visibility = View.GONE
                }
            }
        }

        view.findViewById<Button>(R.id.btn_logout)?.setOnClickListener {
            sessionManager.clear()
            findNavController().navigate(R.id.loginFragment)
        }
        val btnMyClubs: Button = view.findViewById(R.id.btnMyClubs)

        btnMyClubs.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_clubsFragment)
        }
    }
}
