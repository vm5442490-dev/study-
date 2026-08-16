import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

replacement = """    suspend fun isPdfDownloaded(context: Context, urlOrId: String): Boolean = withContext(Dispatchers.IO) {
        val file = getLocalPdfFile(context, urlOrId)
        if (!isValidPdfFile(file)) return@withContext false
        
        // Deep verification: check if PdfRenderer can actually open it
        // This catches partially downloaded files that start with %PDF- but are incomplete
        try {
            android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                android.graphics.pdf.PdfRenderer(fd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Corrupted PDF detected (PdfRenderer failed). Deleting: ${file.name}", e)
            file.delete()
        }
        return@withContext false
    }"""

content = re.sub(r'    fun isPdfDownloaded\(context: Context, urlOrId: String\): Boolean \{\s*val file = getLocalPdfFile\(context, urlOrId\)\s*return isValidPdfFile\(file\)\s*\}', replacement, content)

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
    f.write(content)

