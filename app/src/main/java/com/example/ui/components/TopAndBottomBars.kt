package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyClass
import com.example.ui.theme.*

sealed class NavDestination(
    val route: String,
    val title: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
) {
    data object Home : NavDestination("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Study : NavDestination("study", "Study", Icons.Filled.AutoStories, Icons.Outlined.AutoStories)
    data object Tests : NavDestination("tests", "Tests", Icons.Filled.Assignment, Icons.Outlined.Assignment)
    data object Leaderboard : NavDestination("leaderboard", "Ranks", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents)
    data object More : NavDestination("more", "More", Icons.Filled.Menu, Icons.Outlined.Menu)

    companion object {
        val items: List<NavDestination>
            get() = listOf(Home, Study, Tests, Leaderboard, More)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperStudyTopBar(
    selectedClass: String,
    classes: List<StudyClass>,
    onClassSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var classMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onMenuClick,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(38.dp).testTag("menu_drawer_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(BrandBluePrimary, BrandPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SUPER",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandBluePrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "STUDY",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandPurple,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Exams • Notes • Tests",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Class Selector Chip
                    Box {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { classMenuExpanded = true }
                                .testTag("class_selector_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentClassName = when (selectedClass) {
                                    "class-12" -> "Class 12"
                                    "class-11" -> "Class 11"
                                    "class-10" -> "Class 10"
                                    "class-9" -> "Class 9"
                                    else -> "All"
                                }
                                Text(
                                    text = currentClassName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandIndigo
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Class",
                                    tint = BrandIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = classMenuExpanded,
                            onDismissRequest = { classMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Classes", fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    onClassSelected("all")
                                    classMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Class 12th (Arts / Sci / Comm)", fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    onClassSelected("class-12")
                                    classMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Class 11th", fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    onClassSelected("class-11")
                                    classMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Class 10th (Matric)", fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    onClassSelected("class-10")
                                    classMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Class 9th", fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    onClassSelected("class-9")
                                    classMenuExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        onClick = onSearchClick,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(36.dp).testTag("global_search_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        onClick = onNotificationClick,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(36.dp).testTag("notifications_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(BrandRose)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun SuperStudyBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                thickness = 1.dp
            )
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("super_study_bottom_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavDestination.items.forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(dest.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.iconFilled else dest.iconOutlined,
                                contentDescription = dest.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = dest.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandIndigo,
                            selectedTextColor = BrandIndigo,
                            indicatorColor = BrandIndigo.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${dest.route}")
                    )
                }
            }
        }
    }
}
