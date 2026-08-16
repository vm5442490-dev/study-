package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.ui.theme.*
import com.example.ui.viewmodel.TestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultScreen(
    viewModel: TestViewModel,
    onRetake: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.testState.collectAsState()
    val attempt = state.resultAttempt
    var showReviewOnly by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Result & Scorecard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
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
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retake Test")
                    }

                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.weight(1f).testTag("result_back_to_home_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Back to Home", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier.testTag("test_result_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Score Header Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = attempt?.studentName ?: "Student",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = attempt?.testTitle ?: "Online Test",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big Score Ring
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(3.dp, BrandAmber, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${attempt?.score ?: 0}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "/ ${attempt?.totalMarks ?: 10}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val accuracy = attempt?.accuracyPercentage ?: 0.0
                        val verdict = if (accuracy >= 80) "🌟 उत्कृष्ट प्रदर्शन! (Excellent)" else if (accuracy >= 50) "👍 अच्छा प्रयास! (Good Job)" else "📚 अभ्यास की आवश्यकता है (Need Practice)"
                        Text(
                            text = verdict,
                            color = BrandAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Stats Grid (Correct, Wrong, Skipped, Accuracy, Time)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultStatChip(
                        title = "Correct",
                        value = "${attempt?.correctCount ?: 0}",
                        color = BrandEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatChip(
                        title = "Wrong",
                        value = "${attempt?.wrongCount ?: 0}",
                        color = BrandRose,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatChip(
                        title = "Skipped",
                        value = "${attempt?.skippedCount ?: 0}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    ResultStatChip(
                        title = "Accuracy",
                        value = "${attempt?.accuracyPercentage?.toInt() ?: 0}%",
                        color = BrandBluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Review Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📖 प्रश्न समीक्षा व व्याख्या (Question Review)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Review Question Cards
            itemsIndexed(state.questions) { index, question ->
                val chosenOption = state.userAnswers[question.id]
                val isCorrect = chosenOption.equals(question.correctOption, ignoreCase = true)
                val isSkipped = chosenOption == null

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("review_question_${index + 1}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Q ${index + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandBluePrimary
                            )

                            Surface(
                                color = when {
                                    isCorrect -> BrandEmerald.copy(alpha = 0.15f)
                                    isSkipped -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> BrandRose.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when {
                                        isCorrect -> "CORRECT (+${question.marks})"
                                        isSkipped -> "SKIPPED (0)"
                                        else -> "INCORRECT (0)"
                                    },
                                    color = when {
                                        isCorrect -> BrandEmerald
                                        isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> BrandRose
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = question.questionText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Review Options
                        listOf(
                            "A" to question.optionA,
                            "B" to question.optionB,
                            "C" to question.optionC,
                            "D" to question.optionD
                        ).forEach { (optKey, optText) ->
                            val isThisCorrect = optKey.equals(question.correctOption, ignoreCase = true)
                            val isThisUserChoice = optKey.equals(chosenOption, ignoreCase = true)

                            val bg = when {
                                isThisCorrect -> BrandEmerald.copy(alpha = 0.12f)
                                isThisUserChoice && !isCorrect -> BrandRose.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bg)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$optKey. $optText",
                                    fontSize = 12.sp,
                                    color = when {
                                        isThisCorrect -> BrandEmerald
                                        isThisUserChoice && !isCorrect -> BrandRose
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = if (isThisCorrect || isThisUserChoice) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isThisCorrect) {
                                    Text("✓ Correct", color = BrandEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else if (isThisUserChoice) {
                                    Text("✗ Your Choice", color = BrandRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // In-depth Explanation
                        if (question.explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = BrandAmber.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "💡 विस्तृत व्याख्या (Explanation):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandAmberDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = question.explanation,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatChip(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
