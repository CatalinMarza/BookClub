package com.example.bookclub.data.db

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.bookclub.data.db.dao.BookClubDao
import com.example.bookclub.data.db.dao.CommentDao
import com.example.bookclub.data.db.dao.FollowBookDao
import com.example.bookclub.data.db.dao.FollowUserDao
import com.example.bookclub.data.db.dao.InboxDao
import com.example.bookclub.data.db.dao.MembershipDao
import com.example.bookclub.data.db.dao.UserDao
import com.example.bookclub.data.db.dao.VoteDao
import com.example.bookclub.data.util.PasswordHasher
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.example.bookclub.data.db.dao.ClubReviewDao

// Room DB: declarare entitati, DAO, TypeConverters; configurare builder si seed
@Database(
    entities = [
        UserEntity::class,
        BookClubEntity::class,
        MembershipEntity::class,
        CommentEntity::class,
        VoteEntity::class,
        FollowUserEntity::class,
        FollowBookEntity::class,
        InboxEntity::class,
        ClubReviewEntity::class
    ],
    version = 5, // versiune crescută după modificarea FireBase
    exportSchema = true
)
// clasa principala RoomDatabase
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bookClubDao(): BookClubDao
    abstract fun membershipDao(): MembershipDao
    abstract fun commentDao(): CommentDao
    abstract fun voteDao(): VoteDao
    abstract fun followUserDao(): FollowUserDao
    abstract fun followBookDao(): FollowBookDao
    abstract fun inboxDao(): InboxDao

    abstract fun clubReviewDao(): ClubReviewDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookclub.db"
                )
                    .fallbackToDestructiveMigration(true) // in dev sterge si recreeaza schema la incompatibilitate
                    .build()
                INSTANCE = instance
                val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebug) seedSync(instance) // insereaza useri demo daca baza este goala
                instance
            }

        private fun seedSync(db: AppDatabase) = runBlocking {
            withContext(Dispatchers.IO) {
                val count = db.userDao().count()
                if (count == 0L) {
                    val now = Instant.now()
                    db.userDao().insert(
                        UserEntity(
                            firebaseUid = null,
                            email = "admin@demo.local",
                            password = PasswordHasher.sha256("admin123"),
                            nickname = "admin",
                            role = "ADMIN",
                            createdAt = now
                        )
                    )
                    db.userDao().insert(
                        UserEntity(
                            firebaseUid = null,
                            email = "alice@demo.local",
                            password = PasswordHasher.sha256("password"),
                            nickname = "alice",
                            role = "USER",
                            createdAt = now
                        )
                    )
                    db.userDao().insert(
                        UserEntity(
                            firebaseUid = null,
                            email = "bob@demo.local",
                            password = PasswordHasher.sha256("password"),
                            nickname = "bob",
                            role = "USER",
                            createdAt = now
                        )
                    )
                }
            }
        }
    }
}
