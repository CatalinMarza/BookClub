package com.example.bookclub.ui.register

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
import androidx.navigation.fragment.navArgs
import com.example.bookclub.R
import com.example.bookclub.data.ServiceLocator
import com.example.bookclub.data.db.UserEntity
import com.example.bookclub.data.session.Session
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class RegisterFragment : Fragment() {

    private val args: RegisterFragmentArgs by navArgs()

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
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (session.isLoggedIn()) {
            goToHome()
            return
        }

        val emailInput = view.findViewById<EditText>(R.id.edt_email)
        val nicknameInput = view.findViewById<EditText>(R.id.edt_nickname)
        val passwordInput = view.findViewById<EditText>(R.id.edt_password)
        val confirmInput = view.findViewById<EditText>(R.id.edt_confirm)
        val btnRegister = view.findViewById<Button>(R.id.btn_register)
        val tvLogin = view.findViewById<TextView>(R.id.tv_login)
        val tvError = view.findViewById<TextView>(R.id.tv_error)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        emailInput.setText(args.email)

        tvLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        btnRegister.setOnClickListener {
            val email = emailInput.text?.toString()?.trim().orEmpty()
            val nickname = nicknameInput.text?.toString()?.trim().orEmpty()
            val password = passwordInput.text?.toString().orEmpty()
            val confirm = confirmInput.text?.toString().orEmpty()

            tvError.text = ""

            if (email.isEmpty()) {
                tvError.text = "Email is required"
                return@setOnClickListener
            }

            if (nickname.isEmpty()) {
                tvError.text = "Nickname is required"
                return@setOnClickListener
            }

            if (password.length < 6) {
                tvError.text = "Password must have at least 6 characters"
                return@setOnClickListener
            }

            if (password != confirm) {
                tvError.text = "Passwords do not match"
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btnRegister.isEnabled = false
            tvLogin.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user

                    if (firebaseUser == null) {
                        progress.visibility = View.GONE
                        btnRegister.isEnabled = true
                        tvLogin.isEnabled = true
                        tvError.text = "Registration failed"
                        return@addOnSuccessListener
                    }

                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val localUser = withContext(Dispatchers.IO) {
                                createOrGetLocalUser(
                                    firebaseUid = firebaseUser.uid,
                                    email = firebaseUser.email ?: email,
                                    nickname = nickname
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
                            btnRegister.isEnabled = true
                            tvLogin.isEnabled = true

                            goToHome()

                        } catch (e: Exception) {
                            progress.visibility = View.GONE
                            btnRegister.isEnabled = true
                            tvLogin.isEnabled = true
                            tvError.text = e.message ?: "Could not create local user"
                        }
                    }
                }
                .addOnFailureListener { e ->
                    progress.visibility = View.GONE
                    btnRegister.isEnabled = true
                    tvLogin.isEnabled = true
                    tvError.text = e.message ?: "Registration error"
                }
        }
    }

    private suspend fun createOrGetLocalUser(
        firebaseUid: String,
        email: String,
        nickname: String
    ): UserEntity {
        val userDao = ServiceLocator.db(requireContext()).userDao()

        val existingUser = userDao.getByEmail(email)
        if (existingUser != null) {
            return existingUser
        }

        val newUser = UserEntity(
            firebaseUid = firebaseUid,
            email = email,
            password = "firebase_auth",
            nickname = nickname,
            role = "USER",
            createdAt = Instant.now()
        )

        val newId = userDao.insert(newUser)

        return newUser.copy(id = newId)
    }

    private fun goToHome() {
        val action = RegisterFragmentDirections.actionRegisterFragmentToNavigationHome()
        findNavController().navigate(action)
    }
}