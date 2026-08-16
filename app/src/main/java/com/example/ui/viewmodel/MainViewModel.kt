package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SavedAttemptEntity
import com.example.data.model.*
import com.example.data.repository.SuperStudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val announcements: List<Announcement> = emptyList(),
    val dailyQuiz: OnlineTest? = null,
    val featuredTests: List<OnlineTest> = emptyList(),
    val dailyUpdates: List<DailyUpdate> = emptyList(),
    val topLeaderboard: List<LeaderboardEntry> = emptyList(),
    val selectedClass: String = "class-12",
    val classes: List<StudyClass> = emptyList(),
    val errorMessage: String? = null
)

data class StudyUiState(
    val selectedTab: Int = 0, // 0: Books, 1: Notes, 2: PDFs, 3: Model Papers, 4: PYQs
    val selectedCategory: String = "All Books",
    val selectedSubjectId: String? = null,
    val books: List<Book> = emptyList(),
    val notes: List<StudyNote> = emptyList(),
    val pdfs: List<PdfDocument> = emptyList(),
    val modelPapers: List<ModelPaper> = emptyList(),
    val pyqs: List<PreviousYearPaper> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = false
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SuperStudyRepository(application)

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _studyState = MutableStateFlow(StudyUiState())
    val studyState: StateFlow<StudyUiState> = _studyState.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private val _allTests = MutableStateFlow<List<OnlineTest>>(emptyList())
    val allTests: StateFlow<List<OnlineTest>> = _allTests.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkItem>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val testAttempts: StateFlow<List<SavedAttemptEntity>> = repository.getLocalAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedClass = MutableStateFlow("class-12")
    val selectedClass: StateFlow<String> = _selectedClass.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val classes = repository.getClasses()
                val announcements = repository.getAnnouncements()
                val dailyQuiz = repository.getDailyQuiz()
                val tests = repository.getTests(_selectedClass.value)
                val updates = repository.getDailyUpdates()
                val topLb = repository.getLeaderboard().take(3)
                val fullLb = repository.getLeaderboard()

                _allTests.value = tests
                _leaderboard.value = fullLb
                _homeState.update {
                    it.copy(
                        isLoading = false,
                        classes = classes,
                        announcements = announcements,
                        dailyQuiz = dailyQuiz,
                        featuredTests = tests.filter { t -> t.isFeatured },
                        dailyUpdates = updates,
                        topLeaderboard = topLb,
                        selectedClass = _selectedClass.value
                    )
                }

                loadStudyData()
            } catch (e: Exception) {
                _homeState.update { it.copy(isLoading = false, errorMessage = "कनेक्शन में समस्या है। दोबारा प्रयास करें।") }
            }
        }
    }

    fun setSelectedClass(classId: String) {
        _selectedClass.value = classId
        _homeState.update { it.copy(selectedClass = classId) }
        loadInitialData()
    }

    fun setStudyTab(tabIndex: Int) {
        _studyState.update { it.copy(selectedTab = tabIndex) }
        loadStudyData()
    }

    fun setStudyCategory(category: String) {
        _studyState.update { it.copy(selectedCategory = category) }
        loadStudyData()
    }

    fun setStudySubject(subjectId: String?) {
        _studyState.update { it.copy(selectedSubjectId = subjectId) }
        loadStudyData()
    }

    fun loadStudyData() {
        viewModelScope.launch {
            _studyState.update { it.copy(isLoading = true) }
            val classId = _selectedClass.value
            val subjects = repository.getSubjects(classId)
            val books = repository.getBooks(_studyState.value.selectedCategory, classId)
            val notes = repository.getStudyNotes(classId, _studyState.value.selectedSubjectId)
            val allPdfs = repository.getPdfDocuments(null, classId)
            val allModelPapers = repository.getModelPapers(classId)
            val allPyqs = repository.getPreviousYearPapers(classId)
            
            val subjId = _studyState.value.selectedSubjectId

            val pdfs = if (subjId == null) allPdfs else allPdfs.filter { it.subjectId == subjId }
            val modelPapers = if (subjId == null) allModelPapers else allModelPapers.filter { it.subjectId == subjId }
            val pyqs = if (subjId == null) allPyqs else allPyqs.filter { it.subjectId == subjId }

            _studyState.update {
                it.copy(
                    isLoading = false,
                    subjects = subjects,
                    books = books,
                    notes = notes,
                    pdfs = pdfs,
                    modelPapers = modelPapers,
                    pyqs = pyqs
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleBookmark(itemId: String, itemType: String, title: String, subtitle: String, extra: String = "") {
        viewModelScope.launch {
            repository.toggleBookmark(itemId, itemType, title, subtitle, extra)
        }
    }

    fun removeBookmark(id: String) {
        viewModelScope.launch {
            repository.removeBookmark(id)
        }
    }

    // ----------------------------------------------------
    // PDF MANAGEMENT & UPLOAD ACTIONS
    // ----------------------------------------------------

    suspend fun uploadPdf(
        title: String,
        description: String,
        category: String,
        classId: String,
        subjectId: String,
        subjectName: String,
        fileName: String,
        fileBytes: ByteArray,
        bucket: String = com.example.data.remote.SupabaseClient.StorageBuckets.PDFS
    ): Result<PdfDocument> {
        val result = repository.uploadPdfDocument(
            title = title,
            description = description,
            category = category,
            classId = classId,
            subjectId = subjectId,
            subjectName = subjectName,
            fileName = fileName,
            fileBytes = fileBytes,
            bucket = bucket
        )
        if (result.isSuccess) {
            loadStudyData()
        }
        return result
    }

    suspend fun deletePdf(
        docId: String,
        storagePath: String? = null,
        bucket: String = com.example.data.remote.SupabaseClient.StorageBuckets.PDFS
    ): Result<Unit> {
        val result = repository.deletePdfDocument(docId, storagePath, bucket)
        if (result.isSuccess) {
            loadStudyData()
        }
        return result
    }
}
