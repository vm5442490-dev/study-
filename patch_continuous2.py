import re

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("""                                ContinuousPdfPageItem(
                                    file = localPdfFile!!,
                                    pageIndex = pageIdx,
                                    targetWidth = targetRenderWidth,
                                    surfaceColor = pageSurfaceColor,
                                    rotation = rotationAngle,
                                    pageNumber = pageNumber,
                                    totalPages = totalPages
                                )""",
"""                                ContinuousPdfPageItem(
                                    file = localPdfFile!!,
                                    pageIndex = pageIdx,
                                    targetWidth = targetRenderWidth,
                                    surfaceColor = pageSurfaceColor,
                                    rotation = rotationAngle,
                                    pageNumber = pageNumber,
                                    totalPages = totalPages,
                                    pdfDownloadManager = pdfDownloadManager
                                )""")

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'w') as f:
    f.write(content)
