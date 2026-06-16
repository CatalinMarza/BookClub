package com.example.bookclub.data.db.dao

import androidx.room.*
import com.example.bookclub.data.db.UserEntity

// DAO Room pentru User: metode CRUD si query-uri specifice
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM user WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM user")
    suspend fun count(): Long

    @Query("UPDATE user SET role = :role WHERE email = :email")
    suspend fun updateRoleByEmail(email: String, role: String)
}
