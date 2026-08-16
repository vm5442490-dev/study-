package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyNote
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandPurple
import com.example.ui.viewmodel.MainViewModel

enum class NoteReaderTheme {
    DAY, SEPIA, NIGHT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    note: StudyNote,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val isBookmarked = bookmarks.any { it.itemId == note.id }

    var readerTheme by remember { mutableStateOf(NoteReaderTheme.DAY) }
    var fontSizeMultiplier by remember { mutableStateOf(1.0f) }

    val bgColor = when (readerTheme) {
        NoteReaderTheme.DAY -> MaterialTheme.colorScheme.background
        NoteReaderTheme.SEPIA -> Color(0xFFFDF6E2)
        NoteReaderTheme.NIGHT -> Color(0xFF0F172A)
    }

    val cardColor = when (readerTheme) {
        NoteReaderTheme.DAY -> MaterialTheme.colorScheme.surface
        NoteReaderTheme.SEPIA -> Color(0xFFFFFDF5)
        NoteReaderTheme.NIGHT -> Color(0xFF1E293B)
    }

    val textColor = when (readerTheme) {
        NoteReaderTheme.DAY -> MaterialTheme.colorScheme.onSurface
        NoteReaderTheme.SEPIA -> Color(0xFF4A3B2C)
        NoteReaderTheme.NIGHT -> Color(0xFFF1F5F9)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${note.subjectName} • ${note.chapterTitle}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Font Size Adjuster
                    IconButton(
                        onClick = {
                            fontSizeMultiplier = when (fontSizeMultiplier) {
                                1.0f -> 1.15f
                                1.15f -> 1.3f
                                else -> 1.0f
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font Size",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Reader Theme (Day/Sepia/Night)
                    IconButton(
                        onClick = {
                            readerTheme = when (readerTheme) {
                                NoteReaderTheme.DAY -> NoteReaderTheme.SEPIA
                                NoteReaderTheme.SEPIA -> NoteReaderTheme.NIGHT
                                NoteReaderTheme.NIGHT -> NoteReaderTheme.DAY
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (readerTheme) {
                                NoteReaderTheme.DAY -> Icons.Outlined.LightMode
                                NoteReaderTheme.SEPIA -> Icons.Outlined.WbSunny
                                NoteReaderTheme.NIGHT -> Icons.Outlined.DarkMode
                            },
                            contentDescription = "Theme",
                            tint = when (readerTheme) {
                                NoteReaderTheme.DAY -> MaterialTheme.colorScheme.onSurface
                                NoteReaderTheme.SEPIA -> BrandAmber
                                NoteReaderTheme.NIGHT -> BrandPurple
                            }
                        )
                    }

                    // Bookmark
                    IconButton(
                        onClick = {
                            viewModel.toggleBookmark(
                                itemId = note.id,
                                itemType = "NOTE",
                                title = note.title,
                                subtitle = note.chapterTitle
                            )
                        },
                        modifier = Modifier.testTag("reader_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) BrandAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        modifier = modifier.testTag("book_reader_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Key Points Summary Highlight Box
                if (note.keyPoints.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (readerTheme == NoteReaderTheme.NIGHT) BrandPurple.copy(alpha = 0.2f) else BrandPurple.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📌 मुख्य बिंदु एवं सार (Key Revision Points)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPurple,
                                    fontSize = (14 * fontSizeMultiplier).sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                note.keyPoints.forEach { pt ->
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("• ", fontWeight = FontWeight.Black, color = BrandPurple, fontSize = (14 * fontSizeMultiplier).sp)
                                        Text(
                                            text = pt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textColor,
                                            lineHeight = (22 * fontSizeMultiplier).sp,
                                            fontSize = (13.5f * fontSizeMultiplier).sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Text Body
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor,
                                lineHeight = (28 * fontSizeMultiplier).sp,
                                fontSize = (15 * fontSizeMultiplier).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

