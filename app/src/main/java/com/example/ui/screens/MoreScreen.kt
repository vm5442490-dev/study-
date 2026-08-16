package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.BrandRose
import com.example.ui.theme.BrandIndigo
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ThemeMode

@Composable
fun MoreScreen(
    viewModel: MainViewModel,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToQuestionBank: () -> Unit,
    onNavigateToAdminPdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val testAttempts by viewModel.testAttempts.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("more_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Summary Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandBluePrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SUPER STUDY",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your Complete Board Exam & Study Hub • JAC & NCERT",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Quick Tools Group
        item {
            Text(
                text = "📁 My Library & Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Saved Bookmarks", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("${bookmarks.size} items saved", fontSize = 12.sp) },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = BrandAmber)
                        },
                        trailingContent = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable { onNavigateToBookmarks() }
                            .testTag("more_bookmarks_item")
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Test History & Attempts", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("${testAttempts.size} tests completed", fontSize = 12.sp) },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = BrandEmerald)
                        },
                        trailingContent = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable { onNavigateToHistory() }
                            .testTag("more_history_item")
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Question Bank Practice", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Chapter-wise practice with solutions", fontSize = 12.sp) },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = BrandBluePrimary)
                        },
                        trailingContent = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        },
                        modifier = Modifier
                            .clickable { onNavigateToQuestionBank() }
                            .testTag("more_qbank_item")
                    )
                }
            }
        }

        // App Preferences
        item {
            Text(
                text = "⚙️ Preferences & Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Theme Mode", fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(
                                text = when (themeMode) {
                                    ThemeMode.SYSTEM -> "System Default"
                                    ThemeMode.LIGHT -> "Light Mode"
                                    ThemeMode.DARK -> "Dark Mode"
                                },
                                fontSize = 12.sp
                            )
                        },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = BrandBluePrimary)
                        },
                        modifier = Modifier
                            .clickable { showThemeDialog = true }
                            .testTag("more_theme_item")
                    )
                }
            }
        }

        // About & Version
        item {
            Text(
                text = "ℹ️ About Application",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "SUPER STUDY v1.0.0", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Designed for seamless board exam preparation, online quizzes, and study material distribution. Built with Kotlin, Jetpack Compose, Room & Supabase Cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode(ThemeMode.SYSTEM)
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == ThemeMode.SYSTEM, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("System Default")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode(ThemeMode.LIGHT)
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == ThemeMode.LIGHT, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Light Mode")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode(ThemeMode.DARK)
                                showThemeDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == ThemeMode.DARK, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dark Mode")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
