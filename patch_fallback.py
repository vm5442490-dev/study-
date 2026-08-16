import re

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'r') as f:
    content = f.read()

old_fallback_part = """            } else if (resolvedUrl.startsWith("file://") || resolvedUrl.startsWith("/data/") || resolvedUrl.startsWith("/storage/")) {
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
            }"""

new_fallback_part = """            } else if (resolvedUrl.startsWith("file://") || resolvedUrl.startsWith("/data/") || resolvedUrl.startsWith("/storage/")) {
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
                        context = context, docId = pdf.id, title = pdf.title, category = pdf.category,
                        subjectName = pdf.subjectName.ifBlank { "सामान्य अध्ययन" }, classId = pdf.classId.ifBlank { "Class 12" }, description = pdf.description
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
                    context = context, docId = pdf.id, title = pdf.title, category = pdf.category,
                    subjectName = pdf.subjectName.ifBlank { "सामान्य अध्ययन" }, classId = pdf.classId.ifBlank { "Class 12" }, description = pdf.description
                )
                localPdfFile = fallbackFile
                isDownloaded = true
                val count = PdfDownloadManager.getPdfPageCount(fallbackFile)
                totalPages = count.coerceAtLeast(1)
                renderCurrentPage(fallbackFile, currentPage)
            }"""

content = content.replace(old_fallback_part, new_fallback_part)

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'w') as f:
    f.write(content)
