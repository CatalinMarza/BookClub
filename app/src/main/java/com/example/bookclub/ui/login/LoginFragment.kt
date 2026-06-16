package com.example.bookclub.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bookclub.R
import com.example.bookclub.data.ServiceLocator
import com.example.bookclub.data.db.UserEntity
import com.example.bookclub.data.session.Session
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class LoginFragment : Fragment() {

    companion object {
        private const val ADMIN_EMAIL = "admin@demo.local"
    }

    private val session by lazy {
        ServiceLocator.sessionManager(requireContext())
    }

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (session.isLoggedIn()) {
            goToHome()
            return
        }

        val emailInput = view.findViewById<EditText>(R.id.edt_email)
        val passwordInput = view.findViewById<EditText>(R.id.edt_password)
        val btnLogin = view.findViewById<Button>(R.id.btn_login)
        val tvRegister = view.findViewById<TextView>(R.id.tv_register)
        val tvError = view.findViewById<TextView>(R.id.tv_error)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        btnLogin.setOnClickListener {
            val email = emailInput.text?.toString()?.trim().orEmpty()
            val password = passwordInput.text?.toString().orEmpty()

            tvError.text = ""

            if (email.isEmpty()) {
                tvError.text = "Email is required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                tvError.text = "Password is required"
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user

                    if (firebaseUser == null) {
                        progress.visibility = View.GONE
                        btnLogin.isEnabled = true
                        tvError.text = "Authentication failed"
                        return@addOnSuccessListener
                    }

                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val localUser = withContext(Dispatchers.IO) {
                                getOrCreateLocalUser(
                                    firebaseUid = firebaseUser.uid,
                                    email = firebaseUser.email ?: email,
                                    nickname = firebaseUser.email
                                        ?.substringBefore("@")
                                        ?.ifBlank { "user" }
                                        ?: "user"
                                )
                            }

                            session.save(
                                Session(
                                    userId = localUser.id,
                                    email = localUser.email,
                                    nickname = localUser.nickname,
                                    role = localUser.role,
                                    createdAtEpochMs = localUser.createdAt.toEpochMilli()
                                )
                            )

                            progress.visibility = View.GONE
                            btnLogin.isEnabled = true
                            goToHome()

                        } catch (e: Exception) {
                            progress.visibility = View.GONE
                            btnLogin.isEnabled = true
                            tvError.text = e.message ?: "Could not create local session"
                        }
                    }
                }
                .addOnFailureListener { e ->
                    progress.visibility = View.GONE
                    btnLogin.isEnabled = true
                    tvError.text = e.message ?: "Authentication error"
                }
        }

        tvRegister.setOnClickListener {
            val action = LoginFragmentDirections
                .actionLoginFragmentToRegisterFragment(
                    emailInput.text?.toString()?.trim().orEmpty()
                )

            findNavController().navigate(action)
        }
    }

    private suspend fun getOrCreateLocalUser(
        firebaseUid: String,
        email: String,
        nickname: String
    ): UserEntity {
        val userDao = ServiceLocator.db(requireContext()).userDao()
        val normalizedEmail = email.trim().lowercase()

        val existingUser = userDao.getByEmail(normalizedEmail)

        if (existingUser != null) {
            if (normalizedEmail == ADMIN_EMAIL && existingUser.role != "ADMIN") {
                userDao.updateRoleByEmail(normalizedEmail, "ADMIN")
                return existingUser.copy(role = "ADMIN")
            }

            return existingUser
        }

        val role = if (normalizedEmail == ADMIN_EMAIL) {
            "ADMIN"
        } else {
            "USER"
        }

        val newUser = UserEntity(
            firebaseUid = firebaseUid,
            email = normalizedEmail,
            password = "firebase_auth",
            nickname = nickname,
            role = role,
            createdAt = Instant.now()
        )

        val newId = userDao.insert(newUser)

        return newUser.copy(id = newId)
    }

    private fun goToHome() {
        val action = LoginFragmentDirections.actionLoginFragmentToNavigationHome()
        findNavController().navigate(action)
    }
}