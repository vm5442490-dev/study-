package com.example.data.remote

import com.example.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {

    @GET("rest/v1/announcements")
    suspend fun getAnnouncements(
        @Query("select") select: String = "*",
        @Query(value = "order", encoded = true) order: String = "created_at.desc"
    ): Response<List<Announcement>>

    @GET("rest/v1/classes")
    suspend fun getClasses(
        @Query("select") select: String = "*",
        @Query(value = "order", encoded = true) order: String = "order_index.asc"
    ): Response<List<StudyClass>>

    @GET("rest/v1/subjects")
    suspend fun getSubjects(
        @Query("select") select: String = "*"
    ): Response<List<Subject>>

    @GET("rest/v1/chapters")
    suspend fun getChapters(
        @Query("select") select: String = "*",
        @Query(value = "order", encoded = true) order: String = "chapter_number.asc"
    ): Response<List<Chapter>>

    @GET("rest/v1/books")
    suspend fun getBooks(
        @Query("select") select: String = "*"
    ): Response<List<Book>>

    @GET("rest/v1/notes")
    suspend fun getStudyNotes(
        @Query("select") select: String = "*"
    ): Response<List<StudyNote>>

    @GET("rest/v1/pdf_documents")
    suspend fun getPdfDocuments(
        @Query("select") select: String = "*"
    ): Response<List<PdfDocument>>

    @GET("rest/v1/model_papers")
    suspend fun getModelPapers(
        @Query("select") select: String = "*"
    ): Response<List<ModelPaper>>

    @GET("rest/v1/previous_year_papers")
    suspend fun getPreviousYearPapers(
        @Query("select") select: String = "*"
    ): Response<List<PreviousYearPaper>>

    @GET("rest/v1/questions")
    suspend fun getQuestions(
        @Query("select") select: String = "*"
    ): Response<List<Question>>

    @GET("rest/v1/tests")
    suspend fun getTests(
        @Query("select") select: String = "*"
    ): Response<List<OnlineTest>>

    @GET("rest/v1/daily_updates")
    suspend fun getDailyUpdates(
        @Query("select") select: String = "*",
        @Query(value = "order", encoded = true) order: String = "date.desc"
    ): Response<List<DailyUpdate>>

    @GET("rest/v1/leaderboard")
    suspend fun getLeaderboard(
        @Query("select") select: String = "*",
        @Query(value = "order", encoded = true) order: String = "score.desc",
        @Query("limit") limit: Int = 50
    ): Response<List<LeaderboardEntry>>

    @GET("rest/v1/test_attempts")
    suspend fun getTestAttempts(
        @Query("select") select: String = "*",
        @Query(value = "order", encoded = true) order: String = "completed_at.desc.nullslast",
        @Query("limit") limit: Int = 200
    ): Response<List<TestAttempt>>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/test_attempts")
    suspend fun submitTestAttempt(
        @Body attempt: TestAttempt
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/questions")
    suspend fun insertQuestions(
        @Body questions: List<Question>
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/questions")
    suspend fun insertQuestion(
        @Body question: Question
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/tests")
    suspend fun insertTest(
        @Body test: OnlineTest
    ): Response<Unit>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/pdf_documents")
    suspend fun insertPdfDocument(
        @Body doc: PdfDocument
    ): Response<Unit>

    @DELETE("rest/v1/pdf_documents")
    suspend fun deletePdfDocument(
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/questions")
    suspend fun deleteQuestion(
        @Query("id") idFilter: String
    ): Response<Unit>
}
