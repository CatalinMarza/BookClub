package com.example.bookclub.data.repository

import com.example.bookclub.data.db.UserEntity
import com.example.bookclub.data.db.dao.UserDao
import com.example.bookclub.data.util.PasswordHasher
import java.time.Instant

class AuthRepository(private val userDao: UserDao) {

    suspend fun register(email: String, nickname: String, password: String): UserEntity {
        val normalizedEmail = email.trim().lowercase()
        val existing = userDao.getByEmail(normalizedEmail)
        if (existing != null) throw IllegalStateException("Email already in use")
        val now = Instant.now()
        val entity = UserEntity(
            firebaseUid = null,
            email = normalizedEmail,
            password = PasswordHasher.sha256(password),
            nickname = nickname.trim(),
            role = "USER",
            createdAt = now
        )
        val id = userDao.insert(entity)
        return entity.copy(id = id)
    }

    suspend fun login(email: String, password: String): UserEntity {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.getByEmail(normalizedEmail) ?: throw IllegalArgumentException("Invalid credentials")
        val ok = user.password == PasswordHasher.sha256(password)
        if (!ok) throw IllegalArgumentException("Invalid credentials")
        return user
    }
}
