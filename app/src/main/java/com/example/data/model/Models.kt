package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Announcement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val badge: String = "UPDATE",
    val link: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class StudyClass(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    @Json(name = "order_index") val orderIndex: Int = 0
)

@JsonClass(generateAdapter = true)
data class Subject(
    val id: String = "",
    @Json(name = "class_id") val classId: String = "",
    val name: String = "",
    @Json(name = "icon_name") val iconName: String = "book",
    @Json(name = "color_hex") val colorHex: String = "#1E40AF",
    val code: String = ""
)

@JsonClass(generateAdapter = true)
data class Chapter(
    val id: String = "",
    @Json(name = "subject_id") val subjectId: String = "",
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "chapter_number") val chapterNumber: Int = 1,
    val title: String = "",
    val description: String = ""
)

@JsonClass(generateAdapter = true)
data class Book(
    val id: String = "",
    val title: String = "",
    val category: String = "NCERT Books", // "NCERT Books", "JAC Books", "Other Books"
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "subject_id") val subjectId: String = "",
    @Json(name = "subject_name") val subjectName: String = "",
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "pdf_url") val pdfUrl: String? = null,
    @Json(name = "file_url") val fileUrl: String? = null,
    @Json(name = "storage_path") val storagePath: String? = null,
    @Json(name = "file_path") val filePath: String? = null,
    @Json(name = "bucket") val bucket: String? = null,
    @Json(name = "chapters_count") val chaptersCount: Int = 0,
    val author: String = "NCERT / JAC Board",
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "published") val published: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class StudyNote(
    val id: String = "",
    val title: String = "",
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "subject_id") val subjectId: String = "",
    @Json(name = "subject_name") val subjectName: String = "",
    @Json(name = "chapter_id") val chapterId: String = "",
    @Json(name = "chapter_title") val chapterTitle: String = "",
    val content: String = "",
    val summary: String = "",
    @Json(name = "key_points") val keyPoints: List<String> = emptyList(),
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "published") val published: Boolean? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PdfDocument(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Study Material", // "Books", "Notes", "Model Papers", "PYQ", "Important Documents", "Study Material"
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "subject_id") val subjectId: String = "",
    @Json(name = "subject_name") val subjectName: String = "",
    @Json(name = "file_url") val fileUrl: String = "",
    @Json(name = "file_path") val filePath: String? = null,
    @Json(name = "storage_path") val storagePath: String? = null,
    @Json(name = "bucket") val bucket: String? = null,
    @Json(name = "pdf_url") val pdfUrl: String? = null,
    @Json(name = "pages_count") val pagesCount: Int = 10,
    @Json(name = "file_size") val fileSize: String = "2.4 MB",
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "published") val published: Boolean? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ModelPaper(
    val id: String = "",
    val title: String = "",
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "subject_id") val subjectId: String = "",
    @Json(name = "subject_name") val subjectName: String = "",
    val year: String = "2026",
    @Json(name = "questions_count") val questionsCount: Int = 20,
    @Json(name = "duration_minutes") val durationMinutes: Int = 90,
    @Json(name = "file_url") val fileUrl: String? = null,
    @Json(name = "storage_path") val storagePath: String? = null,
    @Json(name = "file_path") val filePath: String? = null,
    @Json(name = "bucket") val bucket: String? = null,
    @Json(name = "pdf_url") val pdfUrl: String? = null,
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "published") val published: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class PreviousYearPaper(
    val id: String = "",
    val title: String = "",
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "subject_id") val subjectId: String = "",
    @Json(name = "subject_name") val subjectName: String = "",
    val year: String = "2025",
    @Json(name = "exam_type") val examType: String = "Annual Board Exam",
    @Json(name = "questions_count") val questionsCount: Int = 30,
    @Json(name = "file_url") val fileUrl: String? = null,
    @Json(name = "storage_path") val storagePath: String? = null,
    @Json(name = "file_path") val filePath: String? = null,
    @Json(name = "bucket") val bucket: String? = null,
    @Json(name = "pdf_url") val pdfUrl: String? = null,
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "published") val published: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class Question(
    val id: String = "",
    @Json(name = "question_text") val questionText: String = "",
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "option_a") val optionA: String = "",
    @Json(name = "option_b") val optionB: String = "",
    @Json(name = "option_c") val optionC: String = "",
    @Json(name = "option_d") val optionD: String = "",
    @Json(name = "correct_option") val correctOption: String = "A", // "A", "B", "C", "D"
    val explanation: String = "",
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "subject_id") val subjectId: String = "",
    @Json(name = "subject_name") val subjectName: String = "",
    @Json(name = "chapter_id") val chapterId: String = "",
    @Json(name = "chapter_title") val chapterTitle: String = "",
    @Json(name = "question_type") val questionType: String = "MCQ",
    val marks: Int = 1
)

@JsonClass(generateAdapter = true)
data class OnlineTest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "subject_id") val subjectId: String = "",
    @Json(name = "subject_name") val subjectName: String = "",
    @Json(name = "duration_minutes") val durationMinutes: Int = 15,
    @Json(name = "total_marks") val totalMarks: Int = 20,
    @Json(name = "passing_marks") val passingMarks: Int = 8,
    @Json(name = "questions_count") val questionsCount: Int = 10,
    @Json(name = "is_daily_quiz") val isDailyQuiz: Boolean = false,
    @Json(name = "is_featured") val isFeatured: Boolean = false,
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TestAttempt(
    val id: String = "",
    @Json(name = "test_id") val testId: String = "",
    @Json(name = "test_title") val testTitle: String = "",
    @Json(name = "student_name") val studentName: String = "",
    @Json(name = "student_class") val studentClass: String = "",
    @Json(name = "student_email") val studentEmail: String = "",
    @Json(name = "student_mobile") val studentMobile: String = "",
    val score: Int = 0,
    @Json(name = "total_marks") val totalMarks: Int = 0,
    @Json(name = "correct_count") val correctCount: Int = 0,
    @Json(name = "wrong_count") val wrongCount: Int = 0,
    @Json(name = "skipped_count") val skippedCount: Int = 0,
    @Json(name = "accuracy_percentage") val accuracyPercentage: Double = 0.0,
    @Json(name = "time_taken_seconds") val timeTakenSeconds: Int = 0,
    @Json(name = "completed_at") val completedAt: String? = null,
    val rank: Int = 1
)

@JsonClass(generateAdapter = true)
data class LeaderboardEntry(
    val id: String = "",
    @Json(name = "student_name") val studentName: String = "",
    @Json(name = "student_class") val studentClass: String = "",
    val score: Int = 0,
    val accuracy: Double = 0.0,
    @Json(name = "time_taken_seconds") val timeTakenSeconds: Int = 0,
    val rank: Int = 1
)

@JsonClass(generateAdapter = true)
data class DailyUpdate(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val tag: String = "UPDATE",
    val date: String = "",
    @Json(name = "is_pinned") val isPinned: Boolean = false
)

data class BookmarkItem(
    val id: String,
    val itemId: String,
    val itemType: String, // "BOOK", "NOTE", "QUESTION", "PDF", "MODEL_PAPER"
    val title: String,
    val subtitle: String,
    val extraData: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
