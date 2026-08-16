package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.Question
import com.example.ui.theme.*

@Composable
fun TestTimerBadge(
    timeRemainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val isLowTime = timeRemainingSeconds < 120

    Surface(
        modifier = modifier.testTag("test_timer_badge"),
        shape = RoundedCornerShape(20.dp),
        color = if (isLowTime) BrandRose.copy(alpha = 0.15f) else BrandBluePrimary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Time Left",
                tint = if (isLowTime) BrandRose else BrandBluePrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = if (isLowTime) BrandRose else BrandBluePrimary
            )
        }
    }
}

@Composable
fun QuestionOptionItem(
    optionKey: String, // "A", "B", "C", "D"
    optionText: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) BrandBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val bgColor = if (isSelected) BrandBluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("question_option_$optionKey"),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) BrandBluePrimary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionKey,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuestionPaletteBottomSheet(
    questions: List<Question>,
    currentIndex: Int,
    userAnswers: Map<String, String>,
    markedForReview: Set<String>,
    onSelectQuestion: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Question Palette",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PaletteLegendItem(color = BrandEmerald, text = "Answered")
            PaletteLegendItem(color = BrandPurple, text = "Review")
            PaletteLegendItem(color = BrandRose, text = "Skipped")
            PaletteLegendItem(color = MaterialTheme.colorScheme.surfaceVariant, text = "Not Visited")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 46.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
        ) {
            itemsIndexed(questions) { index, q ->
                val isAnswered = userAnswers.containsKey(q.id)
                val isMarked = markedForReview.contains(q.id)
                val isCurrent = index == currentIndex

                val btnBg = when {
                    isMarked -> BrandPurple
                    isAnswered -> BrandEmerald
                    index < currentIndex -> BrandRose
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = if (isMarked || isAnswered || index < currentIndex) Color.White else MaterialTheme.colorScheme.onSurface

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(btnBg)
                        .clickable { onSelectQuestion(index) }
                        .border(
                            width = if (isCurrent) 2.5.dp else 0.dp,
                            color = if (isCurrent) BrandBluePrimary else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("palette_q_${index + 1}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteLegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StudentDetailsStartDialog(
    testTitle: String,
    subjectName: String = "",
    totalQuestions: Int,
    durationMinutes: Int,
    onStartTest: (name: String, studentClass: String, email: String, mobile: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("Class 12th") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("student_details_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📝 Start Test / Quiz",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = BrandBluePrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = testTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (subjectName.isNotBlank()) {
                    Text(
                        text = "विषय (Subject): $subjectName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandIndigo
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = BrandBluePrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⏱️ $durationMinutes मिनट (Time)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBluePrimary
                        )
                        Text(
                            text = "❓ $totalQuestions प्रश्न (Questions)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("विद्यार्थी का नाम (Full Name) *") },
                    placeholder = { Text("उदा. राहुल कुमार") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("student_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = selectedClass,
                    onValueChange = { selectedClass = it },
                    label = { Text("कक्षा (Class / Stream) *") },
                    placeholder = { Text("उदा. Class 12th Arts/Science") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("student_class_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाइल नंबर (Mobile Number) *") },
                    placeholder = { Text("उदा. 9876543210") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("student_mobile_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("जीमेल / ईमेल (Gmail Address)") },
                    placeholder = { Text("उदा. student@gmail.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("student_email_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("रद्द करें")
                    }

                    Button(
                        onClick = {
                            onStartTest(
                                if (name.isBlank()) "Student" else name.trim(),
                                if (selectedClass.isBlank()) "Class 12th" else selectedClass.trim(),
                                email.trim(),
                                mobile.trim()
                            )
                        },
                        modifier = Modifier.weight(1.3f).testTag("dialog_start_test_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("प्रारंभ करें (Start)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
