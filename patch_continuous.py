import re

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""private fun ContinuousPdfPageItem(
    file: File,
    pageIndex: Int,
    targetWidth: Int,
    surfaceColor: Color,
    rotation: Float,
    pageNumber: Int,
    totalPages: Int
) {""",
"""private fun ContinuousPdfPageItem(
    file: File,
    pageIndex: Int,
    targetWidth: Int,
    surfaceColor: Color,
    rotation: Float,
    pageNumber: Int,
    totalPages: Int,
    pdfDownloadManager: com.example.data.util.PdfDownloadManager
) {""")

content = content.replace("""                            ContinuousPdfPageItem(
                                file = it,
                                pageIndex = page,
                                targetWidth = targetRenderWidth,
                                surfaceColor = MaterialTheme.colorScheme.surface,
                                rotation = rotationAngle,
                                pageNumber = page + 1,
                                totalPages = totalPages
                            )""",
"""                            ContinuousPdfPageItem(
                                file = it,
                                pageIndex = page,
                                targetWidth = targetRenderWidth,
                                surfaceColor = MaterialTheme.colorScheme.surface,
                                rotation = rotationAngle,
                                pageNumber = page + 1,
                                totalPages = totalPages,
                                pdfDownloadManager = pdfDownloadManager
                            )""")

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'w') as f:
    f.write(content)
