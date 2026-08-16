package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val itemType: String,
    val title: String,
    val subtitle: String,
    val extraData: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "test_sessions")
data class TestSessionEntity(
    @PrimaryKey val testId: String,
    val answersJson: String, // Map of questionId -> selectedOption
    val markedReviewJson: String, // Set of marked questionIds
    val timeRemainingSeconds: Int,
    val studentName: String,
    val studentClass: String,
    val studentEmail: String = "",
    val studentMobile: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_attempts")
data class SavedAttemptEntity(
    @PrimaryKey val id: String,
    val testId: String,
    val testTitle: String,
    val studentName: String,
    val studentClass: String,
    val studentEmail: String = "",
    val studentMobile: String = "",
    val score: Int,
    val totalMarks: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val accuracyPercentage: Double,
    val timeTakenSeconds: Int,
    val completedAt: Long = System.currentTimeMillis()
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE itemType = :itemType ORDER BY createdAt DESC")
    fun getBookmarksByType(itemType: String): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE itemId = :itemId)")
    suspend fun isBookmarked(itemId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface TestSessionDao {
    @Query("SELECT * FROM test_sessions WHERE testId = :testId")
    suspend fun getSession(testId: String): TestSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: TestSessionEntity)

    @Query("DELETE FROM test_sessions WHERE testId = :testId")
    suspend fun clearSession(testId: String)
}

@Dao
interface SavedAttemptDao {
    @Query("SELECT * FROM saved_attempts ORDER BY completedAt DESC")
    fun getAllAttempts(): Flow<List<SavedAttemptEntity>>

    @Query("SELECT * FROM saved_attempts ORDER BY completedAt DESC")
    suspend fun getAllAttemptsSync(): List<SavedAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: SavedAttemptEntity)
}
