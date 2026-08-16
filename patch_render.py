import re

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'r') as f:
    content = f.read()

replacement = """            val result = PdfDownloadManager.renderPdfPage(file, page - 1, targetWidth = targetRenderWidth)
            if (result.isSuccess) {
                renderedBitmap = result.getOrNull()
            } else {
                android.util.Log.e("PdfViewer", "Failed to render PDF page", result.exceptionOrNull())
                isDownloaded = false
                localPdfFile = null
                errorMessage = "दस्तावेज़ को खोलने में त्रुटि। फ़ाइल शायद corrupted है। (Error: ${result.exceptionOrNull()?.localizedMessage})"
                file.delete()
            }"""

content = content.replace("            val result = PdfDownloadManager.renderPdfPage(file, page - 1, targetWidth = targetRenderWidth)\n            if (result.isSuccess) {\n                renderedBitmap = result.getOrNull()\n            }", replacement)

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'w') as f:
    f.write(content)

