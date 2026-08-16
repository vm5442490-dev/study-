import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

# 1. Patch closeRenderer
old_close = """    fun closeRenderer(file: File?) {
        if (file == null) return
        val path = file.absolutePath
        openRenderers.remove(path)?.let { (fd, renderer) ->
            try { renderer.close() } catch (e: Exception) {}
            try { fd.close() } catch (e: Exception) {}
        }
    }"""

new_close = """    fun closeRenderer(file: File?) {
        if (file == null) return
        val path = file.absolutePath
        val pair = openRenderers.remove(path)
        if (pair != null) {
            viewModelScope.launch(Dispatchers.IO) {
                pdfRenderMutex.withLock {
                    try { pair.second.close() } catch (e: Exception) {}
                    try { pair.first.close() } catch (e: Exception) {}
                }
            }
        }
    }"""

content = content.replace(old_close, new_close)

# 2. Patch isPdfDownloaded
old_is_downloaded = """        try {
            val path = file.absolutePath
            var rendererPair = openRenderers[path]
            if (rendererPair == null) {
                val fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                val newRenderer = android.graphics.pdf.PdfRenderer(fd)
                rendererPair = Pair(fd, newRenderer)
                openRenderers[path] = rendererPair
            }
            if (rendererPair.second.pageCount > 0) {
                return@withContext true
            }
        } catch (e: Exception) {"""

new_is_downloaded = """        try {
            pdfRenderMutex.withLock {
                val path = file.absolutePath
                var rendererPair = openRenderers[path]
                if (rendererPair == null) {
                    val fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val newRenderer = android.graphics.pdf.PdfRenderer(fd)
                    rendererPair = Pair(fd, newRenderer)
                    openRenderers[path] = rendererPair
                }
                if (rendererPair.second.pageCount > 0) {
                    return@withContext true
                }
            }
        } catch (e: Exception) {"""

content = content.replace(old_is_downloaded, new_is_downloaded)

# 3. Patch getPdfPageCount
old_get_page_count = """        try {
            if (!isValidPdfFile(file)) return@withContext 1
            val path = file.absolutePath
            var rendererPair = openRenderers[path]
            if (rendererPair == null) {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val newRenderer = PdfRenderer(fd)
                rendererPair = Pair(fd, newRenderer)
                openRenderers[path] = rendererPair
            }
            rendererPair.second.pageCount.coerceAtLeast(1)
        } catch (e: Exception) {"""

new_get_page_count = """        try {
            if (!isValidPdfFile(file)) return@withContext 1
            pdfRenderMutex.withLock {
                val path = file.absolutePath
                var rendererPair = openRenderers[path]
                if (rendererPair == null) {
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val newRenderer = PdfRenderer(fd)
                    rendererPair = Pair(fd, newRenderer)
                    openRenderers[path] = rendererPair
                }
                rendererPair.second.pageCount.coerceAtLeast(1)
            }
        } catch (e: Exception) {"""

content = content.replace(old_get_page_count, new_get_page_count)

# Make sure kotlinx.coroutines.launch is imported if needed, but it should be available via viewModelScope
if "import kotlinx.coroutines.launch" not in content:
    content = content.replace("import kotlinx.coroutines.Dispatchers", "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.launch")


with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)
