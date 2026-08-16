package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    viewModel: MainViewModel,
    onOpenBook: (Book) -> Unit,
    onOpenNote: (StudyNote) -> Unit,
    onOpenPdf: (PdfDocument) -> Unit,
    onStartTest: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val studyState by viewModel.studyState.collectAsState()
    val allTests by viewModel.allTests.collectAsState()

    val filteredBooks = remember(query, studyState.books) {
        if (query.isBlank()) emptyList()
        else studyState.books.filter { it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true) }
    }

    val filteredNotes = remember(query, studyState.notes) {
        if (query.isBlank()) emptyList()
        else studyState.notes.filter { it.title.contains(query, ignoreCase = true) || it.summary.contains(query, ignoreCase = true) }
    }

    val filteredPdfs = remember(query, studyState.pdfs) {
        if (query.isBlank()) emptyList()
        else studyState.pdfs.filter { it.title.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
    }

    val filteredTests = remember(query, allTests) {
        if (query.isBlank()) emptyList()
        else allTests.filter { it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search books, notes, tests...", fontSize = 14.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("global_search_input")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.testTag("global_search_screen")
    ) { paddingValues ->
        if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("खोजने के लिए ऊपर टाइप करें (उदा. History, Model Paper, Quiz)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else if (filteredBooks.isEmpty() && filteredNotes.isEmpty() && filteredPdfs.isEmpty() && filteredTests.isEmpty()) {
            EmptyStateView(
                title = "कोई परिणाम नहीं मिला",
                message = "'$query' के लिए कोई सामग्री नहीं मिली। कृपया कोई अन्य शब्द खोजें।",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredTests.isNotEmpty()) {
                    item { Text("Tests (${filteredTests.size})", fontWeight = FontWeight.Bold, color = BrandBluePrimary) }
                    items(filteredTests) { test ->
                        ListItem(
                            headlineContent = { Text(test.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("${test.questionsCount} Qs • ${test.durationMinutes} mins", fontSize = 11.sp) },
                            leadingContent = { Icon(Icons.Default.Assignment, contentDescription = null, tint = BrandBluePrimary) },
                            modifier = Modifier.clickable { onStartTest(test.id) }
                        )
                    }
                }

                if (filteredNotes.isNotEmpty()) {
                    item { Text("Study Notes (${filteredNotes.size})", fontWeight = FontWeight.Bold, color = BrandBluePrimary) }
                    items(filteredNotes) { note ->
                        ListItem(
                            headlineContent = { Text(note.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(note.chapterTitle, fontSize = 11.sp) },
                            leadingContent = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = BrandBluePrimary) },
                            modifier = Modifier.clickable { onOpenNote(note) }
                        )
                    }
                }

                if (filteredBooks.isNotEmpty()) {
                    item { Text("Books (${filteredBooks.size})", fontWeight = FontWeight.Bold, color = BrandBluePrimary) }
                    items(filteredBooks) { book ->
                        ListItem(
                            headlineContent = { Text(book.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text(book.category, fontSize = 11.sp) },
                            leadingContent = { Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = BrandBluePrimary) },
                            modifier = Modifier.clickable { onOpenBook(book) }
                        )
                    }
                }

                if (filteredPdfs.isNotEmpty()) {
                    item { Text("PDFs (${filteredPdfs.size})", fontWeight = FontWeight.Bold, color = BrandBluePrimary) }
                    items(filteredPdfs) { pdf ->
                        ListItem(
                            headlineContent = { Text(pdf.title, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("${pdf.category} • ${pdf.fileSize}", fontSize = 11.sp) },
                            leadingContent = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = BrandBluePrimary) },
                            modifier = Modifier.clickable { onOpenPdf(pdf) }
                        )
                    }
                }
            }
        }
    }
}
