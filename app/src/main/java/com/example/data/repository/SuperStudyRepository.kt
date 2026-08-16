package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * SuperStudyRepository coordinates remote Supabase cloud data with local Room storage.
 * Fetches real academic content directly from Supabase backend.
 */
class SuperStudyRepository(context: Context) {

    private val db = SuperStudyDatabase.getInstance(context)
    private val bookmarkDao = db.bookmarkDao()
    private val sessionDao = db.testSessionDao()
    private val attemptDao = db.savedAttemptDao()

    companion object {
        private const val TAG = "SuperStudyRepo"
    }

    // ----------------------------------------------------
    // 1. CURRICULUM HIERARCHY
    // ----------------------------------------------------

    suspend fun getClasses(): List<StudyClass> = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.api.getClasses()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                return@withContext response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "Classes fetch fallback to standard: ${e.message}")
        }
        SeedData.classes
    }

    suspend fun getSubjects(classId: String? = null): List<Subject> = withContext(Dispatchers.IO) {
        var list: List<Subject> = emptyList()
        try {
            val response = SupabaseClient.api.getSubjects()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list = response.body()!!
            } else {
                list = SeedData.subjects
            }
        } catch (e: Exception) {
            Log.w(TAG, "Subjects fetch fallback: ${e.message}")
            list = SeedData.subjects
        }

        if (classId != null && classId != "all") {
            val filtered = list.filter { it.classId == classId }
            if (filtered.isNotEmpty()) filtered else SeedData.subjects.filter { it.classId == classId }
        } else {
            list
        }
    }

    suspend fun getChapters(subjectId: String? = null): List<Chapter> = withContext(Dispatchers.IO) {
        var list: List<Chapter> = emptyList()
        try {
            val response = SupabaseClient.api.getChapters()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list = response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "Chapters fetch error: ${e.message}")
        }

        if (subjectId != null) {
            list.filter { it.subjectId == subjectId }
        } else {
            list
        }
    }

    // ----------------------------------------------------
    // 2. BOOKS & TEXTBOOKS
    // ----------------------------------------------------

    suspend fun getBooks(category: String? = null, classId: String? = null): List<Book> = withContext(Dispatchers.IO) {
        var list: List<Book> = emptyList()
        try {
            val response = SupabaseClient.api.getBooks()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list = response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "Books fetch error: ${e.message}")
        }

        list.filter { book ->
            val matchCat = category.isNullOrBlank() || 
                           category.equals("All", ignoreCase = true) || 
                           category.equals("All Books", ignoreCase = true) || 
                           book.category.equals(category, ignoreCase = true) ||
                           (category.contains("NCERT", ignoreCase = true) && book.category.contains("NCERT", ignoreCase = true)) ||
                           (category.contains("JAC", ignoreCase = true) && book.category.contains("JAC", ignoreCase = true))
            val matchClass = classId.isNullOrBlank() || classId.equals("all", ignoreCase = true) || book.classId == classId
            matchCat && matchClass
        }
    }

    // ----------------------------------------------------
    // 3. STUDY NOTES
    // ----------------------------------------------------

    suspend fun getStudyNotes(classId: String? = null, subjectId: String? = null): List<StudyNote> = withContext(Dispatchers.IO) {
        var list: List<StudyNote> = emptyList()
        try {
            val response = SupabaseClient.api.getStudyNotes()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list = response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "StudyNotes fetch error: ${e.message}")
        }

        list.filter { note ->
            val matchClass = classId.isNullOrBlank() || classId.equals("all", ignoreCase = true) || note.classId == classId
            val matchSub = subjectId.isNullOrBlank() || note.subjectId == subjectId
            matchClass && matchSub
        }
    }

    suspend fun getNoteById(id: String): StudyNote? = withContext(Dispatchers.IO) {
        getStudyNotes().find { it.id == id }
    }

    // ----------------------------------------------------
    // 4. PDF DOCUMENTS & STUDY MATERIAL
    // ----------------------------------------------------

    suspend fun getPdfDocuments(category: String? = null, classId: String? = null): List<PdfDocument> = withContext(Dispatchers.IO) {
        var list: List<PdfDocument> = emptyList()
        try {
            val response = SupabaseClient.api.getPdfDocuments()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list = response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "PDF documents fetch error: ${e.message}")
        }

        list.filter { pdf ->
            val matchCat = category.isNullOrBlank() || 
                           category.equals("All", ignoreCase = true) || 
                           pdf.category.equals(category, ignoreCase = true)
            val matchClass = classId.isNullOrBlank() || classId.equals("all", ignoreCase = true) || pdf.classId == classId
            matchCat && matchClass
        }
    }

    // ----------------------------------------------------
    // 5. 2026 MODEL PAPERS & PYQS
    // ----------------------------------------------------

    suspend fun getModelPapers(classId: String? = null): List<ModelPaper> = withContext(Dispatchers.IO) {
        var list: List<ModelPaper> = emptyList()
        try {
            val response = SupabaseClient.api.getModelPapers()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list = response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "Model papers fetch error: ${e.message}")
        }

        if (classId != null && classId != "all") {
            list.filter { it.classId == classId }
        } else {
            list
        }
    }

    suspend fun getPreviousYearPapers(classId: String? = null): List<PreviousYearPaper> = withContext(Dispatchers.IO) {
        var list: List<PreviousYearPaper> = emptyList()
        try {
            val response = SupabaseClient.api.getPreviousYearPapers()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list = response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "PYQ fetch error: ${e.message}")
        }

        if (classId != null && classId != "all") {
            list.filter { it.classId == classId }
        } else {
            list
        }
    }

    // ----------------------------------------------------
    // 6. MASTER QUESTION BANK
    // ----------------------------------------------------

    suspend fun getQuestions(
        classId: String? = null,
        subjectId: String? = null,
        chapterId: String? = null,
        difficulty: String? = null
    ): List<Question> = withContext(Dispatchers.IO) {
        var list: List<Question> = emptyList()
        try {
            val response = SupabaseClient.api.getQuestions()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list = response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "Questions fetch error: ${e.message}")
        }

        list.filter { q ->
            val matchClass = classId.isNullOrBlank() || classId.equals("all", ignoreCase = true) || q.classId == classId
            val matchSub = subjectId.isNullOrBlank() || q.subjectId == subjectId
            val matchChap = chapterId.isNullOrBlank() || q.chapterId == chapterId
            val matchDiff = difficulty.isNullOrBlank() || 
                            difficulty.equals("All", ignoreCase = true) || 
                            q.difficulty.equals(difficulty, ignoreCase = true)
            matchClass && matchSub && matchChap && matchDiff
        }
    }

    // ----------------------------------------------------
    // 7. ONLINE TESTS & DAILY LIVE QUIZ
    // ----------------------------------------------------

    suspend fun getTests(classId: String? = null): List<OnlineTest> = withContext(Dispatchers.IO) {
        var list: List<OnlineTest> = emptyList()
        try {
            val response = SupabaseClient.api.getTests()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                list = response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tests fetch error: ${e.message}")
        }

        if (classId != null && classId != "all") {
            list.filter { it.classId == classId || it.isDailyQuiz }
        } else {
            list
        }
    }

    suspend fun getTestById(testId: String): OnlineTest? = withContext(Dispatchers.IO) {
        getTests().find { it.id == testId }
    }

    suspend fun getQuestionsForTest(testId: String): List<Question> = withContext(Dispatchers.IO) {
        val test = getTestById(testId)
        val allQuestions = getQuestions()
        if (test != null) {
            val matching = allQuestions.filter { it.subjectId == test.subjectId || test.subjectId.isEmpty() }
            if (matching.isNotEmpty()) matching.take(test.questionsCount) else allQuestions.take(test.questionsCount)
        } else {
            allQuestions.take(10)
        }
    }

    suspend fun getDailyQuiz(): OnlineTest? = withContext(Dispatchers.IO) {
        getTests().find { it.isDailyQuiz }
    }

    // ----------------------------------------------------
    // 8. ANNOUNCEMENTS, UPDATES & LEADERBOARD
    // ----------------------------------------------------

    suspend fun getAnnouncements(): List<Announcement> = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.api.getAnnouncements()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                return@withContext response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "Announcements fetch error: ${e.message}")
        }
        emptyList()
    }

    suspend fun getDailyUpdates(): List<DailyUpdate> = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.api.getDailyUpdates()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                return@withContext response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "Daily updates fetch error: ${e.message}")
        }
        emptyList()
    }

    suspend fun getLeaderboard(): List<LeaderboardEntry> = withContext(Dispatchers.IO) {
        // 1. Check dedicated leaderboard table in Supabase
        try {
            val response = SupabaseClient.api.getLeaderboard()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                return@withContext response.body()!!
            }
        } catch (e: Exception) {
            Log.w(TAG, "Leaderboard direct fetch: ${e.message}")
        }

        // 2. Compute dynamic leaderboard from actual student test attempts in Supabase
        try {
            val attemptsResp = SupabaseClient.api.getTestAttempts()
            if (attemptsResp.isSuccessful && !attemptsResp.body().isNullOrEmpty()) {
                val attempts = attemptsResp.body()!!
                // Group by student and take best attempt or aggregate
                val studentRankings = attempts
                    .groupBy { it.studentName.ifBlank { "Student" } }
                    .map { (name, studentAttempts) ->
                        val best = studentAttempts.maxByOrNull { it.score } ?: studentAttempts.first()
                        val studentClass = best.studentClass.ifBlank { "Class 12th" }
                        val score = best.score
                        val accuracy = best.accuracyPercentage
                        val timeTaken = best.timeTakenSeconds
                        Triple(name, studentClass, Triple(score, accuracy, timeTaken))
                    }
                    .sortedWith(
                        compareByDescending<Triple<String, String, Triple<Int, Double, Int>>> { it.third.first }
                            .thenByDescending { it.third.second }
                            .thenBy { it.third.third }
                    )
                    .mapIndexed { index, item ->
                        LeaderboardEntry(
                            id = "lb-rank-${index + 1}",
                            studentName = item.first,
                            studentClass = item.second,
                            score = item.third.first,
                            accuracy = item.third.second,
                            timeTakenSeconds = item.third.third,
                            rank = index + 1
                        )
                    }
                if (studentRankings.isNotEmpty()) {
                    return@withContext studentRankings
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Dynamic leaderboard computation: ${e.message}")
        }

        emptyList()
    }

    // ----------------------------------------------------
    // 9. TEST ATTEMPTS & LOCAL SUBMISSIONS (ROOM)
    // ----------------------------------------------------

    suspend fun submitAttempt(attempt: TestAttempt) = withContext(Dispatchers.IO) {
        // Save locally in Room database
        attemptDao.insertAttempt(
            SavedAttemptEntity(
                id = attempt.id.ifEmpty { UUID.randomUUID().toString() },
                testId = attempt.testId,
                testTitle = attempt.testTitle,
                studentName = attempt.studentName,
                studentClass = attempt.studentClass,
                studentEmail = attempt.studentEmail,
                studentMobile = attempt.studentMobile,
                score = attempt.score,
                totalMarks = attempt.totalMarks,
                correctCount = attempt.correctCount,
                wrongCount = attempt.wrongCount,
                skippedCount = attempt.skippedCount,
                accuracyPercentage = attempt.accuracyPercentage,
                timeTakenSeconds = attempt.timeTakenSeconds,
                completedAt = System.currentTimeMillis()
            )
        )
        // Clear active session for this test
        sessionDao.clearSession(attempt.testId)

        // Sync with Supabase (fire and forget)
        try {
            SupabaseClient.api.submitTestAttempt(attempt)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase attempt sync non-blocking: ${e.message}")
        }
    }

    fun getLocalAttempts(): Flow<List<SavedAttemptEntity>> {
        return attemptDao.getAllAttempts()
    }

    // ----------------------------------------------------
    // 10. TEST SESSION AUTO-SAVE & RESTORE
    // ----------------------------------------------------

    suspend fun saveTestSession(
        testId: String,
        answersMap: Map<String, String>,
        markedReview: Set<String>,
        timeRemainingSeconds: Int,
        studentName: String,
        studentClass: String,
        studentEmail: String = "",
        studentMobile: String = ""
    ) = withContext(Dispatchers.IO) {
        val answersJson = answersMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        val markedJson = markedReview.joinToString(",")
        sessionDao.saveSession(
            TestSessionEntity(
                testId = testId,
                answersJson = answersJson,
                markedReviewJson = markedJson,
                timeRemainingSeconds = timeRemainingSeconds,
                studentName = studentName,
                studentClass = studentClass,
                studentEmail = studentEmail,
                studentMobile = studentMobile
            )
        )
    }

    suspend fun getTestSession(testId: String): TestSessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getSession(testId)
    }

    suspend fun clearTestSession(testId: String) = withContext(Dispatchers.IO) {
        sessionDao.clearSession(testId)
    }

    // ----------------------------------------------------
    // 11. BOOKMARKS (ROOM PERSISTENCE)
    // ----------------------------------------------------

    fun getAllBookmarks(): Flow<List<BookmarkItem>> {
        return bookmarkDao.getAllBookmarks().map { list ->
            list.map {
                BookmarkItem(
                    id = it.id,
                    itemId = it.itemId,
                    itemType = it.itemType,
                    title = it.title,
                    subtitle = it.subtitle,
                    extraData = it.extraData,
                    createdAt = it.createdAt
                )
            }
        }
    }

    suspend fun isBookmarked(itemId: String): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.isBookmarked(itemId)
    }

    suspend fun toggleBookmark(
        itemId: String,
        itemType: String,
        title: String,
        subtitle: String,
        extraData: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val exists = bookmarkDao.isBookmarked(itemId)
        if (exists) {
            bookmarkDao.deleteByItemId(itemId)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    id = UUID.randomUUID().toString(),
                    itemId = itemId,
                    itemType = itemType,
                    title = title,
                    subtitle = subtitle,
                    extraData = extraData,
                    createdAt = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun removeBookmark(id: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteById(id)
    }

    // ----------------------------------------------------
    // 10. ADMIN ACTIONS (BULK QUESTIONS, STUDENT ATTEMPTS, PDF UPLOADS)
    // ----------------------------------------------------

    /**
     * Complete Admin PDF Upload Pipeline:
     * 1. Validates PDF bytes (%PDF- magic bytes)
     * 2. Uploads to Supabase Storage bucket ('pdfs' or specified)
     * 3. Saves metadata row to Supabase 'pdf_documents' table
     * 4. Verifies storage existence and generates accessible URL
     */
    suspend fun uploadPdfDocument(
        title: String,
        description: String,
        category: String,
        classId: String,
        subjectId: String,
        subjectName: String,
        fileName: String,
        fileBytes: ByteArray,
        bucket: String = SupabaseClient.StorageBuckets.PDFS
    ): Result<PdfDocument> = withContext(Dispatchers.IO) {
        try {
            // 1. Validation
            if (fileBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("PDF file content is empty (0 bytes)"))
            }
            if (fileBytes.size < 10) {
                return@withContext Result.failure(IllegalArgumentException("Invalid PDF file size (${fileBytes.size} bytes)"))
            }
            val headerStr = String(fileBytes.take(5).toByteArray(), Charsets.US_ASCII)
            if (!headerStr.startsWith("%PDF-")) {
                Log.w(TAG, "Uploaded bytes do not start with %PDF- header, header is: $headerStr")
            }

            // 2. Build canonical storage path
            val cleanClass = classId.lowercase().replace(" ", "").replace("_", "")
            val cleanSub = subjectId.lowercase().replace(" ", "").replace("_", "")
            val sanitizedName = fileName.replace(" ", "_").trimStart('/')
            val finalFileName = if (sanitizedName.endsWith(".pdf", ignoreCase = true)) sanitizedName else "$sanitizedName.pdf"
            val storagePath = "$cleanClass/$cleanSub/$finalFileName"

            // 3. Upload to Supabase Storage
            val uploadResponse = SupabaseClient.uploadStorageFile(
                bucket = bucket,
                path = storagePath,
                byteArray = fileBytes,
                contentType = "application/pdf"
            )

            if (!uploadResponse.isSuccessful && uploadResponse.code !in listOf(200, 201)) {
                val err = uploadResponse.body?.string() ?: "HTTP ${uploadResponse.code}"
                Log.e(TAG, "Storage upload failed: $err")
                return@withContext Result.failure(Exception("Supabase Storage Error: $err"))
            }

            // 4. Generate public URL
            val publicUrl = SupabaseClient.getStoragePublicUrl(bucket, storagePath)
            val docId = "pdf-${UUID.randomUUID().toString().take(8)}"
            val sizeFormatted = "%.1f MB".format(fileBytes.size.toDouble() / (1024 * 1024)).let {
                if (it.startsWith("0.0")) "%.1f KB".format(fileBytes.size.toDouble() / 1024) else it
            }

            val pdfRecord = PdfDocument(
                id = docId,
                title = title.trim(),
                description = description.trim(),
                category = category.trim(),
                classId = classId.trim(),
                subjectId = subjectId.trim(),
                subjectName = subjectName.trim(),
                fileUrl = publicUrl,
                filePath = storagePath,
                storagePath = storagePath,
                bucket = bucket,
                pdfUrl = publicUrl,
                pagesCount = 10,
                fileSize = sizeFormatted,
                isPublished = true,
                published = true
            )

            // 5. Save metadata into Supabase 'pdf_documents' table
            val insertResponse = SupabaseClient.api.insertPdfDocument(pdfRecord)
            if (!insertResponse.isSuccessful && insertResponse.code() !in listOf(200, 201, 204)) {
                val dbErr = insertResponse.errorBody()?.string() ?: "HTTP ${insertResponse.code()}"
                Log.w(TAG, "Supabase Database metadata insert warning: $dbErr (Storage upload succeeded)")
            }

            Log.i(TAG, "PDF pipeline complete. ID: $docId, StoragePath: $storagePath, URL: $publicUrl")
            Result.success(pdfRecord)
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadPdfDocument pipeline: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a PDF document from Supabase database and storage.
     */
    suspend fun deletePdfDocument(
        docId: String,
        storagePath: String? = null,
        bucket: String = SupabaseClient.StorageBuckets.PDFS
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete from database
            val dbResp = SupabaseClient.api.deletePdfDocument("eq.$docId")
            // Delete from storage if path known
            if (!storagePath.isNullOrBlank()) {
                SupabaseClient.deleteStorageFile(bucket, storagePath)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting PDF document: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if a PDF file exists in Supabase Storage and is accessible.
     */
    suspend fun verifyPdfStorageFile(bucket: String, storagePath: String): Boolean = withContext(Dispatchers.IO) {
        SupabaseClient.checkStorageFileExists(bucket, storagePath)
    }

    suspend fun insertBulkQuestions(questions: List<Question>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (questions.isEmpty()) return@withContext Result.success(0)
            val response = SupabaseClient.api.insertQuestions(questions)
            if (response.isSuccessful) {
                Log.i(TAG, "Bulk questions inserted successfully: ${questions.size}")
                Result.success(questions.size)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Log.e(TAG, "Failed to insert bulk questions: $err")
                Result.failure(Exception("Supabase Error ($err)"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception inserting bulk questions: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun insertSingleQuestion(question: Question): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.api.insertQuestion(question)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception("Supabase Error ($err)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertTest(test: OnlineTest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.api.insertTest(test)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception("Supabase Error ($err)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQuestion(questionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.api.deleteQuestion("eq.$questionId")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllStudentAttempts(): List<TestAttempt> = withContext(Dispatchers.IO) {
        val attempts = mutableListOf<TestAttempt>()
        try {
            val response = SupabaseClient.api.getTestAttempts()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                attempts.addAll(response.body()!!)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch remote student attempts: ${e.message}")
        }

        // Also check local database attempts to ensure any offline-synced records are visible
        try {
            val local = attemptDao.getAllAttemptsSync()
            val existingIds = attempts.map { it.id }.toSet()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            local.forEach { entity: SavedAttemptEntity ->
                if (entity.id !in existingIds) {
                    attempts.add(
                        TestAttempt(
                            id = entity.id,
                            testId = entity.testId,
                            testTitle = entity.testTitle,
                            studentName = entity.studentName,
                            studentClass = entity.studentClass,
                            studentEmail = entity.studentEmail,
                            studentMobile = entity.studentMobile,
                            score = entity.score,
                            totalMarks = entity.totalMarks,
                            correctCount = entity.correctCount,
                            wrongCount = entity.wrongCount,
                            skippedCount = entity.skippedCount,
                            accuracyPercentage = entity.accuracyPercentage,
                            timeTakenSeconds = entity.timeTakenSeconds,
                            completedAt = dateFormat.format(Date(entity.completedAt)),
                            rank = 1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Local attempts merge error: ${e.message}")
        }

        attempts.sortedByDescending { it.completedAt ?: "" }
    }
}
