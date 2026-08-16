package com.example.data.local

import com.example.data.model.*

/**
 * SeedData provides foundational academic hierarchy (classes & subjects).
 * All educational content (books, notes, PDFs, model papers, questions, tests,
 * announcements, updates, leaderboard) is fetched dynamically from the Supabase database.
 */
object SeedData {

    val classes: List<StudyClass> = listOf(
        StudyClass("class-12", "Class 12th", "12", 1),
        StudyClass("class-11", "Class 11th", "11", 2),
        StudyClass("class-10", "Class 10th", "10", 3),
        StudyClass("class-9", "Class 9th", "9", 4)
    )

    val subjects: List<Subject> = listOf(
        // Class 12
        Subject("sub-hist-12", "class-12", "History (इतिहास)", "history", "#1E40AF", "HIST"),
        Subject("sub-geo-12", "class-12", "Geography (भूगोल)", "public", "#0D9488", "GEO"),
        Subject("sub-pol-12", "class-12", "Political Science (राजनीति)", "account_balance", "#7C3AED", "POL"),
        Subject("sub-phys-12", "class-12", "Physics (भौतिकी)", "bolt", "#EA580C", "PHY"),
        Subject("sub-chem-12", "class-12", "Chemistry (रसायन)", "science", "#D97706", "CHEM"),
        Subject("sub-bio-12", "class-12", "Biology (जीव विज्ञान)", "eco", "#059669", "BIO"),
        Subject("sub-math-12", "class-12", "Mathematics (गणित)", "calculate", "#2563EB", "MATH"),
        Subject("sub-hin-12", "class-12", "Hindi Core / Elective (हिंदी)", "menu_book", "#DC2626", "HIN"),
        Subject("sub-eng-12", "class-12", "English Core (अंग्रेजी)", "language", "#4F46E5", "ENG"),

        // Class 11
        Subject("sub-hist-11", "class-11", "History (इतिहास)", "history", "#1E40AF", "HIST"),
        Subject("sub-geo-11", "class-11", "Geography (भूगोल)", "public", "#0D9488", "GEO"),
        Subject("sub-pol-11", "class-11", "Political Science (राजनीति)", "account_balance", "#7C3AED", "POL"),
        Subject("sub-phys-11", "class-11", "Physics (भौतिकी)", "bolt", "#EA580C", "PHY"),
        Subject("sub-chem-11", "class-11", "Chemistry (रसायन)", "science", "#D97706", "CHEM"),
        Subject("sub-bio-11", "class-11", "Biology (जीव विज्ञान)", "eco", "#059669", "BIO"),
        Subject("sub-math-11", "class-11", "Mathematics (गणित)", "calculate", "#2563EB", "MATH"),

        // Class 10
        Subject("sub-sci-10", "class-10", "Science (विज्ञान)", "science", "#16A34A", "SCI"),
        Subject("sub-math-10", "class-10", "Mathematics (गणित)", "calculate", "#2563EB", "MATH"),
        Subject("sub-sst-10", "class-10", "Social Science (सामाजिक विज्ञान)", "public", "#0D9488", "SST"),
        Subject("sub-hin-10", "class-10", "Hindi (हिंदी)", "menu_book", "#DC2626", "HIN"),
        Subject("sub-eng-10", "class-10", "English (अंग्रेजी)", "language", "#4F46E5", "ENG"),
        Subject("sub-sans-10", "class-10", "Sanskrit (संस्कृत)", "auto_stories", "#9333EA", "SANS"),

        // Class 9
        Subject("sub-sci-9", "class-9", "Science (विज्ञान)", "science", "#16A34A", "SCI"),
        Subject("sub-math-9", "class-9", "Mathematics (गणित)", "calculate", "#2563EB", "MATH"),
        Subject("sub-sst-9", "class-9", "Social Science (सामाजिक विज्ञान)", "public", "#0D9488", "SST"),
        Subject("sub-hin-9", "class-9", "Hindi (हिंदी)", "menu_book", "#DC2626", "HIN"),
        Subject("sub-eng-9", "class-9", "English (अंग्रेजी)", "language", "#4F46E5", "ENG")
    )

    val chapters: List<Chapter> = emptyList()
    val books: List<Book> = emptyList()
    val notes: List<StudyNote> = emptyList()
    val pdfDocuments: List<PdfDocument> = emptyList()
    val modelPapers: List<ModelPaper> = emptyList()
    val previousYearPapers: List<PreviousYearPaper> = emptyList()
    val questions: List<Question> = emptyList()
    val tests: List<OnlineTest> = emptyList()
    val announcements: List<Announcement> = emptyList()
    val dailyUpdates: List<DailyUpdate> = emptyList()
    val leaderboard: List<LeaderboardEntry> = emptyList()
}
