import re

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'r') as f:
    content = f.read()

if "import androidx.lifecycle.viewmodel.compose.viewModel" not in content:
    content = content.replace("import androidx.compose.ui.unit.sp", "import androidx.compose.ui.unit.sp\nimport androidx.lifecycle.viewmodel.compose.viewModel")

content = content.replace("fun PdfViewerScreen(\n    pdf: PdfDocument,\n    onNavigateBack: () -> Unit,\n    modifier: Modifier = Modifier\n) {", 
"""fun PdfViewerScreen(
    pdf: PdfDocument,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pdfDownloadManager: PdfDownloadManager = viewModel()""")

content = content.replace("PdfDownloadManager.", "pdfDownloadManager.")

with open('app/src/main/java/com/example/ui/screens/PdfViewerScreen.kt', 'w') as f:
    f.write(content)
