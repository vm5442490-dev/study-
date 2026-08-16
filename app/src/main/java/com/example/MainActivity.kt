package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.model.Book
import com.example.data.model.PdfDocument
import com.example.data.model.StudyNote
import com.example.ui.components.NavDestination
import com.example.ui.components.SuperStudyBottomBar
import com.example.ui.components.SuperStudyTopBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.TestViewModel
import com.example.ui.viewmodel.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        setContent {
            val themeMode by mainViewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SuperStudyTheme(darkTheme = isDark) {
                SuperStudyApp(mainViewModel = mainViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperStudyApp(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.Home.route

    val homeState by mainViewModel.homeState.collectAsState()
    val selectedClass by mainViewModel.selectedClass.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Shared state for detailed views
    var activeNote by remember { mutableStateOf<StudyNote?>(null) }
    var activePdf by remember { mutableStateOf<PdfDocument?>(null) }

    val context = LocalContext.current
    val activity = context as? Activity
    val navigateBackWithAd: () -> Unit = {
        if (activity != null) {
            com.example.util.InterstitialAdManager.showAdIfReady(activity) {
                navController.popBackStack()
            }
        } else {
            navController.popBackStack()
        }
    }

    val testViewModel: TestViewModel = viewModel()

    val isTopLevelDestination = currentRoute in listOf(
        NavDestination.Home.route,
        NavDestination.Study.route,
        NavDestination.Tests.route,
        NavDestination.Leaderboard.route,
        NavDestination.More.route
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTopLevelDestination,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandBluePrimary)
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "SUPER STUDY",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Complete Study & Exam Hub",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home (मुख्य पृष्ठ)") },
                    selected = currentRoute == NavDestination.Home.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(NavDestination.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AutoStories, contentDescription = null) },
                    label = { Text("Study Resources (अध्ययन)") },
                    selected = currentRoute == NavDestination.Study.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(NavDestination.Study.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                    label = { Text("Online Tests (ऑनलाइन टेस्ट)") },
                    selected = currentRoute == NavDestination.Tests.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(NavDestination.Tests.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Quiz, contentDescription = null) },
                    label = { Text("Question Bank (प्रश्न बैंक)") },
                    selected = currentRoute == "question_bank",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("question_bank")
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                    label = { Text("Leaderboard (लीडरबोर्ड)") },
                    selected = currentRoute == NavDestination.Leaderboard.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(NavDestination.Leaderboard.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                    label = { Text("Saved Bookmarks (बुकमार्क)") },
                    selected = currentRoute == "bookmarks",
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate("bookmarks")
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings & More") },
                    selected = currentRoute == NavDestination.More.route,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        navController.navigate(NavDestination.More.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (isTopLevelDestination) {
                    SuperStudyTopBar(
                        selectedClass = selectedClass,
                        classes = homeState.classes,
                        onClassSelected = { mainViewModel.setSelectedClass(it) },
                        onSearchClick = { navController.navigate("global_search") },
                        onNotificationClick = { navController.navigate("notifications") },
                        onMenuClick = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isTopLevelDestination) {
                    SuperStudyBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            },
            modifier = modifier.fillMaxSize().testTag("main_app_scaffold")
        ) { innerPadding ->
                val isTopLevel = currentRoute in listOf(
                    NavDestination.Home.route,
                    NavDestination.Study.route,
                    NavDestination.Tests.route,
                    NavDestination.Leaderboard.route,
                    NavDestination.More.route
                )
                if (!isTopLevel && currentRoute != "test_result") {
                    BackHandler {
                        navigateBackWithAd()
                    }
                }

            NavHost(
                navController = navController,
                startDestination = NavDestination.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                // 1. HOME SCREEN
                composable(NavDestination.Home.route) {
                    HomeScreen(
                        viewModel = mainViewModel,
                        onNavigateToStudyTab = { tabIndex, category ->
                            mainViewModel.setStudyTab(tabIndex)
                            if (category != null) {
                                mainViewModel.setStudyCategory(category)
                            }
                            navController.navigate(NavDestination.Study.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToTest = { testId ->
                            navController.navigate("test_runner/$testId")
                        },
                        onNavigateToQuestionBank = {
                            navController.navigate("question_bank")
                        },
                        onNavigateToLeaderboard = {
                            navController.navigate(NavDestination.Leaderboard.route)
                        }
                    )
                }

                // 2. STUDY SCREEN
                composable(NavDestination.Study.route) {
                    StudyScreen(
                        viewModel = mainViewModel,
                        onOpenBook = { book ->
                            if (!book.pdfUrl.isNullOrBlank()) {
                                activePdf = PdfDocument(
                                    id = book.id,
                                    title = book.title,
                                    description = "${book.category} • ${book.subjectName}",
                                    category = "Books",
                                    classId = book.classId,
                                    subjectId = book.subjectId,
                                    subjectName = book.subjectName,
                                    fileUrl = book.pdfUrl,
                                    pagesCount = book.chaptersCount.coerceAtLeast(10),
                                    fileSize = "3.2 MB"
                                )
                                navController.navigate("pdf_viewer")
                            } else {
                                activeNote = StudyNote(
                                    id = book.id,
                                    title = book.title,
                                    classId = book.classId,
                                    subjectId = book.subjectId,
                                    subjectName = book.subjectName,
                                    chapterId = "ch-1",
                                    chapterTitle = "संपूर्ण पाठ्यपुस्तक सारांश",
                                    summary = "${book.category} - ${book.author} द्वारा रचित।",
                                    keyPoints = listOf(
                                        "सभी ${book.chaptersCount} अध्यायों का पाठ्यक्रम अनुसार संकलन।",
                                        "बोर्ड परीक्षा के नए पैटर्न पर आधारित महत्वपूर्ण प्रश्न एवं उत्तर।",
                                        "एनसीईआरटी व जेएसी बोर्ड द्वारा अनुमोदित नवीनतम संस्करण।"
                                    ),
                                    content = """
                                        # ${book.title}
                                        
                                        ## प्रकाशक एवं विवरण
                                        - प्रकाशक: ${book.author}
                                        - कुल अध्याय: ${book.chaptersCount}
                                        - बोर्ड: ${book.category}
                                        
                                        ## मुख्य अध्ययन सामग्री
                                        यह पुस्तक छात्रों को अवधारणाओं को सरलता से समझने और परीक्षा में अधिकतम अंक प्राप्त करने में सहायता के लिए तैयार की गई है।
                                        प्रत्येक अध्याय के अंत में अभ्यास प्रश्न, बहुविकल्पीय प्रश्न और मॉडल उत्तर दिए गए हैं।
                                    """.trimIndent()
                                )
                                navController.navigate("book_reader")
                            }
                        },
                        onOpenNote = { note ->
                            if (note.content.startsWith("http://") || note.content.startsWith("https://")) {
                                activePdf = PdfDocument(
                                    id = note.id,
                                    title = note.title,
                                    description = "${note.subjectName} • ${note.chapterTitle}",
                                    category = "Notes",
                                    classId = note.classId,
                                    subjectId = note.subjectId,
                                    subjectName = note.subjectName,
                                    fileUrl = note.content,
                                    pagesCount = 8,
                                    fileSize = "2.0 MB"
                                )
                                navController.navigate("pdf_viewer")
                            } else {
                                activeNote = note
                                navController.navigate("book_reader")
                            }
                        },
                        onOpenPdf = { pdf ->
                            activePdf = pdf
                            navController.navigate("pdf_viewer")
                        }
                    )
                }

                // 3. TESTS SCREEN
                composable(NavDestination.Tests.route) {
                    TestsScreen(
                        viewModel = mainViewModel,
                        onStartTest = { testId ->
                            navController.navigate("test_runner/$testId")
                        }
                    )
                }

                // 4. LEADERBOARD SCREEN
                composable(NavDestination.Leaderboard.route) {
                    LeaderboardScreen(viewModel = mainViewModel)
                }

                // 5. MORE SCREEN
                composable(NavDestination.More.route) {
                    MoreScreen(
                        viewModel = mainViewModel,
                        onNavigateToBookmarks = { navController.navigate("bookmarks") },
                        onNavigateToHistory = { navController.navigate("test_history") },
                        onNavigateToQuestionBank = { navController.navigate("question_bank") },
                        onNavigateToAdminPdf = { navController.navigate("admin_pdf") }
                    )
                }

                // 6. TEST RUNNER
                composable(
                    route = "test_runner/{testId}",
                    arguments = listOf(navArgument("testId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val testId = backStackEntry.arguments?.getString("testId") ?: "test-daily-today"
                    TestRunnerScreen(
                        testId = testId,
                        viewModel = testViewModel,
                        onNavigateBack = navigateBackWithAd,
                        onTestFinished = {
                            navController.navigate("test_result") {
                                popUpTo("test_runner/$testId") { inclusive = true }
                            }
                        }
                    )
                }

                // 7. TEST RESULT & SCORECARD
                composable("test_result") {
                    TestResultScreen(
                        viewModel = testViewModel,
                        onRetake = {
                            val testId = testViewModel.testState.value.test?.id ?: "test-daily-today"
                            navController.navigate("test_runner/$testId") {
                                popUpTo("test_result") { inclusive = true }
                            }
                        },
                        onBackToHome = {
                            navController.navigate(NavDestination.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            }
                        }
                    )
                }

                // 8. BOOK & NOTE READER
                composable("book_reader") {
                    val note = activeNote ?: StudyNote(
                        id = "empty",
                        title = "अध्ययन सामग्री",
                        classId = "",
                        subjectId = "",
                        subjectName = "",
                        chapterId = "",
                        chapterTitle = "",
                        summary = "",
                        keyPoints = emptyList(),
                        content = "कोई सामग्री चयनित नहीं है।"
                    )
                    BookReaderScreen(
                        note = note,
                        viewModel = mainViewModel,
                        onNavigateBack = navigateBackWithAd
                    )
                }

                // 9. PDF VIEWER
                composable("pdf_viewer") {
                    val pdf = activePdf ?: PdfDocument(
                        id = "empty-pdf",
                        title = "PDF दस्तावेज़",
                        description = "कोई PDF चयनित नहीं है",
                        category = "Document",
                        fileUrl = "",
                        pagesCount = 1,
                        fileSize = ""
                    )
                    PdfViewerScreen(
                        pdf = pdf,
                        onNavigateBack = navigateBackWithAd
                    )
                }

                // 10. QUESTION BANK
                composable("question_bank") {
                    QuestionBankScreen(
                        viewModel = mainViewModel,
                        onNavigateBack = navigateBackWithAd
                    )
                }

                // 11. GLOBAL SEARCH
                composable("global_search") {
                    GlobalSearchScreen(
                        viewModel = mainViewModel,
                        onOpenBook = { book ->
                            activeNote = StudyNote(
                                id = book.id,
                                title = book.title,
                                classId = book.classId,
                                subjectId = book.subjectId,
                                subjectName = book.subjectName,
                                chapterId = "ch-1",
                                chapterTitle = "पुस्तक सारांश",
                                summary = book.title,
                                keyPoints = listOf("अध्यायवार संपूर्ण संकलन"),
                                content = book.title
                            )
                            navController.navigate("book_reader")
                        },
                        onOpenNote = { note ->
                            activeNote = note
                            navController.navigate("book_reader")
                        },
                        onOpenPdf = { pdf ->
                            activePdf = pdf
                            navController.navigate("pdf_viewer")
                        },
                        onStartTest = { testId ->
                            navController.navigate("test_runner/$testId")
                        },
                        onNavigateBack = navigateBackWithAd
                    )
                }

                // 12. BOOKMARKS
                composable("bookmarks") {
                    BookmarksScreen(
                        viewModel = mainViewModel,
                        onNavigateBack = navigateBackWithAd
                    )
                }

                // 13. TEST HISTORY
                composable("test_history") {
                    TestHistoryScreen(
                        viewModel = mainViewModel,
                        onNavigateBack = navigateBackWithAd
                    )
                }

                // 14. NOTIFICATIONS
                composable("notifications") {
                    NotificationsScreen(
                        viewModel = mainViewModel,
                        onNavigateBack = navigateBackWithAd
                    )
                }

                // 15. ADMIN PDF STUDIO
                composable("admin_pdf") {
                    AdminPdfScreen(
                        viewModel = mainViewModel,
                        onOpenPdf = { pdf ->
                            activePdf = pdf
                            navController.navigate("pdf_viewer")
                        },
                        onNavigateBack = navigateBackWithAd
                    )
                }
            }
        }
    }
}
