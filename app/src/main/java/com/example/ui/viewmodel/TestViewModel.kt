package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.OnlineTest
import com.example.data.model.Question
import com.example.data.model.TestAttempt
import com.example.data.repository.SuperStudyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TestState(
    val test: OnlineTest? = null,
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val userAnswers: Map<String, String> = emptyMap(), // questionId -> option ("A", "B", "C", "D")
    val markedForReview: Set<String> = emptySet(), // Set of questionIds
    val timeRemainingSeconds: Int = 0,
    val initialDurationSeconds: Int = 0,
    val studentName: String = "",
    val studentClass: String = "Class 12",
    val studentEmail: String = "",
    val studentMobile: String = "",
    val isSubmitted: Boolean = false,
    val isLoading: Boolean = true,
    val resultAttempt: TestAttempt? = null,
    val showSubmitDialog: Boolean = false,
    val showStartDialog: Boolean = true
)

class TestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SuperStudyRepository(application)

    private val _testState = MutableStateFlow(TestState())
    val testState: StateFlow<TestState> = _testState.asStateFlow()

    private var timerJob: Job? = null

    fun initializeTest(testId: String) {
        viewModelScope.launch {
            _testState.update { it.copy(isLoading = true) }
            val test = repository.getTestById(testId) ?: repository.getDailyQuiz()
            val questions = repository.getQuestionsForTest(testId)
            val durationSecs = (test?.durationMinutes ?: 15) * 60

            // Check if saved session exists
            val session = repository.getTestSession(testId)
            if (session != null && session.timeRemainingSeconds > 0) {
                val answers = session.answersJson.split(";")
                    .filter { it.contains(":") }
                    .associate {
                        val parts = it.split(":")
                        parts[0] to parts[1]
                    }
                val marked = session.markedReviewJson.split(",")
                    .filter { it.isNotBlank() }
                    .toSet()

                _testState.update {
                    it.copy(
                        test = test,
                        questions = questions,
                        currentQuestionIndex = 0,
                        userAnswers = answers,
                        markedForReview = marked,
                        timeRemainingSeconds = session.timeRemainingSeconds,
                        initialDurationSeconds = durationSecs,
                        studentName = session.studentName,
                        studentClass = session.studentClass,
                        isLoading = false,
                        showStartDialog = session.studentName.isBlank()
                    )
                }
                if (session.studentName.isNotBlank()) {
                    startTimer()
                }
            } else {
                _testState.update {
                    it.copy(
                        test = test,
                        questions = questions,
                        currentQuestionIndex = 0,
                        userAnswers = emptyMap(),
                        markedForReview = emptySet(),
                        timeRemainingSeconds = durationSecs,
                        initialDurationSeconds = durationSecs,
                        isLoading = false,
                        showStartDialog = true
                    )
                }
            }
        }
    }

    fun startTestWithStudentDetails(name: String, studentClass: String, email: String, mobile: String) {
        _testState.update {
            it.copy(
                studentName = name.ifBlank { "Student" },
                studentClass = studentClass,
                studentEmail = email,
                studentMobile = mobile,
                showStartDialog = false
            )
        }
        startTimer()
        autoSaveSession()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_testState.value.timeRemainingSeconds > 0 && !_testState.value.isSubmitted) {
                delay(1000)
                _testState.update { it.copy(timeRemainingSeconds = it.timeRemainingSeconds - 1) }

                // Auto save every 10 seconds
                if (_testState.value.timeRemainingSeconds % 10 == 0) {
                    autoSaveSession()
                }
            }
            if (_testState.value.timeRemainingSeconds <= 0 && !_testState.value.isSubmitted) {
                submitTest()
            }
        }
    }

    fun selectOption(questionId: String, option: String) {
        _testState.update {
            val newAnswers = it.userAnswers.toMutableMap()
            newAnswers[questionId] = option
            it.copy(userAnswers = newAnswers)
        }
        autoSaveSession()
    }

    fun clearOption(questionId: String) {
        _testState.update {
            val newAnswers = it.userAnswers.toMutableMap()
            newAnswers.remove(questionId)
            it.copy(userAnswers = newAnswers)
        }
        autoSaveSession()
    }

    fun toggleMarkForReview(questionId: String) {
        _testState.update {
            val newMarked = it.markedForReview.toMutableSet()
            if (newMarked.contains(questionId)) {
                newMarked.remove(questionId)
            } else {
                newMarked.add(questionId)
            }
            it.copy(markedForReview = newMarked)
        }
        autoSaveSession()
    }

    fun nextQuestion() {
        _testState.update {
            if (it.currentQuestionIndex < it.questions.size - 1) {
                it.copy(currentQuestionIndex = it.currentQuestionIndex + 1)
            } else {
                it
            }
        }
    }

    fun previousQuestion() {
        _testState.update {
            if (it.currentQuestionIndex > 0) {
                it.copy(currentQuestionIndex = it.currentQuestionIndex - 1)
            } else {
                it
            }
        }
    }

    fun jumpToQuestion(index: Int) {
        if (index in 0 until _testState.value.questions.size) {
            _testState.update { it.copy(currentQuestionIndex = index) }
        }
    }

    fun setShowSubmitDialog(show: Boolean) {
        _testState.update { it.copy(showSubmitDialog = show) }
    }

    fun submitTest() {
        timerJob?.cancel()
        val state = _testState.value
        val questions = state.questions
        val userAnswers = state.userAnswers

        var correctCount = 0
        var wrongCount = 0
        var skippedCount = 0
        var totalScore = 0

        questions.forEach { q ->
            val chosen = userAnswers[q.id]
            if (chosen == null) {
                skippedCount++
            } else if (chosen.equals(q.correctOption, ignoreCase = true)) {
                correctCount++
                totalScore += q.marks
            } else {
                wrongCount++
            }
        }

        val totalQuestions = questions.size
        val attempted = correctCount + wrongCount
        val accuracy = if (attempted > 0) (correctCount.toDouble() / attempted) * 100 else 0.0
        val timeTakenSecs = state.initialDurationSeconds - state.timeRemainingSeconds

        val testTitle = state.test?.title ?: "Test"
        val totalMarks = state.test?.totalMarks ?: (totalQuestions * 1)

        val attempt = TestAttempt(
            id = UUID.randomUUID().toString(),
            testId = state.test?.id ?: "unknown",
            testTitle = testTitle,
            studentName = state.studentName.ifBlank { "Student" },
            studentClass = state.studentClass,
            studentEmail = state.studentEmail,
            studentMobile = state.studentMobile,
            score = totalScore,
            totalMarks = totalMarks,
            correctCount = correctCount,
            wrongCount = wrongCount,
            skippedCount = skippedCount,
            accuracyPercentage = Math.round(accuracy * 10.0) / 10.0,
            timeTakenSeconds = timeTakenSecs,
            rank = if (accuracy >= 80) 1 else if (accuracy >= 60) 3 else 7
        )

        viewModelScope.launch {
            repository.submitAttempt(attempt)
        }

        _testState.update {
            it.copy(
                isSubmitted = true,
                showSubmitDialog = false,
                resultAttempt = attempt
            )
        }
    }

    private fun autoSaveSession() {
        val s = _testState.value
        val testId = s.test?.id ?: return
        viewModelScope.launch {
            repository.saveTestSession(
                testId = testId,
                answersMap = s.userAnswers,
                markedReview = s.markedForReview,
                timeRemainingSeconds = s.timeRemainingSeconds,
                studentName = s.studentName,
                studentClass = s.studentClass,
                studentEmail = s.studentEmail,
                studentMobile = s.studentMobile
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
