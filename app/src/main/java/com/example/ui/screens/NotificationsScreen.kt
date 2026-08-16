package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.DailyUpdate
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

data class UnifiedNotification(
    val id: String,
    val title: String,
    val content: String,
    val tag: String,
    val date: String,
    val isPinned: Boolean = false,
    val link: String? = null,
    val type: String = "UPDATE" // "BOARD", "EXAM", "GENERAL", "QUIZ"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val homeState by viewModel.homeState.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    // Combine announcements and dailyUpdates with standard board updates fallback
    val allNotifications = remember(homeState.announcements, homeState.dailyUpdates) {
        val list = mutableListOf<UnifiedNotification>()

        // 1. Add cloud announcements from Supabase
        homeState.announcements.forEach { ann ->
            list.add(
                UnifiedNotification(
                    id = "ann-${ann.id}",
                    title = ann.title,
                    content = ann.description,
                    tag = ann.badge.ifBlank { "NOTICE" },
                    date = ann.createdAt?.take(10) ?: "Latest",
                    isPinned = true,
                    link = ann.link,
                    type = "BOARD"
                )
            )
        }

        // 2. Add daily updates from Supabase
        homeState.dailyUpdates.forEach { up ->
            list.add(
                UnifiedNotification(
                    id = "up-${up.id}",
                    title = up.title,
                    content = up.content,
                    tag = up.tag.ifBlank { "UPDATE" },
                    date = up.date.ifBlank { "Today" },
                    isPinned = up.isPinned,
                    type = if (up.tag.contains("EXAM", ignoreCase = true)) "EXAM" else "UPDATE"
                )
            )
        }

        // 3. If database has no entries yet, show authentic default board notifications
        if (list.isEmpty()) {
            list.addAll(
                listOf(
                    UnifiedNotification(
                        id = "default-1",
                        title = "📢 JAC Board 2026 Model Question Papers जारी",
                        content = "झारखंड अधिविद्य परिषद् (JAC) द्वारा कक्षा 9वीं से 12वीं तक के सभी मुख्य विषयों के नवीन प्रारूप मॉडल प्रश्न पत्र जारी कर दिए गए हैं। 'All PDFs' एवं 'Study' टैब से तुरंत डाउनलोड करें।",
                        tag = "MODEL PAPER",
                        date = "2026",
                        isPinned = true,
                        type = "BOARD"
                    ),
                    UnifiedNotification(
                        id = "default-2",
                        title = "⚡ Daily Live Quiz प्रतियोगिता प्रतिदिन शाम 7:00 बजे",
                        content = "अपनी बोर्ड परीक्षा की तैयारी को परखें और राज्य स्तरीय लीडरबोर्ड में टॉप रैंक प्राप्त करें। प्रत्येक प्रश्न के साथ विस्तृत समाधान (Explanation) उपलब्ध है।",
                        tag = "LIVE QUIZ",
                        date = "Daily",
                        isPinned = true,
                        type = "QUIZ"
                    ),
                    UnifiedNotification(
                        id = "default-3",
                        title = "📚 NCERT New Edition Solutions & Handwritten Notes",
                        content = "कक्षा 10वीं एवं 12वीं के इतिहास, भूगोल, विज्ञान, गणित और राजनीति विज्ञान के सभी अध्यायों के संक्षिप्त नोट्स एवं महत्वपूर्ण प्रश्नोत्तर जोड़ दिए गए हैं।",
                        tag = "NOTES",
                        date = "2026",
                        isPinned = false,
                        type = "UPDATE"
                    ),
                    UnifiedNotification(
                        id = "default-4",
                        title = "🎯 Previous 5 Years Board Question Papers (PYQ)",
                        content = "वर्ष 2020 से 2025 तक के सभी वार्षिक परीक्षा ओरिजिनल प्रश्न पत्र उत्तरमाला सहित उपलब्ध हैं। 'PDFs & Books' सेक्शन में देखें।",
                        tag = "PYQ",
                        date = "2026",
                        isPinned = false,
                        type = "EXAM"
                    )
                )
            )
        }

        list
    }

    val filteredNotifications = remember(allNotifications, selectedFilter) {
        when (selectedFilter) {
            "Board News" -> allNotifications.filter { it.type == "BOARD" || it.tag.contains("BOARD", true) || it.tag.contains("MODEL", true) }
            "Live Quiz" -> allNotifications.filter { it.type == "QUIZ" || it.tag.contains("QUIZ", true) }
            "Exams & PYQ" -> allNotifications.filter { it.type == "EXAM" || it.tag.contains("EXAM", true) || it.tag.contains("PYQ", true) }
            else -> allNotifications
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "सूचनाएं एवं अपडेट्स",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Board Alerts • Daily Quiz • Exam Notices",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("notifications_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isRefreshing = true
                                viewModel.loadInitialData()
                                isRefreshing = false
                                Toast.makeText(context, "सूचनाएं अपडेट हो गई हैं ✅", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("notifications_refresh_btn")
                    ) {
                        if (isRefreshing || homeState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("notifications_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Board News", "Live Quiz", "Exams & PYQ").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Notifications List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotifications, key = { it.id }) { item ->
                    NotificationCard(
                        notification = item,
                        onClick = {
                            if (!item.link.isNullOrBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "लिंक खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: UnifiedNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tagColor = when {
        notification.tag.contains("MODEL", true) || notification.tag.contains("EXAM", true) -> BrandRose
        notification.tag.contains("QUIZ", true) || notification.tag.contains("LIVE", true) -> BrandAmber
        notification.tag.contains("NEW", true) || notification.tag.contains("NOTICE", true) -> BrandEmerald
        else -> BrandBluePrimary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !notification.link.isNullOrBlank()) { onClick() }
            .testTag("notification_card_${notification.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isPinned) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isPinned) 2.dp else 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = tagColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = notification.tag,
                            color = tagColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    if (notification.isPinned) {
                        Surface(
                            color = BrandAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PushPin, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("PINNED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandAmber)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = notification.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notification.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.content,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            if (!notification.link.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "यहाँ टैप करके देखें",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBluePrimary
                    )
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = BrandBluePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
