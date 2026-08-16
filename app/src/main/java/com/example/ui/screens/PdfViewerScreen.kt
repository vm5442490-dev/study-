package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PdfDocument
import com.example.data.util.PdfDownloadManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError

private fun findActivity(context: Context): Activity? {
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

enum class PdfReadingTheme {
    DEFAULT, SEPIA, DARK
}

enum class PdfViewMode {
    SINGLE_PAGE, CONTINUOUS_SCROLL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdf: PdfDocument,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    val pdfDownloadManager: PdfDownloadManager = if (activity != null) {
        androidx.lifecycle.viewmodel.compose.viewModel(activity)
    } else {
        androidx.lifecycle.viewmodel.compose.viewModel()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current



    // Dynamic resolution based on screen density for ultra-sharp typography
    val targetRenderWidth = remember(configuration.screenWidthDp) {
        (configuration.screenWidthDp * 3.5f).toInt().coerceIn(1200, 2200)
    }

    // Unity Ad State
    var isAdWatched by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(true) }
    var isAdShowing by remember { mutableStateOf(false) }
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }

    LaunchedEffect(Unit) {
        if (!isAdWatched) {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(context, "ca-app-pub-3665825190622425/7326311859", adRequest, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoading = false
                    rewardedAd = null
                    // Allow access on error so they aren't stuck
                    isAdWatched = true
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    isAdLoading = false
                    rewardedAd = ad
                }
            })
        }
    }

    var isDownloaded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var localPdfFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(pdf.pagesCount.coerceAtLeast(1)) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPage by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var resolvedUrlState by remember { mutableStateOf<String?>(null) }

    DisposableEffect(localPdfFile) {
        onDispose {
            localPdfFile?.let { pdfDownloadManager.closeRenderer(it) }
        }
    }


    // Reading preferences
    var readingTheme by remember { mutableStateOf(PdfReadingTheme.DEFAULT) }
    var viewMode by remember { mutableStateOf(PdfViewMode.SINGLE_PAGE) }
    var isFullscreen by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableStateOf(0f) }

    // Single-page pinch-to-zoom & pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4.5f)
        if (scale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }

    // Function to render single page
    fun renderCurrentPage(file: File, page: Int) {
        scope.launch {
            isRenderingPage = true
            scale = 1f
            offset = Offset.Zero
            val result = pdfDownloadManager.renderPdfPage(file, page - 1, targetWidth = targetRenderWidth)
            if (result.isSuccess) {
                renderedBitmap = result.getOrNull()
            } else {
                android.util.Log.e("PdfViewer", "Failed to render PDF page", result.exceptionOrNull())
                isDownloaded = false
                localPdfFile = null
                errorMessage = "दस्तावेज़ को खोलने में त्रुटि। फ़ाइल शायद corrupted है। (Error: ${result.exceptionOrNull()?.localizedMessage})"
                file.delete()
            }
            isRenderingPage = false
            // Preload next and previous pages for instant response
            pdfDownloadManager.preloadAdjacentPages(file, page - 1, totalPages, targetWidth = targetRenderWidth)
        }
    }

    // Initial state check
    fun checkInitialState() {
        scope.launch {
            errorMessage = null
            val resolvedUrl = pdfDownloadManager.resolveFinalPdfUrl(
                fileUrl = pdf.fileUrl,
                storagePath = pdf.storagePath,
                filePath = pdf.filePath,
                pdfUrl = pdf.pdfUrl,
                bucket = pdf.bucket
            )
            resolvedUrlState = resolvedUrl
            android.util.Log.i("PdfViewer", "Checking PDF: id=${pdf.id}, resolvedUrl=$resolvedUrl")
            
            if (resolvedUrl.isNotBlank() && resolvedUrl.startsWith("http")) {
                val exists = pdfDownloadManager.isPdfDownloaded(context, resolvedUrl)
                if (exists) {
                    val file = pdfDownloadManager.getLocalPdfFile(context, resolvedUrl)
                    localPdfFile = file
                    isDownloaded = true
                    val count = pdfDownloadManager.getPdfPageCount(file)
                    totalPages = count.coerceAtLeast(1)
                    renderCurrentPage(file, currentPage)
                } else {
                    // Show download screen
                    isDownloaded = false
                    isDownloading = false
                }
            } else if (resolvedUrl.startsWith("file://") || resolvedUrl.startsWith("/data/") || resolvedUrl.startsWith("/storage/")) {
                val filePath = resolvedUrl.removePrefix("file://")
                val file = File(filePath)
                if (pdfDownloadManager.isValidPdfFile(file)) {
                    localPdfFile = file
                    isDownloaded = true
                    val count = pdfDownloadManager.getPdfPageCount(file)
                    totalPages = count.coerceAtLeast(1)
                    renderCurrentPage(file, currentPage)
                } else {
                    val fallbackFile = pdfDownloadManager.generateFallbackPdf(
                        context = context, docId = pdf.id, title = pdf.title, category = pdf.category,
                        subjectName = pdf.subjectName.ifBlank { "सामान्य अध्ययन" }, classId = pdf.classId.ifBlank { "Class 12" }, description = pdf.description
                    )
                    localPdfFile = fallbackFile
                    isDownloaded = true
                    val count = pdfDownloadManager.getPdfPageCount(fallbackFile)
                    totalPages = count.coerceAtLeast(1)
                    renderCurrentPage(fallbackFile, currentPage)
                }
            } else {
                // Offline fallback PDF
                val fallbackFile = pdfDownloadManager.generateFallbackPdf(
                    context = context, docId = pdf.id, title = pdf.title, category = pdf.category,
                    subjectName = pdf.subjectName.ifBlank { "सामान्य अध्ययन" }, classId = pdf.classId.ifBlank { "Class 12" }, description = pdf.description
                )
                localPdfFile = fallbackFile
                isDownloaded = true
                val count = pdfDownloadManager.getPdfPageCount(fallbackFile)
                totalPages = count.coerceAtLeast(1)
                renderCurrentPage(fallbackFile, currentPage)
            }
        }
    }

    // Manual start download
    fun startDownload() {
        if (isDownloading) return // Prevent concurrent
        val url = resolvedUrlState ?: return
        
        scope.launch {
            isDownloading = true
            errorMessage = null
            downloadProgress = 0
            
            val result = pdfDownloadManager.downloadPdf(context, url) { progress ->
                downloadProgress = progress
            }
            
            isDownloading = false
            
            if (result.isSuccess && result.getOrNull() != null) {
                val file = result.getOrNull()!!
                localPdfFile = file
                isDownloaded = true
                val count = pdfDownloadManager.getPdfPageCount(file)
                totalPages = count.coerceAtLeast(1)
                renderCurrentPage(file, currentPage)
                Toast.makeText(context, "✓ लाइफटाइम ऑफलाइन सेव हो गया!", Toast.LENGTH_SHORT).show()
            } else {
                val exception = result.exceptionOrNull()
                val msg = exception?.message?.lowercase() ?: ""
                errorMessage = when {
                    exception is java.net.UnknownHostException -> "यह PDF अभी device में downloaded नहीं है। PDF download करने के लिए Internet connection आवश्यक है।"
                    msg.contains("404") -> "Document not found. It may have been removed or the link is broken."
                    msg.contains("403") || msg.contains("401") -> "Access denied. This document is private and requires authorization."
                    else -> "PDF download पूरा नहीं हुआ: ${exception?.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    LaunchedEffect(pdf.id, pdf.fileUrl, pdf.storagePath) {
        checkInitialState()
    }

    // Color styling for reading themes
    val readerBackground = when (readingTheme) {
        PdfReadingTheme.DEFAULT -> Color(0xFFE2E8F0)
        PdfReadingTheme.SEPIA -> Color(0xFFFBF0D9)
        PdfReadingTheme.DARK -> Color(0xFF111827)
    }

    val pageSurfaceColor = when (readingTheme) {
        PdfReadingTheme.DEFAULT -> Color.White
        PdfReadingTheme.SEPIA -> Color(0xFFFFFBF0)
        PdfReadingTheme.DARK -> Color(0xFF1E293B)
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !isFullscreen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pdf.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${pdf.subjectName.ifBlank { pdf.category }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isDownloaded) {
                                    Text(
                                        text = "• 🟢 ऑफलाइन उपलब्ध",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandEmerald
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Toggle Continuous Scroll vs Single Page View
                        IconButton(
                            onClick = {
                                viewMode = if (viewMode == PdfViewMode.SINGLE_PAGE) {
                                    PdfViewMode.CONTINUOUS_SCROLL
                                } else {
                                    PdfViewMode.SINGLE_PAGE
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (viewMode == PdfViewMode.SINGLE_PAGE) Icons.Outlined.ViewStream else Icons.Outlined.AutoStories,
                                contentDescription = "Switch View Mode",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Theme / Night Mode Selector
                        IconButton(
                            onClick = {
                                readingTheme = when (readingTheme) {
                                    PdfReadingTheme.DEFAULT -> PdfReadingTheme.SEPIA
                                    PdfReadingTheme.SEPIA -> PdfReadingTheme.DARK
                                    PdfReadingTheme.DARK -> PdfReadingTheme.DEFAULT
                                }
                            }
                        ) {
                            Icon(
                                imageVector = when (readingTheme) {
                                    PdfReadingTheme.DEFAULT -> Icons.Outlined.LightMode
                                    PdfReadingTheme.SEPIA -> Icons.Outlined.WbSunny
                                    PdfReadingTheme.DARK -> Icons.Outlined.DarkMode
                                },
                                contentDescription = "Theme",
                                tint = when (readingTheme) {
                                    PdfReadingTheme.DEFAULT -> MaterialTheme.colorScheme.onSurface
                                    PdfReadingTheme.SEPIA -> BrandAmber
                                    PdfReadingTheme.DARK -> BrandPurple
                                }
                            )
                        }

                        // Rotate 90 deg
                        IconButton(
                            onClick = {
                                rotationAngle = (rotationAngle + 90f) % 360f
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RotateRight,
                                contentDescription = "Rotate",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // External Browser / Viewer Action
                        val externalUrl = resolvedUrlState ?: pdf.fileUrl
                        if (externalUrl.isNotBlank() && externalUrl.startsWith("http")) {
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl))
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No browser/viewer app found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open in External App",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isFullscreen && isDownloaded && totalPages > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // Offline Lifetime Download Action Bar if not yet permanently saved from remote
                        val isRemoteUrl = resolvedUrlState?.startsWith("http") ?: (pdf.fileUrl.isNotBlank() && pdf.fileUrl.startsWith("http"))
                        if (isRemoteUrl && !isDownloading) {
                            Surface(
                                color = BrandEmerald.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = BrandEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "लाइफटाइम ऑफलाइन सेव्ड (Offline Ready)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandEmerald
                                        )
                                    }

                                    TextButton(
                                        onClick = { startDownload() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("री-डाउनलोड", fontSize = 11.sp, color = BrandBluePrimary)
                                    }
                                }
                            }
                        }

                        // Navigation buttons for Single Page View
                        if (viewMode == PdfViewMode.SINGLE_PAGE) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (currentPage > 1) {
                                            currentPage--
                                            localPdfFile?.let { renderCurrentPage(it, currentPage) }
                                        }
                                    },
                                    enabled = currentPage > 1,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("पिछला")
                                }

                                // Clickable Page Indicator to Jump to Page
                                Surface(
                                    onClick = { showJumpDialog = true },
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Page $currentPage / $totalPages",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.UnfoldMore,
                                            contentDescription = "Jump",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (currentPage < totalPages) {
                                            currentPage++
                                            localPdfFile?.let { renderCurrentPage(it, currentPage) }
                                        }
                                    },
                                    enabled = currentPage < totalPages,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                                ) {
                                    Text("अगला")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("pdf_viewer_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(readerBackground),
            contentAlignment = Alignment.Center
        ) {
            when {
                !isAdWatched -> {
                    // Show a mandatory Ad watch screen
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Watch Ad",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "PDF अनलॉक करें",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "इस डॉक्यूमेंट को पढ़ने के लिए एक छोटा सा विज्ञापन देखें।",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            if (isAdLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("विज्ञापन लोड हो रहा है...", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Button(
                                    onClick = {
                                        if (activity != null && rewardedAd != null) {
                                            isAdShowing = true
                                            
                                            rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                                                override fun onAdShowedFullScreenContent() {}
                                                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                                                    isAdShowing = false
                                                    isAdWatched = true
                                                    rewardedAd = null
                                                }
                                                override fun onAdDismissedFullScreenContent() {
                                                    isAdShowing = false
                                                    rewardedAd = null
                                                }
                                            }
                                            
                                            rewardedAd?.show(activity) { rewardItem ->
                                                isAdWatched = true
                                            }
                                        } else {
                                            isAdWatched = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isAdShowing) {
                                        Text("विज्ञापन चल रहा है...")
                                    } else {
                                        Text("विज्ञापन देखें")
                                    }
                                }
                            }
                        }
                    }
                }
                isDownloading -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.size(72.dp),
                                    strokeWidth = 6.dp,
                                    color = BrandBluePrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = "$downloadProgress%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "PDF सुरक्षित डाउनलोड हो रही है...",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "एक बार डाउनलोड होने के बाद यह आजीवन ऑफलाइन उपलब्ध रहेगी।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                errorMessage != null && !isDownloaded -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PDF लोड करने में समस्या हुई",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "कृपया इंटरनेट कनेक्शन की जांच करें।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { startDownload() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("दोबारा प्रयास करें (Retry)")
                            }
                        }
                    }
                }
                !isDownloaded -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = BrandBluePrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PDF तैयार करें",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "यह PDF आपके device में उपलब्ध नहीं है।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (pdf.fileSize.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "File Size: ${pdf.fileSize}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandBluePrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { startDownload() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download PDF")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onNavigateBack,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }

                isDownloaded && localPdfFile != null -> {
                    if (viewMode == PdfViewMode.SINGLE_PAGE) {
                        // SINGLE PAGE INTERACTIVE VIEW WITH PINCH-ZOOM & PAN
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .transformable(state = transformState)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            scale = if (scale > 1f) 1f else 2.5f
                                            offset = Offset.Zero
                                        },
                                        onTap = {
                                            isFullscreen = !isFullscreen
                                        }
                                    )
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (renderedBitmap != null) {
                                Card(
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(containerColor = pageSurfaceColor),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y,
                                            rotationZ = rotationAngle
                                        )
                                ) {
                                    Image(
                                        bitmap = renderedBitmap!!.asImageBitmap(),
                                        contentDescription = "Page $currentPage of ${pdf.title}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                            }

                            // Loading indicator while rendering page
                            if (isRenderingPage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = BrandBluePrimary)
                                }
                            }

                            // Floating Controls: Reset Zoom & Fullscreen Exit
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (scale > 1.05f) {
                                    FloatingActionButton(
                                        onClick = {
                                            scale = 1f
                                            offset = Offset.Zero
                                        },
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomOutMap,
                                            contentDescription = "Reset Zoom",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (isFullscreen) {
                                    FloatingActionButton(
                                        onClick = { isFullscreen = false },
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FullscreenExit,
                                            contentDescription = "Exit Fullscreen",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // CONTINUOUS VERTICAL SCROLL VIEW OF ALL PAGES
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { isFullscreen = !isFullscreen }
                                    )
                                },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(totalPages) { pageIdx ->
                                val pageNumber = pageIdx + 1
                                ContinuousPdfPageItem(
                                    file = localPdfFile!!,
                                    pageIndex = pageIdx,
                                    targetWidth = targetRenderWidth,
                                    surfaceColor = pageSurfaceColor,
                                    rotation = rotationAngle,
                                    pageNumber = pageNumber,
                                    totalPages = totalPages,
                                    pdfDownloadManager = pdfDownloadManager
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Fallback study card reader layout
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = pdf.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pdf.description.ifBlank { "यह आधिकारिक अध्ययन सामग्री सीधे सुपर स्टडी पोर्टल द्वारा प्रमाणित है।" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Jump to Page Dialog
    if (showJumpDialog) {
        var pageInputText by remember { mutableStateOf(currentPage.toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("पेज पर जाएं (Jump to Page)") },
            text = {
                Column {
                    Text("1 से $totalPages के बीच पेज नंबर दर्ज करें:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pageInputText,
                        onValueChange = { pageInputText = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val target = pageInputText.toIntOrNull()
                                if (target != null && target in 1..totalPages) {
                                    currentPage = target
                                    localPdfFile?.let { renderCurrentPage(it, currentPage) }
                                    showJumpDialog = false
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pageInputText.toIntOrNull()
                        if (target != null && target in 1..totalPages) {
                            currentPage = target
                            localPdfFile?.let { renderCurrentPage(it, currentPage) }
                            showJumpDialog = false
                        } else {
                            Toast.makeText(context, "1 से $totalPages के बीच पेज चुनें", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Text("खोलें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

/**
 * High-performance reusable Composable for rendering individual pages in Continuous Scroll mode.
 */
@Composable
private fun ContinuousPdfPageItem(
    file: File,
    pageIndex: Int,
    targetWidth: Int,
    surfaceColor: Color,
    rotation: Float,
    pageNumber: Int,
    totalPages: Int,
    pdfDownloadManager: com.example.data.util.PdfDownloadManager
) {
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(true) }

    LaunchedEffect(file, pageIndex, targetWidth) {
        isRendering = true
        val res = pdfDownloadManager.renderPdfPage(file, pageIndex, targetWidth)
        if (res.isSuccess) {
            pageBitmap = res.getOrNull()
        }
        isRendering = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(rotationZ = rotation),
                contentAlignment = Alignment.Center
            ) {
                if (pageBitmap != null) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "Page $pageNumber",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandBluePrimary, modifier = Modifier.size(36.dp))
                    }
                }
            }

            // Page label bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "$pageNumber / $totalPages",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
    }
}
