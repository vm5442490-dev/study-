import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

# Update getPdfPageCount
old_page_count = """            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fileDescriptor ->
                PdfRenderer(fileDescriptor).use { renderer ->
                    renderer.pageCount.coerceAtLeast(1)
                }
            }"""

new_page_count = """            val path = file.absolutePath
            var rendererPair = openRenderers[path]
            if (rendererPair == null) {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val newRenderer = PdfRenderer(fd)
                rendererPair = Pair(fd, newRenderer)
                openRenderers[path] = rendererPair
            }
            rendererPair.second.pageCount.coerceAtLeast(1)"""

content = content.replace(old_page_count, new_page_count)

# Update isPdfDownloaded
old_deep_verify = """            android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                android.graphics.pdf.PdfRenderer(fd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        return@withContext true
                    }
                }
            }"""

new_deep_verify = """            val path = file.absolutePath
            var rendererPair = openRenderers[path]
            if (rendererPair == null) {
                val fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                val newRenderer = android.graphics.pdf.PdfRenderer(fd)
                rendererPair = Pair(fd, newRenderer)
                openRenderers[path] = rendererPair
            }
            if (rendererPair.second.pageCount > 0) {
                return@withContext true
            }"""

content = content.replace(old_deep_verify, new_deep_verify)

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)
