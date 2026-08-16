import re

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("val pdfDownloadManager: PdfDownloadManager = viewModel()", 
"""val activity = LocalContext.current as? androidx.activity.ComponentActivity
    val pdfDownloadManager: PdfDownloadManager = if (activity != null) {
        androidx.lifecycle.viewmodel.compose.viewModel(activity)
    } else {
        androidx.lifecycle.viewmodel.compose.viewModel()
    }""")

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'w') as f:
    f.write(content)
