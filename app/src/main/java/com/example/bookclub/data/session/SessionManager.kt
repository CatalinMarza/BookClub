package com.example.bookclub.data.session

import android.content.Context

// gestioneaza sessiune locala folosind datele salvate a in SharedPreferences
data class Session(
    val userId: Long,
    val email: String,
    val nickname: String,
    val role: String,
    val createdAtEpochMs: Long
)

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = prefs.contains(KEY_USER_ID)

    fun save(session: Session) {
        prefs.edit()
            .putLong(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_NICKNAME, session.nickname)
            .putString(KEY_ROLE, session.role)
            .putLong(KEY_CREATED_AT, session.createdAtEpochMs)
            .apply()
    }

    fun get(): Session? {
        if (!isLoggedIn()) return null
        val id = prefs.getLong(KEY_USER_ID, -1L)
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val nickname = prefs.getString(KEY_NICKNAME, null) ?: return null
        val role = prefs.getString(KEY_ROLE, null) ?: return null
        val createdAt = prefs.getLong(KEY_CREATED_AT, 0L)
        return Session(id, email, nickname, role, createdAt)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // Acces rapid la user-ul curent
    val currentUserId: Long?
        get() = if (isLoggedIn()) prefs.getLong(KEY_USER_ID, -1L) else null

    val currentNickname: String?
        get() = if (isLoggedIn()) prefs.getString(KEY_NICKNAME, null) else null

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_ROLE = "role"
        private const val KEY_CREATED_AT = "created_at"
    }
}
