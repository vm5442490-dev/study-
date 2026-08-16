package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun StudyScreen(
    viewModel: MainViewModel,
    onOpenBook: (Book) -> Unit,
    onOpenNote: (StudyNote) -> Unit,
    onOpenPdf: (PdfDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    val studyState by viewModel.studyState.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val tabTitles = listOf("📚 Books", "📝 Notes", "📑 PDFs", "📄 Model Papers", "⏳ PYQ Papers")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("study_screen")
    ) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = studyState.selectedTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BrandBluePrimary,
            modifier = Modifier.fillMaxWidth().testTag("study_tab_row")
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = studyState.selectedTab == index,
                    onClick = { viewModel.setStudyTab(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (studyState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Sub-filters (Category or Subject)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (studyState.selectedTab) {
                0 -> {
                    // Books categories
                    listOf("All Books", "NCERT Books", "JAC Books").forEach { cat ->
                        FilterChip(
                            selected = studyState.selectedCategory == cat,
                            onClick = { viewModel.setStudyCategory(cat) },
                            label = { Text(cat, fontSize = 12.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
                1, 2, 3, 4 -> {
                    FilterChip(
                        selected = studyState.selectedSubjectId == null,
                        onClick = { viewModel.setStudySubject(null) },
                        label = { Text("All Subjects", fontSize = 12.sp) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    studyState.subjects.forEach { sub ->
                        FilterChip(
                            selected = studyState.selectedSubjectId == sub.id,
                            onClick = { viewModel.setStudySubject(sub.id) },
                            label = { Text(sub.name, fontSize = 12.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        // Content Area
        if (studyState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBluePrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (studyState.selectedTab) {
                    0 -> {
                        // Books
                        if (studyState.books.isEmpty()) {
                            item { EmptyStateView(title = "कोई पुस्तक उपलब्ध नहीं है", message = "इस श्रेणी में पुस्तकें जल्द जोड़ी जाएंगी।") }
                        } else {
                            items(studyState.books) { book ->
                                BookItemCard(
                                    book = book,
                                    onOpenClick = onOpenBook
                                )
                            }
                        }
                    }
                    1 -> {
                        // Study Notes
                        if (studyState.notes.isEmpty()) {
                            item { EmptyStateView(title = "कोई नोट्स उपलब्ध नहीं हैं", message = "इस विषय के अध्याय नोट्स जल्द उपलब्ध होंगे।") }
                        } else {
                            items(studyState.notes) { note ->
                                val isBookmarked = bookmarks.any { it.itemId == note.id }
                                StudyNoteCard(
                                    note = note,
                                    isBookmarked = isBookmarked,
                                    onReadClick = onOpenNote,
                                    onBookmarkToggle = {
                                        viewModel.toggleBookmark(
                                            itemId = note.id,
                                            itemType = "NOTE",
                                            title = note.title,
                                            subtitle = note.chapterTitle
                                        )
                                    }
                                )
                            }
                        }
                    }
                    2 -> {
                        // PDFs
                        if (studyState.pdfs.isEmpty()) {
                            item { EmptyStateView(title = "कोई PDF उपलब्ध नहीं है", message = "PDF दस्तावेज़ जल्द ही अपलोड किए जाएंगे।") }
                        } else {
                            items(studyState.pdfs) { pdf ->
                                PdfDocumentCard(
                                    pdf = pdf,
                                    onOpenClick = onOpenPdf
                                )
                            }
                        }
                    }
                    3 -> {
                        // Model Papers
                        if (studyState.modelPapers.isEmpty()) {
                            item { EmptyStateView(title = "कोई मॉडल पेपर उपलब्ध नहीं है", message = "2026 बोर्ड मॉडल सेट जल्द जारी किए जाएंगे।") }
                        } else {
                            items(studyState.modelPapers) { mp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("model_paper_${mp.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = BrandOrange,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = mp.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(text = "${mp.year} Set • ${mp.questionsCount} Questions • ${mp.durationMinutes} mins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                onOpenPdf(
                                                    PdfDocument(
                                                        id = mp.id,
                                                        title = mp.title,
                                                        description = "Model Examination Paper",
                                                        category = "Model Papers",
                                                        classId = mp.classId,
                                                        subjectId = mp.subjectId,
                                                        fileUrl = mp.fileUrl.orEmpty(),
                                                        storagePath = mp.storagePath,
                                                        filePath = mp.filePath,
                                                        pdfUrl = mp.pdfUrl,
                                                        bucket = mp.bucket,
                                                        pagesCount = 10,
                                                        fileSize = "2.2 MB"
                                                    )
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("PDF खोलें", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        // PYQ Papers
                        if (studyState.pyqs.isEmpty()) {
                            item { EmptyStateView(title = "कोई PYQ पेपर उपलब्ध नहीं है", message = "पिछले वर्षों के प्रश्न पत्र जल्द जोड़े जाएंगे।") }
                        } else {
                            items(studyState.pyqs) { pyq ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("pyq_card_${pyq.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HistoryEdu,
                                            contentDescription = null,
                                            tint = BrandPurple,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = pyq.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(text = "Year ${pyq.year} • ${pyq.examType} • ${pyq.questionsCount} Qs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                onOpenPdf(
                                                    PdfDocument(
                                                        id = pyq.id,
                                                        title = pyq.title,
                                                        description = "Previous Year Examination Paper",
                                                        category = "PYQ",
                                                        classId = pyq.classId,
                                                        subjectId = pyq.subjectId,
                                                        fileUrl = pyq.fileUrl.orEmpty(),
                                                        storagePath = pyq.storagePath,
                                                        filePath = pyq.filePath,
                                                        pdfUrl = pyq.pdfUrl,
                                                        bucket = pyq.bucket,
                                                        pagesCount = 14,
                                                        fileSize = "3.1 MB"
                                                    )
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("PYQ खोलें", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
