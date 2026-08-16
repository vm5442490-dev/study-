package com.example.data.util
import kotlinx.coroutines.async
import androidx.lifecycle.viewModelScope

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument as AndroidPdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import com.example.data.model.Book
import com.example.data.model.ModelPaper
import com.example.data.model.PdfDocument
import com.example.data.model.PreviousYearPaper
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class PdfDownloadManager(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application), ComponentCallbacks2 {

    init {
        application.registerComponentCallbacks(this)
    }



    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            pageBitmapCache.trimToSize(cacheSize / 2)
        }
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            pageBitmapCache.evictAll()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}
    override fun onLowMemory() {
        pageBitmapCache.evictAll()
    }

    private val TAG = "PdfDownloadManager"

    // High-performance OkHttpClient with timeouts & redirect handling
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // In-memory Bitmap LRU cache for ultra-fast instant page switching
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 6).coerceAtLeast(32 * 1024) // 1/6th of available runtime memory
    private val pageBitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }


    // Mutex to prevent multiple parallel PdfRenderer native allocations which cause OOM
    private val pdfRenderMutex = kotlinx.coroutines.sync.Mutex()
    
    private val openRenderers = java.util.concurrent.ConcurrentHashMap<String, Pair<ParcelFileDescriptor, PdfRenderer>>()
    
    fun closeRenderer(file: File?) {
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
    }
    
    override fun onCleared() {
        super.onCleared()
        getApplication<android.app.Application>().unregisterComponentCallbacks(this)
        pageBitmapCache.evictAll()
        openRenderers.keys().toList().forEach { path ->
            closeRenderer(File(path))
        }
    }


    /**
     * Extracts Google Drive file ID from various URL formats.
     */
    fun extractGoogleDriveFileId(url: String): String? {
        val trimmed = url.trim()
        val match1 = Regex("/file/d/([a-zA-Z0-9_-]+)").find(trimmed)
        if (match1 != null) return match1.groupValues[1]
        val match2 = Regex("[?&]id=([a-zA-Z0-9_-]+)").find(trimmed)
        if (match2 != null) return match2.groupValues[1]
        val match3 = Regex("/open\\?id=([a-zA-Z0-9_-]+)").find(trimmed)
        if (match3 != null) return match3.groupValues[1]
        return null
    }

    /**
     * Canonical URL resolver for any PDF document in Super Study.
     * Accurately resolves:
     * 1. Direct full HTTP/HTTPS URLs (Supabase Storage public/authenticated, Google Drive, Dropbox, CDN)
     * 2. Relative Supabase Storage paths (e.g. "class12/history/chapter1.pdf")
     * 3. Prefixed paths (e.g. "pdfs/class12/history/chapter1.pdf", "study-materials/class10/math.pdf")
     * 4. Accidental duplicated prefix URLs (e.g. "https://.../storage/v1/object/public/pdfs/https://...")
     * 5. Local file URIs (e.g. "file:///...")
     */
    fun getPdfUrl(
        fileUrl: String? = null,
        storagePath: String? = null,
        filePath: String? = null,
        pdfUrl: String? = null,
        bucket: String? = null
    ): String {
        // Find the first non-blank candidate
        val rawCandidate = listOfNotNull(
            storagePath?.takeIf { it.isNotBlank() },
            fileUrl?.takeIf { it.isNotBlank() },
            filePath?.takeIf { it.isNotBlank() },
            pdfUrl?.takeIf { it.isNotBlank() }
        ).firstOrNull()?.trim() ?: return ""

        // Check for accidental duplicate URL embedding (e.g. "https://...supabase.co/storage/v1/object/public/pdfs/https://...")
        if (rawCandidate.contains("http://") || rawCandidate.contains("https://")) {
            val lastHttpIdx = rawCandidate.lastIndexOf("https://").takeIf { it >= 0 }
                ?: rawCandidate.lastIndexOf("http://")
            val cleanUrl = if (lastHttpIdx > 0) {
                rawCandidate.substring(lastHttpIdx).trim()
            } else {
                rawCandidate
            }
            return normalizePdfUrl(cleanUrl)
        }

        // Local file URIs or paths
        if (rawCandidate.startsWith("file://") || rawCandidate.startsWith("/data/") || rawCandidate.startsWith("/storage/")) {
            return rawCandidate
        }

        // Relative path in Supabase Storage
        val cleanPath = rawCandidate.trimStart('/')
        
        // Determine target bucket
        val (targetBucket, finalPath) = when {
            cleanPath.startsWith("pdfs/") -> "pdfs" to cleanPath.removePrefix("pdfs/").trimStart('/')
            cleanPath.startsWith("study-materials/") -> "study-materials" to cleanPath.removePrefix("study-materials/").trimStart('/')
            cleanPath.startsWith("pdf-documents/") -> "pdf-documents" to cleanPath.removePrefix("pdf-documents/").trimStart('/')
            cleanPath.startsWith("book-covers/") -> "book-covers" to cleanPath.removePrefix("book-covers/").trimStart('/')
            !bucket.isNullOrBlank() -> bucket.trim() to cleanPath
            else -> "pdfs" to cleanPath
        }

        return SupabaseClient.getStoragePublicUrl(targetBucket, finalPath)
    }

    /**
     * Resolves PDF URL from a PdfDocument model.
     */
    fun getPdfUrl(pdf: PdfDocument): String {
        return getPdfUrl(
            fileUrl = pdf.fileUrl,
            storagePath = pdf.storagePath,
            filePath = pdf.filePath,
            pdfUrl = pdf.pdfUrl,
            bucket = pdf.bucket
        )
    }

    /**
     * Resolves PDF URL from a Book model.
     */
    fun getPdfUrl(book: Book): String {
        return getPdfUrl(
            fileUrl = book.fileUrl,
            storagePath = book.storagePath,
            filePath = book.filePath,
            pdfUrl = book.pdfUrl,
            bucket = book.bucket
        )
    }

    /**
     * Resolves PDF URL from a ModelPaper model.
     */
    fun getPdfUrl(mp: ModelPaper): String {
        return getPdfUrl(
            fileUrl = mp.fileUrl,
            storagePath = mp.storagePath,
            filePath = mp.filePath,
            pdfUrl = mp.pdfUrl,
            bucket = mp.bucket
        )
    }

    /**
     * Resolves PDF URL from a PreviousYearPaper model.
     */
    fun getPdfUrl(pyq: PreviousYearPaper): String {
        return getPdfUrl(
            fileUrl = pyq.fileUrl,
            storagePath = pyq.storagePath,
            filePath = pyq.filePath,
            pdfUrl = pyq.pdfUrl,
            bucket = pyq.bucket
        )
    }

    /**
     * Unified suspendable URL resolver that generates a signed URL if the file is private
     * or inaccessible via public endpoint.
     */
    suspend fun resolveFinalPdfUrl(
        fileUrl: String? = null,
        storagePath: String? = null,
        filePath: String? = null,
        pdfUrl: String? = null,
        bucket: String? = null
    ): String = withContext(Dispatchers.IO) {
        val baseCandidate = getPdfUrl(fileUrl, storagePath, filePath, pdfUrl, bucket)
        
        // If it's a local file or not Supabase, return directly
        if (!baseCandidate.contains("supabase.co")) return@withContext baseCandidate
        
        val targetBucket = bucket ?: extractBucketFromUrl(baseCandidate) ?: "pdfs"
        val targetPath = storagePath ?: filePath ?: extractPathFromUrl(baseCandidate) ?: return@withContext baseCandidate
        
        // Check if public URL is accessible
        val isPubliclyAccessible = SupabaseClient.checkStorageFileExists(targetBucket, targetPath)
        if (isPubliclyAccessible) {
            return@withContext SupabaseClient.getStoragePublicUrl(targetBucket, targetPath)
        }
        
        // Fallback to generating a signed URL if public access fails (assumes private bucket or RLS)
        val signedUrl = SupabaseClient.getStorageSignedUrl(targetBucket, targetPath)
        if (signedUrl != null) {
            return@withContext signedUrl
        }
        
        // Return base candidate as fallback if signing fails
        return@withContext baseCandidate
    }

    private fun extractBucketFromUrl(url: String): String? {
        val regex = "/object/(?:public|sign)/([^/]+)/".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun extractPathFromUrl(url: String): String? {
        val regex = "/object/(?:public|sign)/[^/]+/(.*)".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    /**
     * Normalizes and cleans various cloud storage URLs (Supabase Storage, Google Drive, Dropbox).
     */
    fun normalizePdfUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""

        // Handle Google Drive shared link conversion to direct download link
        if (trimmed.contains("drive.google.com") || trimmed.contains("docs.google.com")) {
            val fileId = extractGoogleDriveFileId(trimmed)
            if (fileId != null) {
                return "https://drive.usercontent.google.com/download?id=$fileId&export=download&authuser=0&confirm=t"
            }
        }

        // Handle Dropbox links
        if (trimmed.contains("dropbox.com")) {
            return when {
                trimmed.contains("dl=0") -> trimmed.replace("dl=0", "dl=1")
                !trimmed.contains("dl=1") && !trimmed.contains("raw=1") -> {
                    if (trimmed.contains("?")) "$trimmed&dl=1" else "$trimmed?dl=1"
                }
                else -> trimmed
            }
        }

        return trimmed
    }

    /**
     * Verifies if a local file is a genuine, non-corrupted PDF by checking the %PDF- header magic bytes.
     */
    fun isValidPdfFile(file: File?): Boolean {
        if (file == null || !file.exists() || file.length() < 100) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(5)
                val read = input.read(header)
                if (read >= 5) {
                    val headerStr = String(header, Charsets.US_ASCII)
                    headerStr.startsWith("%PDF-")
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking PDF magic bytes: ${e.message}")
            false
        }
    }

    /**
     * Permanent offline storage directory in internal files (persists permanently for lifetime access).
     */
    fun getPermanentPdfDir(context: Context): File {
        val dir = File(context.filesDir, "downloaded_pdfs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Gets the permanent local file destination for a given PDF URL or document ID.
     */
    fun getLocalPdfFile(context: Context, urlOrId: String): File {
        val hash = hashUrl(urlOrId)
        val permDir = getPermanentPdfDir(context)
        val permFile = File(permDir, "$hash.pdf")

        // Also check if previously cached in cacheDir for backward compatibility
        if (!permFile.exists()) {
            val oldCacheFile = File(File(context.cacheDir, "downloaded_pdfs"), "$hash.pdf")
            if (isValidPdfFile(oldCacheFile)) {
                try {
                    oldCacheFile.copyTo(permFile, overwrite = true)
                    oldCacheFile.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed migrating cached file to permanent storage: ${e.message}")
                }
            }
        }

        return permFile
    }

    /**
     * Checks whether the PDF is already downloaded and saved locally for lifetime offline access.
     */
    suspend fun isPdfDownloaded(context: Context, urlOrId: String): Boolean = withContext(Dispatchers.IO) {
        val file = getLocalPdfFile(context, urlOrId)
        if (!isValidPdfFile(file)) return@withContext false
        
        // Deep verification: check if PdfRenderer can actually open it
        // This catches partially downloaded files that start with %PDF- but are incomplete
        try {
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
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Corrupted PDF detected (PdfRenderer failed). Deleting: ${file.name}", e)
            file.delete()
        }
        return@withContext false
    }

    /**
     * Deletes a downloaded PDF from permanent storage if requested.
     */
    fun deleteDownloadedPdf(context: Context, urlOrId: String): Boolean {
        val file = getLocalPdfFile(context, urlOrId)
        val hash = hashUrl(urlOrId)
        // Clear bitmap cache for this file
        pageBitmapCache.snapshot().keys.forEach { key ->
            if (key.contains(hash) || key.contains(file.name)) {
                pageBitmapCache.remove(key)
            }
        }
        return if (file.exists()) file.delete() else false
    }

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
                viewModelScope.async {
                    val result = performActualDownload(context, rawNormalized, targetFile, onProgress)
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

    private suspend fun performActualDownload(context: Context, rawNormalized: String, targetFile: File, onProgress: (Int) -> Unit): Result<File> {
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
                                    val savedFile = saveResponseBodyToFile(retryResp.body!!, targetFile, onProgress)
                                    if (savedFile != null && isValidPdfFile(savedFile)) {
                                        return@withContext Result.success(savedFile)
                                    }
                                }
                            }
                        } else {
                            val savedFile = saveResponseBodyToFile(body, targetFile, onProgress)
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



    private suspend fun saveResponseBodyToFile(
        body: okhttp3.ResponseBody,
        targetFile: File,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val contentLength = body.contentLength()
        val inputStream = body.byteStream()
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        val outputStream = FileOutputStream(tempFile)

        try {
            val buffer = ByteArray(16384)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 99)
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (tempFile.exists() && tempFile.length() > 100 && isValidPdfFile(tempFile)) {
                if (targetFile.exists()) targetFile.delete()
                if (tempFile.renameTo(targetFile)) {
                    return@withContext targetFile
                }
            }
            tempFile.delete()
            null
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            Log.e(TAG, "Error saving response stream: ${e.message}")
            null
        }
    }

    /**
     * Renders a specific page of a local PDF file into a sharp, high-resolution Android Bitmap.
     */
    suspend fun renderPdfPage(
        file: File,
        pageIndex: Int,
        targetWidth: Int = 1400
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val cacheKey = "${file.absolutePath}_p${pageIndex}_w$targetWidth"
        val cached = pageBitmapCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext Result.success(cached)
        }

        pdfRenderMutex.withLock {
            // Check again in case it was rendered while waiting for lock
            val cachedAgain = pageBitmapCache.get(cacheKey)
            if (cachedAgain != null && !cachedAgain.isRecycled) {
                return@withContext Result.success(cachedAgain)
            }

            try {
                if (!isValidPdfFile(file)) {
                    return@withContext Result.failure(IllegalArgumentException("PDF file does not exist or is invalid"))
                }

                val path = file.absolutePath
                var rendererPair = openRenderers[path]
                if (rendererPair == null) {
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val newRenderer = PdfRenderer(fd)
                    rendererPair = Pair(fd, newRenderer)
                    openRenderers[path] = rendererPair
                }
                val renderer = rendererPair.second
                
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Page index $pageIndex out of range (Total: ${renderer.pageCount})")
                    )
                }

                renderer.openPage(pageIndex).use { page ->
                            val aspectRatio = page.height.toFloat() / page.width.toFloat()
                            // Ensure high clarity for reading Hindi & small math formulas
                            val safeWidth = targetWidth.coerceIn(720, 1600) // Lowered max width to prevent OOM
                            val targetHeight = (safeWidth * aspectRatio).toInt().coerceIn(1, 3000)

                            val bitmap = Bitmap.createBitmap(safeWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            pageBitmapCache.put(cacheKey, bitmap)
                            Result.success(bitmap)
                        }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) { // Catch Throwable to trap OutOfMemoryError
                Log.e(TAG, "Error rendering PDF page $pageIndex: ${e.message}", e)
                Result.failure(if (e is Exception) e else Exception(e))
            }
        }
    }

    /**
     * Preloads adjacent pages for instant flipping without loading screens.
     */
    suspend fun preloadAdjacentPages(file: File, currentPage: Int, totalPages: Int, targetWidth: Int = 1400) = withContext(Dispatchers.IO) {
        if (!isValidPdfFile(file)) return@withContext
        val pagesToPreload = listOf(currentPage - 2, currentPage).filter { it in 0 until totalPages }
        for (page in pagesToPreload) {
            val cacheKey = "${file.absolutePath}_p${page}_w$targetWidth"
            if (pageBitmapCache.get(cacheKey) == null) {
                renderPdfPage(file, page, targetWidth)
            }
        }
    }

    /**
     * Gets the total page count of a local PDF file using native PdfRenderer.
     */
    suspend fun getPdfPageCount(file: File): Int = withContext(Dispatchers.IO) {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page count: ${e.message}")
            1
        }
    }

    /**
     * Generates a high-quality educational PDF document with cover page, notes, formulas,
     * and practice questions if remote PDF is offline or unavailable.
     */
    suspend fun generateFallbackPdf(
        context: Context,
        docId: String,
        title: String,
        category: String,
        subjectName: String,
        classId: String,
        description: String = ""
    ): File = withContext(Dispatchers.IO) {
        val targetFile = getLocalPdfFile(context, "generated_$docId")
        if (isValidPdfFile(targetFile)) {
            return@withContext targetFile
        }

        val document = AndroidPdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = android.graphics.Color.rgb(79, 70, 229)
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.rgb(51, 65, 85)
            textSize = 12f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = android.graphics.Color.rgb(30, 41, 59)
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bgPaint = Paint().apply {
            color = android.graphics.Color.rgb(248, 250, 252)
        }

        val accentCardPaint = Paint().apply {
            color = android.graphics.Color.rgb(238, 242, 255)
        }

        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val primaryBarPaint = Paint().apply {
            color = android.graphics.Color.rgb(79, 70, 229)
        }

        val watermarkPaint = Paint().apply {
            color = android.graphics.Color.argb(20, 79, 70, 229)
            textSize = 36f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // --- PAGE 1: Cover & Main Summary ---
        val pageInfo1 = AndroidPdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1: Canvas = page1.canvas

        // Header Top Bar
        canvas1.drawRect(0f, 0f, pageWidth.toFloat(), 55f, primaryBarPaint)
        val headerTextPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas1.drawText("SUPER STUDY 2026 • OFFICIAL STUDY PORTAL", 30f, 34f, headerTextPaint)

        // Watermark
        canvas1.drawText("SUPER STUDY APP", pageWidth / 2f, pageHeight / 2f, watermarkPaint)

        // Title Box
        canvas1.drawRoundRect(30f, 75f, (pageWidth - 30).toFloat(), 200f, 14f, 14f, accentCardPaint)
        canvas1.drawRoundRect(30f, 75f, (pageWidth - 30).toFloat(), 200f, 14f, 14f, borderPaint)
        
        canvas1.drawText("$subjectName • $classId • $category", 50f, 108f, subPaint)
        
        val displayTitle = if (title.length > 42) title.take(40) + "..." else title
        canvas1.drawText(displayTitle, 50f, 138f, titlePaint)
        
        val descText = if (description.isNotBlank()) description else "एनसीईआरटी व जेएसी बोर्ड 2026 परीक्षा के नवीनतम पाठ्यक्रम पर आधारित संपूर्ण अध्ययन सामग्री।"
        val shortDesc = if (descText.length > 70) descText.take(68) + "..." else descText
        canvas1.drawText(shortDesc, 50f, 172f, textPaint)

        // Section 1: Overview
        canvas1.drawText("1. मुख्य बिंदु एवं संक्षिप्त सारांश (Chapter Summary)", 30f, 235f, headerPaint)
        var y1 = 265f
        val points = listOf(
            "• बोर्ड परीक्षा 2026 के लिए सबसे महत्वपूर्ण एवं बार-बार पूछे जाने वाले सिद्धांत।",
            "• सभी सूत्रों (Formulas), परिभाषाओं और रेखाचित्रों (Diagrams) का चरणबद्ध समावेश।",
            "• त्वरित दोहराव (Quick Revision) के लिए विशेष रूप से तैयार किया गया हस्तलिखित प्रारूप।",
            "• वस्तुनिष्ठ (Objective MCQ) और विषयपरक (Subjective) दोनों प्रश्नों का संतुलन।",
            "• 100% परीक्षा-उपयोगी और सुपर स्टडी ऐप द्वारा सत्यापित अध्ययन सामग्री।"
        )
        for (pt in points) {
            canvas1.drawText(pt, 40f, y1, textPaint)
            y1 += 26f
        }

        // Section 2: Key Concepts Card
        canvas1.drawRoundRect(30f, 420f, (pageWidth - 30).toFloat(), 760f, 12f, 12f, bgPaint)
        canvas1.drawRoundRect(30f, 420f, (pageWidth - 30).toFloat(), 760f, 12f, 12f, borderPaint)

        canvas1.drawText("2. महत्वपूर्ण सूत्र एवं अवधारणाएं (Key Concepts & Formulas)", 50f, 455f, headerPaint)
        
        val formulas = listOf(
            "1. कूलॉम का नियम (Coulomb's Law): F = (1 / 4πε₀) · (|q₁ · q₂| / r²)",
            "2. विद्युत क्षेत्र की तीव्रता (Electric Field Intensity): E = F / q₀ = (1 / 4πε₀) · (q / r²)",
            "3. गाउस का नियम (Gauss's Theorem): Φ = ∮ E · dA = q_enclosed / ε₀",
            "4. विद्युत द्विध्रुव आघूर्ण (Dipole Moment): p = q · (2a), दिशा: -q से +q की ओर",
            "5. द्विध्रुव के कारण अक्षीय स्थिति में क्षेत्र: E_axial = (1 / 4πε₀) · (2p / r³)",
            "6. द्विध्रुव के कारण निरक्षीय स्थिति में क्षेत्र: E_equatorial = (1 / 4πε₀) · (p / r³)",
            "7. समविभव पृष्ठ (Equipotential Surface): ऐसा पृष्ठ जिसके प्रत्येक बिंदु पर विभव समान होता है।"
        )
        var formulaY = 495f
        for (f in formulas) {
            canvas1.drawText(f, 50f, formulaY, textPaint)
            formulaY += 32f
        }

        // Footer
        canvas1.drawText("Super Study App • झारखंड बोर्ड & CBSE • Page 1 / 2", 30f, pageHeight - 20f, textPaint)
        document.finishPage(page1)

        // --- PAGE 2: Practice Questions & Solutions ---
        val pageInfo2 = AndroidPdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = document.startPage(pageInfo2)
        val canvas2: Canvas = page2.canvas

        // Header
        canvas2.drawRect(0f, 0f, pageWidth.toFloat(), 45f, primaryBarPaint)
        canvas2.drawText("SUPER STUDY 2026 • अभ्यास प्रश्न एवं मॉडल उत्तर (Model Practice Set)", 30f, 28f, headerTextPaint)
        canvas2.drawText("SUPER STUDY APP", pageWidth / 2f, pageHeight / 2f, watermarkPaint)

        // Question 1
        var y2 = 75f
        canvas2.drawText("प्रश्न 1. गाउस के प्रमेय को लिखिए तथा इसे सिद्ध कीजिए। (5 अंक)", 30f, y2, headerPaint)
        y2 += 24f
        val q1Ans = listOf(
            "उत्तर: किसी बंद पृष्ठ से गुजरने वाला कुल विद्युत फ्लक्स उस पृष्ठ के भीतर उपस्थित",
            "कुल आवेश का 1/ε₀ गुना होता है। अर्थात्  Φ = ∮ E · dA = q / ε₀",
            "उपपत्ति: बिंदु आवेश q से r दूरी पर स्थित गोलीय गॉसीय पृष्ठ पर E = (1/4πε₀)(q/r²)",
            "कुल फ्लक्स Φ = E · ∮ dA = (q/4πε₀r²) · (4πr²) = q/ε₀  (इति सिद्धम्)"
        )
        for (ans in q1Ans) {
            canvas2.drawText(ans, 45f, y2, textPaint)
            y2 += 22f
        }

        // Question 2
        y2 += 15f
        canvas2.drawText("प्रश्न 2. विद्युत फ्लक्स का मात्रक और विमीय सूत्र लिखिए। (2 अंक)", 30f, y2, headerPaint)
        y2 += 24f
        canvas2.drawText("उत्तर: SI मात्रक: N·m²/C (या Volt·meter)। विमीय सूत्र: [M L³ T⁻³ A⁻¹]", 45f, y2, textPaint)

        // Question 3
        y2 += 35f
        canvas2.drawText("प्रश्न 3. दो बिंदु आवेश 2μC और 3μC एक दूसरे से 30 cm की दूरी पर स्थित हैं। बल ज्ञात करें। (3 अंक)", 30f, y2, headerPaint)
        y2 += 24f
        val q3Ans = listOf(
            "हल: q₁ = 2 × 10⁻⁶ C, q₂ = 3 × 10⁻⁶ C, r = 0.3 m",
            "F = (9 × 10⁹) × (2 × 10⁻⁶ × 3 × 10⁻⁶) / (0.3)²",
            "F = (54 × 10⁻³) / 0.09 = 0.6 Newton (प्रतिकर्षण बल)"
        )
        for (ans in q3Ans) {
            canvas2.drawText(ans, 45f, y2, textPaint)
            y2 += 22f
        }

        // Study tips box
        y2 += 30f
        canvas2.drawRoundRect(30f, y2, (pageWidth - 30).toFloat(), y2 + 130f, 10f, 10f, accentCardPaint)
        canvas2.drawRoundRect(30f, y2, (pageWidth - 30).toFloat(), y2 + 130f, 10f, 10f, borderPaint)
        canvas2.drawText("★ परीक्षा सफलता टिप्स (Board Exam Strategy):", 45f, y2 + 30f, headerPaint)
        canvas2.drawText("1. सभी सूत्रों को अलग कॉपी में लिखें और प्रतिदिन 15 मिनट अभ्यास करें।", 45f, y2 + 58f, textPaint)
        canvas2.drawText("2. पिछले 5 वर्षों के प्रश्न पत्रों (PYQ) और मॉडल सेट को समय सीमा में हल करें।", 45f, y2 + 82f, textPaint)
        canvas2.drawText("3. सुपर स्टडी ऐप पर दैनिक ऑनलाइन टेस्ट देकर अपनी रैंक और सटीकता जांचें।", 45f, y2 + 106f, textPaint)

        // Footer
        canvas2.drawText("Super Study App • झारखंड बोर्ड & CBSE • Page 2 / 2", 30f, pageHeight - 20f, textPaint)
        document.finishPage(page2)

        // Save generated PDF to permanent file
        val outputStream = FileOutputStream(targetFile)
        document.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        document.close()

        targetFile
    }

    private fun hashUrl(url: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(url.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            url.hashCode().toString()
        }
    }
}

