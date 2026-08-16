import re

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'r') as f:
    content = f.read()

cleanup_effect = """
    DisposableEffect(localPdfFile) {
        onDispose {
            localPdfFile?.let { pdfDownloadManager.closeRenderer(it) }
        }
    }
"""

content = content.replace("    var resolvedUrlState by remember { mutableStateOf<String?>(null) }", "    var resolvedUrlState by remember { mutableStateOf<String?>(null) }\n" + cleanup_effect)

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'w') as f:
    f.write(content)
