import re

with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'r') as f:
    content = f.read()

new_code = """
    // Global scope to ensure downloads complete even if UI is detached
    private val downloadScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private val activeDownloads = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Deferred<Result<File>>>()

    /**
     * Downloads a PDF file from a remote URL to permanent local storage for lifetime offline viewing.
     * Reports download progress from 0 to 100.
     */
    suspend fun downloadPdf(
        context: Context,
        url: String,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val rawNormalized = normalizePdfUrl(url)
        try {
            if (rawNormalized.isBlank() || !rawNormalized.startsWith("http")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid URL: $url"))
            }

            val targetFile = getLocalPdfFile(context, url)
            if (isValidPdfFile(targetFile)) {
                withContext(Dispatchers.Main) { onProgress(100) }
                return@withContext Result.success(targetFile)
            }

            val deferred = activeDownloads.getOrPut(rawNormalized) {
                downloadScope.async {
                    val result = performActualDownload(context, rawNormalized, targetFile)
                    activeDownloads.remove(rawNormalized)
                    result
                }
            }

            // Await the independent job. It won't get cancelled if this scope cancels!
            val result = deferred.await()
            if (result.isSuccess) {
                withContext(Dispatchers.Main) { onProgress(100) }
            }
            return@withContext result

        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading PDF from $rawNormalized: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun performActualDownload(context: Context, rawNormalized: String, targetFile: File): Result<File> {
        return withContext(Dispatchers.IO) {
            val candidateUrls = mutableListOf<String>()
            candidateUrls.add(rawNormalized)

            if (rawNormalized.contains("supabase.co/storage/v1/object/")) {
                if (rawNormalized.contains("/object/public/pdfs/")) {
                    candidateUrls.add(rawNormalized.replace("/object/public/pdfs/", "/object/public/study-materials/"))
                    candidateUrls.add(rawNormalized.replace("/object/public/pdfs/", "/object/public/pdf-documents/"))
                    candidateUrls.add(rawNormalized.replace("/object/public/pdfs/", "/object/authenticated/pdfs/"))
                } else if (rawNormalized.contains("/object/public/study-materials/")) {
                    candidateUrls.add(rawNormalized.replace("/object/public/study-materials/", "/object/public/pdfs/"))
                    candidateUrls.add(rawNormalized.replace("/object/public/study-materials/", "/object/public/pdf-documents/"))
                } else if (rawNormalized.contains("/object/public/pdf-documents/")) {
                    candidateUrls.add(rawNormalized.replace("/object/public/pdf-documents/", "/object/public/pdfs/"))
                    candidateUrls.add(rawNormalized.replace("/object/public/pdf-documents/", "/object/public/study-materials/"))
                }
            }

            val gDriveId = extractGoogleDriveFileId(rawNormalized)
            if (gDriveId != null) {
                candidateUrls.add("https://drive.google.com/uc?export=download&id=$gDriveId&confirm=t")
                candidateUrls.add("https://docs.google.com/uc?export=download&id=$gDriveId&confirm=t")
            }

            var lastError: Exception? = null

            for (tryUrl in candidateUrls) {
                try {
                    val reqBuilder = Request.Builder()
                        .url(tryUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile)")
                        .addHeader("Accept", "application/pdf,application/octet-stream,*/*")

                    if (tryUrl.contains("supabase.co")) {
                        reqBuilder.addHeader("apikey", SupabaseClient.SUPABASE_PUBLISHABLE_KEY)
                        reqBuilder.addHeader("Authorization", "Bearer ${SupabaseClient.SUPABASE_PUBLISHABLE_KEY}")
                    }

                    val request = reqBuilder.build()
                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful && response.body != null) {
                        val body = response.body!!
                        val contentType = body.contentType()?.toString()?.lowercase() ?: ""
                        
                        if (contentType.contains("text/html") && gDriveId != null) {
                            val html = body.string()
                            val confirmMatch = Regex("confirm=([0-9a-zA-Z_-]+)").find(html)
                            if (confirmMatch != null) {
                                val token = confirmMatch.groupValues[1]
                                val confirmedUrl = "https://drive.usercontent.google.com/download?id=$gDriveId&export=download&confirm=$token"
                                val retryReq = Request.Builder().url(confirmedUrl).build()
                                val retryResp = httpClient.newCall(retryReq).execute()
                                if (retryResp.isSuccessful && retryResp.body != null) {
                                    val savedFile = saveResponseBodyToFile(retryResp.body!!, targetFile) { }
                                    if (savedFile != null && isValidPdfFile(savedFile)) {
                                        return@withContext Result.success(savedFile)
                                    }
                                }
                            }
                        } else {
                            val savedFile = saveResponseBodyToFile(body, targetFile) { }
                            if (savedFile != null && isValidPdfFile(savedFile)) {
                                return@withContext Result.success(savedFile)
                            }
                        }
                    } else {
                        lastError = Exception("HTTP ${response.code}: ${response.message}")
                    }
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Attempt failed for $tryUrl: ${e.message}")
                }
            }
            Result.failure(lastError ?: Exception("Could not download valid PDF from server"))
        }
    }
"""

start_str = "    /**\n     * Downloads a PDF file from a remote URL"
end_str = "Result.failure(e)\n        }\n    }"

start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx) + len(end_str)

if start_idx != -1 and end_idx != -1:
    new_file = content[:start_idx] + new_code.strip() + "\n\n" + content[end_idx:]
    with open('app/src/main/java/com/example/data/util/PdfDownloadManager.kt', 'w') as f:
        f.write(new_file)
    print("Patched successfully")
else:
    print("Could not find boundaries")
