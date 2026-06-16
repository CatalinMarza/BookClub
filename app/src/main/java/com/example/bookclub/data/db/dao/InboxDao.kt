// file: com/example/bookclub/data/db/dao/InboxDao.kt
package com.example.bookclub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.bookclub.data.db.InboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {

    @Insert
    suspend fun insert(e: InboxEntity): Long

    @Query("""
        SELECT * FROM inbox
        WHERE userId = :userId
        ORDER BY isRead ASC, createdAt DESC
    """)
    fun listForUser(userId: Long): Flow<List<InboxEntity>>

    @Query("UPDATE inbox SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE inbox SET isRead = 1 WHERE userId = :userId AND isRead = 0")
    suspend fun markAllRead(userId: Long)

    @Query("DELETE FROM inbox WHERE id = :id")
    suspend fun deleteById(id: Long)
}