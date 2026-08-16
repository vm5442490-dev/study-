package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OnlineTest
import com.example.ui.components.*
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToStudyTab: (tabIndex: Int, category: String?) -> Unit,
    onNavigateToTest: (String) -> Unit,
    onNavigateToQuestionBank: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val homeState by viewModel.homeState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadInitialData()
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier.testTag("home_screen_scaffold"),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (homeState.isLoading && homeState.announcements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBluePrimary)
            }
        } else if (homeState.errorMessage != null && homeState.announcements.isEmpty()) {
            ErrorStateView(
                message = homeState.errorMessage ?: "त्रुटि",
                onRetry = { viewModel.loadInitialData() },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("home_scroll_column"),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Prominent Announcement Banner
                item {
                    val announcement = homeState.announcements.firstOrNull()
                    if (announcement != null) {
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            AnnouncementBannerCard(
                                announcement = announcement,
                                onViewClick = { onNavigateToStudyTab(0, null) }
                            )
                        }
                    }
                }

                // 2. Quick Access Cards (2x2 Grid)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "⚡ त्वरित पहुंच (Quick Access)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickAccessCard(
                                title = "Daily Quiz",
                                subtitle = "आज का टेस्ट दें",
                                badgeText = "LIVE",
                                icon = Icons.Default.Bolt,
                                gradientColors = listOf(Color(0xFFEA580C), Color(0xFFF97316)),
                                onClick = {
                                    homeState.dailyQuiz?.let { onNavigateToTest(it.id) }
                                        ?: onNavigateToTest("test-daily-today")
                                },
                                modifier = Modifier.weight(1f)
                            )

                            QuickAccessCard(
                                title = "Study Notes",
                                subtitle = "अध्यायवार नोट्स",
                                badgeText = "BEST",
                                icon = Icons.Default.MenuBook,
                                gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
                                onClick = { onNavigateToStudyTab(1, null) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickAccessCard(
                                title = "All PDFs",
                                subtitle = "किताबें और पेपर",
                                badgeText = "FREE",
                                icon = Icons.Default.PictureAsPdf,
                                gradientColors = listOf(Color(0xFFE11D48), Color(0xFFF43F5E)),
                                onClick = { onNavigateToStudyTab(2, null) },
                                modifier = Modifier.weight(1f)
                            )

                            QuickAccessCard(
                                title = "Leaderboard",
                                subtitle = "रैंक और स्कोर",
                                badgeText = "TOP",
                                icon = Icons.Default.EmojiEvents,
                                gradientColors = listOf(Color(0xFF059669), Color(0xFF10B981)),
                                onClick = onNavigateToLeaderboard,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Daily Quiz Hero Banner
                item {
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        DailyQuizCard(
                            test = homeState.dailyQuiz,
                            onStartClick = onNavigateToTest
                        )
                    }
                }

                // AdMob Banner (Between Daily Quiz and JAC Class 12)
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        AndroidView(
                            factory = { context ->
                                AdView(context).apply {
                                    setAdSize(AdSize.BANNER)
                                    // Real Ad Unit ID for production
                                    adUnitId = "ca-app-pub-3665825190622425/5013536957"
                                    loadAd(AdRequest.Builder().build())
                                }
                            }
                        )
                    }
                }

                // 3.5 JAC Class 12 Special Banner
                item {
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToStudyTab(0, "JAC Class 12") },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandEmerald.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = "Class 12",
                                            tint = BrandEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "JAC Board Class 12",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandEmerald
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Model Papers, PYQs & Notes (2026)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Go",
                                    tint = BrandEmerald,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Study Resources Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📚 अध्ययन सामग्री (Study Resources)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { onNavigateToStudyTab(0, null) }) {
                                Text("सभी देखें", color = BrandBluePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudyResourceGridCard(
                                    title = "NCERT Books",
                                    subtitle = "सभी विषय पुस्तकें",
                                    icon = Icons.Default.MenuBook,
                                    iconBgColor = BrandBluePrimary,
                                    onClick = { onNavigateToStudyTab(0, "NCERT Books") },
                                    modifier = Modifier.weight(1f)
                                )
                                StudyResourceGridCard(
                                    title = "JAC Books",
                                    subtitle = "झारखंड बोर्ड स्पेशल",
                                    icon = Icons.Default.LibraryBooks,
                                    iconBgColor = BrandEmerald,
                                    onClick = { onNavigateToStudyTab(0, "JAC Books") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudyResourceGridCard(
                                    title = "Model Papers",
                                    subtitle = "2026 परीक्षा सेट",
                                    icon = Icons.Default.Description,
                                    iconBgColor = BrandOrange,
                                    onClick = { onNavigateToStudyTab(3, null) },
                                    modifier = Modifier.weight(1f)
                                )
                                StudyResourceGridCard(
                                    title = "PYQ Papers",
                                    subtitle = "विगत वर्षों के प्रश्न",
                                    icon = Icons.Default.HistoryEdu,
                                    iconBgColor = BrandPurple,
                                    onClick = { onNavigateToStudyTab(4, null) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudyResourceGridCard(
                                    title = "Question Bank",
                                    subtitle = "अभ्यास प्रश्नोत्तरी",
                                    icon = Icons.Default.Quiz,
                                    iconBgColor = BrandTeal,
                                    onClick = onNavigateToQuestionBank,
                                    modifier = Modifier.weight(1f)
                                )
                                StudyResourceGridCard(
                                    title = "Study Notes",
                                    subtitle = "रिवीजन नोट्स",
                                    icon = Icons.Default.AutoStories,
                                    iconBgColor = BrandRose,
                                    onClick = { onNavigateToStudyTab(1, null) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 5. Latest Online Tests
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📝 नवीनतम ऑनलाइन टेस्ट (Latest Tests)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            homeState.featuredTests.forEach { test ->
                                TestItemCard(
                                    test = test,
                                    onStartClick = onNavigateToTest
                                )
                            }
                        }
                    }
                }

                // 6. Top 3 Leaderboard Preview
                if (homeState.topLeaderboard.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏆 लीडरबोर्ड टॉपर्स (Top Ranks)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(onClick = onNavigateToLeaderboard) {
                                    Text("पूरा लीडरबोर्ड", color = BrandBluePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LeaderboardPodium(entries = homeState.topLeaderboard)
                        }
                    }
                }

                // 7. Daily Updates Feed
                if (homeState.dailyUpdates.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "📢 दैनिक अपडेट्स (Daily Updates)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                homeState.dailyUpdates.forEach { update ->
                                    DailyUpdateCard(update = update)
                                }
                            }
                        }
                    }
                }
                
            }
        }
    }
}
