package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DailyQuizCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TestItemCard
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TestsScreen(
    viewModel: MainViewModel,
    onStartTest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTests by viewModel.allTests.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredTests = remember(allTests, selectedFilter) {
        when (selectedFilter) {
            "Daily Quiz" -> allTests.filter { it.isDailyQuiz }
            "Featured" -> allTests.filter { it.isFeatured }
            else -> allTests
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("tests_screen")
    ) {
        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Daily Quiz", "Featured").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBluePrimary.copy(alpha = 0.15f),
                        selectedLabelColor = BrandBluePrimary
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedFilter == "All" || selectedFilter == "Daily Quiz") {
                item {
                    DailyQuizCard(
                        test = homeState.dailyQuiz,
                        onStartClick = onStartTest
                    )
                }
            }

            if (filteredTests.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "कोई टेस्ट उपलब्ध नहीं है",
                        message = "नए टेस्ट जल्द ही जोड़े जाएंगे।"
                    )
                }
            } else {
                items(filteredTests) { test ->
                    TestItemCard(
                        test = test,
                        onStartClick = onStartTest
                    )
                }
            }
        }
    }
}
