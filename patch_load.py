import re

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'r') as f:
    content = f.read()

# I will replace the loadPdfFile function entirely.
old_load = """    // Load or initialize PDF
    fun loadPdfFile(forceDownload: Boolean = false) {
        scope.launch {
            errorMessage = null
            val resolvedUrl = PdfDownloadManager.resolveFinalPdfUrl(
                fileUrl = pdf.fileUrl,
                storagePath = pdf.storagePath,
                filePath = pdf.filePath,
                pdfUrl = pdf.pdfUrl,
                bucket = pdf.bucket
            )
            resolvedUrlState = resolvedUrl
            android.util.Log.i("PdfViewer", "Opening PDF: id=${pdf.id}, title=${pdf.title}, resolvedUrl=$resolvedUrl, storagePath=${pdf.storagePath}, bucket=${pdf.bucket}")
            
            if (resolvedUrl.isNotBlank() && resolvedUrl.startsWith("http")) {
                val exists = PdfDownloadManager.isPdfDownloaded(context, resolvedUrl)
                if (exists && !forceDownload) {
                    val file = PdfDownloadManager.getLocalPdfFile(context, resolvedUrl)
                    localPdfFile = file
                    isDownloaded = true
                    val count = PdfDownloadManager.getPdfPageCount(file)
                    totalPages = count.coerceAtLeast(1)
                    renderCurrentPage(file, currentPage)
                } else {
                    isDownloading = true
                    val result = PdfDownloadManager.downloadPdf(context, resolvedUrl) { progress ->
                        downloadProgress = progress
                    }
                    isDownloading = false
                    if (result.isSuccess && result.getOrNull() != null) {
                        val file = result.getOrNull()!!
                        localPdfFile = file
                        isDownloaded = true
                        val count = PdfDownloadManager.getPdfPageCount(file)
                        totalPages = count.coerceAtLeast(1)
                        renderCurrentPage(file, currentPage)
                        Toast.makeText(context, "✓ लाइफटाइम ऑफलाइन सेव हो गया!", Toast.LENGTH_SHORT).show()
                    } else {
                        val exception = result.exceptionOrNull()
                        val msg = exception?.message?.lowercase() ?: ""
                        errorMessage = when {
                            exception is java.net.UnknownHostException -> "Network error: Unable to connect. Please check your internet connection."
                            msg.contains("404") -> "Document not found. It may have been removed or the link is broken."
                            msg.contains("403") || msg.contains("401") -> "Access denied. This document is private and requires authorization."
                            else -> "Failed to load document: ${exception?.localizedMessage ?: "Unknown error"}"
                        }
                    }
                }
            } else if (resolvedUrl.startsWith("file://") || resolvedUrl.startsWith("/data/") || resolvedUrl.startsWith("/storage/")) {
                val filePath = resolvedUrl.removePrefix("file://")
                val file = File(filePath)
                if (PdfDownloadManager.isValidPdfFile(file)) {
                    localPdfFile = file
                    isDownloaded = true
                    val count = PdfDownloadManager.getPdfPageCount(file)
                    totalPages = count.coerceAtLeast(1)
                    renderCurrentPage(file, currentPage)
                } else {
                    val fallbackFile = PdfDownloadManager.generateFallbackPdf(
                        context = context,
                        docId = pdf.id,
                        title = pdf.title,
                        category = pdf.category,
                        subjectName = pdf.subjectName.ifBlank { "सामान्य अध्ययन" },
                        classId = pdf.classId.ifBlank { "Class 12" },
                        description = pdf.description
                    )
                    localPdfFile = fallbackFile
                    isDownloaded = true
                    val count = PdfDownloadManager.getPdfPageCount(fallbackFile)
                    totalPages = count.coerceAtLeast(1)
                    renderCurrentPage(fallbackFile, currentPage)
                }
            } else {
                // Offline fallback PDF
                val fallbackFile = PdfDownloadManager.generateFallbackPdf(
                    context = context,
                    docId = pdf.id,
                    title = pdf.title,
                    category = pdf.category,
                    subjectName = pdf.subjectName.ifBlank { "सामान्य अध्ययन" },
                    classId = pdf.classId.ifBlank { "Class 12" },
                    description = pdf.description
                )
                localPdfFile = fallbackFile
                isDownloaded = true
                val count = PdfDownloadManager.getPdfPageCount(fallbackFile)
                totalPages = count.coerceAtLeast(1)
                renderCurrentPage(fallbackFile, currentPage)
            }
        }
    }"""

new_load = """    // Initial state check
    fun checkInitialState() {
        scope.launch {
            errorMessage = null
            val resolvedUrl = PdfDownloadManager.resolveFinalPdfUrl(
                fileUrl = pdf.fileUrl,
                storagePath = pdf.storagePath,
                filePath = pdf.filePath,
                pdfUrl = pdf.pdfUrl,
                bucket = pdf.bucket
            )
            resolvedUrlState = resolvedUrl
            android.util.Log.i("PdfViewer", "Checking PDF: id=${pdf.id}, resolvedUrl=$resolvedUrl")
            
            if (resolvedUrl.isNotBlank() && resolvedUrl.startsWith("http")) {
                val exists = PdfDownloadManager.isPdfDownloaded(context, resolvedUrl)
                if (exists) {
                    val file = PdfDownloadManager.getLocalPdfFile(context, resolvedUrl)
                    localPdfFile = file
                    isDownloaded = true
                    val count = PdfDownloadManager.getPdfPageCount(file)
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
                if (PdfDownloadManager.isValidPdfFile(file)) {
                    localPdfFile = file
                    isDownloaded = true
                    val count = PdfDownloadManager.getPdfPageCount(file)
                    totalPages = count.coerceAtLeast(1)
                    renderCurrentPage(file, currentPage)
                } else {
                    isDownloaded = false
                    isDownloading = false
                }
            } else {
                isDownloaded = false
                isDownloading = false
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
            
            val result = PdfDownloadManager.downloadPdf(context, url) { progress ->
                downloadProgress = progress
            }
            
            isDownloading = false
            
            if (result.isSuccess && result.getOrNull() != null) {
                val file = result.getOrNull()!!
                localPdfFile = file
                isDownloaded = true
                val count = PdfDownloadManager.getPdfPageCount(file)
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
    }"""

content = content.replace(old_load, new_load)
content = content.replace("LaunchedEffect(pdf.id, pdf.fileUrl, pdf.storagePath) {\n        loadPdfFile()\n    }", "LaunchedEffect(pdf.id, pdf.fileUrl, pdf.storagePath) {\n        checkInitialState()\n    }")

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'w') as f:
    f.write(content)

