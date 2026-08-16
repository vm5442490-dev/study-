package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestRunnerScreen(
    testId: String,
    viewModel: TestViewModel,
    onNavigateBack: () -> Unit,
    onTestFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.testState.collectAsState()
    var showPaletteSheet by remember { mutableStateOf(false) }

    LaunchedEffect(testId) {
        viewModel.initializeTest(testId)
    }

    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted) {
            onTestFinished()
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BrandBluePrimary)
        }
        return
    }

    if (state.showStartDialog) {
        StudentDetailsStartDialog(
            testTitle = state.test?.title ?: "Online Test",
            subjectName = state.test?.subjectName ?: "",
            totalQuestions = state.questions.size,
            durationMinutes = state.test?.durationMinutes ?: 15,
            onStartTest = { name, studentClass, email, mobile ->
                viewModel.startTestWithStudentDetails(name, studentClass, email, mobile)
            },
            onDismiss = onNavigateBack
        )
        return
    }

    val currentQuestion = state.questions.getOrNull(state.currentQuestionIndex)

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("test_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit")
                        }
                        Column {
                            Text(
                                text = state.test?.title ?: "Test",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Question ${state.currentQuestionIndex + 1} of ${state.questions.size}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TestTimerBadge(timeRemainingSeconds = state.timeRemainingSeconds)

                        IconButton(
                            onClick = { showPaletteSheet = true },
                            modifier = Modifier.testTag("open_palette_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Palette",
                                tint = BrandBluePrimary
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.previousQuestion() },
                        enabled = state.currentQuestionIndex > 0,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("prev_question_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev", fontSize = 12.sp)
                    }

                    if (currentQuestion != null) {
                        val isMarked = state.markedForReview.contains(currentQuestion.id)
                        IconButton(
                            onClick = { viewModel.toggleMarkForReview(currentQuestion.id) },
                            modifier = Modifier.testTag("mark_review_button")
                        ) {
                            Icon(
                                imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Mark Review",
                                tint = if (isMarked) BrandPurple else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (state.userAnswers.containsKey(currentQuestion.id)) {
                            TextButton(
                                onClick = { viewModel.clearOption(currentQuestion.id) },
                                modifier = Modifier.testTag("clear_response_button")
                            ) {
                                Text("Clear", fontSize = 12.sp, color = BrandRose)
                            }
                        }
                    }

                    if (state.currentQuestionIndex < state.questions.size - 1) {
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("next_question_button")
                        ) {
                            Text("Next", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.setShowSubmitDialog(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("submit_test_button")
                        ) {
                            Text("Submit Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("test_runner_screen")
    ) { paddingValues ->
        if (currentQuestion == null) {
            EmptyStateView(title = "प्रश्न लोड नहीं हो सके", modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Question Header (Q#, Marks, Subject)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = BrandBluePrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Question ${state.currentQuestionIndex + 1} / ${state.questions.size}",
                                color = BrandBluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = BrandEmerald.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+${currentQuestion.marks} Mark",
                                color = BrandEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Question Text & Image
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = currentQuestion.questionText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            )

                            if (!currentQuestion.imageUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AsyncImage(
                                        model = currentQuestion.imageUrl,
                                        contentDescription = "Question Image / Diagram",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 120.dp, max = 260.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }

                // Options (A, B, C, D)
                val selectedOption = state.userAnswers[currentQuestion.id]

                item {
                    QuestionOptionItem(
                        optionKey = "A",
                        optionText = currentQuestion.optionA,
                        isSelected = selectedOption == "A",
                        onSelect = { viewModel.selectOption(currentQuestion.id, "A") }
                    )
                }
                item {
                    QuestionOptionItem(
                        optionKey = "B",
                        optionText = currentQuestion.optionB,
                        isSelected = selectedOption == "B",
                        onSelect = { viewModel.selectOption(currentQuestion.id, "B") }
                    )
                }
                item {
                    QuestionOptionItem(
                        optionKey = "C",
                        optionText = currentQuestion.optionC,
                        isSelected = selectedOption == "C",
                        onSelect = { viewModel.selectOption(currentQuestion.id, "C") }
                    )
                }
                item {
                    QuestionOptionItem(
                        optionKey = "D",
                        optionText = currentQuestion.optionD,
                        isSelected = selectedOption == "D",
                        onSelect = { viewModel.selectOption(currentQuestion.id, "D") }
                    )
                }
            }
        }
    }

    // Question Palette Bottom Sheet
    if (showPaletteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaletteSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            QuestionPaletteBottomSheet(
                questions = state.questions,
                currentIndex = state.currentQuestionIndex,
                userAnswers = state.userAnswers,
                markedForReview = state.markedForReview,
                onSelectQuestion = { index ->
                    viewModel.jumpToQuestion(index)
                    showPaletteSheet = false
                }
            )
        }
    }

    // Submit Confirmation Dialog
    if (state.showSubmitDialog) {
        val total = state.questions.size
        val answered = state.userAnswers.size
        val unanswered = total - answered

        AlertDialog(
            onDismissRequest = { viewModel.setShowSubmitDialog(false) },
            title = { Text("Submit Test?") },
            text = {
                Column {
                    Text("क्या आप टेस्ट सबमिट करना चाहते हैं?")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("• हल किए गए प्रश्न: $answered / $total", fontWeight = FontWeight.Bold)
                    Text("• छूटे हुए प्रश्न: $unanswered", color = BrandRose)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitTest() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald)
                ) {
                    Text("हाँ, सबमिट करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowSubmitDialog(false) }) {
                    Text("नहीं, जारी रखें")
                }
            }
        )
    }
}
